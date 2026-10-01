package com.thonyy.furnacedistributor.neoforge.client;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.FurnaceDistributorClient;
import com.thonyy.furnacedistributor.client.DistributorSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FurnaceDistributor.MOD_ID, dist = Dist.CLIENT)
public final class FurnaceDistributorNeoForgeClient {

    public FurnaceDistributorNeoForgeClient(ModContainer modContainer) {
        FurnaceDistributorClient.init();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new DistributorSettingsScreen(parent));
    }
}
