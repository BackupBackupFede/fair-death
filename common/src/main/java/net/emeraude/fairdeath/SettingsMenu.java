package net.emeraude.fairdeath;

import net.emeraude.fairdeath.compat.Click;
import net.emeraude.fairdeath.compat.ClickRelayMenu;
import net.emeraude.fairdeath.core.Category;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * {@code /fairdeath settings}: what is kept on death, in a vanilla chest so that no client mod is
 * needed. Each category is an icon with a pane under it, green when kept, red when it drops; a
 * click on either switches it and saves the config.
 *
 * <p>The chest only displays: its slots hold copies, every click is intercepted before vanilla sees
 * it, and the client is resynced afterwards because a vanilla client predicts the move it asked for.
 */
public final class SettingsMenu extends ClickRelayMenu {

    private static final int ROWS = 3, SIZE = ROWS * 9, RULES = 4;
    /** Icon slots in the middle row, in {@link Category} order; pane slots right under them. */
    private static final int[] ICON = {11, 12, 13, 14, 15};

    private final ServerPlayer viewer;
    private final SimpleContainer display;
    private final ResourceKey<Level> openedIn;

    public SettingsMenu(int id, ServerPlayer viewer) {
        this(id, viewer, new SimpleContainer(SIZE));
    }

    private SettingsMenu(int id, ServerPlayer viewer, SimpleContainer display) {
        super(MenuType.GENERIC_9x3, id, viewer.getInventory(), display, ROWS);
        this.viewer = viewer;
        this.display = display;
        this.openedIn = viewer.level().dimension();
        draw();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SettingsMenu(id, player),
            Component.literal("Fair Death - kept on death")));
    }

    /** The slot to click to switch {@code category}: its icon. The pane under it works too. */
    public static int slotOf(Category category) {
        return ICON[category.ordinal()];
    }

    public ItemStack shown(int slot) {
        return display.getItem(slot);
    }

    private void draw() {
        for (int i = 0; i < SIZE; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(RULES, icon(ItemIds.stack("minecraft:book", 1), "Fixed rules", ChatFormatting.YELLOW,
            List.of("Always kept: totem, maps, signed books", "Always drops: shulkers, bundles", "Vanishing curse: gone")));
        for (Category category : Category.values()) {
            boolean kept = FairDeath.config().kept.get(category);
            int slot = slotOf(category);
            display.setItem(slot, icon(ItemIds.stack(category.iconId, 1), category.title, ChatFormatting.WHITE,
                List.of(category.contents, kept ? "Kept" : "Drops")));
            display.setItem(slot + 9, icon(ItemIds.stack(kept ? "minecraft:lime_stained_glass_pane" : "minecraft:red_stained_glass_pane", 1),
                kept ? "Kept" : "Drops", kept ? ChatFormatting.GREEN : ChatFormatting.RED,
                List.of(category.title)));
        }
    }

    @Override
    protected void onClick(int slot, int button, Click click, Player player) {
        try {
            if (player == viewer && click == Click.PICKUP && (button == 0 || button == 1)) {
                for (Category category : Category.values()) {
                    if (slot != slotOf(category) && slot != slotOf(category) + 9) continue;
                    boolean kept = !FairDeath.config().kept.get(category);
                    FairDeath.config().kept.set(category, kept);
                    String failure = FairDeath.saveConfig();
                    FairDeath.LOGGER.info("{} set {} to {}", viewer.getName().getString(), category.title, kept ? "kept" : "dropped");
                    if (failure != null) viewer.sendSystemMessage(Component.literal("Not saved: " + failure).withStyle(ChatFormatting.RED));
                    draw();
                }
            }
        } finally {
            // Undo whatever the client predicted: nothing on the cursor, every slot as the server has it.
            if (viewer.containerMenu == this) {
                setCarried(ItemStack.EMPTY);
                sendAllDataToRemote();
            }
            viewer.inventoryMenu.sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return false;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return false;
    }

    /** Closed by vanilla's per-tick check on death, on a dimension change and on disconnect. */
    @Override
    public boolean stillValid(Player player) {
        return player == viewer && viewer.isAlive() && !viewer.isRemoved() && viewer.level().dimension() == openedIn;
    }

    private static ItemStack icon(ItemStack stack, String name, ChatFormatting colour, List<String> lore) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(s -> s.withItalic(false).withColor(colour)));
        stack.set(DataComponents.LORE, new ItemLore(lore.stream()
            .map(line -> (Component) Component.literal(line).withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))
            .toList()));
        return stack;
    }
}
