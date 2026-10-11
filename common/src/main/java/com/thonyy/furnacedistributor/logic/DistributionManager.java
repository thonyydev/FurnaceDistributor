package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.furnace.FurnaceRecipes;
import com.thonyy.furnacedistributor.furnace.FurnaceTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntFunction;

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
        Plan plan = plan(player, furnaces, heldItem);
        if (plan.eligible() == 0) {
            PlayerFeedback.actionBar(player, result(0, 0, furnaces.size(), heldItem.getCount(), plan.incompatible(), plan.noSpace())
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ServerConfig config = ServerConfig.get();
        int targets = config.smartDistribution ? plan.eligible() : furnaces.size();
        if (!config.allowPartialDistribution && heldItem.getCount() < targets) {
            OperationGuard.error(player, "not_enough_items");
            return;
        }
        boolean[] used = new boolean[furnaces.size()];
        int distributed = move(furnaces, plan, heldItem.getCount(), ignored -> heldItem, 1, used);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        // One complete result: a second action-bar message would immediately hide the quantities.
        PlayerFeedback.actionBar(player, result(distributed, count(used), furnaces.size(), heldItem.getCount(), plan.incompatible(), plan.noSpace())
                .withStyle(heldItem.isEmpty() && plan.incompatible() + plan.noSpace() == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    public static void distributeBulk(ServerPlayer player, BlockPos pos1, BlockPos pos2, long selected,
                                       net.minecraft.resources.Identifier dimension) {
        if (!player.level().dimension().identifier().equals(dimension)) {
            OperationGuard.error(player, "no_saved_area");
            return;
        }
        ServerConfig config = ServerConfig.get();
        if (!config.enableBulkDistribution) {
            OperationGuard.error(player, "bulk_disabled");
            return;
        }
        if (!InventorySelection.valid(selected, config.maxBulkSlots)) {
            OperationGuard.error(player, "bulk_invalid_slots", config.maxBulkSlots);
            return;
        }
        List<FurnaceTarget> furnaces = OperationGuard.find(player, pos1, pos2);
        if (furnaces == null) return;
        if (furnaces.isEmpty()) {
            OperationGuard.error(player, "no_furnaces");
            return;
        }
        // Inventory contents are read only here, after the complete area's access checks.
        var inventory = player.getInventory().getNonEquipmentItems();
        var groups = InventorySelection.groups(selected, inventory::get);
        if (groups.isEmpty()) {
            OperationGuard.error(player, "bulk_empty");
            return;
        }
        boolean[] used = new boolean[furnaces.size()];
        boolean[] accepted = new boolean[furnaces.size()];
        boolean[] hadCapacity = new boolean[furnaces.size()];
        int distributed = 0;
        boolean insufficient = false;
        for (var group : groups) {
            ItemStack prototype = inventory.get(group.getFirst()).copy();
            int available = 0;
            for (int slot : group) {
                ItemStack source = inventory.get(slot);
                if (ItemStack.isSameItemSameComponents(prototype, source)) {
                    available = (int) Math.min(Integer.MAX_VALUE, (long) available + source.getCount());
                }
            }
            if (available == 0) continue;
            Plan plan = plan(player, furnaces, prototype);
            for (int i = 0; i < furnaces.size(); i++) {
                accepted[i] |= plan.inputs()[i].accepted();
                hadCapacity[i] |= plan.capacities()[i] > 0;
            }
            int targets = config.smartDistribution ? plan.eligible() : furnaces.size();
            // Keep the configured minimum-items policy per component-equivalent group.
            if (!config.allowPartialDistribution && available < targets) {
                insufficient = true;
                continue;
            }
            distributed += move(furnaces, plan, available, index -> inventory.get(group.get(index)), group.size(), used);
        }
        int incompatible = 0;
        int noSpace = 0;
        long remaining = 0;
        for (int i = 0; i < furnaces.size(); i++) {
            if (used[i]) continue;
            if (!accepted[i]) incompatible++;
            else if (!hadCapacity[i]) noSpace++;
        }
        for (int slot = 0; slot < InventorySelection.SLOTS; slot++) {
            if ((selected & (1L << slot)) != 0) remaining += inventory.get(slot).getCount();
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        // A destination used by any material/fuel is counted once; unknown exclusions stay unlabelled.
        MutableComponent feedback = result(distributed, count(used), furnaces.size(), 0, incompatible, noSpace);
        if (remaining > 0) feedback.append(" · ").append(Component.translatable("message.furnacedistributor.bulk_remaining", remaining));
        if (insufficient) feedback.append(" · ").append(Component.translatable("message.furnacedistributor.not_enough_items"));
        PlayerFeedback.actionBar(player, feedback.withStyle(distributed == 0 ? ChatFormatting.RED
                : remaining == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
    }

    private record Plan(int[] capacities, FurnaceTransfer.Inputs[] inputs, FurnaceRecipes recipes,
                        int eligible, int incompatible, int noSpace) { }

    private static Plan plan(ServerPlayer player, List<FurnaceTarget> furnaces, ItemStack heldItem) {
        // Preserve the established preference for fuel when an item is both fuel and input.
        boolean fuel = player.level().fuelValues().isFuel(heldItem);
        int[] capacities = new int[furnaces.size()];
        FurnaceTransfer.Inputs[] inputs = new FurnaceTransfer.Inputs[furnaces.size()];
        int eligible = 0;
        int incompatible = 0;
        int noSpace = 0;
        FurnaceRecipes recipes = new FurnaceRecipes(player.level(), heldItem);
        for (int i = 0; i < furnaces.size(); i++) {
            inputs[i] = FurnaceTransfer.inputs(furnaces.get(i), heldItem, fuel, recipes);
            capacities[i] = inputs[i].capacity();
            if (!inputs[i].accepted()) incompatible++;
            else if (capacities[i] == 0) noSpace++;
            if (capacities[i] > 0) eligible++;
        }
        return new Plan(capacities, inputs, recipes, eligible, incompatible, noSpace);
    }

    private static int move(List<FurnaceTarget> furnaces, Plan plan, int available,
                            IntFunction<ItemStack> sources, int sourceCount, boolean[] used) {
        int[] amounts = DistributionPlan.allocate(available, plan.capacities(), ServerConfig.get().smartDistribution);
        int distributed = 0;
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] == 0) continue;
            int moved = FurnaceTransfer.insert(furnaces.get(i), plan.inputs()[i], amounts[i], sources, sourceCount, plan.recipes());
            distributed += moved;
            if (moved > 0) used[i] = true;
        }
        return distributed;
    }

    private static int count(boolean[] values) {
        int count = 0;
        for (boolean value : values) if (value) count++;
        return count;
    }

    private static MutableComponent result(int distributed, int used, int total, int remaining, int incompatible, int noSpace) {
        MutableComponent result = distributed == 0
                ? Component.translatable("message.furnacedistributor.distributed_none", total)
                : Component.translatable("message.furnacedistributor.distributed_items", distributed, used, total);
        if (incompatible > 0 && noSpace > 0) {
            result.append(" · ").append(Component.translatable("message.furnacedistributor.skipped_mixed", noSpace, incompatible));
        } else if (incompatible + noSpace > 0) {
            result.append(" · ").append(Component.translatable("message.furnacedistributor."
                    + (noSpace > 0 ? "skipped_space" : "skipped_item"), incompatible + noSpace));
        }
        if (distributed > 0 && remaining > 0) {
            result.append(" · ").append(Component.translatable("message.furnacedistributor.remaining_items", remaining));
        }
        return result;
    }

    private DistributionManager() { }
}
