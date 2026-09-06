package com.thonyy.furnacedistributor.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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

        while (KeyBindings.CANCEL_KEY.consumeClick()) {
            handleCancel(minecraft);
        }
    }

    private static void handleCancel(Minecraft minecraft) {
        boolean hasSelection =
                FurnaceSelectionHandler.isInSelectionMode()
                        || Collector.isInCollectionMode();

        if (!hasSelection) {
            return;
        }

        FurnaceSelectionHandler.resetSelection();
        Collector.resetCollection();

        minecraft.player.displayClientMessage(
                Component.translatable(
                                "message.furnacedistributor.selection_cancelled"
                        )
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }

    private ClientEvents() {
    }
}