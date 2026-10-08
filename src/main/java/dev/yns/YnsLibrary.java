package dev.yns;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** Merkt sich, welche Styles es gibt: lokal (Packs + Einzeltexturen) und optional Cloud. */
final class YnsLibrary {
    private YnsLibrary() {}

    /** Kategorie-ID -> Dateinamen der lokalen Packs, die dafuer Texturen enthalten. */
    static volatile Map<String, List<String>> local = Map.of();
    /** Kategorien, fuer die lose Einzeltexturen in yns/textures/ liegen. */
    static volatile Set<String> ownCats = Set.of();
    static volatile List<YnsCloud.Style> cloud = List.of();
    static volatile String cloudError = "";

    static void scanLocal(Path stylesDir) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Category c : Category.ALL) map.put(c.id(), new ArrayList<>());
        try {
            Files.createDirectories(stylesDir);
            List<Path> items;
            try (Stream<Path> s = Files.list(stylesDir)) {
                items = s.filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".zip"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                        .toList();
            }
            for (Path p : items) {
                try (PackSource src = PackSource.of(p)) {
                    Set<String> found = new HashSet<>();
                    for (String e : src.entries()) {
                        String n = Category.normalize(e);
                        for (Category c : Category.ALL) if (!found.contains(c.id()) && c.matches(n)) found.add(c.id());
                    }
                    for (String id : found) map.get(id).add(p.getFileName().toString());
                } catch (IOException | RuntimeException ignored) {
                    // kaputtes Pack ueberspringen
                }
            }
        } catch (IOException ignored) {}
        local = map;

        Set<String> own = new HashSet<>();
        for (String virt : OwnTextures.scan(YnsStore.TEXTURES_DIR).keySet()) {
            for (Category c : Category.ALL) if (c.matches(virt)) own.add(c.id());
        }
        ownCats = own;
    }

    static void loadCloud(String baseUrl) {
        try {
            cloud = YnsCloud.fetchIndex(baseUrl);
            cloudError = YnsCloud.base(baseUrl).isEmpty() ? "Keine Cloud-URL eingestellt" : "";
        } catch (Exception e) {
            cloudError = "Cloud nicht erreichbar: " + e.getMessage();
        }
    }
}
