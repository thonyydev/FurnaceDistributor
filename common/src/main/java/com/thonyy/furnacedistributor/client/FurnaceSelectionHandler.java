package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class FurnaceSelectionHandler {

    private static BlockPos firstPos;
    private static BlockPos secondPos;

    // A área confirmada continua disponível após o contorno desaparecer.
    private static BlockPos lastConfirmedFirstPos;
    private static BlockPos lastConfirmedSecondPos;
    private static int lastConfirmedFurnaceCount;

    private static boolean selectionMode = false;

    private static int remainingDisplayTicks = 0;

    public static void handleSelection() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!selectionMode && mc.player.isShiftKeyDown()) {
            if (!hasConfirmedArea()) {
                PlayerFeedback.actionBar(mc.player, Component.translatable("message.furnacedistributor.no_saved_area")
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }
            firstPos = lastConfirmedFirstPos;
            secondPos = lastConfirmedSecondPos;
            remainingDisplayTicks = ClientConfig.get().selectionDisplayTicks;
            Collector.resetCollection();
            SelectionFeedback.confirmed(false, lastConfirmedFurnaceCount);
            Distributor.distributeItems(firstPos, secondPos);
            return;
        }

        HitResult hitResult = mc.hitResult;

        if (
                hitResult == null
                        || hitResult.getType() != HitResult.Type.BLOCK
        ) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.look_at_furnace"
                            )
                            .withStyle(ChatFormatting.RED));

            return;
        }

        BlockPos pos =
                ((BlockHitResult) hitResult).getBlockPos();

        if (
                !(mc.level
                        .getBlockState(pos)
                        .getBlock()
                        instanceof AbstractFurnaceBlock)
        ) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.invalid_block"
                            )
                            .withStyle(ChatFormatting.RED));

            return;
        }

        /*
         * Primeira posição.
         */
        if (!selectionMode) {
            Collector.resetCollection();
            firstPos = pos;
            secondPos = null;

            selectionMode = true;
            remainingDisplayTicks = 0;

            SelectionFeedback.show(Component.translatable(
                                    "message.furnacedistributor.first_selected"
                            )
                            .withStyle(ChatFormatting.GREEN));

            return;
        }

        /*
         * Segunda posição.
         */
        secondPos = pos;
        selectionMode = false;

        int furnaceCount = SelectionPreview.validate(mc, firstPos, secondPos);
        if (furnaceCount < 0) {
            resetSelection();
            return;
        }

        if (furnaceCount == 0) {
            PlayerFeedback.actionBar(mc.player, Component.translatable(
                                    "message.furnacedistributor.no_furnaces"
                            )
                            .withStyle(ChatFormatting.RED));

            resetSelection();
            return;
        }

        SelectionFeedback.show(Component.translatable(
                                "message.furnacedistributor.area_selected",
                                furnaceCount,
                                KeyBindings.COLLECT_KEY.getTranslatedKeyMessage(),
                                mc.options.keyShift.getTranslatedKeyMessage(),
                                KeyBindings.DISTRIBUTE_KEY.getTranslatedKeyMessage()
                        )
                        .withStyle(ChatFormatting.GREEN));

        /*
         * Mantém a seleção visível por aproximadamente
         * 3 segundos após confirmar a segunda posição.
         */
        remainingDisplayTicks = ClientConfig.get().selectionDisplayTicks;

        lastConfirmedFirstPos = firstPos.immutable();
        lastConfirmedSecondPos = secondPos.immutable();
        lastConfirmedFurnaceCount = furnaceCount;
        SelectionFeedback.confirmed(false, furnaceCount);
        Collector.resetCollection();

        Distributor.distributeItems(
                firstPos,
                secondPos
        );
    }

    public static void tick() {
        /*
         * Enquanto ainda estamos esperando a segunda posição,
         * a primeira deve continuar sendo exibida.
         */
        if (selectionMode) {
            return;
        }

        if (firstPos == null || secondPos == null) {
            return;
        }

        if (remainingDisplayTicks > 0) {
            remainingDisplayTicks--;
        }

        if (remainingDisplayTicks <= 0) {
            resetSelection();
        }
    }

    public static void resetSelection() {
        firstPos = null;
        secondPos = null;

        selectionMode = false;
        remainingDisplayTicks = 0;
    }

    public static boolean hasConfirmedArea() {
        return lastConfirmedFirstPos != null
                && lastConfirmedSecondPos != null;
    }

    public static BlockPos getLastConfirmedFirstPos() {
        return lastConfirmedFirstPos;
    }

    public static BlockPos getLastConfirmedSecondPos() {
        return lastConfirmedSecondPos;
    }

    public static void clearConfirmedArea() {
        lastConfirmedFirstPos = null;
        lastConfirmedSecondPos = null;
        lastConfirmedFurnaceCount = 0;
    }

    public static boolean isInSelectionMode() {
        return selectionMode;
    }

    public static BlockPos getFirstPos() {
        return firstPos;
    }

    public static BlockPos getSecondPos() {
        return secondPos;
    }

    public static BlockPos getLookingAtPos(
            Minecraft mc
    ) {
        HitResult hitResult = mc.hitResult;

        if (
                hitResult != null
                        && hitResult.getType()
                        == HitResult.Type.BLOCK
        ) {
            return ((BlockHitResult) hitResult)
                    .getBlockPos();
        }

        return null;
    }

    private FurnaceSelectionHandler() {
    }
}
