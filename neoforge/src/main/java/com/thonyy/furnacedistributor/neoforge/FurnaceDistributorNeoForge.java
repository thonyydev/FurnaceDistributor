package com.thonyy.furnacedistributor.neoforge;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import net.neoforged.fml.common.Mod;

@Mod(FurnaceDistributor.MOD_ID)
public final class FurnaceDistributorNeoForge {

    public FurnaceDistributorNeoForge() {
        FurnaceDistributor.init();
    }
}
