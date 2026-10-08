package dev.yns;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Einzelne Texturen (z. B. totem_of_undying.png), die der Spieler lose in .minecraft/yns/textures/ legt.
 * Es wird nur der Dateiname gebraucht - YNS findet selbst heraus, wohin die Textur gehoert.
 * Wer will, kann auch die volle Struktur "assets/minecraft/textures/..." verwenden.
 */
final class OwnTextures {
    private OwnTextures() {}

    /** Ordner, in denen eine lose Datei liegen koennte (in dieser Reihenfolge ausprobiert). */
    private static final String[] DIRS = {
            "item", "block", "entity/end_crystal", "gui/sprites/hud",
            "entity/shield", "entity/equipment/humanoid", "entity/equipment/humanoid_leggings", "models/armor"
    };

    /** Virtueller Pack-Pfad (assets/minecraft/...) -> Datei auf der Festplatte. */
    static Map<String, Path> scan(Path dir) {
        Map<String, Path> out = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                String rel = dir.relativize(p).toString().replace('\\', '/');
                String lower = rel.toLowerCase(Locale.ROOT);
                if (!lower.endsWith(".png") && !lower.endsWith(".png.mcmeta")) continue;
                int a = lower.indexOf("assets/minecraft/");
                if (a >= 0) {
                    out.put(Category.normalize(lower.substring(a)), p);
                    continue;
                }
                String name = lower.substring(lower.lastIndexOf('/') + 1);
                for (String d : DIRS) {
                    String virt = Category.normalize("assets/minecraft/textures/" + d + "/" + name);
                    for (Category c : Category.ALL) {
                        if (c.matches(virt)) { out.put(virt, p); break; }
                    }
                }
            }
        } catch (IOException ignored) {}
        return out;
    }
}
