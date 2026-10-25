package net.emeraude.fairdeath;

import net.emeraude.fairdeath.compat.Compat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug only: one line per item with its creative tabs, durability, armour flag and what death
 * does to it with the current settings. Written when {@code FAIRDEATH_INVENTORY=<file>} is set
 * alongside the self-test switch.
 */
final class InventoryDump {

    private InventoryDump() {}

    static String requestedFile() {
        return System.getenv("FAIRDEATH_INVENTORY");
    }

    static void write(MinecraftServer server, Path file) throws IOException {
        Map<String, Set<Item>> tabs = CreativeTabs.everyTab(server);
        List<String> lines = new ArrayList<>();
        lines.add("# tabs\t" + String.join(",", tabs.keySet()));
        lines.add("id\tdurability\tarmour\ttabs\tverdict");
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) continue;
            boolean durability = stack.has(DataComponents.MAX_DAMAGE);
            List<String> in = new ArrayList<>();
            for (Map.Entry<String, Set<Item>> tab : tabs.entrySet()) {
                if (tab.getValue().contains(item)) in.add(tab.getKey());
            }
            lines.add(ItemIds.id(item) + "\t" + durability + "\t" + (durability && Compat.wornAsArmour(stack))
                + "\t" + String.join(",", in) + "\t" + DeathKeeper.verdict(stack, server));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
        FairDeath.LOGGER.info("[SELFTEST] inventory written: {} items to {}", lines.size() - 2, file.toAbsolutePath());
    }
}
