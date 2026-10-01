package com.thonyy.furnacedistributor.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;

import java.util.ArrayList;
import java.util.List;

public final class FurnaceArea {
    public record Scan(List<BlockPos> positions, String error) {
        public boolean valid() { return error == null; }
    }

    public static AreaBounds bounds(BlockPos first, BlockPos second) {
        return AreaBounds.between(first.getX(), first.getY(), first.getZ(),
                second.getX(), second.getY(), second.getZ());
    }

    public static Scan scan(Level level, BlockPos first, BlockPos second) {
        AreaBounds area = bounds(first, second);
        if (!area.isWithinVolumeLimit()) {
            return new Scan(List.of(), "area_too_large");
        }
        if (area.minY() < level.getMinBuildHeight() || area.maxY() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(new BlockPos(area.minX(), area.minY(), area.minZ()))
                || !level.getWorldBorder().isWithinBounds(new BlockPos(area.maxX(), area.maxY(), area.maxZ()))) {
            return new Scan(List.of(), "area_outside_world");
        }
        // Check chunks once before scanning; never cause a chunk load on either side.
        for (int x = area.minX() >> 4; x <= area.maxX() >> 4; x++) {
            for (int z = area.minZ() >> 4; z <= area.maxZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return new Scan(List.of(), "area_unloaded");
                }
            }
        }
        List<BlockPos> furnaces = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(),
                area.maxX(), area.maxY(), area.maxZ())) {
            if (level.getBlockState(pos).getBlock() instanceof AbstractFurnaceBlock) {
                if (furnaces.size() == AreaBounds.MAX_FURNACES) {
                    return new Scan(List.of(), "too_many_furnaces");
                }
                furnaces.add(pos.immutable());
            }
        }
        return new Scan(List.copyOf(furnaces), null);
    }

    private FurnaceArea() { }
}
