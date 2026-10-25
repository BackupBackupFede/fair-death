package net.emeraude.fairdeath.core;

import net.emeraude.fairdeath.core.KeepRule.Facts;
import net.emeraude.fairdeath.core.KeepRule.Outcome;
import net.emeraude.fairdeath.core.KeepRule.Verdict;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KeepRuleTest {

    private static final Set<Category> DEFAULTS = EnumSet.of(Category.ARMOUR, Category.TOOLS_AND_WEAPONS);
    private static final Set<Category> NONE = Set.of();

    private static Facts plain(Category... in) {
        return new Facts(false, false, false, false, false, false, Set.of(in));
    }

    private static Facts armour() {
        return new Facts(false, false, false, false, true, false, Set.of());
    }

    private static Facts tool() {
        return new Facts(false, false, false, false, false, true, Set.of());
    }

    // --- defaults: gear kept, the rest drops -----------------------------------------------------

    @Test
    void armourAndToolsAreKeptByDefault() {
        assertEquals(Verdict.KEEP_ARMOUR, KeepRule.decide(armour(), DEFAULTS));
        assertEquals(Verdict.KEEP_TOOL, KeepRule.decide(tool(), DEFAULTS));
    }

    @Test
    void stacksDropByDefault() {
        assertEquals(Verdict.DROP_STACKS, KeepRule.decide(plain(), DEFAULTS));
        assertEquals(Verdict.DROP_STACKS, KeepRule.decide(plain(Category.ORES), DEFAULTS));
    }

    @Test
    void alwaysKeepListNeedsNoSwitch() {
        Facts totem = new Facts(false, false, false, true, false, false, Set.of());
        assertEquals(Verdict.KEEP_LIST, KeepRule.decide(totem, NONE));
    }

    // --- the gear switches -----------------------------------------------------------------------

    @Test
    void armourOffLetsArmourDropButKeepsTools() {
        Set<Category> on = EnumSet.of(Category.TOOLS_AND_WEAPONS);
        assertEquals(Verdict.DROP_GEAR_OFF, KeepRule.decide(armour(), on));
        assertEquals(Verdict.KEEP_TOOL, KeepRule.decide(tool(), on));
    }

    @Test
    void toolsOffLetsToolsDropButKeepsArmour() {
        Set<Category> on = EnumSet.of(Category.ARMOUR);
        assertEquals(Verdict.KEEP_ARMOUR, KeepRule.decide(armour(), on));
        assertEquals(Verdict.DROP_GEAR_OFF, KeepRule.decide(tool(), on));
    }

    // --- the extra categories --------------------------------------------------------------------

    @Test
    void categorySwitchedOnKeepsItsItems() {
        assertEquals(Verdict.KEEP_CATEGORY, KeepRule.decide(plain(Category.ORES), EnumSet.of(Category.ORES)));
        assertEquals(Verdict.KEEP_CATEGORY, KeepRule.decide(plain(Category.UTILITIES), EnumSet.of(Category.UTILITIES)));
        assertEquals(Verdict.KEEP_CATEGORY, KeepRule.decide(plain(Category.FOOD), EnumSet.of(Category.FOOD)));
    }

    @Test
    void anotherCategoryDoesNotKeepIt() {
        assertEquals(Verdict.DROP_STACKS, KeepRule.decide(plain(Category.ORES), EnumSet.of(Category.FOOD, Category.UTILITIES)));
    }

    @Test
    void anItemInTwoCategoriesIsKeptByEither() {
        // The milk bucket: Food & Drinks and Utilities.
        Facts milk = plain(Category.FOOD, Category.UTILITIES);
        assertEquals(Verdict.KEEP_CATEGORY, KeepRule.decide(milk, EnumSet.of(Category.UTILITIES)));
        assertEquals(Verdict.KEEP_CATEGORY, KeepRule.decide(milk, EnumSet.of(Category.FOOD)));
        assertEquals(Verdict.DROP_STACKS, KeepRule.decide(milk, NONE));
    }

    // --- what no switch can override -------------------------------------------------------------

    @Test
    void curseBeatsEverything() {
        Facts cursed = new Facts(true, true, true, true, true, true, Set.of(Category.values()));
        Verdict v = KeepRule.decide(cursed, EnumSet.allOf(Category.class));
        assertEquals(Verdict.VANISH_CURSE, v);
        assertFalse(v.kept());
    }

    @Test
    void fullShulkerBoxDropsWhateverIsSwitchedOn() {
        Facts full = new Facts(false, false, true, false, false, false, Set.of(Category.values()));
        assertEquals(Verdict.DROP_HOLDS_ITEMS, KeepRule.decide(full, EnumSet.allOf(Category.class)));
        Facts listed = new Facts(false, true, false, true, true, false, Set.of());
        assertEquals(Verdict.DROP_LIST, KeepRule.decide(listed, EnumSet.allOf(Category.class)));
    }

    @Test
    void durableToolHoldingItemsDrops() {
        Facts backpack = new Facts(false, false, true, false, false, true, Set.of());
        assertEquals(Verdict.DROP_HOLDS_ITEMS, KeepRule.decide(backpack, DEFAULTS));
    }

    // --- every combination: the first rule that answers decides ---------------------------------

    @Test
    void everyCombinationFollowsTheOrder() {
        Category[] extra = {Category.FOOD, Category.ORES, Category.UTILITIES};
        for (int bits = 0; bits < (1 << 6); bits++) {
            boolean curse = (bits & 1) != 0, drop = (bits & 2) != 0, holds = (bits & 4) != 0,
                keep = (bits & 8) != 0, isArmour = (bits & 16) != 0, isTool = (bits & 32) != 0;
            for (int cats = 0; cats < 8; cats++) {
                Set<Category> in = EnumSet.noneOf(Category.class);
                for (int i = 0; i < 3; i++) if ((cats & (1 << i)) != 0) in.add(extra[i]);
                for (int sw = 0; sw < 32; sw++) {
                    Set<Category> on = EnumSet.noneOf(Category.class);
                    for (Category c : Category.values()) if ((sw & (1 << c.ordinal())) != 0) on.add(c);

                    boolean inOn = in.stream().anyMatch(on::contains);
                    Outcome expected = curse ? Outcome.VANISH
                        : drop || holds ? Outcome.DROP
                        : keep ? Outcome.KEEP
                        : isArmour && on.contains(Category.ARMOUR) ? Outcome.KEEP
                        : isTool && on.contains(Category.TOOLS_AND_WEAPONS) ? Outcome.KEEP
                        : inOn ? Outcome.KEEP
                        : Outcome.DROP;

                    Facts facts = new Facts(curse, drop, holds, keep, isArmour, isTool, in);
                    assertEquals(expected, KeepRule.decide(facts, on).outcome, bits + "/" + cats + "/" + sw);
                }
            }
        }
    }

    @Test
    void everyVerdictExplainsItself() {
        for (Verdict v : Verdict.values()) assertFalse(v.reason.isBlank(), v.name());
    }
}
