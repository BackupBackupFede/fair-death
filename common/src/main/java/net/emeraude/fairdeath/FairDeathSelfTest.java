package net.emeraude.fairdeath;

import com.mojang.authlib.GameProfile;
import net.emeraude.fairdeath.core.Config;
import net.emeraude.fairdeath.compat.Click;
import net.emeraude.fairdeath.compat.ClickRelayMenu;
import net.emeraude.fairdeath.core.KeepRule;
import net.emeraude.fairdeath.core.Category;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Behaviour check for a dedicated server, off unless the environment variable
 * {@code <MODID>_SELFTEST=true} is set. Gradle's runServer forks a JVM that inherits the
 * environment, so it works on both loaders without touching any build file.
 *
 * <p>Why this exists: "it compiles" says nothing about a mixin, which resolves its target only at
 * runtime, and "the server started" says nothing about a cold code path. This runs the real thing
 * on a player with no connection, checks the outcome, and stops the server — so one command per
 * loader per Minecraft line tells you whether the mod still works.
 *
 * <p>A fake player dies for real ({@code die}, so vanilla's whole drop path runs), then respawns
 * through {@code restoreFrom}, exactly as PlayerList does. The witness replays the same death with
 * every switch off: if that still passes, the check measures nothing.
 */
public final class FairDeathSelfTest {

    /**
     * Derived from the mod id rather than written out, so the generator has nothing to rename and
     * the name can never drift from what selftest.ps1 sets. For mod id "coolmod": COOLMOD_SELFTEST.
     */
    private static final String SWITCH = FairDeath.MOD_ID.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "") + "_SELFTEST";

    private FairDeathSelfTest() {}

    public static boolean requested() {
        return "true".equalsIgnoreCase(System.getenv(SWITCH));
    }

    public static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        try {
            ServerLevel level = server.overworld();
            String inventory = InventoryDump.requestedFile();
            if (inventory != null && !inventory.isBlank()) InventoryDump.write(server, java.nio.file.Path.of(inventory));
            check(failures, "commands", server.getCommands().getDispatcher().getRoot().getChild(FairDeath.MOD_ID) != null
                ? null : "/fairdeath is not registered");
            check(failures, "lists", lists(server));
            check(failures, "categories", categories(server));
            check(failures, "switched death", switchedDeath(server, level));
            check(failures, "settings menu", settingsMenu(server, level));
            check(failures, "death", death(server, level));
            check(failures, "witness", witness(server, level));
        } catch (Throwable t) {
            FairDeath.LOGGER.error("[SELFTEST] crashed", t);
            failures.add("crash: " + t);
        }
        FairDeath.LOGGER.info("[SELFTEST] RESULT {}", failures.isEmpty() ? "PASS" : "FAIL " + failures);
        server.halt(false);
    }

    private static void check(List<String> failures, String name, String error) {
        if (error == null) {
            FairDeath.LOGGER.info("[SELFTEST] {} OK", name);
        } else {
            FairDeath.LOGGER.error("[SELFTEST] {} FAILED: {}", name, error);
            failures.add(name + ": " + error);
        }
    }

    /** The default lists and gear, read through real stacks rather than the pure rule. */
    private static String lists(MinecraftServer server) {
        String[][] expect = {
            {"minecraft:shulker_box", "DROP_LIST"}, {"minecraft:totem_of_undying", "KEEP_LIST"},
            {"minecraft:filled_map", "KEEP_LIST"}, {"minecraft:written_book", "KEEP_LIST"},
            {"minecraft:diamond_chestplate", "KEEP_ARMOUR"}, {"minecraft:elytra", "KEEP_ARMOUR"},
            {"minecraft:diamond_sword", "KEEP_TOOL"}, {"minecraft:shield", "KEEP_TOOL"},
            {"minecraft:diamond", "DROP_STACKS"}, {"minecraft:carved_pumpkin", "DROP_STACKS"}};
        for (String[] e : expect) {
            KeepRule.Verdict got = DeathKeeper.verdict(stack(e[0], 1), server);
            if (got != KeepRule.Verdict.valueOf(e[1])) return e[0] + " is " + got + ", expected " + e[1];
        }
        return null;
    }

    /**
     * Each category holds its obvious items and not the others, and each switch keeps them while
     * the rest still drops. Off and on in one check, so it is its own witness.
     */
    private static String categories(MinecraftServer server) {
        String[][] members = {
            {"FOOD", "minecraft:bread"}, {"FOOD", "minecraft:potion"},
            {"ORES", "minecraft:diamond"}, {"ORES", "minecraft:diamond_block"}, {"ORES", "minecraft:raw_iron"}, {"ORES", "minecraft:iron_ore"},
            {"UTILITIES", "minecraft:water_bucket"}, {"UTILITIES", "minecraft:arrow"}, {"UTILITIES", "minecraft:snowball"}, {"UTILITIES", "minecraft:ender_pearl"}};
        for (String[] m : members) {
            if (!DeathKeeper.categoriesOf(stack(m[1], 1), server).contains(Category.valueOf(m[0]))) return m[1] + " is not in " + m[0];
        }
        for (String outside : new String[]{"minecraft:dirt", "minecraft:enchanted_book", "minecraft:netherite_upgrade_smithing_template"}) {
            if (!DeathKeeper.categoriesOf(stack(outside, 1), server).isEmpty()) return outside + " is in " + DeathKeeper.categoriesOf(stack(outside, 1), server);
        }

        Config on = FairDeath.config();
        try {
            for (Category category : new Category[]{Category.FOOD, Category.ORES, Category.UTILITIES}) {
                Config one = new Config();
                one.kept.set(category, true);
                FairDeath.useConfig(one);
                for (String[] m : members) {
                    KeepRule.Verdict got = DeathKeeper.verdict(stack(m[1], 1), server);
                    boolean shouldKeep = m[0].equals(category.name());
                    if (got.kept() != shouldKeep) return category.title + " on: " + m[1] + " is " + got;
                }
            }
            Config noArmour = new Config();
            noArmour.kept.set(Category.ARMOUR, false);
            FairDeath.useConfig(noArmour);
            if (DeathKeeper.verdict(stack("minecraft:diamond_chestplate", 1), server) != KeepRule.Verdict.DROP_GEAR_OFF) return "Armour off, the chestplate is still kept";
            if (DeathKeeper.verdict(stack("minecraft:diamond_sword", 1), server) != KeepRule.Verdict.KEEP_TOOL) return "Armour off, the sword drops";

            Config noTools = new Config();
            noTools.kept.set(Category.TOOLS_AND_WEAPONS, false);
            FairDeath.useConfig(noTools);
            if (DeathKeeper.verdict(stack("minecraft:diamond_sword", 1), server) != KeepRule.Verdict.DROP_GEAR_OFF) return "Tools off, the sword is still kept";
            if (DeathKeeper.verdict(stack("minecraft:diamond_chestplate", 1), server) != KeepRule.Verdict.KEEP_ARMOUR) return "Tools off, the chestplate drops";

            Config all = new Config();
            for (Category category : Category.values()) all.kept.set(category, true);
            FairDeath.useConfig(all);
            ItemStack full = stack("minecraft:shulker_box", 1);
            full.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack("minecraft:diamond", 1))));
            if (DeathKeeper.verdict(full, server).kept()) return "everything on, a shulker box full of diamonds is kept";
            return null;
        } finally {
            FairDeath.useConfig(on);
        }
    }

    /** The kit dies with Armour off and Ores on: the chestplate drops, the diamonds stay, the sword stays. */
    private static String switchedDeath(MinecraftServer server, ServerLevel level) {
        Config on = FairDeath.config();
        Config switched = new Config();
        switched.kept.set(Category.ARMOUR, false);
        switched.kept.set(Category.ORES, true);
        FairDeath.useConfig(switched);
        List<ItemEntity> built = new ArrayList<>();
        DeathKeeper.recorder = built;
        try {
            ServerPlayer player = player(server, level);
            Inventory inv = player.getInventory();
            TestKit.give(inv, level);
            player.die(level.damageSources().generic());
            if (!inv.getItem(TestKit.CHEST).isEmpty()) return "Armour off, the chestplate stayed on";
            if (built.stream().noneMatch(e -> id(e.getItem()).equals("minecraft:diamond_chestplate"))) return "Armour off, the chestplate is not on the ground";
            ItemStack diamonds = inv.getItem(TestKit.DIAMONDS);
            if (diamonds.getCount() != 64 || !id(diamonds).equals("minecraft:diamond")) return "Ores on, the diamonds did not stay in their slot";
            if (inv.getItem(TestKit.SWORD).isEmpty()) return "the sword did not stay";
            if (!inv.getItem(TestKit.DIRT).isEmpty()) return "the dirt was kept";
            ServerPlayer respawned = player(server, level);
            respawned.restoreFrom(player, false);
            if (respawned.getInventory().getItem(TestKit.DIAMONDS).getCount() != 64) return "the diamonds did not come back on respawn";
            return respawned.getInventory().getItem(TestKit.CHEST).isEmpty() ? null : "a chestplate came back on respawn";
        } finally {
            DeathKeeper.recorder = null;
            built.forEach(ItemEntity::discard);
            FairDeath.useConfig(on);
        }
    }

    /** Clicks in the settings chest, as the network handler would: icon, then pane, both switch. */
    private static String settingsMenu(MinecraftServer server, ServerLevel level) {
        Config on = FairDeath.config();
        FairDeath.useConfig(new Config());
        try {
            ServerPlayer player = player(server, level);
            SettingsMenu menu = new SettingsMenu(1, player);
            for (Category category : Category.values()) {
                String pane = id(menu.shown(SettingsMenu.slotOf(category) + 9));
                String want = category.defaultKept ? "minecraft:lime_stained_glass_pane" : "minecraft:red_stained_glass_pane";
                if (!pane.equals(want)) return category.title + " starts as " + pane;
            }

            int food = SettingsMenu.slotOf(Category.FOOD);
            ClickRelayMenu.click(menu, food, 0, Click.PICKUP, player);
            if (!FairDeath.config().kept.food) return "a click on the Food icon did not switch it on";
            if (!id(menu.shown(food + 9)).equals("minecraft:lime_stained_glass_pane")) return "the Food pane did not turn green";

            int armour = SettingsMenu.slotOf(Category.ARMOUR);
            ClickRelayMenu.click(menu, armour + 9, 0, Click.PICKUP, player);
            if (FairDeath.config().kept.armour) return "a click on the Armour pane did not switch it off";
            if (!FairDeath.config().kept.toolsAndWeapons || FairDeath.config().kept.ores || FairDeath.config().kept.utilities) {
                return "a click switched another category";
            }

            // Shift-click, number key, throw: nothing may change, nothing may leave the chest.
            ClickRelayMenu.click(menu, food, 0, Click.QUICK_MOVE, player);
            ClickRelayMenu.click(menu, food, 0, Click.THROW, player);
            ClickRelayMenu.click(menu, food, 0, Click.SWAP, player);
            if (!FairDeath.config().kept.food) return "a shift-click, throw or swap switched a category";
            if (!player.getInventory().isEmpty()) return "an icon ended up in the player's inventory";
            if (menu.shown(food).isEmpty()) return "an icon left the chest";
            return null;
        } finally {
            FairDeath.useConfig(on);
            FairDeath.saveConfig();
        }
    }

    /**
     * The same death must fail with every switch off, and again with only the glow off: the first
     * run stops at the kept slots, so without the second the glow check would never be measured.
     */
    private static String witness(MinecraftServer server, ServerLevel level) {
        Config allOff = new Config();
        allOff.kept.set(Category.ARMOUR, false);
        allOff.kept.set(Category.TOOLS_AND_WEAPONS, false);
        allOff.glowingDrops = false;
        allOff.deathCoordinates = false;
        String error = witness(server, level, "mod off", allOff, "slot");
        if (error != null) return error;

        Config glowOff = new Config();
        glowOff.glowingDrops = false;
        return witness(server, level, "glow off", glowOff, "without glowing");
    }

    private static String witness(MinecraftServer server, ServerLevel level, String name, Config config, String expected) {
        Config on = FairDeath.config();
        FairDeath.useConfig(config);
        try {
            String result = death(server, level);
            if (result == null) return "the death check still passes with " + name;
            if (!result.contains(expected)) return name + ": failed for the wrong reason (" + result + ")";
            FairDeath.LOGGER.info("[SELFTEST] witness: with {}, death fails as it should ({})", name, result);
            return null;
        } finally {
            FairDeath.useConfig(on);
        }
    }

    /**
     * Checklist case 1: full kit, a cursed pickaxe, stacks, a shulker box and a tool holding items.
     *
     * <p>The drops are read from the entities the death actually built, not from the world: in 26.x
     * no spawn chunk stays loaded, and before the first tick a forced chunk is not yet visible to
     * entity queries. NeoForge also holds the entities back until its drops event has run.
     */
    private static String death(MinecraftServer server, ServerLevel level) {
        List<ItemEntity> built = new ArrayList<>();
        DeathKeeper.recorder = built;
        try {
            ServerPlayer player = player(server, level);
            player.setPos(0.5, 120, 0.5);
            Inventory inv = player.getInventory();
            TestKit.give(inv, level);

            player.die(level.damageSources().generic());

            // The dead player keeps the gear, slot for slot, and nothing else.
            String slots = expectSlots(inv, "dead player");
            if (slots != null) return slots;

            // The rest is on the ground, glowing; the cursed pickaxe is nowhere.
            List<String> ground = new ArrayList<>();
            for (ItemEntity item : built) {
                if (item.getItem().isEmpty()) continue;
                String id = id(item.getItem());
                if (item.isRemoved()) return id + " was built then removed";
                if (!item.hasGlowingTag()) return id + " dropped without glowing";
                ground.add(id);
            }
            Collections.sort(ground);
            if (!ground.equals(TestKit.DROPPED)) return "on the ground: " + ground + ", expected " + TestKit.DROPPED;

            // Respawn: what PlayerList.respawn does with a fresh player.
            ServerPlayer respawned = player(server, level);
            respawned.restoreFrom(player, false);
            return expectSlots(respawned.getInventory(), "respawned player");
        } finally {
            DeathKeeper.recorder = null;
            built.forEach(ItemEntity::discard);
        }
    }

    private static String expectSlots(Inventory inv, String who) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            String got = stack.isEmpty() ? null : id(stack);
            String want = TestKit.keptAt(i);
            if (want == null ? got != null : !want.equals(got)) {
                return who + ", slot " + i + ": " + got + ", expected " + want;
            }
        }
        return null;
    }

    private static String id(ItemStack stack) {
        return ItemIds.id(stack.getItem());
    }

    /**
     * A player with a connection that has no channel. Vanilla sends packets on the way (a recipe
     * unlocking, for one), and an unopened Connection simply queues them instead of throwing.
     */
    private static ServerPlayer player(MinecraftServer server, ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "selftest");
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false));
        player.getInventory().clearContent();
        return player;
    }

    private static ItemStack stack(String id, int count) {
        return ItemIds.stack(id, count);
    }
}
