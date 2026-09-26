package com.thonyy.furnacedistributor.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

public final class ClientEvents {

    private static ClientLevel previousLevel;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ClientEvents::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        // Verifica antes do retorno: o tick sem mundo também limpa a sessão.
        // Uma nova instância inclui troca de dimensão e reconexão ao mesmo mundo.
        if (minecraft.level != previousLevel) {
            FurnaceSelectionHandler.resetSelection();
            FurnaceSelectionHandler.clearConfirmedArea();
            Collector.resetCollection();
            previousLevel = minecraft.level;
        }

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
                        || FurnaceSelectionHandler.hasConfirmedArea()
                        || Collector.isInCollectionMode();

        if (!hasSelection) {
            return;
        }

        FurnaceSelectionHandler.resetSelection();
        FurnaceSelectionHandler.clearConfirmedArea();
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
