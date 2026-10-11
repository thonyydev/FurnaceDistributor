package com.thonyy.furnacedistributor.neoforge.compat;

import com.thonyy.furnacedistributor.furnace.FurnaceAccess;
import com.thonyy.furnacedistributor.furnace.FurnaceRecipes;
import ironfurnaces.items.augments.ItemAugmentBlasting;
import ironfurnaces.items.augments.ItemAugmentFactory;
import ironfurnaces.items.augments.ItemAugmentGenerator;
import ironfurnaces.items.augments.ItemAugmentSmoking;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Astryxion Iron Furnaces NeoForge 26.2 / 1.0.0. No tier-specific registry checks or reflection. */
public final class IronFurnacesAccess implements FurnaceAccess {
    @Override public boolean supports(BlockEntity entity) { return entity instanceof BlockIronFurnaceTileBase; }
    @Override public Container inventory(BlockEntity entity) { return (BlockIronFurnaceTileBase) entity; }

    @Override
    public int[] inputSlots(BlockEntity entity, ItemStack stack, boolean vanillaFuel) {
        BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase) entity;
        if (!augmentsSynchronized(furnace)) return new int[0];
        if (furnace.isGenerator()) return new int[]{BlockIronFurnaceTileBase.GENERATOR_FUEL};
        if (furnace.isFactory()) {
            // The Container contract filters these candidates to the tier's 2, 4 or 6 active inputs.
            return BlockIronFurnaceTileBase.FACTORY_INPUT.clone();
        }
        if (!furnace.isFurnace()) return new int[0];
        boolean fuel = vanillaFuel || BlockIronFurnaceTileBase.isItemFuel(stack, furnace.recipeType, furnace.getLevel());
        return new int[]{fuel ? BlockIronFurnaceTileBase.FUEL : BlockIronFurnaceTileBase.INPUT};
    }

    @Override
    public boolean canInsert(BlockEntity entity, int slot, ItemStack stack, FurnaceRecipes recipes) {
        BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase) entity;
        if (!augmentsSynchronized(furnace)) return false;
        if (furnace.isGenerator()) {
            // The Container contract includes Smoking/Blasting generator rules and NeoForge fuel hooks.
            // isItemFuel also accepts ItemHeater, and is therefore not a generator validator.
            return slot == BlockIronFurnaceTileBase.GENERATOR_FUEL && furnace.canPlaceItem(slot, stack);
        }
        boolean active = furnace.isFactory()
                ? java.util.Arrays.stream(BlockIronFurnaceTileBase.FACTORY_INPUT).anyMatch(input -> input == slot)
                : furnace.isFurnace() && (slot == BlockIronFurnaceTileBase.INPUT || slot == BlockIronFurnaceTileBase.FUEL);
        // hasRecipe in Iron caches by Item; also query the recipe manager with the exact components.
        return active && (slot == BlockIronFurnaceTileBase.FUEL || recipes.has(furnace.recipeType))
                && furnace.canPlaceItem(slot, stack);
    }

    @Override
    public int insertionLimit(BlockEntity entity, int slot, ItemStack stack) {
        BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase) entity;
        int limit = FurnaceAccess.super.insertionLimit(entity, slot, stack);
        // SlotIronFurnaceInputGenerator.safeInsert accepts one such fuel: the generator tick replaces
        // the entire fuel stack with its crafting remainder, instead of shrinking one item first.
        return furnace.isGenerator() && slot == BlockIronFurnaceTileBase.GENERATOR_FUEL
                && stack.getCraftingRemainder() != null ? Math.min(1, limit) : limit;
    }

    private static boolean augmentsSynchronized(BlockIronFurnaceTileBase furnace) {
        // isGenerator/isFactory read currentAugment, updated by Iron on the next tick. Reject a pending
        // blue/red change: that tick can eject all inventory contents. Never update Iron's state ourselves.
        var blue = furnace.getItem(BlockIronFurnaceTileBase.AUGMENT_BLUE).getItem();
        var red = furnace.getItem(BlockIronFurnaceTileBase.AUGMENT_RED).getItem();
        int mode = blue instanceof ItemAugmentGenerator ? 2 : blue instanceof ItemAugmentFactory ? 1 : 0;
        int recipe = red instanceof ItemAugmentBlasting ? 1 : red instanceof ItemAugmentSmoking ? 2 : 0;
        return furnace.currentAugment[2] == mode && furnace.currentAugment[0] == recipe;
    }

    @Override
    public int[] outputSlots(BlockEntity entity) {
        int[] inputs = BlockIronFurnaceTileBase.FACTORY_INPUT;
        int[] outputs = new int[inputs.length + 1];
        outputs[0] = BlockIronFurnaceTileBase.OUTPUT;
        // The 26.2 port's canFactorySmelt/factorySmelt/autoFactoryIO still pair input with input + 6.
        // There is no public FACTORY_OUTPUT or pairing accessor. The offset is the input-array length.
        // Include inactive outputs: a tier/augment change must not strand already produced items.
        for (int i = 0; i < inputs.length; i++) outputs[i + 1] = inputs[i] + inputs.length;
        return outputs;
    }

    @Override
    public boolean canAccess(BlockEntity entity, ServerPlayer player) {
        // Iron has no lock/canOpen API. stillValid adds the menu's 8-block limit; the distributor
        // validates its configured range and mayInteract centrally, as for vanilla.
        return !entity.isRemoved() && entity.getLevel() == player.level()
                && player.level().getBlockEntity(entity.getBlockPos()) == entity;
    }

    @Override public void awardExperience(BlockEntity entity, ServerPlayer player) {
        // The mod's own output slots call this method. It awards recipes/XP and clears the ledger.
        BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase) entity;
        if (!furnace.isGenerator()) furnace.unlockRecipes(player);
    }
}
