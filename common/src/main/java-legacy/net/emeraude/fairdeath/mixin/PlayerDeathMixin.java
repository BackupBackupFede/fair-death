package net.emeraude.fairdeath.mixin;

import net.emeraude.fairdeath.DeathKeeper;
import net.emeraude.fairdeath.compat.Compat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps vanilla's death drop. Vanilla, when keepInventory is off: destroy the vanishing-cursed items,
 * then drop the whole inventory. Before it, the kept items are taken out; after it, they go back.
 *
 * <p>The curse is applied here first, with vanilla's own method: once an item is set aside, vanilla
 * can no longer see it, and a cursed sword would survive.
 */
@Mixin(Player.class)
public abstract class PlayerDeathMixin {

    @Shadow
    protected abstract void destroyVanishingCursedItems();

    @Unique
    private boolean fairdeath$active;
    @Unique
    private DeathKeeper.Kept fairdeath$kept;

    @Inject(method = "dropEquipment()V", at = @At("HEAD"))
    private void fairdeath$beforeDrop(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        if (Compat.keepInventory((ServerLevel) player.level())) return;
        fairdeath$active = true;
        destroyVanishingCursedItems();
        fairdeath$kept = DeathKeeper.take(player);
        DeathKeeper.startDrops();
    }

    @Inject(method = "dropEquipment()V", at = @At("RETURN"))
    private void fairdeath$afterDrop(CallbackInfo ci) {
        if (!fairdeath$active) return;
        fairdeath$active = false;
        DeathKeeper.endDrops();
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (fairdeath$kept != null) {
            DeathKeeper.putBack(player, fairdeath$kept);
            fairdeath$kept = null;
        }
        DeathKeeper.tellWhere(player);
    }
}
