package net.emeraude.fairdeath;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Items by id: several Items.* fields were renamed or regrouped in 26.x, ids were not. */
public final class ItemIds {

    private ItemIds() {}

    public static ItemStack stack(String id, int count) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (id(item).equals(id)) return new ItemStack(item, count);
        }
        throw new IllegalArgumentException("unknown item " + id);
    }

    public static String id(Item item) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
    }
}
