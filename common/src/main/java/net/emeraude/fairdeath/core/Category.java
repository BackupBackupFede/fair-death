package net.emeraude.fairdeath.core;

/**
 * The switches of {@code /fairdeath settings}, in menu order. The first two are the gear and are on
 * by default; the other three keep more on top of it and are off.
 */
public enum Category {
    ARMOUR("Armour", "minecraft:diamond_chestplate", true,
        "Armour pieces, elytra"),
    TOOLS_AND_WEAPONS("Tools & weapons", "minecraft:diamond_pickaxe", true,
        "Swords, pickaxes, hoes, bows..."),
    FOOD("Food & Drinks", "minecraft:golden_apple", false,
        "Food, potions"),
    ORES("Ores", "minecraft:raw_iron", false,
        "Ores, ingots, gems, netherite"),
    UTILITIES("Utilities", "minecraft:water_bucket", false,
        "Buckets, throwables, arrows");

    public final String title;
    public final String iconId;
    public final boolean defaultKept;
    public final String contents;

    Category(String title, String iconId, boolean defaultKept, String contents) {
        this.title = title;
        this.iconId = iconId;
        this.defaultKept = defaultKept;
        this.contents = contents;
    }
}
