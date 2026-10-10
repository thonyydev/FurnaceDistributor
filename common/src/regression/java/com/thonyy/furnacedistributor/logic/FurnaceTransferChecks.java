package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.furnace.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

import java.util.Arrays;
import java.util.Random;

/** Small Container fixtures exercise the real transfer code, without mocking a server/world. */
final class FurnaceTransferChecks {
    private static int checks;

    static int run() {
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
        check(FurnaceTransfer.capacity(ItemStack.EMPTY, stack, 64) == 64, "empty input");
        check(FurnaceTransfer.capacity(new ItemStack(Items.COBBLESTONE, 32), stack, 64) == 32, "partial input");
        check(FurnaceTransfer.capacity(new ItemStack(Items.COBBLESTONE, 64), stack, 64) == 0, "full input");
        check(FurnaceTransfer.capacity(new ItemStack(Items.IRON_ORE, 32), stack, 64) == 0, "incompatible input");
        ItemStack named = stack.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("components"));
        check(FurnaceTransfer.capacity(named, stack, 64) == 0, "components cannot mix");
        check(FurnaceTransfer.capacity(ItemStack.EMPTY, new ItemStack(Items.ENDER_PEARL), 64) == 16, "item limit");
        stack.set(DataComponents.MAX_STACK_SIZE, 8);
        check(FurnaceTransfer.capacity(new ItemStack(Items.COBBLESTONE, 12), stack, 64) == 0, "component stack limit");
        check(FurnaceTransfer.capacity(ItemStack.EMPTY, named, 16) == 16, "container limit");

        FurnaceBlockEntity vanilla = new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        check(FurnaceAccessRegistry.find(vanilla).access() instanceof VanillaFurnaceAccess, "vanilla adapter");
        check(FurnaceAccessRegistry.find(new BlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState()))
                .access() instanceof VanillaFurnaceAccess, "blast adapter");
        check(FurnaceAccessRegistry.find(new SmokerBlockEntity(BlockPos.ZERO, Blocks.SMOKER.defaultBlockState()))
                .access() instanceof VanillaFurnaceAccess, "smoker adapter");
        check(FurnaceAccessRegistry.find(null) == null, "no entity");
        vanilla.setRemoved();
        check(FurnaceAccessRegistry.find(vanilla) == null, "removed entity");

        // Cases 1/2/3: physical furnace totals are unchanged when a normal adapter is substituted.
        equal(DistributionPlan.allocate(128, new int[]{64, 64, 64}, true), new int[]{43, 43, 42}, "128 / 3");
        equal(DistributionPlan.allocate(256, new int[]{64, 64, 64, 64}, true), new int[]{64, 64, 64, 64}, "mixed normal");
        equal(DistributionPlan.allocate(128, new int[]{384, 64, 64}, true), new int[]{43, 43, 42}, "factory does not monopolize");
        equal(DistributionPlan.allocate(224, new int[]{96, 128}, true), new int[]{96, 128}, "capacity example");
        equal(DistributionPlan.allocate(128, new int[]{384, 384, 384}, true), new int[]{43, 43, 42}, "three factories");

        Fixture fixture = new Fixture();
        fixture.inventory.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        fixture.inventory.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        ItemStack source = new ItemStack(Items.COBBLESTONE, 128);
        FurnaceTransfer.Inputs inputs = FurnaceTransfer.inputs(fixture.target, source, false, null);
        equal(inputs.capacities(), new int[]{32, 0, 64}, "factory partial capacities");
        check(inputs.capacity() == 96, "factory capacity sum");
        check(FurnaceTransfer.insert(fixture.target, inputs, 128, source, null) == 96 && source.getCount() == 32,
                "debit only committed inserts");
        equal(new int[]{fixture.inventory.getItem(0).getCount(), fixture.inventory.getItem(1).getCount(),
                fixture.inventory.getItem(2).getCount()}, new int[]{64, 64, 64}, "factory filled exactly");

        fixture = new Fixture();
        source = named.copyWithCount(12);
        inputs = FurnaceTransfer.inputs(fixture.target, source, false, null);
        check(FurnaceTransfer.insert(fixture.target, inputs, 12, source, null) == 12 && source.isEmpty(), "named inserts");
        for (int slot = 0; slot < 3; slot++) {
            check(fixture.inventory.getItem(slot).getCount() == 4
                    && ItemStack.isSameItemSameComponents(named, fixture.inventory.getItem(slot)), "preserve components");
        }
        fixture.inventory.getItem(0).shrink(1);
        check(fixture.inventory.getItem(1).getCount() == 4 && named.getCount() == 64, "no shared mutable stacks");

        fixture = new Fixture();
        source = new ItemStack(Items.COBBLESTONE, 12);
        inputs = FurnaceTransfer.inputs(fixture.target, source, false, null);
        fixture.inventory.setItem(0, new ItemStack(Items.IRON_ORE, 1));
        check(FurnaceTransfer.insert(fixture.target, inputs, 12, source, null) == 8 && source.getCount() == 4,
                "recheck changed input; preserve uninserted source");
        check(fixture.inventory.getItem(0).is(Items.IRON_ORE), "never overwrite changed input");

        fixture = new Fixture();
        Inventory player = new Inventory(null);
        for (int slot = 3; slot < 6; slot++) fixture.inventory.setItem(slot, named.copyWithCount(slot));
        FurnaceTransfer.Collected collected = FurnaceTransfer.collect(fixture.target, player);
        check(collected.count() == 12 && !collected.remaining(), "all factory outputs collected");
        check(player.items.getFirst().getCount() == 12 && ItemStack.isSameItemSameComponents(player.items.getFirst(), named),
                "output components preserved");
        check(FurnaceTransfer.collect(fixture.target, player).count() == 0, "repeat collection cannot duplicate");

        for (int i = 0; i < player.items.size(); i++) player.items.set(i, new ItemStack(Items.STONE, 64));
        fixture.inventory.setItem(3, named.copyWithCount(80));
        fixture.inventory.setItem(4, named.copyWithCount(5));
        // Bypass SimpleContainer.setItem's clamp to represent an upstream oversized output.
        fixture.inventory.getItem(3).setCount(80);
        collected = FurnaceTransfer.collect(fixture.target, player);
        check(collected.count() == 0 && collected.remaining() && fixture.inventory.getItem(3).getCount() == 80,
                "full inventory preserves exact outputs");
        player.items.set(0, named.copyWithCount(60));
        collected = FurnaceTransfer.collect(fixture.target, player);
        check(collected.count() == 4 && collected.remaining() && fixture.inventory.getItem(3).getCount() == 76
                && fixture.inventory.getItem(4).getCount() == 5, "oversized partial remainder is never clamped/lost");

        // Planning snapshots the item/components, while execution uses the current quantity and capacity.
        fixture = new Fixture();
        source = new ItemStack(Items.COAL, 64);
        inputs = FurnaceTransfer.inputs(fixture.target, source, true, null);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("changed after planning"));
        check(FurnaceTransfer.insert(fixture.target, inputs, 64, source, null) == 0 && source.getCount() == 64
                && fixture.inventory.isEmpty(), "changed source components cancel stale plan");
        source = new ItemStack(Items.CHARCOAL, 64);
        check(FurnaceTransfer.insert(fixture.target, inputs, 64, source, null) == 0 && source.getCount() == 64,
                "changed source item cancels stale plan");
        source = new ItemStack(Items.COAL, 7);
        check(FurnaceTransfer.insert(fixture.target, inputs, 64, source, null) == 7 && source.isEmpty(),
                "reduced source quantity debits only available items");
        equal(DistributionPlan.allocate(192, new int[]{64, 64, 64}, true), new int[]{64, 64, 64}, "generator 192 / 3");
        equal(DistributionPlan.allocate(100, new int[]{64, 64, 64}, true), new int[]{34, 33, 33}, "generator 100 / 3");
        equal(DistributionPlan.allocate(96, new int[]{32, 64}, true), new int[]{32, 64}, "generator partial redistribution");

        Random random = new Random(4321211);
        for (int iteration = 0; iteration < 3000; iteration++) {
            int[][] slots = new int[1 + random.nextInt(64)][];
            int[] capacities = new int[slots.length];
            for (int f = 0; f < slots.length; f++) {
                slots[f] = new int[1 + random.nextInt(6)];
                for (int s = 0; s < slots[f].length; s++) slots[f][s] = random.nextInt(65);
                capacities[f] = Arrays.stream(slots[f]).sum();
            }
            int total = random.nextInt(4097);
            for (boolean smart : new boolean[]{false, true}) {
                int[] amounts = DistributionPlan.allocate(total, capacities, smart);
                int moved = 0;
                for (int f = 0; f < slots.length; f++) {
                    int[] split = DistributionPlan.allocate(amounts[f], slots[f], true);
                    check(Arrays.stream(split).sum() == amounts[f], "nested allocation conserves furnace share");
                    for (int s = 0; s < split.length; s++) check(split[s] >= 0 && split[s] <= slots[f][s], "processing capacity");
                    moved += Arrays.stream(split).sum();
                }
                check(moved <= total, "no item creation");
                if (smart) check(moved == Math.min(total, Arrays.stream(capacities).sum()), "no unnecessary remainder");
            }
        }
        return checks;
    }

    private static final class Fixture implements FurnaceAccess {
        final SimpleContainer inventory = new SimpleContainer(6);
        final FurnaceTarget target = new FurnaceTarget(new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState()), this);
        @Override public boolean supports(BlockEntity entity) { return true; }
        @Override public Container inventory(BlockEntity entity) { return inventory; }
        @Override public int[] inputSlots(BlockEntity entity, ItemStack stack, boolean fuel) { return new int[]{0, 1, 2}; }
        @Override public int[] outputSlots(BlockEntity entity) { return new int[]{3, 4, 5}; }
        @Override public boolean canInsert(BlockEntity entity, int slot, ItemStack stack, FurnaceRecipes recipes) { return true; }
        @Override public boolean canAccess(BlockEntity entity, ServerPlayer player) { return true; }
        @Override public void awardExperience(BlockEntity entity, ServerPlayer player) { }
    }

    private static void equal(int[] actual, int[] expected, String label) { check(Arrays.equals(actual, expected), label); }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
