package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.network.CollectPacket;
import com.thonyy.furnacedistributor.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class Collector {

    private static BlockPos firstPos = null;
    private static BlockPos secondPos = null;

    private static final int DISPLAY_TICKS = 60;
    private static int remainingDisplayTicks = 0;

    private static boolean collectionMode = false;

    private static final int MAX_FURNACES = 64;

    public static void handleCollection() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        // A coleta da área salva não depende do bloco sob a mira.
        if (FurnaceSelectionHandler.hasConfirmedArea()) {
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
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collect_look_at_furnace"
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
                                    "message.furnacedistributor.collect_invalid_block"
                            )
                            .withStyle(ChatFormatting.RED),
                    true
            );

            return;
        }

        /*
         * Primeira seleção.
         */
        if (!collectionMode) {
            firstPos = pos;
            secondPos = null;

            collectionMode = true;
            remainingDisplayTicks = 0;

            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collect_first_selected"
                            )
                            .withStyle(ChatFormatting.AQUA),
                    false
            );

            return;
        }

        /*
         * Segunda seleção.
         */
        secondPos = pos;
        collectionMode = false;

        remainingDisplayTicks = DISPLAY_TICKS;

        int furnaceCount = countFurnaces(mc);

        if (furnaceCount == 0) {
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collect_no_furnaces"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            resetCollection();
            return;
        }

        if (furnaceCount > MAX_FURNACES) {
            mc.player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collect_too_many_furnaces",
                                    MAX_FURNACES
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            resetCollection();
            return;
        }

        mc.player.displayClientMessage(
                Component.translatable(
                                "message.furnacedistributor.collecting",
                                furnaceCount
                        )
                        .withStyle(ChatFormatting.AQUA),
                false
        );

        ModNetworking.sendCollect(
                new CollectPacket(
                        firstPos,
                        secondPos
                )
        );
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

    public static void tick() {
        if (collectionMode) {
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
