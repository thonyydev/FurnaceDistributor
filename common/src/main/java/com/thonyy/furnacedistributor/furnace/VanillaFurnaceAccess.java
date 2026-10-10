package com.thonyy.furnacedistributor.furnace;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

public final class VanillaFurnaceAccess implements FurnaceAccess {
    @Override public boolean supports(BlockEntity entity) { return entity instanceof AbstractFurnaceBlockEntity; }
    @Override public Container inventory(BlockEntity entity) { return (AbstractFurnaceBlockEntity) entity; }
    @Override public int[] inputSlots(BlockEntity entity, ItemStack stack, boolean fuel) { return new int[]{fuel ? 1 : 0}; }
    @Override public int[] outputSlots(BlockEntity entity) { return new int[]{2}; }

    @Override
    public boolean canInsert(BlockEntity entity, int slot, ItemStack stack, FurnaceRecipes recipes) {
        return (slot == 1 || recipes.has(entity instanceof BlastFurnaceBlockEntity ? RecipeType.BLASTING
                : entity instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING))
                && inventory(entity).canPlaceItem(slot, stack);
    }

    @Override public boolean canAccess(BlockEntity entity, ServerPlayer player) {
        return ((AbstractFurnaceBlockEntity) entity).canOpen(player);
    }
    @Override public void awardExperience(BlockEntity entity, ServerPlayer player) {
        ((AbstractFurnaceBlockEntity) entity).awardUsedRecipesAndPopExperience(player);
    }
}
