package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ClientEvents {

    private static ClientLevel previousLevel;
    private static LocalPlayer previousPlayer;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ClientEvents::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        // Verifica antes do retorno: o tick sem mundo também limpa a sessão.
        // Uma nova instância inclui troca de dimensão e reconexão ao mesmo mundo.
        if (minecraft.level != previousLevel || minecraft.player != previousPlayer
                || (minecraft.player != null && (!minecraft.player.isAlive() || minecraft.player.isSpectator()))) {
            FurnaceSelectionHandler.resetSelection();
            FurnaceSelectionHandler.clearConfirmedArea();
            Collector.resetCollection();
            SelectionPreview.clear();
            SelectionFeedback.clear();
            previousLevel = minecraft.level;
            previousPlayer = minecraft.player;
        }

        if (minecraft.player == null || minecraft.level == null) {
            drainKeys();
            return;
        }

        FurnaceSelectionHandler.tick();
        Collector.tick();
        SelectionFeedback.tick();

        if (minecraft.screen != null || !minecraft.isWindowActive() || !minecraft.player.isAlive()
                || minecraft.player.isSpectator()) {
            drainKeys();
            return;
        }

        if (KeyBindings.SETTINGS_KEY.consumeClick()) {
            drainKeys();
            minecraft.setScreen(new DistributorSettingsScreen());
            return;
        }

        // Cancel wins over action keys queued in the same tick; never collect while cancelling.
        if (KeyBindings.CANCEL_KEY.consumeClick()) {
            drainKeys();
            handleCancel(minecraft);
            return;
        }

        while (KeyBindings.DISTRIBUTE_KEY.consumeClick()) {
            FurnaceSelectionHandler.handleSelection();
        }

        if (KeyBindings.COLLECT_KEY.consumeClick()) {
            // Multiple queued clicks must not start and confirm a new selection in one tick.
            while (KeyBindings.COLLECT_KEY.consumeClick()) { }
            Collector.handleCollection();
        }
    }

    private static void drainKeys() {
        while (KeyBindings.DISTRIBUTE_KEY.consumeClick()) { }
        while (KeyBindings.COLLECT_KEY.consumeClick()) { }
        while (KeyBindings.CANCEL_KEY.consumeClick()) { }
        while (KeyBindings.SETTINGS_KEY.consumeClick()) { }
    }

    private static void handleCancel(Minecraft minecraft) {
        boolean hasSelection =
                FurnaceSelectionHandler.isInSelectionMode()
                        || FurnaceSelectionHandler.hasConfirmedArea()
                        || Collector.getFirstCollectPos() != null;

        if (!hasSelection) {
            return;
        }

        FurnaceSelectionHandler.resetSelection();
        FurnaceSelectionHandler.clearConfirmedArea();
        Collector.resetCollection();
        SelectionPreview.clear();
        SelectionFeedback.clear();

        PlayerFeedback.actionBar(minecraft.player, Component.translatable(
                                "message.furnacedistributor.selection_cancelled"
                        )
                        .withStyle(ChatFormatting.YELLOW));
    }

    private ClientEvents() {
    }
}
