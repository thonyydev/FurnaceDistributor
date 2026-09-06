package com.thonyy.furnacedistributor;

import com.thonyy.furnacedistributor.network.ModNetworking;

public final class FurnaceDistributor {

    public static final String MOD_ID = "furnacedistributor";

    public static void init() {
        ModNetworking.register();
    }

    private FurnaceDistributor() {
    }
}