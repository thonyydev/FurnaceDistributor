package com.thonyy.furnacedistributor.logic;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class InventoryTransfer {

    /**
     * Reduz a pilha somente pela quantidade realmente colocada no inventário.
     * Não usa Inventory.add: o restante não pode ser descartado no Criativo.
     */
    static void insert(Inventory inventory, ItemStack remaining) {
        int originalCount = remaining.getCount();

        // Completa pilhas existentes antes de ocupar novos slots.
        for (ItemStack target : inventory.items) {
            merge(inventory, target, remaining);
        }

        // Pode completar uma pilha na mão secundária, mas não a preenche vazia.
        for (ItemStack target : inventory.offhand) {
            merge(inventory, target, remaining);
        }

        // Apenas inventário principal e hotbar; nunca ocupa slots de armadura.
        for (int slot = 0; slot < inventory.items.size() && !remaining.isEmpty(); slot++) {
            if (!inventory.items.get(slot).isEmpty()) {
                continue;
            }

            int moved = Math.min(
                    remaining.getCount(),
                    Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize())
            );

            if (moved <= 0) {
                continue;
            }

            ItemStack inserted = remaining.copy();
            inserted.setCount(moved);
            inventory.items.set(slot, inserted);
            remaining.shrink(moved);
        }

        if (remaining.getCount() < originalCount) {
            inventory.setChanged();
        }
    }

    private static void merge(Inventory inventory, ItemStack target, ItemStack remaining) {
        if (remaining.isEmpty() || target.isEmpty() || !target.isStackable()
                || !ItemStack.isSameItemSameComponents(target, remaining)) {
            return;
        }

        int limit = Math.min(target.getMaxStackSize(), inventory.getMaxStackSize());
        int moved = Math.min(remaining.getCount(), limit - target.getCount());

        if (moved > 0) {
            target.grow(moved);
            remaining.shrink(moved);
        }
    }

    private InventoryTransfer() {
    }
}
