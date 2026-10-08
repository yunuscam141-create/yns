package dev.yns;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Liest ein Resource Pack - egal ob .zip oder entpackter Ordner (auch mit Unterordner im Zip). */
abstract class PackSource implements Closeable {
    private String prefix = "";
    private List<String> cache;

    static PackSource of(Path p) throws IOException {
        PackSource s = Files.isDirectory(p) ? new DirSource(p) : new ZipSrc(p);
        try {
            s.detectPrefix();
        } catch (IOException | RuntimeException e) {
            s.close();
            throw e;
        }
        return s;
    }

    abstract List<String> rawEntries() throws IOException;

    abstract InputStream rawOpen(String name) throws IOException;

    private List<String> raw() throws IOException {
        if (cache == null) cache = rawEntries();
        return cache;
    }

    private void detectPrefix() throws IOException {
        String best = null;
        for (String e : raw()) {
            if (e.equals("pack.mcmeta")) { prefix = ""; return; }
            if (e.endsWith("/pack.mcmeta") && (best == null || e.length() < best.length())) best = e;
        }
        if (best != null) prefix = best.substring(0, best.length() - "pack.mcmeta".length());
    }

    /** Dateinamen relativ zur Pack-Wurzel (mit '/'). */
    List<String> entries() throws IOException {
        List<String> out = new ArrayList<>();
        for (String e : raw()) if (e.startsWith(prefix)) out.add(e.substring(prefix.length()));
        return out;
    }

    InputStream open(String name) throws IOException {
        return rawOpen(prefix + name);
    }

    private static final class ZipSrc extends PackSource {
        private final ZipFile zf;

        ZipSrc(Path p) throws IOException { zf = new ZipFile(p.toFile()); }

        @Override List<String> rawEntries() {
            List<String> l = new ArrayList<>();
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory()) l.add(e.getName().replace('\\', '/'));
            }
            return l;
        }

        @Override InputStream rawOpen(String name) throws IOException {
            ZipEntry e = zf.getEntry(name);
            if (e == null) throw new IOException("Fehlt im Zip: " + name);
            return zf.getInputStream(e);
        }

        @Override public void close() throws IOException { zf.close(); }
    }

    private static final class DirSource extends PackSource {
        private final Path root;

        DirSource(Path root) { this.root = root; }

        @Override List<String> rawEntries() throws IOException {
            try (Stream<Path> s = Files.walk(root)) {
                return s.filter(Files::isRegularFile)
                        .map(p -> root.relativize(p).toString().replace('\\', '/'))
                        .toList();
            }
        }

        @Override InputStream rawOpen(String name) throws IOException {
            return Files.newInputStream(root.resolve(name));
        }

        @Override public void close() {}
    }
}
