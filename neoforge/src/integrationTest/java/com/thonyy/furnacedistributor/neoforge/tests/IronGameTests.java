package com.thonyy.furnacedistributor.neoforge.tests;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.furnace.*;
import com.thonyy.furnacedistributor.logic.*;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

import static com.thonyy.furnacedistributor.neoforge.tests.VanillaGameTests.*;

/** Registered only when Iron is really loaded; tests use its actual entities, recipes and tick. */
@PrefixGameTestTemplate(false)
public class IronGameTests {
    static BlockIronFurnaceTileBase iron(GameTestHelper helper, int index, String name, boolean factory) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ironfurnaces", name + "_furnace"));
        helper.assertTrue(block != Blocks.AIR, "known Iron block " + name);
        helper.setBlock(FIRST.offset(index, 0, 0), block);
        BlockIronFurnaceTileBase furnace = helper.getBlockEntity(FIRST.offset(index, 0, 0));
        if (factory) {
            furnace.setItem(BlockIronFurnaceTileBase.AUGMENT_BLUE, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("ironfurnaces", "augment_factory"))));
            tick(furnace);
            helper.assertTrue(furnace.isFactory(), "actual Factory Augment activates mode");
            furnace.furnaceSettings.autoSplit = 0;
        }
        return furnace;
    }
    static void tick(BlockIronFurnaceTileBase furnace) {
        BlockIronFurnaceTileBase.tick(furnace.getLevel(), furnace.getBlockPos(), furnace.getBlockState(), furnace);
    }
    static int inputCount(BlockIronFurnaceTileBase furnace) {
        return Arrays.stream(BlockIronFurnaceTileBase.FACTORY_INPUT).map(slot -> furnace.getItem(slot).getCount()).sum();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void normal128AndMixed256(GameTestHelper helper) {
        var a = iron(helper, 0, "iron", false);
        var b = iron(helper, 1, "gold", false);
        var c = iron(helper, 2, "diamond", false);
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 128));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 43 && b.getItem(0).getCount() == 43 && c.getItem(0).getCount() == 42,
                "case 2: all normal Iron tiers balanced");
        var v1 = furnace(helper, 0, Blocks.FURNACE);
        var v2 = furnace(helper, 1, Blocks.FURNACE);
        c.clearContent();
        var d = iron(helper, 3, "netherite", false);
        player.getInventory().items.set(0, new ItemStack(Items.COBBLESTONE, 256));
        distribute(helper, player, 4);
        helper.assertTrue(v1.getItem(0).getCount() == 64 && v2.getItem(0).getCount() == 64
                && c.getItem(0).getCount() == 64 && d.getItem(0).getCount() == 64 && player.getMainHandItem().isEmpty(),
                "case 3: vanilla + Iron");
        helper.assertTrue(FurnaceArea.scan(helper.getLevel(), absolute(helper, 0), absolute(helper, 3)).positions().size() == 4,
                "mixed selection detection");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void normalOccupiedComponentsAndAugments(GameTestHelper helper) {
        var a = iron(helper, 0, "iron", false);
        var b = iron(helper, 1, "gold", false);
        a.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        b.setItem(0, new ItemStack(Items.IRON_ORE, 5));
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 64));
        distribute(helper, player, 2);
        helper.assertTrue(a.getItem(0).getCount() == 64 && b.getItem(0).getCount() == 5 && b.getItem(0).is(Items.IRON_ORE)
                && player.getMainHandItem().getCount() == 32, "cases 4/5: partial capacity and incompatible input");
        a.clearContent(); b.clearContent();
        ItemStack named = new ItemStack(Items.COBBLESTONE, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("exact data"));
        player.getInventory().items.set(0, named);
        distribute(helper, player, 2);
        helper.assertTrue(a.getItem(0).getCount() == 5 && b.getItem(0).getCount() == 5 && a.getItem(0).has(DataComponents.CUSTOM_NAME),
                "Iron input components preserved");
        a.clearContent(); b.clearContent();
        a.setItem(BlockIronFurnaceTileBase.AUGMENT_RED, new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ironfurnaces", "augment_blasting"))));
        tick(a);
        player.getInventory().items.set(0, new ItemStack(Items.COBBLESTONE, 10));
        distribute(helper, player, 2);
        helper.assertTrue(a.getItem(0).isEmpty() && b.getItem(0).getCount() == 10, "blasting augment restricts recipes");
        player.getInventory().items.set(0, new ItemStack(Items.DIAMOND, 10));
        distribute(helper, player, 2);
        helper.assertTrue(player.getMainHandItem().getCount() == 10, "Iron rejects invalid recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void factoryActiveTiersAndCapacity(GameTestHelper helper) {
        for (int i = 0; i < 3; i++) {
            var furnace = iron(helper, i, new String[]{"copper", "gold", "diamond"}[i], true);
            ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
            var target = FurnaceAccessRegistry.find(furnace);
            var inputs = FurnaceTransfer.inputs(target, stack, false, new FurnaceRecipes(helper.getLevel(), stack));
            int active = (int) Arrays.stream(inputs.capacities()).filter(capacity -> capacity > 0).count();
            int expected = furnace.getTier() == 0 ? 2 : furnace.getTier() == 1 ? 4 : 6;
            helper.assertTrue(active == expected && inputs.capacity() == expected * 64, "tier contract for " + furnace.getTier());
            ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 64));
            DistributionManager.distribute(player, absolute(helper, i), absolute(helper, i));
            helper.assertTrue(inputCount(furnace) == 64 && furnace.getItem(0).isEmpty(), "case 6: actual Factory inputs only");
            for (int slot : BlockIronFurnaceTileBase.FACTORY_INPUT) {
                helper.assertTrue(furnace.getItem(slot).isEmpty() || furnace.canPlaceItem(slot, stack), "inactive input remains empty");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void factoryPartialAndGlobalBalance(GameTestHelper helper) {
        var a = iron(helper, 0, "diamond", true);
        var b = iron(helper, 1, "diamond", true);
        var c = iron(helper, 2, "diamond", true);
        int[] slots = BlockIronFurnaceTileBase.FACTORY_INPUT;
        a.setItem(slots[0], new ItemStack(Items.COBBLESTONE, 32));
        a.setItem(slots[1], new ItemStack(Items.COBBLESTONE, 64));
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 64);
        var inputs = FurnaceTransfer.inputs(FurnaceAccessRegistry.find(a), stack, false, new FurnaceRecipes(helper.getLevel(), stack));
        helper.assertTrue(inputs.capacities()[0] == 32 && inputs.capacities()[1] == 0 && inputs.capacities()[2] == 64
                && inputs.capacity() == 288, "case 7: exact partial capacity");
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 128));
        distribute(helper, player, 3);
        helper.assertTrue(inputCount(a) - 96 == 43 && inputCount(b) == 43 && inputCount(c) == 42, "case 8: global furnace balance");
        helper.assertTrue(a.getItem(slots[1]).getCount() == 64 && player.getMainHandItem().isEmpty(), "full processing slot unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void factoryAndVanillaBalance(GameTestHelper helper) {
        var a = iron(helper, 0, "diamond", true);
        var b = furnace(helper, 1, Blocks.FURNACE);
        var c = iron(helper, 2, "iron", false);
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 128));
        distribute(helper, player, 3);
        helper.assertTrue(inputCount(a) == 43 && b.getItem(0).getCount() == 43 && c.getItem(0).getCount() == 42,
                "Factory cannot monopolize normal furnace shares");
        a.inventory.subList(7, 19).replaceAll(ignored -> ItemStack.EMPTY);
        b.clearContent(); c.clearContent();
        ServerConfig.get().smartDistribution = false;
        b.setItem(0, new ItemStack(Items.IRON_ORE, 1));
        player.getInventory().items.set(0, new ItemStack(Items.COBBLESTONE, 60));
        distribute(helper, player, 3);
        helper.assertTrue(inputCount(a) == 20 && c.getItem(0).getCount() == 20 && player.getMainHandItem().getCount() == 20,
                "legacy non-smart distribution preserved");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void factoryOutputsFullPartialComponentsAndXp(GameTestHelper helper) {
        var furnace = iron(helper, 0, "diamond", true);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()).orElseThrow();
        furnace.setRecipeUsed(recipe);
        int[] outputs = FurnaceAccessRegistry.find(furnace).access().outputSlots(furnace);
        ItemStack named = new ItemStack(Items.IRON_INGOT, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("factory output"));
        for (int slot : outputs) furnace.setItem(slot, named.copy());
        ServerPlayer player = player(helper, ItemStack.EMPTY);
        for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        collect(helper, player, 1);
        helper.assertTrue(furnace.recipes.size() == 1 && Arrays.stream(outputs).map(slot -> furnace.getItem(slot).getCount()).sum() == 70,
                "case 10: full inventory leaves items and XP intact");
        player.getInventory().items.set(1, named.copyWithCount(60));
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(outputs[0]).getCount() == 6 && furnace.recipes.isEmpty(), "partial output clears XP once");
        helper.assertTrue(!helper.getEntities(EntityType.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "Iron XP delivered");
        int orbs = helper.getEntities(EntityType.EXPERIENCE_ORB, FIRST, 8).size();
        player.getInventory().items.set(1, ItemStack.EMPTY);
        player.getInventory().items.set(2, ItemStack.EMPTY);
        collect(helper, player, 1);
        collect(helper, player, 1);
        helper.assertTrue(Arrays.stream(outputs).allMatch(slot -> furnace.getItem(slot).isEmpty())
                && player.getInventory().items.get(1).getCount() + player.getInventory().items.get(2).getCount() == 66,
                "case 9: all outputs collected, including inactive normal output");
        helper.assertTrue(player.getInventory().items.get(1).has(DataComponents.CUSTOM_NAME)
                && helper.getEntities(EntityType.EXPERIENCE_ORB, FIRST, 8).size() == orbs, "preserve data; no duplicate XP");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void xpDisabledAndGeneratorExcluded(GameTestHelper helper) {
        var furnace = iron(helper, 0, "iron", false);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()).orElseThrow();
        furnace.setRecipeUsed(recipe);
        furnace.setItem(BlockIronFurnaceTileBase.OUTPUT, new ItemStack(Items.IRON_INGOT, 2));
        ServerPlayer player = player(helper, ItemStack.EMPTY);
        ServerConfig.get().collectExperience = false;
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(2).isEmpty() && furnace.recipes.size() == 1, "disabled XP preserves ledger");
        furnace.setItem(BlockIronFurnaceTileBase.AUGMENT_BLUE, new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ironfurnaces", "augment_generator"))));
        tick(furnace);
        helper.assertTrue(furnace.isGenerator(), "generator mode active");
        player.getInventory().items.set(0, new ItemStack(Items.COBBLESTONE, 16));
        distribute(helper, player, 1);
        helper.assertTrue(player.getMainHandItem().getCount() == 16 && furnace.getItem(6).isEmpty(), "generator/augment slots untouched");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void factoryProcessesRealRecipesAndXp(GameTestHelper helper) {
        var furnace = iron(helper, 0, "diamond", true);
        ServerPlayer player = player(helper, new ItemStack(Items.RAW_IRON, 6));
        distribute(helper, player, 1);
        furnace.setEnergy(furnace.getCapacity());
        for (int i = 0; i < 300 && inputCount(furnace) > 0; i++) tick(furnace);
        helper.assertTrue(inputCount(furnace) == 0 && furnace.recipes.values().intStream().sum() == 6, "six actual cooking processes");
        for (int input : BlockIronFurnaceTileBase.FACTORY_INPUT) {
            helper.assertTrue(furnace.getItem(input + BlockIronFurnaceTileBase.FACTORY_INPUT.length).is(Items.IRON_INGOT),
                    "real recipe output pairs match adapter");
        }
        collect(helper, player, 1);
        helper.assertTrue(player.getInventory().items.stream().filter(stack -> stack.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum() == 6
                && furnace.recipes.isEmpty() && !helper.getEntities(EntityType.EXPERIENCE_ORB, FIRST, 8).isEmpty(),
                "end-to-end Factory processing, collection and XP");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NAMESPACE)
    public static void everyRegisteredFurnaceTier(GameTestHelper helper) {
        int supported = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("ironfurnaces")
                    || !(block instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) continue;
            var entity = entityBlock.newBlockEntity(absolute(helper, 0), block.defaultBlockState());
            if (!(entity instanceof BlockIronFurnaceTileBase)) continue;
            helper.assertTrue(FurnaceAccessRegistry.find(entity).access() instanceof
                    com.thonyy.furnacedistributor.neoforge.compat.IronFurnacesAccess, "adapter covers " + BuiltInRegistries.BLOCK.getKey(block));
            supported++;
        }
        helper.assertTrue(supported >= 10, "all normal/special Iron tiers found: " + supported);
        helper.succeed();
    }
}
