package com.thonyy.furnacedistributor.client;

public final class FurnaceDistributorClient {

    public static void init() {
        KeyBindings.register();
        ClientEvents.register();
    }

    private FurnaceDistributorClient() {
    }
}