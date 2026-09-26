package com.thonyy.furnacedistributor.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class CollectionManager {

    public static void collect(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<AbstractFurnaceBlockEntity> furnaces =
                findFurnaces(player, pos1, pos2);

        if (furnaces.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.no_valid_furnaces"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

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
        }

        if (totalCollected == 0) {

            player.displayClientMessage(
                    Component.translatable(
                                    hasRemainingItems
                                            ? "message.furnacedistributor.inventory_full"
                                            : "message.furnacedistributor.no_smelted_items"
                            )
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );

        } else {

            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collected_items",
                                    totalCollected,
                                    furnacesWithItems
                            )
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
        }

        if (totalCollected > 0 && hasRemainingItems) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.collection_partial"
                            )
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        player.inventoryMenu.broadcastChanges();
    }

    private static List<AbstractFurnaceBlockEntity> findFurnaces(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<AbstractFurnaceBlockEntity> furnaces =
                new ArrayList<>();

        int minX = Math.min(
                pos1.getX(),
                pos2.getX()
        );

        int minY = Math.min(
                pos1.getY(),
                pos2.getY()
        );

        int minZ = Math.min(
                pos1.getZ(),
                pos2.getZ()
        );

        int maxX = Math.max(
                pos1.getX(),
                pos2.getX()
        );

        int maxY = Math.max(
                pos1.getY(),
                pos2.getY()
        );

        int maxZ = Math.max(
                pos1.getZ(),
                pos2.getZ()
        );

        for (
                BlockPos pos :
                BlockPos.betweenClosed(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )
        ) {
            if (
                    !(player.level()
                            .getBlockState(pos)
                            .getBlock()
                            instanceof AbstractFurnaceBlock)
            ) {
                continue;
            }

            if (
                    player.level()
                            .getBlockEntity(pos)
                            instanceof AbstractFurnaceBlockEntity furnace
            ) {
                furnaces.add(furnace);
            }
        }

        return furnaces;
    }

    private CollectionManager() {
    }
}
