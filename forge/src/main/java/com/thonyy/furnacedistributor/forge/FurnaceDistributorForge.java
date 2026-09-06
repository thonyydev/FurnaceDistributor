package com.thonyy.furnacedistributor.forge;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.FurnaceDistributorClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(FurnaceDistributor.MOD_ID)
public final class FurnaceDistributorForge {

    public FurnaceDistributorForge() {
        FurnaceDistributor.init();

        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> FurnaceDistributorClient::init
        );
    }
}