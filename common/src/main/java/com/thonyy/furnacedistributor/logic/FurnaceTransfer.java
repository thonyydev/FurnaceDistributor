package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.furnace.FurnaceRecipes;
import com.thonyy.furnacedistributor.furnace.FurnaceTarget;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Inventory mutations run synchronously on the server thread, using independent stack copies. */
public final class FurnaceTransfer {
    public record Inputs(int[] slots, int[] capacities, boolean accepted, ItemStack plannedStack) {
        public int capacity() { return java.util.Arrays.stream(capacities).sum(); }
    }
    public record Collected(int count, boolean remaining) { }

    public static int capacity(ItemStack existing, ItemStack offered, int limit) {
        if (offered.isEmpty() || (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, offered))) return 0;
        return Math.max(0, Math.min(limit, offered.getMaxStackSize()) - existing.getCount());
    }

    public static Inputs inputs(FurnaceTarget target, ItemStack stack, boolean fuel, FurnaceRecipes recipes) {
        int[] slots = target.access().inputSlots(target.entity(), stack, fuel);
        int[] capacities = new int[slots.length];
        boolean accepted = false;
        for (int i = 0; i < slots.length; i++) {
            if (!target.access().canInsert(target.entity(), slots[i], stack, recipes)) continue;
            accepted = true;
            capacities[i] = capacity(target.inventory().getItem(slots[i]), stack,
                    target.access().insertionLimit(target.entity(), slots[i], stack));
        }
        return new Inputs(slots, capacities, accepted, stack.copy());
    }

    /** Recheck each destination before writing; debit the source by the amount actually committed. */
    public static int insert(FurnaceTarget target, Inputs inputs, int amount, ItemStack source, FurnaceRecipes recipes) {
        // A changed item/components also invalidates the recipe snapshot used to plan this transfer.
        if (!ItemStack.isSameItemSameComponents(inputs.plannedStack(), source)) return 0;
        int[] shares = DistributionPlan.allocate(Math.min(amount, source.getCount()), inputs.capacities(), true);
        int moved = 0;
        for (int i = 0; i < shares.length; i++) {
            int slot = inputs.slots()[i];
            if (shares[i] <= 0 || !target.access().canInsert(target.entity(), slot, source, recipes)) continue;
            ItemStack existing = target.inventory().getItem(slot);
            int count = Math.min(source.getCount(), Math.min(shares[i],
                    capacity(existing, source, target.access().insertionLimit(target.entity(), slot, source))));
            if (count <= 0) continue;
            ItemStack inserted = (existing.isEmpty() ? source : existing).copyWithCount(existing.getCount() + count);
            target.inventory().setItem(slot, inserted);
            source.shrink(count);
            target.entity().setChanged();
            moved += count;
        }
        return moved;
    }

    public static Collected collect(FurnaceTarget target, Inventory inventory) {
        int moved = 0;
        boolean remainingItems = false;
        for (int slot : target.access().outputSlots(target.entity())) {
            ItemStack output = target.inventory().getItem(slot);
            if (output.isEmpty()) continue;
            ItemStack remaining = output.copy();
            InventoryTransfer.insert(inventory, remaining);
            int collected = output.getCount() - remaining.getCount();
            remainingItems |= !remaining.isEmpty();
            if (collected <= 0) continue;
            // Never clamp an oversized remainder through Container.setItem (some mod tiers can produce one).
            // Shrink only the actual source output, after inserting a separate copy in the player inventory.
            output.shrink(collected);
            if (output.isEmpty()) target.inventory().setItem(slot, ItemStack.EMPTY);
            target.entity().setChanged();
            moved += collected;
        }
        return new Collected(moved, remainingItems);
    }

    private FurnaceTransfer() { }
}
