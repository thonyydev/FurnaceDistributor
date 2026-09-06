package com.thonyy.furnacedistributor.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class FurnaceSelectionHandler {

    private static final int MAX_FURNACES = 64;

    // Minecraft roda normalmente a 20 ticks por segundo.
    // 60 ticks = aproximadamente 3 segundos.
    private static final int DISPLAY_TICKS = 60;

    private static BlockPos firstPos;
    private static BlockPos secondPos;

    private static boolean selectionMode = false;

    private static int remainingDisplayTicks = 0;

    public static void handleSelection() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        HitResult hitResult = mc.hitResult;

        if (
                hitResult == null
                        || hitResult.getType() != HitResult.Type.BLOCK
        ) {
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.look_at_furnace"
                            )
                            .withStyle(ChatFormatting.RED),
                    true
            );

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
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.invalid_block"
                            )
                            .withStyle(ChatFormatting.RED),
                    true
            );

            return;
        }

        /*
         * Primeira posição.
         */
        if (!selectionMode) {
            firstPos = pos;
            secondPos = null;

            selectionMode = true;
            remainingDisplayTicks = 0;

            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.first_selected"
                            )
                            .withStyle(ChatFormatting.GREEN),
                    false
            );

            return;
        }

        /*
         * Segunda posição.
         */
        secondPos = pos;
        selectionMode = false;

        int furnaceCount = countFurnaces(mc);

        if (furnaceCount == 0) {
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.no_furnaces"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            resetSelection();
            return;
        }

        if (furnaceCount > MAX_FURNACES) {
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.too_many_furnaces",
                                    MAX_FURNACES
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            resetSelection();
            return;
        }

        mc.player.displayClientMessage(
                Component.translatable(
                                "message.furnacedistributor.area_selected",
                                furnaceCount
                        )
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        /*
         * Mantém a seleção visível por aproximadamente
         * 3 segundos após confirmar a segunda posição.
         */
        remainingDisplayTicks = DISPLAY_TICKS;

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

    private static int countFurnaces(
            Minecraft mc
    ) {
        if (
                firstPos == null
                        || secondPos == null
                        || mc.level == null
        ) {
            return 0;
        }

        int minX = Math.min(
                firstPos.getX(),
                secondPos.getX()
        );

        int minY = Math.min(
                firstPos.getY(),
                secondPos.getY()
        );

        int minZ = Math.min(
                firstPos.getZ(),
                secondPos.getZ()
        );

        int maxX = Math.max(
                firstPos.getX(),
                secondPos.getX()
        );

        int maxY = Math.max(
                firstPos.getY(),
                secondPos.getY()
        );

        int maxZ = Math.max(
                firstPos.getZ(),
                secondPos.getZ()
        );

        int count = 0;

        for (
                BlockPos pos :
                BlockPos.betweenClosed(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )
        ) {
            if (
                    mc.level
                            .getBlockState(pos)
                            .getBlock()
                            instanceof AbstractFurnaceBlock
            ) {
                count++;
            }
        }

        return count;
    }

    public static void resetSelection() {
        firstPos = null;
        secondPos = null;

        selectionMode = false;
        remainingDisplayTicks = 0;
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