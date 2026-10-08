package dev.yns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Einstellungen + alle Ordner. Alles liegt in .minecraft/yns/ */
public final class YnsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Optionale eigene Cloud (Repo-Root mit index.json). Standardmaessig aus - nichts wird aus dem Internet geladen. */
    public static final String DEFAULT_CLOUD = "";

    public static final Path ROOT = FabricLoader.getInstance().getGameDir().resolve("yns");
    public static final Path STYLES_DIR = ROOT.resolve("styles");   // <- "Ordner oeffnen" zeigt hierher
    public static final Path TEXTURES_DIR = ROOT.resolve("textures"); // <- lose Einzeltexturen
    public static final Path CACHE_DIR = ROOT.resolve("cache");
    public static final Path PACK_DIR = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("YNS-Active");
    public static final String PACK_ID = "file/YNS-Active";
    private final Path file = ROOT.resolve("config.json");

    /** Kategorie-ID -> "local:datei.zip" oder "cloud:style_id" (leer = Vanilla). */
    public final Map<String, String> selected = new LinkedHashMap<>();
    public String cloudBaseUrl = DEFAULT_CLOUD;

    public void load() {
        try {
            Files.createDirectories(STYLES_DIR);
            Files.createDirectories(TEXTURES_DIR);
            if (Files.exists(file)) {
                Data d = GSON.fromJson(Files.readString(file), Data.class);
                if (d != null) {
                    if (d.selected != null) selected.putAll(d.selected);
                    if (d.cloudBaseUrl != null && !d.cloudBaseUrl.toLowerCase().contains("bauloo")) cloudBaseUrl = d.cloudBaseUrl;
                }
            } else {
                save();
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        try {
            Files.createDirectories(ROOT);
            Data d = new Data();
            d.selected = selected;
            d.cloudBaseUrl = cloudBaseUrl;
            Files.writeString(file, GSON.toJson(d));
        } catch (IOException ignored) {}
    }

    /** Oeffnet den Styles-Ordner im Datei-Explorer des Betriebssystems. */
    public static void openStylesFolder() { openFolder(STYLES_DIR); }

    /** Oeffnet den Ordner fuer einzelne Texturen. */
    public static void openTexturesFolder() { openFolder(TEXTURES_DIR); }

    private static void openFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            String os = System.getProperty("os.name", "").toLowerCase();
            String path = dir.toAbsolutePath().toString();
            String[] cmd = os.contains("win") ? new String[]{"explorer", path}
                    : os.contains("mac") ? new String[]{"open", path}
                    : new String[]{"xdg-open", path};
            new ProcessBuilder(cmd).start();
        } catch (Exception ignored) {}
    }

    public static final class Data {
        Map<String, String> selected;
        String cloudBaseUrl;
    }
}
