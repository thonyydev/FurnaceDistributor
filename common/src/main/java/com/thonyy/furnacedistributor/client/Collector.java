package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.network.CollectPacket;
import com.thonyy.furnacedistributor.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class Collector {

    private static BlockPos firstPos = null;
    private static BlockPos secondPos = null;

    private static int remainingDisplayTicks = 0;

    private static boolean collectionMode = false;

    public static void handleCollection() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || mc.screen != null || !mc.isWindowActive()
                || !mc.player.isAlive() || mc.player.isSpectator()) {
            return;
        }

        BlockPos target = FurnaceSelectionHandler.getLookingAtPos(mc);
        boolean furnaceTarget = SelectionPreview.isSupportedFurnace(mc, target);
        CollectionShortcut.Action action = CollectionShortcut.resolve(collectionMode,
                FurnaceSelectionHandler.hasConfirmedArea(), mc.player.isShiftKeyDown(), furnaceTarget);

        // A seleção pendente e o atalho contextual têm prioridade, sem substituir a área salva.
        if (action == CollectionShortcut.Action.COLLECT_SAVED) {
            ModNetworking.sendCollect(
                    new CollectPacket(
                            FurnaceSelectionHandler.getLastConfirmedFirstPos(),
                            FurnaceSelectionHandler.getLastConfirmedSecondPos()
                    )
            );

            return;
        }

        HitResult hitResult = mc.hitResult;

        if (
                hitResult == null
                        || hitResult.getType() != HitResult.Type.BLOCK
        ) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.collect_look_at_furnace"
                            )
                            .withStyle(ChatFormatting.RED));

            return;
        }

        BlockPos pos =
                ((BlockHitResult) hitResult).getBlockPos();

        if (!furnaceTarget) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.collect_invalid_block"
                            )
                            .withStyle(ChatFormatting.RED));

            return;
        }

        /*
         * Primeira seleção.
         */
        if (action == CollectionShortcut.Action.SELECT_FIRST) {
            FurnaceSelectionHandler.resetSelection();
            SelectionPreview.clear();
            firstPos = pos.immutable();
            secondPos = null;

            collectionMode = true;
            remainingDisplayTicks = 0;

            SelectionFeedback.clear();
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                            "message.furnacedistributor.collect_selection_started",
                            KeyBindings.COLLECT_KEY.getTranslatedKeyMessage(),
                            KeyBindings.CANCEL_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            return;
        }

        /*
         * Segunda seleção.
         */
        secondPos = pos.immutable();
        collectionMode = false;

        remainingDisplayTicks = ClientConfig.get().selectionDisplayTicks;

        int furnaceCount = SelectionPreview.validate(mc, firstPos, secondPos);
        if (furnaceCount < 0) {
            resetCollection();
            return;
        }

        if (furnaceCount == 0) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.collect_no_furnaces"
                            )
                            .withStyle(ChatFormatting.RED));

            resetCollection();
            return;
        }

        SelectionFeedback.show(Component.translatable(
                                "message.furnacedistributor.collecting",
                                furnaceCount
                        )
                        .withStyle(ChatFormatting.AQUA));
        SelectionFeedback.confirmed(true, furnaceCount);

        ModNetworking.sendCollect(
                new CollectPacket(
                        firstPos,
                        secondPos
                )
        );
    }

    public static void tick() {
        if (collectionMode) {
            Minecraft mc = Minecraft.getInstance();
            if (!SelectionPreview.isSupportedFurnace(mc, firstPos)) {
                resetCollection();
                SelectionPreview.clear();
                SelectionFeedback.clear();
                if (mc.player != null) {
                    PlayerFeedback.actionBar(mc.player, Component.translatable(
                            "message.furnacedistributor.collect_selection_lost").withStyle(ChatFormatting.YELLOW));
                }
            }
            return;
        }

        if (firstPos == null || secondPos == null) {
            return;
        }

        if (remainingDisplayTicks > 0) {
            remainingDisplayTicks--;
        }

        if (remainingDisplayTicks <= 0) {
            resetCollection();
        }
    }

    public static void resetCollection() {
        firstPos = null;
        secondPos = null;
        collectionMode = false;
        remainingDisplayTicks = 0;
    }

    public static boolean isInCollectionMode() {
        return collectionMode;
    }

    public static BlockPos getFirstCollectPos() {
        return firstPos;
    }

    public static BlockPos getSecondCollectPos() {
        return secondPos;
    }

    private Collector() {
    }
}
