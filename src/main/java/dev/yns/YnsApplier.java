package dev.yns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Baut das Pack im Hintergrund und laedt dann die Texturen im Spiel neu. */
public final class YnsApplier {
    private YnsApplier() {}

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "YNS-Apply");
        t.setDaemon(true);
        return t;
    });

    public static void apply(YnsStore store, Consumer<String> status) {
        final var selected = new java.util.LinkedHashMap<>(store.selected);
        final String base = store.cloudBaseUrl;
        POOL.execute(() -> {
            try {
                status.accept("Baue Texturen ...");
                int n = YnsBuilder.build(YnsStore.PACK_DIR, selected, YnsStore.STYLES_DIR, YnsStore.CACHE_DIR, base, status);
                status.accept("Fertig - " + n + " Dateien aktiv. Lade Texturen neu ...");
                MinecraftClientAccess.execute(YnsApplier::reload);
            } catch (Exception e) {
                status.accept("Fehler: " + e.getMessage());
            }
        });
    }

    private static void reload() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        List<String> before = new ArrayList<>(m.getEnabledIds());
        List<String> after = new ArrayList<>(before);
        after.remove(YnsStore.PACK_ID);
        after.add(YnsStore.PACK_ID);          // ganz oben = hoechste Prioritaet
        m.setEnabledProfiles(after);
        if (!after.equals(before)) {
            mc.options.refreshResourcePacks(m); // speichert + laedt neu
        } else {
            mc.reloadResources();
        }
    }
}
