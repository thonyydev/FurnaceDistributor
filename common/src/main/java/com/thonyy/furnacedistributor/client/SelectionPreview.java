package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.logic.AreaBounds;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractFurnaceBlock;

/** World-position preview only: server inventories and permissions remain authoritative. */
public final class SelectionPreview {
    private static ClientLevel level;
    private static BlockPos first;
    private static BlockPos second;
    private static long expires;
    private static FurnaceArea.Scan cached;

    public static boolean isSupportedFurnace(Minecraft minecraft, BlockPos pos) {
        return minecraft.level != null && pos != null
                && !minecraft.level.isOutsideBuildHeight(pos)
                && minecraft.level.getWorldBorder().isWithinBounds(pos)
                && minecraft.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && minecraft.level.getBlockState(pos).getBlock() instanceof AbstractFurnaceBlock;
    }

    public static FurnaceArea.Scan get(Minecraft minecraft, BlockPos pos1, BlockPos pos2) {
        long now = minecraft.level.getGameTime();
        if (level != minecraft.level || !pos1.equals(first) || !pos2.equals(second)
                || cached == null || now >= expires || now < expires - 10) {
            level = minecraft.level;
            first = pos1.immutable();
            second = pos2.immutable();
            cached = FurnaceArea.scan(level, first, second);
            expires = now + 10;
        }
        return cached;
    }

    public static int validate(Minecraft minecraft, BlockPos pos1, BlockPos pos2) {
        // Confirmation always rescans; a cached visual must not approve a stale selection.
        FurnaceArea.Scan scan = FurnaceArea.scan(minecraft.level, pos1, pos2);
        if (!scan.valid()) {
            PlayerFeedback.actionBar(minecraft.player, Component.translatable("message.furnacedistributor." + scan.error(),
                    scan.error().equals("area_too_large") ? AreaBounds.MAX_VOLUME : AreaBounds.MAX_FURNACES)
                    .withStyle(ChatFormatting.RED));
            return -1;
        }
        return scan.positions().size();
    }

    public static void clear() {
        level = null;
        first = second = null;
        cached = null;
    }

    private SelectionPreview() { }
}
