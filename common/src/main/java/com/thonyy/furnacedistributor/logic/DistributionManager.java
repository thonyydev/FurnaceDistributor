package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DistributionManager {
    public static void distribute(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        List<AbstractFurnaceBlockEntity> furnaces = OperationGuard.find(player, pos1, pos2);
        if (furnaces == null) return;
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            OperationGuard.error(player, "no_item");
            return;
        }
        if (furnaces.isEmpty()) {
            OperationGuard.error(player, "no_furnaces");
            return;
        }
        // Preserve the established preference for fuel when an item is both fuel and input.
        boolean fuel = AbstractFurnaceBlockEntity.isFuel(heldItem);
        int slot = fuel ? 1 : 0;
        int[] capacities = new int[furnaces.size()];
        int eligible = 0;
        boolean accepted = false;
        SingleRecipeInput input = new SingleRecipeInput(heldItem.copy());
        Map<RecipeType<?>, Boolean> recipes = new HashMap<>();
        for (int i = 0; i < furnaces.size(); i++) {
            AbstractFurnaceBlockEntity furnace = furnaces.get(i);
            RecipeType<? extends AbstractCookingRecipe> type =
                    furnace instanceof BlastFurnaceBlockEntity ? RecipeType.BLASTING
                            : furnace instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
            boolean validInput = fuel || recipes.computeIfAbsent(type, ignored ->
                    player.level().getRecipeManager().getRecipeFor(type, input, player.level()).isPresent());
            if (!validInput || !furnace.canPlaceItem(slot, heldItem)) continue;
            accepted = true;
            ItemStack existing = furnace.getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, heldItem)) continue;
            capacities[i] = Math.max(0, furnace.getMaxStackSize(heldItem) - existing.getCount());
            if (capacities[i] > 0) eligible++;
        }
        if (eligible == 0) {
            OperationGuard.error(player, accepted ? "no_capacity" : "invalid_item");
            return;
        }
        ServerConfig config = ServerConfig.get();
        int targets = config.smartDistribution ? eligible : furnaces.size();
        if (!config.allowPartialDistribution && heldItem.getCount() < targets) {
            OperationGuard.error(player, "not_enough_items");
            return;
        }
        int[] amounts = DistributionPlan.allocate(heldItem.getCount(), capacities, config.smartDistribution);
        int distributed = 0;
        int used = 0;
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] == 0) continue;
            AbstractFurnaceBlockEntity furnace = furnaces.get(i);
            ItemStack inserted = furnace.getItem(slot).isEmpty() ? heldItem.copy() : furnace.getItem(slot).copy();
            inserted.setCount(furnace.getItem(slot).getCount() + amounts[i]);
            furnace.setItem(slot, inserted);
            furnace.setChanged();
            distributed += amounts[i];
            used++;
        }
        heldItem.shrink(distributed);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        // One complete result: a second action-bar message would immediately hide the quantities.
        PlayerFeedback.actionBar(player, Component.translatable(heldItem.isEmpty()
                        ? "message.furnacedistributor.distributed_items" : "message.furnacedistributor.distributed_items_partial",
                distributed, used, heldItem.getCount()).withStyle(heldItem.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    private DistributionManager() { }
}
