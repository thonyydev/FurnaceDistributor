package com.thonyy.furnacedistributor.fabric;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import net.fabricmc.api.ModInitializer;

public final class FurnaceDistributorFabric
        implements ModInitializer {

    @Override
    public void onInitialize() {
        FurnaceDistributor.init();
    }
}