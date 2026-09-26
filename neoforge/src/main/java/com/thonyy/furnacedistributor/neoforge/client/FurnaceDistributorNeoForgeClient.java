package com.thonyy.furnacedistributor.neoforge.client;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.FurnaceDistributorClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = FurnaceDistributor.MOD_ID, dist = Dist.CLIENT)
public final class FurnaceDistributorNeoForgeClient {

    public FurnaceDistributorNeoForgeClient() {
        FurnaceDistributorClient.init();
    }
}
