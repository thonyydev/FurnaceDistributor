package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.furnace.FurnaceTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class CollectionManager {

    public static void collect(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<FurnaceTarget> furnaces =
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

        for (FurnaceTarget furnace : furnaces) {
            FurnaceTransfer.Collected result = FurnaceTransfer.collect(furnace, player.getInventory());
            hasRemainingItems |= result.remaining();
            if (result.count() <= 0) continue;
            totalCollected += result.count();
            furnacesWithItems++;
            if (ServerConfig.get().collectExperience) {
                // One ledger per furnace, shared by all processes; clear it only after a real extraction.
                furnace.access().awardExperience(furnace.entity(), player);
                furnace.entity().setChanged();
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
