package net.emeraude.fairdeath.core;

import java.util.Set;

/**
 * Decides what happens to one item stack when a player dies, given the switches the server has on.
 *
 * <p>Pure Java on purpose: the caller reads the stack once into {@link Facts}, so the whole order of
 * priority is proven by unit tests without a server. The first rule that answers wins:
 * <ol>
 *   <li>Curse of Vanishing: the item vanishes, as in vanilla, even if it would be kept.</li>
 *   <li>Always-drop list (shulker boxes, bundles, plus the {@code fairdeath:always_drop} tag), or the
 *       item holds other items: it drops. A kept shulker box would be keepInventory in disguise, so
 *       holding items beats every list and switch below.</li>
 *   <li>Always-keep list (totem, spyglass, recovery compass, filled maps, signed books, plus the
 *       {@code fairdeath:always_keep} tag): it is kept.</li>
 *   <li>Armour, or any other item with durability, while its switch is on (both on by default).</li>
 *   <li>A category the server switched on: Food & Drinks, Ores, Utilities (all off by default).</li>
 *   <li>Anything else drops, including gear whose switch is off.</li>
 * </ol>
 */
public final class KeepRule {

    private KeepRule() {}

    /** What the rule needs to know about one non-empty stack. */
    public record Facts(
        boolean vanishingCurse,
        boolean alwaysDrop,
        boolean holdsItems,
        boolean alwaysKeep,
        boolean armour,
        boolean toolOrWeapon,
        Set<Category> categories
    ) {}

    /** The outcome, with the rule that produced it. */
    public enum Verdict {
        VANISH_CURSE(Outcome.VANISH, "Curse of Vanishing"),
        DROP_LIST(Outcome.DROP, "always drops"),
        DROP_HOLDS_ITEMS(Outcome.DROP, "it holds items"),
        KEEP_LIST(Outcome.KEEP, "always kept"),
        KEEP_ARMOUR(Outcome.KEEP, "Armour is kept"),
        KEEP_TOOL(Outcome.KEEP, "Tools & weapons are kept"),
        KEEP_CATEGORY(Outcome.KEEP, "its category is kept"),
        DROP_GEAR_OFF(Outcome.DROP, "this gear is set to drop"),
        DROP_STACKS(Outcome.DROP, "nothing keeps it");

        public final Outcome outcome;
        public final String reason;

        Verdict(Outcome outcome, String reason) {
            this.outcome = outcome;
            this.reason = reason;
        }

        public boolean kept() {
            return outcome == Outcome.KEEP;
        }
    }

    public enum Outcome { KEEP, DROP, VANISH }

    public static Verdict decide(Facts item, Set<Category> switchedOn) {
        if (item.vanishingCurse()) return Verdict.VANISH_CURSE;
        if (item.alwaysDrop()) return Verdict.DROP_LIST;
        if (item.holdsItems()) return Verdict.DROP_HOLDS_ITEMS;
        if (item.alwaysKeep()) return Verdict.KEEP_LIST;
        if (item.armour() && switchedOn.contains(Category.ARMOUR)) return Verdict.KEEP_ARMOUR;
        if (item.toolOrWeapon() && switchedOn.contains(Category.TOOLS_AND_WEAPONS)) return Verdict.KEEP_TOOL;
        for (Category category : item.categories()) {
            if (switchedOn.contains(category)) return Verdict.KEEP_CATEGORY;
        }
        return item.armour() || item.toolOrWeapon() ? Verdict.DROP_GEAR_OFF : Verdict.DROP_STACKS;
    }
}
