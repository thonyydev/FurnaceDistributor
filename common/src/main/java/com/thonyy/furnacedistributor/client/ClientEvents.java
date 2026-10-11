package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
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
            SelectionHud.reset();
            previousLevel = minecraft.level;
            previousPlayer = minecraft.player;
        }

        SelectionHud.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            drainKeys();
            return;
        }

        FurnaceSelectionHandler.tick();
        Collector.tick();
        SelectionFeedback.tick();

        if (minecraft.gui.screen() != null || !minecraft.isWindowActive() || !minecraft.player.isAlive()
                || minecraft.player.isSpectator()) {
            drainKeys();
            return;
        }

        if (consumeClicks(KeyBindings.SETTINGS_KEY)) {
            drainKeys();
            minecraft.gui.setScreen(new DistributorSettingsScreen());
            return;
        }

        // Cancel wins over action keys queued in the same tick; never collect while cancelling.
        if (consumeClicks(KeyBindings.CANCEL_KEY)) {
            drainKeys();
            handleCancel(minecraft);
            return;
        }

        if (consumeClicks(KeyBindings.DISTRIBUTE_KEY)) {
            FurnaceSelectionHandler.handleSelection();
        }

        if (consumeClicks(KeyBindings.COLLECT_KEY)) {
            Collector.handleCollection();
        }
    }

    private static void drainKeys() {
        consumeClicks(KeyBindings.DISTRIBUTE_KEY);
        consumeClicks(KeyBindings.COLLECT_KEY);
        consumeClicks(KeyBindings.CANCEL_KEY);
        consumeClicks(KeyBindings.SETTINGS_KEY);
    }

    private static boolean consumeClicks(KeyMapping key) {
        boolean clicked = key.consumeClick();
        // One action per key/tick; queued repeats cannot start and confirm the same selection.
        while (key.consumeClick()) { }
        return clicked;
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
