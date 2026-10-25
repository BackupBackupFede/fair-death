package net.emeraude.fairdeath.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** The few calls whose shape differs between 1.21.1 and 26.x. This is the 1.21.1 side. */
public final class Compat {

    private Compat() {}

    public static boolean keepInventory(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    public static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    public static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.hasPermission(level);
    }

    /** Worn on the head, chest, legs or feet (armour, elytra, turtle shell...), whatever its durability. */
    public static boolean wornAsArmour(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        return equipable != null && equipable.getEquipmentSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }
}
