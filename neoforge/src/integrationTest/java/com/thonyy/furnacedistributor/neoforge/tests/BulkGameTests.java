package com.thonyy.furnacedistributor.neoforge.tests;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.logic.DistributionManager;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.Arrays;

import static com.thonyy.furnacedistributor.neoforge.tests.VanillaGameTests.*;

public final class BulkGameTests {
    static void bulk(GameTestHelper helper, ServerPlayer player, int furnaces, long mask) {
        DistributionManager.distributeBulk(player, absolute(helper, 0), absolute(helper, furnaces - 1), mask, player.level().dimension().identifier());
    }

    static Component feedback(ServerPlayer player) {
        return lastFeedback(player);
    }

    static void result(GameTestHelper helper, ServerPlayer player, int moved, int used, int total) {
        var text = (TranslatableContents) feedback(player).getContents();
        helper.assertTrue(text.getKey().equals(moved == 0 ? "message.furnacedistributor.distributed_none" : "message.furnacedistributor.distributed_items")
                && Arrays.equals(text.getArgs(), moved == 0 ? new Object[]{total} : new Object[]{moved, used, total}),
                "feedback uses actual committed item total and distinct furnace count");
    }

    public static void fiveStacksAnd32Furnaces(GameTestHelper helper) {
        var furnaces = new java.util.ArrayList<AbstractFurnaceBlockEntity>();
        for (int i = 0; i < 32; i++) {
            BlockPos pos = FIRST.offset(i % 4, i / 16, i / 4 % 4);
            helper.setBlock(pos, Blocks.FURNACE);
            furnaces.add(helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class));
        }
        var player = player(helper, new ItemStack(Items.RAW_IRON, 64));
        for (int slot = 1; slot < 5; slot++) player.getInventory().setItem(slot, new ItemStack(Items.RAW_IRON, 64));
        player.getInventory().setItem(8, new ItemStack(Items.RAW_IRON, 64));
        DistributionManager.distributeBulk(player, helper.absolutePos(FIRST), helper.absolutePos(FIRST.offset(3, 1, 3)), 31, player.level().dimension().identifier());
        for (var furnace : furnaces) helper.assertTrue(furnace.getItem(0).getCount() == 10, "equivalent sources share one balanced plan");
        for (int slot = 0; slot < 5; slot++) helper.assertTrue(player.getInventory().getItem(slot).isEmpty(), "selected sources debited");
        helper.assertTrue(player.getInventory().getItem(8).getCount() == 64, "unselected equivalent stack untouched");
        result(helper, player, 320, 32, 32);
        DistributionManager.distributeBulk(player, helper.absolutePos(FIRST), helper.absolutePos(FIRST.offset(3, 1, 3)), 31, player.level().dimension().identifier());
        helper.assertTrue(furnaces.stream().mapToInt(furnace -> furnace.getItem(0).getCount()).sum() == 320,
                "replaying an emptied selection cannot duplicate items");
        helper.succeed();
    }

    public static void mixedMaterialsFuelAndPartial(GameTestHelper helper) {
        var iron = furnace(helper, 0, Blocks.FURNACE);
        var gold = furnace(helper, 1, Blocks.BLAST_FURNACE);
        var sand = furnace(helper, 2, Blocks.FURNACE);
        var smoker = furnace(helper, 3, Blocks.SMOKER);
        iron.setItem(0, new ItemStack(Items.RAW_IRON, 1));
        gold.setItem(0, new ItemStack(Items.RAW_GOLD, 1));
        sand.setItem(0, new ItemStack(Items.SAND, 1));
        var player = player(helper, new ItemStack(Items.RAW_IRON, 64));
        player.getInventory().setItem(1, new ItemStack(Items.RAW_GOLD, 32));
        player.getInventory().setItem(2, new ItemStack(Items.SAND, 16));
        player.getInventory().setItem(3, new ItemStack(Items.COAL, 64));
        bulk(helper, player, 4, 15);
        helper.assertTrue(iron.getItem(0).getCount() == 64 && gold.getItem(0).getCount() == 33
                && sand.getItem(0).getCount() == 17 && smoker.getItem(0).isEmpty(), "actual smelting/blasting/smoking recipes and occupied inputs respected");
        for (var furnace : new AbstractFurnaceBlockEntity[]{iron, gold, sand, smoker}) {
            helper.assertTrue(furnace.getItem(1).getCount() == 16, "coal fuel balanced independently of material recipes");
        }
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getInventory().getItem(1).isEmpty()
                && player.getInventory().getItem(2).isEmpty() && player.getInventory().getItem(3).isEmpty(), "only actual credits debited; partial leftover retained");
        result(helper, player, 175, 4, 4);
        helper.assertTrue(feedback(player).getSiblings().stream().noneMatch(part -> part.getContents() instanceof TranslatableContents text
                && text.getKey().contains("skipped")), "furnace accepting fuel is not also counted as an excluded material destination");
        helper.succeed();
    }

    public static void liveInventoryAndComponents(GameTestHelper helper) {
        var first = furnace(helper, 0, Blocks.FURNACE);
        var second = furnace(helper, 1, Blocks.FURNACE);
        var third = furnace(helper, 2, Blocks.BLAST_FURNACE);
        ItemStack a = new ItemStack(Items.RAW_IRON, 8);
        a.set(DataComponents.CUSTOM_NAME, Component.literal("a"));
        ItemStack b = new ItemStack(Items.RAW_IRON, 8);
        b.set(DataComponents.CUSTOM_NAME, Component.literal("b"));
        first.setItem(0, a.copyWithCount(1));
        second.setItem(0, b.copyWithCount(1));
        third.setItem(0, new ItemStack(Items.RAW_GOLD, 1));
        ItemStack expectedA = a.copy();
        var player = player(helper, new ItemStack(Items.RAW_IRON, 64));
        player.getInventory().setItem(1, new ItemStack(Items.RAW_IRON, 64));
        player.getInventory().setItem(2, a);
        player.getInventory().setItem(3, b);
        // The intent contains no old item snapshots: removal/replacement before confirmation is authoritative.
        player.getInventory().setItem(0, ItemStack.EMPTY);
        player.getInventory().setItem(1, new ItemStack(Items.RAW_GOLD, 4));
        bulk(helper, player, 3, 15);
        helper.assertTrue(first.getItem(0).getCount() == 9 && second.getItem(0).getCount() == 9 && third.getItem(0).getCount() == 5
                && ItemStack.isSameItemSameComponents(first.getItem(0), expectedA)
                && first.getItem(0).get(DataComponents.CUSTOM_NAME).getString().equals("a")
                && second.getItem(0).get(DataComponents.CUSTOM_NAME).getString().equals("b"), "live slot contents and distinct components preserved");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().allMatch(ItemStack::isEmpty), "exactly 20 live selected items transferred");
        result(helper, player, 20, 3, 3);
        helper.succeed();
    }

    public static void fullIncompatibleAndInsufficient(GameTestHelper helper) {
        var a = furnace(helper, 0, Blocks.FURNACE);
        var b = furnace(helper, 1, Blocks.BLAST_FURNACE);
        furnace(helper, 2, Blocks.SMOKER);
        a.setItem(0, new ItemStack(Items.RAW_IRON, 64));
        b.setItem(0, new ItemStack(Items.RAW_IRON, 64));
        var player = player(helper, new ItemStack(Items.RAW_IRON, 64));
        bulk(helper, player, 3, 1);
        result(helper, player, 0, 0, 3);
        var skipped = feedback(player).getSiblings().stream().filter(part -> part.getContents() instanceof TranslatableContents text
                && text.getKey().equals("message.furnacedistributor.skipped_mixed")).findFirst().orElseThrow();
        helper.assertTrue(Arrays.equals(((TranslatableContents) skipped.getContents()).getArgs(), new Object[]{2, 1})
                && player.getMainHandItem().getCount() == 64, "full and incompatible distinguished without loss");
        for (int i = 0; i < 3; i++) furnace(helper, i, Blocks.FURNACE).clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 2));
        bulk(helper, player, 3, 1);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && feedback(player).getSiblings().stream().anyMatch(part ->
                part.getContents() instanceof TranslatableContents text && text.getKey().equals("message.furnacedistributor.not_enough_items")),
                "server minimum-items policy preserved and explained");
        ServerConfig.get().allowPartialDistribution = true;
        try {
            bulk(helper, player, 3, 1);
            result(helper, player, 2, 2, 3);
            helper.assertTrue(feedback(player).getSiblings().isEmpty(), "eligible destinations without enough items are never called full");
        } finally { ServerConfig.get().allowPartialDistribution = false; }
        for (int i = 0; i < 3; i++) furnace(helper, i, Blocks.FURNACE).clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 16));
        player.getInventory().setItem(1, new ItemStack(Items.RAW_GOLD, 16));
        bulk(helper, player, 3, 3);
        result(helper, player, 16, 3, 3);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty() && player.getInventory().getItem(1).getCount() == 16,
                "lower inventory-index material deterministically owns competing empty inputs; later material stays in inventory");
        helper.succeed();
    }

    public static void slotLimitsAndCooldown(GameTestHelper helper) {
        var furnace = furnace(helper, 0, Blocks.FURNACE);
        var player = player(helper, new ItemStack(Items.RAW_IRON, 16));
        player.getInventory().setItem(1, new ItemStack(Items.COAL, 16));
        DistributionManager.distributeBulk(player, absolute(helper, 0), absolute(helper, 0), 1, net.minecraft.world.level.Level.NETHER.identifier());
        for (long invalid : new long[]{0, -1, 1L << 36, 1L << 40}) bulk(helper, player, 1, invalid);
        ServerConfig.get().maxBulkSlots = 1;
        try { bulk(helper, player, 1, 3); }
        finally { ServerConfig.get().maxBulkSlots = 36; }
        ServerConfig.get().enableBulkDistribution = false;
        try { bulk(helper, player, 1, 1); }
        finally { ServerConfig.get().enableBulkDistribution = true; }
        helper.assertTrue(furnace.isEmpty() && player.getMainHandItem().getCount() == 16
                && player.getInventory().getItem(1).getCount() == 16, "all invalid masks, server limits and disablement reject before mutation");
        ServerConfig.get().requestCooldownTicks = 5;
        try {
            bulk(helper, player, 1, 1);
            player.getInventory().setItem(0, new ItemStack(Items.RAW_IRON, 8));
            bulk(helper, player, 1, 1);
            distribute(helper, player, 1);
            helper.assertTrue(furnace.getItem(0).getCount() == 16 && player.getMainHandItem().getCount() == 8,
                    "bulk, repeated bulk and normal distribution share rate limiting");
        } finally { ServerConfig.get().requestCooldownTicks = 0; }
        helper.succeed();
    }

    public static void areaAccessAndChunks(GameTestHelper helper) {
        var first = furnace(helper, 0, Blocks.FURNACE);
        var second = furnace(helper, 1, Blocks.FURNACE);
        lock(helper, second);
        var player = player(helper, new ItemStack(Items.RAW_IRON, 16));
        bulk(helper, player, 2, 1);
        helper.assertTrue(first.isEmpty() && second.isEmpty() && player.getMainHandItem().getCount() == 16,
                "locked second furnace rejects the entire bulk operation before mutation");
        BlockPos pos = absolute(helper, 0);
        player.setPos(pos.getX() + 200, pos.getY(), pos.getZ());
        bulk(helper, player, 1, 1);
        helper.assertTrue(first.isEmpty() && player.getMainHandItem().getCount() == 16, "distant player rejected");
        BlockPos unloaded = pos.offset(10000, 0, 10000);
        try {
            // Changing only the mock coordinates avoids vanilla's entity-movement chunk tickets.
            var position = net.minecraft.world.entity.Entity.class.getDeclaredField("position");
            position.setAccessible(true);
            Object original = position.get(player);
            try {
                position.set(player, new net.minecraft.world.phys.Vec3(unloaded.getX(), unloaded.getY(), unloaded.getZ()));
                helper.assertFalse(helper.getLevel().hasChunk(unloaded.getX() >> 4, unloaded.getZ() >> 4), "fixture starts unloaded");
                DistributionManager.distributeBulk(player, unloaded, unloaded, 1, player.level().dimension().identifier());
                helper.assertTrue(!helper.getLevel().hasChunk(unloaded.getX() >> 4, unloaded.getZ() >> 4)
                        && player.getMainHandItem().getCount() == 16 && !FurnaceArea.scan(helper.getLevel(), unloaded, unloaded).valid(),
                        "bulk does not load a chunk or debit its source");
                DistributionManager.distributeBulk(player, unloaded, unloaded.offset(100000, 0, 0), 1, player.level().dimension().identifier());
                helper.assertTrue(player.getMainHandItem().getCount() == 16, "oversized area rejected");
            } finally { position.set(player, original); }
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        helper.succeed();
    }

    private BulkGameTests() { }
}
