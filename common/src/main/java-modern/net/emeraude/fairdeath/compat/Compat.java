package net.emeraude.fairdeath.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.gamerules.GameRules;

/** The few calls whose shape differs between 1.21.1 and 26.x. This is the 26.x side. */
public final class Compat {

    private Compat() {}

    public static boolean keepInventory(ServerLevel level) {
        return level.getGameRules().get(GameRules.KEEP_INVENTORY);
    }

    public static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, Identifier.parse(id));
    }

    public static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    /** Worn on the head, chest, legs or feet (armour, elytra, turtle shell...), whatever its durability. */
    public static boolean wornAsArmour(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }
}
