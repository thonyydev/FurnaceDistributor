package com.thonyy.furnacedistributor.fabric.client;

import com.thonyy.furnacedistributor.client.FurnaceDistributorClient;
import net.fabricmc.api.ClientModInitializer;

public final class FurnaceDistributorFabricClient
        implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FurnaceDistributorClient.init();
        FabricRenderEvents.register();
    }
}