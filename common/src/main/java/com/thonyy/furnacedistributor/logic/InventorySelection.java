package com.thonyy.furnacedistributor.logic;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Only the 36 non-equipment inventory slots; a bitset cannot contain duplicate indices. */
public final class InventorySelection {
    public static final int SLOTS = 36;
    public static final long ALL = (1L << SLOTS) - 1;

    public static boolean valid(long slots, int limit) {
        return slots != 0 && (slots & ~ALL) == 0 && Long.bitCount(slots) <= limit;
    }

    public static long matching(long selected, int anchor, IntFunction<ItemStack> inventory) {
        if (anchor < 0 || anchor >= SLOTS) return selected;
        ItemStack stack = inventory.apply(anchor);
        if (stack.isEmpty()) return selected;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ItemStack.isSameItemSameComponents(stack, inventory.apply(slot))) selected |= 1L << slot;
        }
        return selected;
    }

    /** Ascending inventory index decides group priority and which source stack is debited first. */
    public static List<List<Integer>> groups(long selected, IntFunction<ItemStack> inventory) {
        List<List<Integer>> groups = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            if ((selected & (1L << slot)) == 0 || inventory.apply(slot).isEmpty()) continue;
            List<Integer> match = null;
            for (var group : groups) {
                if (ItemStack.isSameItemSameComponents(inventory.apply(group.getFirst()), inventory.apply(slot))) {
                    match = group;
                    break;
                }
            }
            if (match == null) {
                match = new ArrayList<>();
                groups.add(match);
            }
            match.add(slot);
        }
        return groups;
    }

    private InventorySelection() { }
}
