package dev.yns;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Eine Kategorie (Totem, Obsidian, ...) = eine Liste von Pfad-Regeln.
 * Nur Dateien, die auf eine Regel passen, werden aus dem gewaehlten Pack uebernommen.
 */
public record Category(String id, String name, List<Pattern> rules) {

    private static final String T = "^assets/minecraft/textures/";
    private static final String PNG = "\\.png(\\.mcmeta)?$";

    private static Category of(String id, String name, String... regex) {
        return new Category(id, name, java.util.Arrays.stream(regex).map(Pattern::compile).toList());
    }

    public boolean matches(String normalizedPath) {
        for (Pattern p : rules) if (p.matcher(normalizedPath).find()) return true;
        return false;
    }

    /** Alte Ordnernamen (blocks/items) und "web.png" auf das 1.21-Format bringen. */
    public static String normalize(String path) {
        String p = path.replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        p = p.replace("/textures/blocks/", "/textures/block/").replace("/textures/items/", "/textures/item/");
        if (p.endsWith("/textures/block/web.png")) p = p.substring(0, p.length() - 7) + "cobweb.png";
        return p;
    }

    private static final String TOOLS = "(wooden|stone|iron|golden|diamond|netherite)";

    public static final List<Category> ALL = List.of(
            of("totem", "Totem", T + "item/totem_of_undying" + PNG),
            of("obsidian", "Obsidian", T + "block/(crying_)?obsidian" + PNG),
            of("crystal", "Crystal",
                    T + "entity/end_crystal/[a-z0-9_]+" + PNG,
                    T + "item/end_crystal" + PNG),
            of("anchor", "Anchor", T + "block/respawn_anchor_[a-z0-9_]+" + PNG),
            of("sword", "Schwert", T + "item/" + TOOLS + "_sword" + PNG),
            of("pearl", "Enderperle",
                    T + "item/ender_pearl" + PNG,
                    "^assets/minecraft/items/ender_pearl\\.json$",
                    "^assets/minecraft/models/item/ender_pearl\\.json$"),
            of("gap", "Goldener Apfel", T + "item/(enchanted_)?golden_apple" + PNG),
            of("sky", "Himmel", T + "environment/[a-z0-9_]+" + PNG),
            of("hotbar", "Hotbar", T + "gui/sprites/hud/hotbar[a-z_]*" + PNG),
            of("cobweb", "Spinnennetz", T + "block/cobweb" + PNG),
            of("fire", "Feuer", T + "block/fire_(0|1|layer_0|layer_1)" + PNG),
            of("mace", "Mace", T + "item/mace" + PNG, "^assets/minecraft/models/item/mace\\.json$"),
            of("armor", "Ruestung",
                    T + "item/(leather|chainmail|iron|golden|diamond|netherite)_(helmet|chestplate|leggings|boots)" + PNG,
                    T + "entity/equipment/humanoid(_leggings)?/[a-z0-9_]+" + PNG,
                    T + "models/armor/[a-z0-9_]+" + PNG),
            of("shield", "Schild",
                    T + "entity/shield_base(_nopattern)?" + PNG,
                    T + "entity/shield/[a-z0-9_]+" + PNG,
                    T + "gui/sprites/container/slot/shield" + PNG),
            of("pickaxe", "Spitzhacke", T + "item/" + TOOLS + "_pickaxe" + PNG),
            of("stone", "Stein / Erze",
                    T + "block/(stone|coal_ore|copper_ore|diamond_ore|emerald_ore|gold_ore|iron_ore|lapis_ore|redstone_ore)" + PNG),
            of("deepslate", "Deepslate", T + "block/([a-z_]*_)?deepslate[a-z_]*" + PNG),
            of("glowstone", "Glowstone", T + "block/glowstone" + PNG)
    );

    public static Category byId(String id) {
        for (Category c : ALL) if (c.id.equals(id)) return c;
        return null;
    }
}
