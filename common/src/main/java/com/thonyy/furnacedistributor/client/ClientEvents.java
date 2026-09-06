package com.thonyy.furnacedistributor.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public final class ClientEvents {

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ClientEvents::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        FurnaceSelectionHandler.tick();
        Collector.tick();

        while (KeyBindings.DISTRIBUTE_KEY.consumeClick()) {
            FurnaceSelectionHandler.handleSelection();
        }

        while (KeyBindings.COLLECT_KEY.consumeClick()) {
            Collector.handleCollection();
        }
    }

    private ClientEvents() {
    }
}