package net.emeraude.fairdeath.mixin;

import net.emeraude.fairdeath.DeathKeeper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes a dying player's drops glow. The drop paths differ on every line (Player.drop, then
 * createItemStackToDrop, armour through EntityEquipment) and NeoForge holds the entities back for
 * its drops event, but all of them build the entity with this one constructor.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void fairdeath$glow(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        DeathKeeper.onDropBuilt((ItemEntity) (Object) this);
    }
}
