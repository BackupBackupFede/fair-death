package net.emeraude.fairdeath;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/**
 * One inventory that exercises every rule, used by the self-test and by the debug command
 * {@code /fairdeath kit}: what the self-test proves is exactly what the in-game pass replays.
 */
public final class TestKit {

    public static final int SWORD = 0, CURSED = 1, DIAMONDS = 2, DIRT = 3, SHULKER = 4, FULL_HOE = 5, CHEST = 38, OFFHAND = 40;

    /** What must still be on the player after death, by slot (null = empty). */
    private static final String[] KEPT = new String[41];
    /** What must be on the ground, sorted. The cursed pickaxe is in neither list. */
    public static final List<String> DROPPED = List.of("minecraft:diamond", "minecraft:diamond_hoe", "minecraft:dirt", "minecraft:shulker_box");

    static {
        KEPT[SWORD] = "minecraft:diamond_sword";
        KEPT[CHEST] = "minecraft:diamond_chestplate";
        KEPT[OFFHAND] = "minecraft:totem_of_undying";
    }

    private TestKit() {}

    /** 41 slots in 1.21.1, 43 in 26.x (body and saddle, always empty on a player). */
    public static String keptAt(int slot) {
        return slot < KEPT.length ? KEPT[slot] : null;
    }

    /** Overwrites the kit's slots only; everything else in the inventory is left alone. */
    public static void give(Inventory inv, ServerLevel level) {
        inv.setItem(SWORD, stack("minecraft:diamond_sword", 1));
        ItemStack cursed = stack("minecraft:golden_pickaxe", 1);
        cursed.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
        inv.setItem(CURSED, cursed);
        inv.setItem(DIAMONDS, stack("minecraft:diamond", 64));
        inv.setItem(DIRT, stack("minecraft:dirt", 64));
        ItemStack shulker = stack("minecraft:shulker_box", 1);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack("minecraft:cobblestone", 64))));
        inv.setItem(SHULKER, shulker);
        // A tool holding items: it wears out, yet it drops (it would smuggle its contents otherwise).
        ItemStack fullHoe = stack("minecraft:diamond_hoe", 1);
        fullHoe.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack("minecraft:dirt", 1))));
        fullHoe.set(DataComponents.CUSTOM_NAME, Component.literal("Test: hoe holding dirt (drops)"));
        inv.setItem(FULL_HOE, fullHoe);
        inv.setItem(CHEST, stack("minecraft:diamond_chestplate", 1));
        inv.setItem(OFFHAND, stack("minecraft:totem_of_undying", 1));
    }

    private static ItemStack stack(String id, int count) {
        return ItemIds.stack(id, count);
    }
}
