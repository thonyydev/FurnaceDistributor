package com.thonyy.furnacedistributor.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class DistributionManager {

    public static void distribute(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.no_item"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            return;
        }

        List<AbstractFurnaceBlockEntity> furnaces =
                findFurnaces(player, pos1, pos2);

        if (furnaces.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.no_furnaces"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            return;
        }

        /*
         * API vanilla compartilhada por Fabric e NeoForge.
         */
        boolean isFuel =
                AbstractFurnaceBlockEntity.isFuel(heldItem);

        SingleRecipeInput recipeInput = new SingleRecipeInput(heldItem.copy());

        boolean isSmeltable =
                player.level()
                        .getRecipeManager()
                        .getRecipeFor(
                                RecipeType.SMELTING,
                                recipeInput,
                                player.level()
                        )
                        .isPresent();

        if (!isFuel && !isSmeltable) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.invalid_item"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            return;
        }

        int totalItems = heldItem.getCount();

        int itemsPerFurnace =
                totalItems / furnaces.size();

        int remainder =
                totalItems % furnaces.size();

        if (itemsPerFurnace == 0) {
            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.not_enough_items"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            return;
        }

        int distributed = 0;

        /*
         * Furnace:
         *
         * 0 = input
         * 1 = fuel
         * 2 = output
         */
        int slot = isFuel ? 1 : 0;

        for (AbstractFurnaceBlockEntity furnace : furnaces) {

            ItemStack slotStack =
                    furnace.getItem(slot);

            if (!canInsert(slotStack, heldItem)) {
                continue;
            }

            int amount = itemsPerFurnace;

            if (remainder > 0) {
                amount++;
                remainder--;
            }

            int inserted =
                    insertIntoFurnace(
                            furnace,
                            slot,
                            heldItem,
                            amount
                    );

            distributed += inserted;
        }

        heldItem.shrink(distributed);

        player.inventoryMenu.broadcastChanges();

        player.displayClientMessage(
                Component.translatable(
                                "message.furnacedistributor.distributed_items",
                                distributed,
                                furnaces.size()
                        )
                        .withStyle(ChatFormatting.GREEN),
                false
        );
    }

    private static List<AbstractFurnaceBlockEntity> findFurnaces(
            ServerPlayer player,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<AbstractFurnaceBlockEntity> furnaces =
                new ArrayList<>();

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        for (BlockPos pos :
                BlockPos.betweenClosed(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )) {

            if (!(player.level()
                    .getBlockState(pos)
                    .getBlock()
                    instanceof AbstractFurnaceBlock)) {
                continue;
            }

            if (player.level()
                    .getBlockEntity(pos)
                    instanceof AbstractFurnaceBlockEntity furnace) {

                furnaces.add(furnace);
            }
        }

        return furnaces;
    }

    private static boolean canInsert(
            ItemStack furnaceStack,
            ItemStack heldItem
    ) {
        return furnaceStack.isEmpty()
                || (
                ItemStack.isSameItemSameComponents(
                        furnaceStack,
                        heldItem
                )
                        && furnaceStack.getCount()
                        < furnaceStack.getMaxStackSize()
        );
    }

    private static int insertIntoFurnace(
            AbstractFurnaceBlockEntity furnace,
            int slot,
            ItemStack source,
            int requestedAmount
    ) {
        ItemStack furnaceStack =
                furnace.getItem(slot);

        if (furnaceStack.isEmpty()) {

            int amount = Math.min(
                    requestedAmount,
                    source.getMaxStackSize()
            );

            ItemStack newStack = source.copy();
            newStack.setCount(amount);

            furnace.setItem(slot, newStack);
            furnace.setChanged();

            return amount;
        }

        int availableSpace =
                furnaceStack.getMaxStackSize()
                        - furnaceStack.getCount();

        int amount =
                Math.min(
                        requestedAmount,
                        availableSpace
                );

        if (amount <= 0) {
            return 0;
        }

        furnaceStack.grow(amount);
        furnace.setChanged();

        return amount;
    }

    private DistributionManager() {
    }
}
