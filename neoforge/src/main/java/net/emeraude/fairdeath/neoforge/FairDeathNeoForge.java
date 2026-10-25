package net.emeraude.fairdeath.neoforge;

import net.emeraude.fairdeath.FairDeath;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/** NeoForge entry point (both sides). All logic is loader-agnostic in {@link FairDeath}. */
@Mod(FairDeath.MOD_ID)
public final class FairDeathNeoForge {

    public FairDeathNeoForge(IEventBus modBus, ModContainer container) {
        FairDeath.init(FMLPaths.CONFIGDIR.get());
    }
}
