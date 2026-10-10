package com.thonyy.furnacedistributor.furnace;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

/** One physical furnace, with any number of processing slots. Never cached across operations. */
public record FurnaceTarget(BlockEntity entity, FurnaceAccess access) {
    public Container inventory() { return access.inventory(entity); }
}
