package com.thonyy.furnacedistributor.furnace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class FurnaceAccessRegistry {
    private static volatile List<FurnaceAccess> adapters = List.of(new VanillaFurnaceAccess());
    private static final Map<Class<?>, Optional<FurnaceAccess>> TYPES = new ConcurrentHashMap<>();

    /** Platform startup only. Specific integrations take precedence over the vanilla fallback. */
    public static synchronized void register(FurnaceAccess access) {
        List<FurnaceAccess> updated = new ArrayList<>(adapters);
        updated.addFirst(access);
        adapters = List.copyOf(updated);
        TYPES.clear();
    }

    public static FurnaceTarget find(BlockEntity entity) {
        if (entity == null || entity.isRemoved()) return null;
        FurnaceAccess access = TYPES.computeIfAbsent(entity.getClass(), ignored ->
                adapters.stream().filter(adapter -> adapter.supports(entity)).findFirst()).orElse(null);
        return access == null ? null : new FurnaceTarget(entity, access);
    }

    /** Caller checks loaded chunks first. Keeps the existing vanilla block detection unchanged. */
    public static boolean isFurnace(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof AbstractFurnaceBlock
                || (state.hasBlockEntity() && find(level.getBlockEntity(pos)) != null);
    }

    private FurnaceAccessRegistry() { }
}
