package net.emeraude.fairdeath.mixin;

import net.emeraude.fairdeath.DeathKeeper;
import net.emeraude.fairdeath.compat.Compat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On respawn vanilla copies the inventory only on the way back from the End, with keepInventory, or
 * for a spectator. In every other case, carry over what the dead player still holds: the kept items.
 * Same signature on every line.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("RETURN"))
    private void fairdeath$restoreKept(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (restoreAll || oldPlayer.isSpectator() || Compat.keepInventory((ServerLevel) self.level())) return;
        DeathKeeper.restore(oldPlayer, self);
    }
}
