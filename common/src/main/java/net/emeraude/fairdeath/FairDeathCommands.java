package net.emeraude.fairdeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.emeraude.fairdeath.compat.Compat;
import net.emeraude.fairdeath.core.KeepRule;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** {@code /fairdeath check} for everyone, {@code /fairdeath reload} for operators. */
public final class FairDeathCommands {

    private static final int OPERATOR = 2;

    private FairDeathCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // The Commands constructor runs more than once (startup validation, /reload).
        if (dispatcher.getRoot().getChild(FairDeath.MOD_ID) != null) return;
        dispatcher.register(Commands.literal(FairDeath.MOD_ID)
            .then(Commands.literal("check").executes(FairDeathCommands::check))
            .then(Commands.literal("reload")
                .requires(source -> Compat.hasPermission(source, OPERATOR))
                .executes(FairDeathCommands::reload))
            .then(Commands.literal("settings")
                .requires(source -> Compat.hasPermission(source, OPERATOR))
                .executes(FairDeathCommands::settings))
            // Debug only, and operators only: the debug file can be left behind on a server.
            .then(Commands.literal("kit")
                .requires(source -> FairDeath.debug() && Compat.hasPermission(source, OPERATOR))
                .executes(FairDeathCommands::kit)));
    }

    private static int kit(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        TestKit.give(player.getInventory(), (ServerLevel) player.level());
        player.sendSystemMessage(info("Test kit given."));
        return 1;
    }

    private static int check(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSystemMessage(error("Only a player can check the item in their hand."));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(error("Hold an item in your main hand first."));
            return 0;
        }
        String name = stack.getHoverName().getString();
        if (Compat.keepInventory((ServerLevel) player.level())) {
            player.sendSystemMessage(info(name + ": kept (keepInventory is on)"));
            return 1;
        }
        KeepRule.Verdict verdict = DeathKeeper.verdict(stack, source.getServer());
        String outcome = switch (verdict.outcome) {
            case KEEP -> "kept";
            case DROP -> "drops";
            case VANISH -> "vanishes";
        };
        String reason = verdict.reason;
        if (verdict == KeepRule.Verdict.KEEP_CATEGORY) {
            reason = String.join(", ", DeathKeeper.categoriesOf(stack, source.getServer()).stream()
                .filter(FairDeath.config().kept::get).map(c -> c.title).toList()) + " is kept";
        }
        player.sendSystemMessage(info(name + ": " + outcome + " (" + reason + ")"));
        return 1;
    }

    private static int settings(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendSystemMessage(error("Open this as a player, or edit config/fairdeath.json."));
            return 0;
        }
        SettingsMenu.open(player);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        String failure = FairDeath.reloadConfig();
        ctx.getSource().sendSystemMessage(failure == null
            ? info("Fair Death config reloaded.")
            : error("Config not reloaded: " + failure));
        return failure == null ? 1 : 0;
    }

    private static Component info(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static Component error(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }
}
