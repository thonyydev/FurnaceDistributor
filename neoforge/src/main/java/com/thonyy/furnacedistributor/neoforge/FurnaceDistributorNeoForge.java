package com.thonyy.furnacedistributor.neoforge;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.neoforge.compat.IronFurnacesIntegration;
import dev.architectury.platform.Platform;
import net.neoforged.fml.common.Mod;

@Mod(FurnaceDistributor.MOD_ID)
public final class FurnaceDistributorNeoForge {

    public FurnaceDistributorNeoForge() {
        if (Platform.isModLoaded("ironfurnaces")) {
            IronFurnacesIntegration.register();
        }
        FurnaceDistributor.init();
    }
}
