package com.thonyy.furnacedistributor.furnace;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Registered during platform initialization. Slot/mode decisions are evaluated per operation. */
public interface FurnaceAccess {
    /** Type predicate, independent of mutable mode/tier/slots: the registry caches by entity class. */
    boolean supports(BlockEntity entity);
    Container inventory(BlockEntity entity);
    /** Unique candidate slots; canInsert must reject currently inactive processing slots. */
    int[] inputSlots(BlockEntity entity, ItemStack stack, boolean vanillaFuel);
    /** Unique, valid output indices, including any retrievable output left by a mode change. */
    int[] outputSlots(BlockEntity entity);
    boolean canInsert(BlockEntity entity, int slot, ItemStack stack, FurnaceRecipes recipes);
    /** Container limit, optionally narrowed by the destination's own insertion contract. */
    default int insertionLimit(BlockEntity entity, int slot, ItemStack stack) {
        return inventory(entity).getMaxStackSize(stack);
    }
    boolean canAccess(BlockEntity entity, ServerPlayer player);
    void awardExperience(BlockEntity entity, ServerPlayer player);
}
