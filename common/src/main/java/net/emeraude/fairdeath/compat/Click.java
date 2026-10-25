package net.emeraude.fairdeath.compat;

/**
 * A menu click, by kind. Mirrors vanilla's own enum, which is {@code ClickType} in 1.21.1 and
 * {@code ContainerInput} in 26.x — same constants, same order.
 */
public enum Click {
    PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL
}
