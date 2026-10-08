package dev.yns;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Baut aus der Auswahl (Kategorie -> Style) ein einziges Resource-Pack als Ordner. */
final class YnsBuilder {
    private YnsBuilder() {}

    static final String MCMETA =
            "{\"pack\":{\"description\":\"YNS - deine Auswahl\",\"pack_format\":75,\"min_format\":65,\"max_format\":9999}}";

    static int build(Path outDir, Map<String, String> selected, Path stylesDir, Path cacheDir,
                     String baseUrl, Consumer<String> status) throws IOException {
        deleteTree(outDir);
        Files.createDirectories(outDir);
        Set<String> written = new HashSet<>();
        int files = 0;

        for (Category c : Category.ALL) {
            String key = selected.getOrDefault(c.id(), "");
            if (key == null || key.isBlank()) continue;
            if (key.startsWith("own:")) {
                int own = files;
                for (Map.Entry<String, Path> e : OwnTextures.scan(YnsStore.TEXTURES_DIR).entrySet()) {
                    String norm = e.getKey();
                    if (!c.matches(norm) || !written.add(norm)) continue;
                    Path target = outDir.resolve(norm);
                    Files.createDirectories(target.getParent());
                    Files.copy(e.getValue(), target, StandardCopyOption.REPLACE_EXISTING);
                    files++;
                }
                if (files == own) status.accept(c.name() + ": keine eigenen Texturen gefunden");
                continue;
            }
            Path src = resolve(key, stylesDir, cacheDir, baseUrl, status);
            if (src == null) { status.accept(c.name() + ": Style nicht gefunden"); continue; }

            int before = files;
            try (PackSource ps = PackSource.of(src)) {
                for (String name : ps.entries()) {
                    String norm = Category.normalize(name);
                    if (!c.matches(norm) || !written.add(norm)) continue;
                    Path target = outDir.resolve(norm);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = ps.open(name)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    files++;
                }
            }
            if (files == before) status.accept(c.name() + ": keine passenden Dateien");
        }
        Files.writeString(outDir.resolve("pack.mcmeta"), MCMETA);
        return files;
    }

    private static Path resolve(String key, Path stylesDir, Path cacheDir, String baseUrl,
                                Consumer<String> status) throws IOException {
        if (key.startsWith("local:")) {
            String name = key.substring(6);
            Path p = stylesDir.resolve(name).normalize();
            if (!p.getParent().equals(stylesDir.normalize())) return null; // nur direkt im Ordner
            return Files.exists(p) ? p : null;
        }
        if (key.startsWith("cloud:")) {
            String id = key.substring(6);
            if (YnsLibrary.cloud.isEmpty()) YnsLibrary.loadCloud(baseUrl);
            for (YnsCloud.Style s : YnsLibrary.cloud) {
                if (s.id().equals(id)) {
                    status.accept("Lade " + s.name() + " ...");
                    return YnsCloud.download(baseUrl, s, cacheDir);
                }
            }
        }
        return null;
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
