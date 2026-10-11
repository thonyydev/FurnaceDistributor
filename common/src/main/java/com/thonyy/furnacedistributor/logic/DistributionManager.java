package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.furnace.FurnaceRecipes;
import com.thonyy.furnacedistributor.furnace.FurnaceTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class DistributionManager {
    public static void distribute(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        List<FurnaceTarget> furnaces = OperationGuard.find(player, pos1, pos2);
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
        boolean fuel = player.level().fuelValues().isFuel(heldItem);
        int[] capacities = new int[furnaces.size()];
        FurnaceTransfer.Inputs[] inputs = new FurnaceTransfer.Inputs[furnaces.size()];
        int eligible = 0;
        boolean accepted = false;
        FurnaceRecipes recipes = new FurnaceRecipes(player.level(), heldItem);
        for (int i = 0; i < furnaces.size(); i++) {
            inputs[i] = FurnaceTransfer.inputs(furnaces.get(i), heldItem, fuel, recipes);
            accepted |= inputs[i].accepted();
            capacities[i] = inputs[i].capacity();
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
            int moved = FurnaceTransfer.insert(furnaces.get(i), inputs[i], amounts[i], heldItem, recipes);
            distributed += moved;
            if (moved > 0) used++;
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        // One complete result: a second action-bar message would immediately hide the quantities.
        PlayerFeedback.actionBar(player, Component.translatable(heldItem.isEmpty()
                        ? "message.furnacedistributor.distributed_items" : "message.furnacedistributor.distributed_items_partial",
                distributed, used, heldItem.getCount()).withStyle(heldItem.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    private DistributionManager() { }
}
