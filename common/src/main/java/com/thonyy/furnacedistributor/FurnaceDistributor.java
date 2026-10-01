package com.thonyy.furnacedistributor;

import com.thonyy.furnacedistributor.network.ModNetworking;
import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.logic.OperationGuard;

public final class FurnaceDistributor {

    public static final String MOD_ID = "furnacedistributor";

    public static void init() {
        ServerConfig.load();
        OperationGuard.register();
        ModNetworking.register();
    }

    private FurnaceDistributor() {
    }
}
