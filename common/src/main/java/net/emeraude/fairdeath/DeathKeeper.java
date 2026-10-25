package net.emeraude.fairdeath;

import net.emeraude.fairdeath.compat.Compat;
import net.emeraude.fairdeath.core.KeepRule;
import net.emeraude.fairdeath.core.Category;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What happens to a player's inventory around vanilla's death drop.
 *
 * <p>The kept items never leave the player entity: they are taken out just before vanilla drops the
 * inventory, put back in the dead player right after, and copied into the respawned player. A
 * player who logs out on the death screen is saved with them, so nothing is stored anywhere else.
 */
public final class DeathKeeper {

    /**
     * Built in rather than shipped as tag files: without Fabric API, Fabric does not load the
     * {@code data/} of a mod jar. A world datapack can still add to both lists through the tags.
     */
    private static final Set<String> DEFAULT_DROP = Set.of(
        "minecraft:shulker_box", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
        "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box",
        "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
        "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box",
        "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
        "minecraft:red_shulker_box", "minecraft:black_shulker_box",
        "minecraft:bundle", "minecraft:white_bundle", "minecraft:orange_bundle", "minecraft:magenta_bundle",
        "minecraft:light_blue_bundle", "minecraft:yellow_bundle", "minecraft:lime_bundle", "minecraft:pink_bundle",
        "minecraft:gray_bundle", "minecraft:light_gray_bundle", "minecraft:cyan_bundle", "minecraft:purple_bundle",
        "minecraft:blue_bundle", "minecraft:brown_bundle", "minecraft:green_bundle", "minecraft:red_bundle",
        "minecraft:black_bundle"
    );
    /** Filled maps (and the 26.x explorer maps) and signed books: hours of work, not loot. */
    private static final Set<String> DEFAULT_KEEP = Set.of(
        "minecraft:totem_of_undying", "minecraft:spyglass", "minecraft:recovery_compass",
        "minecraft:filled_map", "minecraft:written_book", "minecraft:abandoned_camp_map",
        "minecraft:buried_ancient_city_map", "minecraft:buried_mineshaft_map",
        "minecraft:buried_treasure_map", "minecraft:buried_trial_chambers_map",
        "minecraft:desert_pyramid_map", "minecraft:desert_village_map", "minecraft:jungle_pyramid_map",
        "minecraft:ocean_monument_map", "minecraft:plains_village_map", "minecraft:savanna_village_map",
        "minecraft:snowy_village_map", "minecraft:swamp_hut_map", "minecraft:taiga_village_map",
        "minecraft:warm_ocean_ruins_map", "minecraft:woodland_mansion_map"
    );
    /** Raw ores, ore blocks, ingots, gems, nuggets, netherite and storage blocks. Ids absent from a line are ignored. */
    private static final Set<String> ORES = Set.of(
        "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper", "minecraft:raw_iron_block",
        "minecraft:raw_gold_block", "minecraft:raw_copper_block", "minecraft:coal_ore",
        "minecraft:deepslate_coal_ore", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
        "minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:gold_ore",
        "minecraft:deepslate_gold_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
        "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "minecraft:lapis_ore",
        "minecraft:deepslate_lapis_ore", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
        "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "minecraft:ancient_debris",
        "minecraft:coal", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot",
        "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:diamond", "minecraft:emerald",
        "minecraft:lapis_lazuli", "minecraft:redstone", "minecraft:quartz", "minecraft:amethyst_shard",
        "minecraft:iron_nugget", "minecraft:gold_nugget", "minecraft:copper_nugget", "minecraft:coal_block",
        "minecraft:iron_block", "minecraft:gold_block", "minecraft:copper_block", "minecraft:diamond_block",
        "minecraft:emerald_block", "minecraft:lapis_block", "minecraft:redstone_block",
        "minecraft:netherite_block"
    );
    /** Buckets, things you throw, ammunition. */
    private static final Set<String> UTILITIES = Set.of(
        "minecraft:bucket", "minecraft:water_bucket", "minecraft:lava_bucket", "minecraft:milk_bucket",
        "minecraft:powder_snow_bucket", "minecraft:axolotl_bucket", "minecraft:cod_bucket",
        "minecraft:salmon_bucket", "minecraft:pufferfish_bucket", "minecraft:tropical_fish_bucket",
        "minecraft:tadpole_bucket", "minecraft:sulfur_cube_bucket", "minecraft:snowball", "minecraft:egg",
        "minecraft:blue_egg", "minecraft:brown_egg", "minecraft:ender_pearl", "minecraft:splash_potion",
        "minecraft:lingering_potion", "minecraft:wind_charge", "minecraft:fire_charge", "minecraft:arrow",
        "minecraft:spectral_arrow", "minecraft:tipped_arrow", "minecraft:firework_rocket"
    );
    private static final String FOOD_TAB = "minecraft:food_and_drinks";

    public static final TagKey<Item> ALWAYS_DROP = Compat.itemTag(FairDeath.MOD_ID + ":always_drop");
    public static final TagKey<Item> ALWAYS_KEEP = Compat.itemTag(FairDeath.MOD_ID + ":always_keep");
    public static final TagKey<Item> ORES_TAG = Compat.itemTag(FairDeath.MOD_ID + ":ores");
    public static final TagKey<Item> UTILITIES_TAG = Compat.itemTag(FairDeath.MOD_ID + ":utilities");

    /** Items set aside during one death, with the slot each came from. */
    public record Kept(int[] slots, ItemStack[] stacks) {}

    /**
     * True while vanilla drops a dying player's inventory and glowing drops are on. A flag, not a
     * counter: deaths never nest, and a flag cannot stay stuck if a death throws halfway.
     */
    private static boolean glowingDrops = false;
    private static boolean dropping = false;
    /** Self-test only: when set, collects every item entity built during a death drop. */
    static List<ItemEntity> recorder = null;

    private DeathKeeper() {}

    public static KeepRule.Verdict verdict(ItemStack stack, MinecraftServer server) {
        String id = ItemIds.id(stack.getItem());
        boolean durability = stack.has(DataComponents.MAX_DAMAGE);
        boolean armour = durability && Compat.wornAsArmour(stack);
        return KeepRule.decide(new KeepRule.Facts(
            EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP),
            DEFAULT_DROP.contains(id) || stack.is(ALWAYS_DROP),
            holdsItems(stack),
            DEFAULT_KEEP.contains(id) || stack.is(ALWAYS_KEEP),
            armour,
            durability && !armour,
            categoriesOf(stack, server)
        ), FairDeath.config().kept.switchedOn());
    }

    /** Which of the extra categories (Food & Drinks, Ores, Utilities) the item belongs to, switched on or not. */
    public static Set<Category> categoriesOf(ItemStack stack, MinecraftServer server) {
        String id = ItemIds.id(stack.getItem());
        Set<Category> in = EnumSet.noneOf(Category.class);
        if (CreativeTabs.contains(server, FOOD_TAB, stack)) in.add(Category.FOOD);
        if (ORES.contains(id) || stack.is(ORES_TAG)) in.add(Category.ORES);
        if (UTILITIES.contains(id) || stack.is(UTILITIES_TAG)) in.add(Category.UTILITIES);
        return in;
    }

    private static boolean holdsItems(ItemStack stack) {
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItems().iterator().hasNext()) return true;
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return bundle != null && !bundle.isEmpty();
    }

    /** Takes the kept items out of the inventory. Cursed items must already be gone. */
    public static Kept take(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !verdict(stack, player.level().getServer()).kept()) continue;
            slots.add(i);
            stacks.add(inventory.removeItemNoUpdate(i));
        }
        return new Kept(slots.stream().mapToInt(Integer::intValue).toArray(), stacks.toArray(new ItemStack[0]));
    }

    /** Puts the kept items back in the dead player, each in its own slot. */
    public static void putBack(ServerPlayer player, Kept kept) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < kept.slots().length; i++) {
            ItemStack stack = kept.stacks()[i];
            if (inventory.getItem(kept.slots()[i]).isEmpty()) {
                inventory.setItem(kept.slots()[i], stack);
            } else if (!inventory.add(stack)) {
                // Another mod refilled the slot in between. Never delete an item: drop it instead.
                FairDeath.LOGGER.warn("Slot {} of {} was taken back, dropping {}", kept.slots()[i], player.getName().getString(), stack);
                ServerLevel level = (ServerLevel) player.level();
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack));
            }
        }
    }

    /** Copies what the dead player still carries into the respawned one, slot for slot. */
    public static void restore(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        Inventory from = oldPlayer.getInventory();
        Inventory to = newPlayer.getInventory();
        for (int i = 0; i < from.getContainerSize(); i++) {
            ItemStack stack = from.getItem(i);
            if (!stack.isEmpty()) to.setItem(i, stack.copy());
        }
    }

    public static void startDrops() {
        dropping = true;
        glowingDrops = FairDeath.config().glowingDrops;
    }

    public static void endDrops() {
        dropping = false;
        glowingDrops = false;
    }

    /** Called for every item entity built while vanilla drops a dying player's inventory. */
    public static void onDropBuilt(ItemEntity item) {
        if (!dropping) return;
        if (glowingDrops) item.setGlowingTag(true);
        if (recorder != null) recorder.add(item);
    }

    public static void tellWhere(ServerPlayer player) {
        if (!FairDeath.config().deathCoordinates) return;
        BlockPos pos = player.blockPosition();
        ServerLevel level = (ServerLevel) player.level();
        player.sendSystemMessage(Component.literal(
            "You died at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " in " + Compat.dimensionId(level) + "."
        ).withStyle(ChatFormatting.GRAY));
    }
}
