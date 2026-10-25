package net.emeraude.fairdeath.fabric;

import net.emeraude.fairdeath.FairDeath;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry point (both sides). All logic is loader-agnostic in {@link FairDeath}. */
public final class FairDeathFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FairDeath.init(FabricLoader.getInstance().getConfigDir());
    }
}
