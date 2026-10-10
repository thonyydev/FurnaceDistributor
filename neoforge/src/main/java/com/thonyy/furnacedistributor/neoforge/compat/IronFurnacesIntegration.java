package com.thonyy.furnacedistributor.neoforge.compat;

import com.thonyy.furnacedistributor.furnace.FurnaceAccessRegistry;

/** Loaded only inside the NeoForge entrypoint's mod-present branch. */
public final class IronFurnacesIntegration {
    public static void register() {
        FurnaceAccessRegistry.register(new IronFurnacesAccess());
    }

    private IronFurnacesIntegration() { }
}
