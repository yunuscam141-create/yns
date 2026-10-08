package dev.yns;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class YnsScreen extends Screen {
    private static final int PER_PAGE = 8;
    private static String currentCat = Category.ALL.get(0).id();
    private static int page = 0;

    private final Screen parent;
    private final YnsStore store;
    // eigene Namen, damit Screen.width/height (Fenstergroesse) nicht ueberdeckt werden
    private int winX, winY, winW, winH;
    private volatile String status = "";

    private record Entry(String key, String label) {}

    public YnsScreen(Screen parent, YnsStore store) {
        super(Text.literal("YNS Styles"));
        this.parent = parent;
        this.store = store;
        refreshLibrary();
    }

    private void refreshLibrary() {
        status = "Suche Styles ...";
        Thread t = new Thread(() -> {
            YnsLibrary.scanLocal(YnsStore.STYLES_DIR);
            YnsLibrary.loadCloud(store.cloudBaseUrl);
            status = YnsLibrary.cloudError;
            MinecraftClientAccess.execute(() -> {
                if (MinecraftClient.getInstance().currentScreen == this) clearAndInit();
            });
        }, "YNS-Scan");
        t.setDaemon(true);
        t.start();
    }

    private List<Entry> entries() {
        List<Entry> l = new ArrayList<>();
        l.add(new Entry("", "Standard (Vanilla)"));
        if (YnsLibrary.ownCats.contains(currentCat)) l.add(new Entry("own:", "[Eigene Texturen]"));
        for (String f : YnsLibrary.local.getOrDefault(currentCat, List.of())) {
            l.add(new Entry("local:" + f, "[Pack] " + cut(f.replaceAll("(?i)\\.zip$", ""), 38)));
        }
        for (YnsCloud.Style s : YnsLibrary.cloud) {
            if (!s.category().equals(currentCat)) continue;
            String by = s.by().isEmpty() ? "" : " (" + s.by() + ")";
            l.add(new Entry("cloud:" + s.id(), "[Cloud] " + cut(s.name() + by, 38)));
        }
        return l;
    }

    private static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    protected void init() {
        winW = Math.min(860, this.width - 20);
        winH = Math.min(470, this.height - 20);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        // --- links: Kategorien (2 Spalten) ---
        int gx = winX + 20, gy = winY + 68, cw = 138, ch = 20, step = 23;
        for (int i = 0; i < Category.ALL.size(); i++) {
            Category c = Category.ALL.get(i);
            boolean active = c.id().equals(currentCat);
            boolean has = !store.selected.getOrDefault(c.id(), "").isBlank();
            String label = (active ? "> " : "") + c.name() + (has ? " *" : "");
            int x = gx + (i % 2) * (cw + 4);
            int y = gy + (i / 2) * step;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                currentCat = c.id();
                page = 0;
                clearAndInit();
            }).dimensions(x, y, cw, ch).build());
        }

        // --- rechts: Styles der gewaehlten Kategorie ---
        int rx = winX + 20 + 2 * (cw + 4) + 24;
        int rw = winX + winW - 20 - rx;
        List<Entry> list = entries();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        String activeKey = store.selected.getOrDefault(currentCat, "");
        int ry = winY + 90;
        for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
            Entry e = list.get(i);
            boolean on = e.key().equals(activeKey);
            addDrawableChild(ButtonWidget.builder(Text.literal((on ? "> " : "") + e.label()), b -> choose(e.key()))
                    .dimensions(rx, ry, rw, 20).build());
            ry += 23;
        }
        int py = winY + 90 + PER_PAGE * 23 + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); })
                .dimensions(rx, py, 30, 20).build()).active = page > 0;
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); })
                .dimensions(rx + 34, py, 30, 20).build()).active = page < pages - 1;

        // --- unten: Ordner oeffnen usw. ---
        int by = winY + winH - 34;
        addDrawableChild(ButtonWidget.builder(Text.literal("Pack-Ordner"), b -> YnsStore.openStylesFolder())
                .dimensions(winX + 20, by, 110, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Textur-Ordner"), b -> YnsStore.openTexturesFolder())
                .dimensions(winX + 134, by, 120, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Aktualisieren"), b -> refreshLibrary())
                .dimensions(winX + 258, by, 110, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurücksetzen"), b -> resetAll())
                .dimensions(winX + 372, by, 110, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Schließen"), b -> close())
                .dimensions(winX + winW - 130, by, 110, 24).build());
    }

    private void choose(String key) {
        if (key.isEmpty()) store.selected.remove(currentCat);
        else store.selected.put(currentCat, key);
        store.save();
        YnsApplier.apply(store, s -> status = s);
        clearAndInit();
    }

    private void resetAll() {
        store.selected.clear();
        store.save();
        YnsApplier.apply(store, s -> status = s);
        clearAndInit();
    }

    private void clearAndInit() {
        clearChildren();
        init();
    }

    @Override
    public void close() {
        store.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        // Fenster: dunkles Nachtblau mit violetter Kopfleiste und cyanfarbener Linie
        ctx.fill(winX, winY, winX + winW, winY + winH, 0xF0100E1C);
        ctx.fill(winX, winY, winX + winW, winY + 50, 0xFF2A1B5E);
        ctx.fill(winX, winY + 50, winX + winW, winY + 52, 0xFF22D3EE);
        ctx.fill(winX, winY, winX + 4, winY + winH, 0xFF8B5CF6);

        ctx.drawTextWithShadow(textRenderer, "YNS Styles", winX + 22, winY + 12, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Custom Textures. Your Style.", winX + 22, winY + 28, 0xFFB9A7F5);

        int active = store.selected.size();
        String badge = active + " aktiv";
        ctx.drawTextWithShadow(textRenderer, badge, winX + winW - 20 - textRenderer.getWidth(badge), winY + 20, 0xFF22D3EE);

        int rx = winX + 20 + 2 * (138 + 4) + 24;
        ctx.fill(rx - 10, winY + 58, winX + winW - 10, winY + winH - 44, 0x80070612);
        ctx.fill(rx - 10, winY + 58, rx - 9, winY + winH - 44, 0xFF8B5CF6);

        Category c = Category.byId(currentCat);
        ctx.drawTextWithShadow(textRenderer, "KATEGORIE", winX + 22, winY + 56, 0xFF8B5CF6);
        ctx.drawTextWithShadow(textRenderer, "STYLES: " + (c == null ? "" : c.name()), rx, winY + 66, 0xFF22D3EE);

        List<Entry> list = entries();
        if (list.size() == 1) {
            ctx.drawTextWithShadow(textRenderer, "Noch nichts da. Lege ein Pack in den Pack-Ordner", rx, winY + 120, 0xFFC9C3E6);
            ctx.drawTextWithShadow(textRenderer, "oder einzelne PNGs in den Textur-Ordner,", rx, winY + 134, 0xFFC9C3E6);
            ctx.drawTextWithShadow(textRenderer, "dann 'Aktualisieren' klicken.", rx, winY + 148, 0xFFC9C3E6);
        }
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        ctx.drawTextWithShadow(textRenderer, (page + 1) + "/" + pages, rx + 72, winY + 90 + PER_PAGE * 23 + 10, 0xFFB9A7F5);

        String st = status;
        if (st != null && !st.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, st, winX + 22, winY + winH - 52, 0xFFFFFFFF);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }
}
