package net.emeraude.fairdeath.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

/** 26.x: {@code clicked} takes a {@link ContainerInput}. Every click is handed to {@link #onClick}, none to vanilla. */
public abstract class ClickRelayMenu extends ChestMenu {

    protected ClickRelayMenu(MenuType<?> type, int id, Inventory inventory, Container container, int rows) {
        super(type, id, inventory, container, rows);
    }

    @Override
    public final void clicked(int slot, int button, ContainerInput input, Player player) {
        onClick(slot, button, Click.valueOf(input.name()), player);
    }

    protected abstract void onClick(int slot, int button, Click click, Player player);

    /** What the network handler does with a click packet, for the self-test. */
    public static void click(ChestMenu menu, int slot, int button, Click click, Player player) {
        menu.clicked(slot, button, ContainerInput.valueOf(click.name()), player);
    }
}
