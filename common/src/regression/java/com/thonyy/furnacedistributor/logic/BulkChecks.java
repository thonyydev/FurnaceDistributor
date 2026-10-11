package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.network.BulkDistributePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

final class BulkChecks {
    private static int checks;

    static int run() {
        checks = 0;
        selection();
        packets();
        transfers();
        return checks;
    }

    private static void selection() {
        for (long invalid : new long[]{0, -1, Long.MIN_VALUE, 1L << 36, (1L << 40) | 1}) {
            check(!InventorySelection.valid(invalid, 36), "out-of-range slot bitsets rejected");
        }
        check(InventorySelection.valid(InventorySelection.ALL, 36), "all 36 inventory and hotbar slots valid");
        check(!InventorySelection.valid(3, 1) && InventorySelection.valid(1L << 35, 1), "server slot budget");
        long mask = 0;
        mask ^= 1L << 10;
        check(mask == 1024, "manual selection marks only the chosen slot");
        mask ^= 1L << 10;
        check(mask == 0, "manual deselection");
        var inventory = new net.minecraft.world.entity.player.Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        var items = inventory.getNonEquipmentItems();
        items.set(0, new ItemStack(Items.RAW_IRON, 64));
        items.set(9, new ItemStack(Items.RAW_IRON, 16));
        items.set(35, new ItemStack(Items.RAW_IRON, 8));
        ItemStack named = new ItemStack(Items.RAW_IRON, 32);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("different"));
        items.set(1, named);
        items.set(10, new ItemStack(Items.RAW_GOLD, 32));
        inventory.setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, new ItemStack(Items.RAW_IRON, 64));
        mask = InventorySelection.matching(1L << 10, 0, items::get);
        check(mask == ((1L << 0) | (1L << 9) | (1L << 35) | (1L << 10)), "matching includes hotbar/main, preserves other selections, excludes changed components/offhand");
        check(items.get(0).getCount() == 64 && items.get(9).getCount() == 16 && items.get(35).getCount() == 8,
                "selection never moves or consumes items");
        check(InventorySelection.matching(mask, -1, items::get) == mask
                && InventorySelection.matching(mask, 36, items::get) == mask
                && InventorySelection.matching(mask, 34, items::get) == mask, "invalid/empty matching anchor changes nothing");
        var groups = InventorySelection.groups(mask | 2, items::get);
        check(groups.equals(List.of(List.of(0, 9, 35), List.of(1), List.of(10))), "component-aware groups ordered by ascending inventory slot");
        items.set(0, ItemStack.EMPTY);
        items.set(9, new ItemStack(Items.COAL, 12));
        check(InventorySelection.groups(mask, items::get).equals(List.of(List.of(9), List.of(10), List.of(35))),
                "inventory replacements/removals before confirmation use live contents");
    }

    private static void packets() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var intent = new BulkDistributePacket(new BlockPos(-30, -64, 10), new BlockPos(32, 200, -4), InventorySelection.ALL,
                    net.minecraft.world.level.Level.OVERWORLD.identifier());
            BulkDistributePacket.STREAM_CODEC.encode(buffer, intent);
            check(buffer.readableBytes() == 24 + 1 + intent.dimension().toString().length(), "bounded intent contains no stack snapshots or client quantities");
            check(BulkDistributePacket.STREAM_CODEC.decode(buffer).equals(intent) && !buffer.isReadable(), "bulk intent round trip");
            for (long invalid : new long[]{0, -1, 1L << 36}) {
                buffer.clear();
                buffer.writeBlockPos(BlockPos.ZERO);
                buffer.writeBlockPos(BlockPos.ZERO);
                buffer.writeLong(invalid);
                boolean rejected = false;
                try { BulkDistributePacket.STREAM_CODEC.decode(buffer); }
                catch (IllegalArgumentException expected) { rejected = true; }
                check(rejected, "invalid wire bitset rejected before handler/area scan");
            }
            for (int bytes = 0; bytes < 24; bytes++) {
                buffer.clear();
                buffer.writeZero(bytes);
                boolean rejected = false;
                try { BulkDistributePacket.STREAM_CODEC.decode(buffer); }
                catch (IndexOutOfBoundsException expected) { rejected = true; }
                check(rejected, "truncated packet rejected at length " + bytes);
            }
            buffer.clear();
            buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeBlockPos(BlockPos.ZERO);
            buffer.writeLong(1);
            buffer.writeUtf("Invalid Dimension!");
            boolean rejected = false;
            try { BulkDistributePacket.STREAM_CODEC.decode(buffer); }
            catch (net.minecraft.IdentifierException expected) { rejected = true; }
            check(rejected, "malformed dimension identifier rejected");
        } finally { buffer.release(); }
    }

    private static void transfers() {
        var fixture = new FurnaceTransferChecks.Fixture();
        ItemStack[] sources = {new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64)};
        var plan = FurnaceTransfer.inputs(fixture.target, sources[0], false, null);
        check(FurnaceTransfer.insert(fixture.target, plan, 128, index -> sources[index], 2, null) == 128, "combined sources transfer their actual total");
        check(fixture.inventory.getItem(0).getCount() == 43 && fixture.inventory.getItem(1).getCount() == 43
                && fixture.inventory.getItem(2).getCount() == 42, "factory slot balance independent of source stack boundaries");
        check(Arrays.stream(sources).allMatch(ItemStack::isEmpty), "real sources debited without a synthetic owner");
        check(FurnaceTransfer.insert(fixture.target, plan, 128, index -> sources[index], 2, null) == 0, "repeated transfer cannot create items");
        fixture = new FurnaceTransferChecks.Fixture();
        fixture.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 63));
        fixture.inventory.setItem(1, new ItemStack(Items.IRON_ORE, 10));
        fixture.inventory.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
        ItemStack[] live = {new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.COBBLESTONE, 8)};
        plan = FurnaceTransfer.inputs(fixture.target, live[0], false, null);
        live[0] = new ItemStack(Items.DIAMOND, 8);
        check(FurnaceTransfer.insert(fixture.target, plan, 16, index -> live[index], 2, null) == 1
                && live[0].is(Items.DIAMOND) && live[0].getCount() == 8 && live[1].getCount() == 7,
                "changed source is skipped; partial capacity debits only the matching live stack");
        fixture.target.entity().setRemoved();
        check(FurnaceTransfer.insert(fixture.target, plan, 1, index -> live[index], 2, null) == 0, "removed destination cannot receive items");
        fixture = new FurnaceTransferChecks.Fixture();
        ItemStack[] changing = {new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64)};
        plan = FurnaceTransfer.inputs(fixture.target, changing[0], false, null);
        int[] reads = {0};
        int movedDuringChange = FurnaceTransfer.insert(fixture.target, plan, 128, index -> {
            if (++reads[0] == 2) changing[1] = new ItemStack(Items.DIAMOND, 64);
            return changing[index];
        }, 2, null);
        check(movedDuringChange == 64 && changing[0].isEmpty() && changing[1].is(Items.DIAMOND)
                && changing[1].getCount() == 64, "replacement during transfer is reread, never debited using a stale stack reference");
        fixture = new FurnaceTransferChecks.Fixture();
        var disappearing = fixture;
        ItemStack[] current = {new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64)};
        plan = FurnaceTransfer.inputs(fixture.target, current[0], false, null);
        reads[0] = 0;
        int movedBeforeRemoval = FurnaceTransfer.insert(fixture.target, plan, 128, index -> {
            if (++reads[0] == 1) disappearing.target.entity().setRemoved();
            return current[index];
        }, 2, null);
        check(movedBeforeRemoval == 0 && Arrays.stream(current).mapToInt(ItemStack::getCount).sum() == 128,
                "destination removed while source is read cannot commit a transfer");
        Random random = new Random(26236);
        for (int iteration = 0; iteration < 1000; iteration++) {
            fixture = new FurnaceTransferChecks.Fixture();
            int original = 0;
            int space = 0;
            ItemStack[] stacks = new ItemStack[1 + random.nextInt(36)];
            for (int i = 0; i < stacks.length; i++) {
                stacks[i] = new ItemStack(Items.COBBLESTONE, 1 + random.nextInt(64));
                original += stacks[i].getCount();
            }
            for (int i = 0; i < 3; i++) {
                int count = random.nextInt(65);
                fixture.inventory.setItem(i, new ItemStack(Items.COBBLESTONE, count));
                space += 64 - count;
            }
            plan = FurnaceTransfer.inputs(fixture.target, stacks[0], false, null);
            int before = fixture.inventory.getItem(0).getCount() + fixture.inventory.getItem(1).getCount() + fixture.inventory.getItem(2).getCount();
            int moved = FurnaceTransfer.insert(fixture.target, plan, original, index -> stacks[index], stacks.length, null);
            int after = fixture.inventory.getItem(0).getCount() + fixture.inventory.getItem(1).getCount() + fixture.inventory.getItem(2).getCount();
            check(moved == Math.min(space, original) && moved == after - before, "exact partial transfer credit");
            check(Arrays.stream(stacks).mapToInt(ItemStack::getCount).sum() + moved == original, "conservation across up to 36 real sources");
            for (int i = 0; i < 3; i++) check(fixture.inventory.getItem(i).getCount() <= 64, "no slot overflow");
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private BulkChecks() { }
}
