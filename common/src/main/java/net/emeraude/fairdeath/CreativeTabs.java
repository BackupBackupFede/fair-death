package net.emeraude.fairdeath;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Which items each creative tab holds, read once per server (Fair Death uses Food & Drinks).
 *
 * <p>The tabs exist on a dedicated server too (checked in the server jars of every line), and
 * building them also runs the loaders' own hooks, which is where other mods add their items. In
 * singleplayer the client shares these tabs, so they are read once and copied: nothing here
 * touches them again.
 */
public final class CreativeTabs {

    private static MinecraftServer builtFor;
    private static Map<String, Set<Item>> everyTab = Map.of();

    private CreativeTabs() {}

    public static Set<Item> items(MinecraftServer server, String tabId) {
        return everyTab(server).getOrDefault(tabId, Set.of());
    }

    /** Every creative tab by id, in registry order. */
    public static synchronized Map<String, Set<Item>> everyTab(MinecraftServer server) {
        if (builtFor != server) build(server);
        return everyTab;
    }

    public static boolean contains(MinecraftServer server, String tabId, ItemStack stack) {
        return items(server, tabId).contains(stack.getItem());
    }

    private static void build(MinecraftServer server) {
        CreativeModeTabs.tryRebuildTabContents(server.overworld().enabledFeatures(), false, server.registryAccess());
        Map<String, Set<Item>> all = new LinkedHashMap<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            String id = String.valueOf(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab));
            Set<Item> items = new HashSet<>();
            for (ItemStack stack : tab.getDisplayItems()) items.add(stack.getItem());
            all.put(id, Set.copyOf(items));
        }
        everyTab = all;
        builtFor = server;
        FairDeath.LOGGER.info("Creative tabs read: {}", summary(all));
    }

    private static String summary(Map<String, Set<Item>> all) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, Set<Item>> tab : all.entrySet()) {
            if (tab.getValue().isEmpty()) continue;
            if (s.length() > 0) s.append(", ");
            s.append(tab.getKey()).append(' ').append(tab.getValue().size());
        }
        return s.toString();
    }
}
