package com.thonyy.furnacedistributor.client;

public final class FurnaceDistributorClient {

    public static void init() {
        ClientConfig.load();
        KeyBindings.register();
        ClientEvents.register();
        SelectionHud.register();
    }

    private FurnaceDistributorClient() {
    }
}
