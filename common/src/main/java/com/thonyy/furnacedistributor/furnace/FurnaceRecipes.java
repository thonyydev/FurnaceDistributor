package com.thonyy.furnacedistributor.furnace;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/** Cache only this request's exact stack, including components; never cache by item identity. */
public final class FurnaceRecipes {
    private final ServerLevel level;
    private final SingleRecipeInput input;
    private final Map<RecipeType<?>, Boolean> results = new HashMap<>();

    public FurnaceRecipes(ServerLevel level, ItemStack stack) {
        this.level = level;
        this.input = new SingleRecipeInput(stack.copy());
    }

    public boolean has(RecipeType<? extends AbstractCookingRecipe> type) {
        return results.computeIfAbsent(type, ignored ->
                level.recipeAccess().getRecipeFor(type, input, level).isPresent());
    }
}
