package dev.yns;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Optional: laedt index.json und Packs aus einem eigenen Cloud-Repository (Standard: aus). */
final class YnsCloud {
    private YnsCloud() {}

    record Style(String id, String category, String name, String sha256, String by) {}

    static String base(String url) {
        if (url == null) return "";
        url = url.trim();
        return url.isEmpty() || url.endsWith("/") ? url : url + "/";
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "YNS-Fabric-Mod");
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode() + " bei " + url);
        return c;
    }

    static List<Style> fetchIndex(String baseUrl) throws IOException {
        String b = base(baseUrl);
        if (b.isEmpty()) return List.of();
        String text;
        try (InputStream in = open(b + "index.json").getInputStream()) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return parseIndex(text);
    }

    static List<Style> parseIndex(String text) {
        List<Style> out = new ArrayList<>();
        JsonArray arr = JsonParser.parseString(text).getAsJsonObject().getAsJsonArray("styles");
        if (arr == null) return out;
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            String id = str(o, "id");
            String cat = str(o, "category");
            if (id.isEmpty() || cat.isEmpty() || !id.matches("[A-Za-z0-9_\\-]+")) continue; // kein Pfad-Trick
            out.add(new Style(id, cat, str(o, "name").isEmpty() ? id : str(o, "name"), str(o, "sha256"), str(o, "by")));
        }
        return out;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    /** Laedt das Pack beim ersten Mal herunter (prueft SHA-256) und gibt den Cache-Pfad zurueck. */
    static Path download(String baseUrl, Style s, Path cacheDir) throws IOException {
        Files.createDirectories(cacheDir);
        Path target = cacheDir.resolve(s.id() + ".zip");
        if (Files.isRegularFile(target) && (s.sha256().isEmpty() || sha256(target).equalsIgnoreCase(s.sha256()))) return target;

        Path tmp = cacheDir.resolve(s.id() + ".zip.part");
        try (InputStream in = open(base(baseUrl) + "packs/" + s.id() + ".zip").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!s.sha256().isEmpty() && !sha256(tmp).equalsIgnoreCase(s.sha256())) {
            Files.deleteIfExists(tmp);
            throw new IOException("Pruefsumme stimmt nicht: " + s.id());
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    static String sha256(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte x : md.digest()) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
