package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.List;

public final class CollectionManager {

    public static void collect(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<AbstractFurnaceBlockEntity> furnaces =
                OperationGuard.find(player, pos1, pos2);

        if (furnaces == null) return;

        if (furnaces.isEmpty()) {
            PlayerFeedback.actionBar(player, Component.translatable(
                                    "message.furnacedistributor.no_valid_furnaces"
                            )
                            .withStyle(ChatFormatting.RED));

            return;
        }

        int totalCollected = 0;
        int furnacesWithItems = 0;
        boolean hasRemainingItems = false;

        for (AbstractFurnaceBlockEntity furnace : furnaces) {

            ItemStack output = furnace.getItem(2);

            if (output.isEmpty()) {
                continue;
            }

            int originalCount = output.getCount();

            ItemStack remaining = output.copy();

            InventoryTransfer.insert(player.getInventory(), remaining);

            if (!remaining.isEmpty()) {
                hasRemainingItems = true;
            }

            int collected =
                    originalCount - remaining.getCount();

            if (collected <= 0) {
                continue;
            }

            totalCollected += collected;
            furnacesWithItems++;

            if (remaining.isEmpty()) {
                furnace.setItem(
                        2,
                        ItemStack.EMPTY
                );
            } else {
                furnace.setItem(
                        2,
                        remaining
                );
            }

            furnace.setChanged();
            if (ServerConfig.get().collectExperience) {
                // Vanilla clears the recipe ledger, including after a partial extraction.
                furnace.awardUsedRecipesAndPopExperience(player);
                furnace.setChanged();
            }
        }

        if (totalCollected == 0) {

            PlayerFeedback.actionBar(player, Component.translatable(
                                    hasRemainingItems
                                            ? "message.furnacedistributor.inventory_full"
                                            : "message.furnacedistributor.no_smelted_items"
                            )
                            .withStyle(ChatFormatting.YELLOW));

        } else {

            PlayerFeedback.actionBar(player, Component.translatable(
                                    hasRemainingItems ? "message.furnacedistributor.collected_items_partial"
                                            : "message.furnacedistributor.collected_items",
                                    totalCollected,
                                    furnacesWithItems
                            )
                            .withStyle(hasRemainingItems ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        }

        if (totalCollected > 0) player.inventoryMenu.broadcastChanges();
    }

    private CollectionManager() {
    }
}
