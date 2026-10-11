package com.thonyy.furnacedistributor.neoforge.tests;

import com.thonyy.furnacedistributor.furnace.*;
import com.thonyy.furnacedistributor.logic.*;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;

import java.util.Arrays;

import static com.thonyy.furnacedistributor.neoforge.tests.IronGameTests.*;
import static com.thonyy.furnacedistributor.neoforge.tests.VanillaGameTests.*;
import static ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase.*;

/** Actual Iron Furnaces 26.2 / 1.0.0 entities and ticks; never included in production jars. */
public class GeneratorGameTests {
    private static ItemStack augment(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("ironfurnaces", "augment_" + name)));
    }

    private static void mode(BlockIronFurnaceTileBase furnace, String name) {
        furnace.setItem(AUGMENT_BLUE, name.isEmpty() ? ItemStack.EMPTY : augment(name));
        tick(furnace);
        furnace.furnaceSettings.autoSplit = 0;
    }

    private static BlockIronFurnaceTileBase generator(GameTestHelper helper, int index, String tier) {
        var furnace = iron(helper, index, tier, false);
        mode(furnace, "generator");
        helper.assertTrue(furnace.isGenerator(), "real Generator Augment activates " + tier);
        return furnace;
    }

    private static FurnaceTransfer.Inputs plan(GameTestHelper helper, BlockIronFurnaceTileBase furnace, ItemStack stack) {
        return FurnaceTransfer.inputs(FurnaceAccessRegistry.find(furnace), stack, false, new FurnaceRecipes(helper.getLevel(), stack));
    }

    private static int stored(BlockIronFurnaceTileBase furnace) { return furnace.getItem(GENERATOR_FUEL).getCount(); }

    public static void balanced192And100(GameTestHelper helper) {
        var a = generator(helper, 0, "iron");
        var b = generator(helper, 1, "gold");
        var c = generator(helper, 2, "diamond");
        var player = player(helper, new ItemStack(Items.COAL, 192));
        distribute(helper, player, 3);
        helper.assertTrue(stored(a) == 64 && stored(b) == 64 && stored(c) == 64 && player.getMainHandItem().isEmpty(), "192 -> 64/64/64");
        a.setItem(GENERATOR_FUEL, ItemStack.EMPTY); b.setItem(GENERATOR_FUEL, ItemStack.EMPTY); c.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 100));
        distribute(helper, player, 3);
        helper.assertTrue(stored(a) == 34 && stored(b) == 33 && stored(c) == 33 && player.getMainHandItem().isEmpty(), "100 -> 34/33/33");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 92));
        distribute(helper, player, 3);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 12));
        distribute(helper, player, 3); distribute(helper, player, 3);
        helper.assertTrue(stored(a) + stored(b) + stored(c) + player.getMainHandItem().getCount() == 204
                && player.getMainHandItem().getCount() == 12, "repeated full transfers conserve all coal");
        helper.succeed();
    }

    public static void partialAndRedistribution(GameTestHelper helper) {
        var a = generator(helper, 0, "iron");
        var b = generator(helper, 1, "gold");
        a.setItem(GENERATOR_FUEL, new ItemStack(Items.COAL, 32));
        var player = player(helper, new ItemStack(Items.COAL, 64));
        distribute(helper, player, 1);
        helper.assertTrue(stored(a) == 64 && player.getMainHandItem().getCount() == 32, "only remaining 32 debited");
        a.setItem(GENERATOR_FUEL, new ItemStack(Items.COAL, 32));
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 96));
        distribute(helper, player, 2);
        helper.assertTrue(stored(a) == 64 && stored(b) == 64 && player.getMainHandItem().isEmpty(), "32 + 64 remaining capacity");
        helper.succeed();
    }

    public static void invalidAndIncompatibleFuel(GameTestHelper helper) {
        var furnace = generator(helper, 0, "diamond");
        var player = player(helper, ItemStack.EMPTY);
        for (var item : new net.minecraft.world.item.Item[]{Items.COBBLESTONE, Items.IRON_ORE, Items.DIAMOND}) {
            player.getInventory().getNonEquipmentItems().set(0, new ItemStack(item, 16));
            helper.assertFalse(furnace.canPlaceItem(GENERATOR_FUEL, player.getMainHandItem()), "Iron rejects invalid fuel");
            distribute(helper, player, 1);
            helper.assertTrue(stored(furnace) == 0 && player.getMainHandItem().getCount() == 16
                    && furnace.getItem(INPUT).isEmpty() && inputCount(furnace) == 0, "no processing fallback");
        }
        furnace.setItem(GENERATOR_FUEL, new ItemStack(Items.CHARCOAL, 32));
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 64));
        distribute(helper, player, 1);
        helper.assertTrue(furnace.getItem(GENERATOR_FUEL).is(Items.CHARCOAL) && stored(furnace) == 32
                && player.getMainHandItem().getCount() == 64, "coal never overwrites charcoal");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.CHARCOAL, 64));
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 64 && player.getMainHandItem().getCount() == 32, "charcoal follows actual Iron validator");
        helper.succeed();
    }

    public static void componentsAndLimits(GameTestHelper helper) {
        var furnace = generator(helper, 0, "iron");
        ItemStack named = new ItemStack(Items.COAL, 20);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("generator fuel"));
        named.set(DataComponents.MAX_STACK_SIZE, 8);
        var player = player(helper, named);
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 8 && player.getMainHandItem().getCount() == 12
                && ItemStack.isSameItemSameComponents(furnace.getItem(GENERATOR_FUEL), named), "stack component limit and data preserved");
        furnace.getItem(GENERATOR_FUEL).shrink(4);
        helper.assertTrue(named.getCount() == 12, "source and destination are independent");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 16));
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 4 && player.getMainHandItem().getCount() == 16, "components prevent mixing");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.LAVA_BUCKET));
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 1 && furnace.getItem(GENERATOR_FUEL).is(Items.LAVA_BUCKET)
                && player.getMainHandItem().isEmpty(), "nonstackable fuel limit");
        helper.succeed();
    }

    public static void moddedFuelAndCraftingRemainder(GameTestHelper helper) {
        var furnace = generator(helper, 0, "gold");
        var player = player(helper, new ItemStack(IntegrationTestMod.GENERATOR_FUEL.get(), 64));
        helper.assertTrue(furnace.canPlaceItem(GENERATOR_FUEL, player.getMainHandItem()), "NeoForge fuel event accepted by Iron");
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 64 && player.getMainHandItem().isEmpty(), "real registered test-mod fuel");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(IntegrationTestMod.REMAINDER_FUEL.get(), 16));
        distribute(helper, player, 1); distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 1 && player.getMainHandItem().getCount() == 15, "stackable remainder fuel limited to one, like Iron GUI");
        tick(furnace);
        helper.assertTrue(furnace.getItem(GENERATOR_FUEL).is(Items.BUCKET) && stored(furnace) == 1, "real tick retains remainder in fuel slot");
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(GENERATOR_FUEL).is(Items.BUCKET) && player.getMainHandItem().getCount() == 15,
                "collection never takes generator fuel or its remainder");
        helper.succeed();
    }

    public static void mixedAllModes(GameTestHelper helper) {
        var vanilla = furnace(helper, 0, Blocks.FURNACE);
        var normal = iron(helper, 1, "iron", false);
        var factory = iron(helper, 2, "diamond", true);
        var generator = generator(helper, 3, "gold");
        var player = player(helper, new ItemStack(Items.COAL, 192));
        boolean factoryAccepts = Arrays.stream(FACTORY_INPUT).anyMatch(slot -> factory.canPlaceItem(slot, player.getMainHandItem()))
                && new FurnaceRecipes(helper.getLevel(), player.getMainHandItem()).has(factory.recipeType);
        helper.assertFalse(factoryAccepts, "stock Iron recipes do not process coal in Factory");
        distribute(helper, player, 4);
        helper.assertTrue(vanilla.getItem(1).getCount() == 64 && normal.getItem(FUEL).getCount() == 64
                && stored(generator) == 64 && inputCount(factory) == 0 && factory.getItem(FUEL).isEmpty()
                && player.getMainHandItem().isEmpty(), "physical furnace balance excludes invalid Factory processing input");
        helper.assertTrue(FurnaceArea.scan(helper.getLevel(), absolute(helper, 0), absolute(helper, 3)).positions().size() == 4,
                "one selection detects all four states");
        helper.succeed();
    }

    public static void dynamicModeTransitions(GameTestHelper helper) {
        var furnace = iron(helper, 0, "diamond", false);
        var adapter = FurnaceAccessRegistry.find(furnace).access(); // Populate class cache before mode changes.
        var player = player(helper, new ItemStack(Items.COAL, 12));
        mode(furnace, "generator"); distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 12, "Normal -> Generator uses generator fuel");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        mode(furnace, "");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 12)); distribute(helper, player, 1);
        helper.assertTrue(furnace.getItem(FUEL).getCount() == 12 && stored(furnace) == 0, "Generator -> Normal uses normal fuel");
        furnace.setItem(FUEL, ItemStack.EMPTY);
        mode(furnace, "factory"); mode(furnace, "generator");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 12)); distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 12 && inputCount(furnace) == 0, "Factory -> Generator");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        mode(furnace, "factory");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.RAW_IRON, 12)); distribute(helper, player, 1);
        helper.assertTrue(inputCount(furnace) == 12 && stored(furnace) == 0
                && FurnaceAccessRegistry.find(furnace).access() == adapter, "Generator -> Factory, same cached adapter");
        helper.succeed();
    }

    public static void augmentChangesBetweenPlanAndWrite(GameTestHelper helper) {
        var furnace = generator(helper, 0, "diamond");
        for (String next : new String[]{"", "factory"}) {
            ItemStack source = new ItemStack(Items.COAL, 64);
            var inputs = plan(helper, furnace, source);
            furnace.setItem(AUGMENT_BLUE, next.isEmpty() ? ItemStack.EMPTY : augment(next));
            helper.assertTrue(furnace.isGenerator(), "Iron has not updated its mode before tick");
            helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source,
                    new FurnaceRecipes(helper.getLevel(), source)) == 0 && source.getCount() == 64 && stored(furnace) == 0,
                    "pending augment change cancels old Generator plan");
            tick(furnace);
            helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source,
                    new FurnaceRecipes(helper.getLevel(), source)) == 0 && source.getCount() == 64, "settled mode also cancels old slot");
            mode(furnace, "generator");
        }
        ItemStack source = new ItemStack(Items.COAL, 64);
        var inputs = plan(helper, furnace, source);
        furnace.setItem(AUGMENT_RED, augment("smoking"));
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source,
                new FurnaceRecipes(helper.getLevel(), source)) == 0 && source.getCount() == 64, "pending red augment change rejected too");
        tick(furnace);
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source,
                new FurnaceRecipes(helper.getLevel(), source)) == 0 && source.getCount() == 64, "smoking Generator does not burn coal");
        furnace.setItem(AUGMENT_RED, ItemStack.EMPTY); tick(furnace);
        for (String previous : new String[]{"", "factory"}) {
            mode(furnace, previous);
            source = new ItemStack(previous.isEmpty() ? Items.COAL : Items.RAW_IRON, 64);
            inputs = plan(helper, furnace, source);
            helper.assertTrue(inputs.capacity() > 0, "old mode plan has real capacity");
            mode(furnace, "generator");
            helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source,
                    new FurnaceRecipes(helper.getLevel(), source)) == 0 && source.getCount() == 64,
                    "Normal/Factory plan cannot use its old slots in Generator");
        }
        helper.succeed();
    }

    public static void smokingBlastingAndHeaterRules(GameTestHelper helper) {
        var furnace = generator(helper, 0, "iron");
        var player = player(helper, new ItemStack(Items.BREAD, 16));
        furnace.setItem(AUGMENT_RED, augment("smoking")); tick(furnace);
        helper.assertTrue(furnace.canPlaceItem(GENERATOR_FUEL, player.getMainHandItem()), "Smoking accepts nutrition as fuel");
        distribute(helper, player, 1);
        helper.assertTrue(furnace.getItem(GENERATOR_FUEL).is(Items.BREAD) && stored(furnace) == 16
                && player.getMainHandItem().isEmpty(), "Smoking generator does not require a cooking recipe");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 16)); distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 0 && player.getMainHandItem().getCount() == 16, "Smoking rejects coal");
        furnace.setItem(AUGMENT_RED, augment("blasting")); tick(furnace);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.DIAMOND, 16));
        helper.assertTrue(furnace.hasGeneratorBlastingRecipe(player.getMainHandItem())
                && furnace.canPlaceItem(GENERATOR_FUEL, player.getMainHandItem()), "Blasting Generator has its own diamond energy recipe");
        distribute(helper, player, 1);
        helper.assertTrue(furnace.getItem(GENERATOR_FUEL).is(Items.DIAMOND) && stored(furnace) == 16
                && player.getMainHandItem().isEmpty(), "diamond valid only under real Blasting Generator rules");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COBBLESTONE, 16)); distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 0 && player.getMainHandItem().getCount() == 16, "Blasting rejects cobblestone despite cooking recipe");
        furnace.setItem(AUGMENT_RED, ItemStack.EMPTY); tick(furnace);
        var heater = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof ironfurnaces.items.ItemHeater).findFirst().orElseThrow();
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(heater));
        helper.assertTrue(isItemFuel(player.getMainHandItem(), furnace.recipeType, furnace.getLevel())
                && !furnace.canPlaceItem(GENERATOR_FUEL, player.getMainHandItem()), "isItemFuel accepts Heater but Generator Container rejects it");
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 0 && player.getMainHandItem().getCount() == 1, "no Heater insertion or processing fallback");
        helper.succeed();
    }

    public static void changedStacksBetweenPlanAndWrite(GameTestHelper helper) {
        var furnace = generator(helper, 0, "iron");
        ItemStack source = new ItemStack(Items.COAL, 64);
        var recipes = new FurnaceRecipes(helper.getLevel(), source);
        var inputs = plan(helper, furnace, source);
        furnace.setItem(GENERATOR_FUEL, new ItemStack(Items.CHARCOAL, 32));
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source, recipes) == 0
                && source.getCount() == 64 && stored(furnace) == 32, "changed destination cannot be overwritten");
        furnace.setItem(GENERATOR_FUEL, new ItemStack(Items.COAL, 60));
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source, recipes) == 4
                && source.getCount() == 60 && stored(furnace) == 64, "changed quantity rechecks capacity");
        furnace.setItem(GENERATOR_FUEL, ItemStack.EMPTY);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source, recipes) == 0
                && source.getCount() == 60 && stored(furnace) == 0, "changed source components cancel old plan");
        ItemStack different = new ItemStack(Items.CHARCOAL, 32);
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, different, recipes) == 0
                && different.getCount() == 32, "changed source item cancels old plan");
        source = new ItemStack(Items.COAL, 7);
        helper.assertTrue(FurnaceTransfer.insert(FurnaceAccessRegistry.find(furnace), inputs, 64, source, recipes) == 7
                && source.isEmpty() && stored(furnace) == 7, "smaller source quantity conserves actual available coal");
        helper.succeed();
    }

    public static void everyRegisteredGeneratorTier(GameTestHelper helper) {
        int supported = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("ironfurnaces") || !(block instanceof EntityBlock entityBlock)) continue;
            var entity = entityBlock.newBlockEntity(absolute(helper, 0), block.defaultBlockState());
            if (!(entity instanceof BlockIronFurnaceTileBase)) continue;
            helper.setBlock(FIRST, block);
            BlockIronFurnaceTileBase furnace = helper.getBlockEntity(FIRST, BlockIronFurnaceTileBase.class);
            mode(furnace, "generator");
            var player = player(helper, new ItemStack(Items.COAL, 64));
            distribute(helper, player, 1);
            helper.assertTrue(stored(furnace) == 64 && player.getMainHandItem().isEmpty(), "Generator distribution tier " + BuiltInRegistries.BLOCK.getKey(block));
            supported++;
        }
        helper.assertTrue(supported == 13, "all 13 registered normal/special tiers exercised: " + supported);
        helper.succeed();
    }

    public static void generatorOperationGuard(GameTestHelper helper) {
        var furnace = generator(helper, 0, "gold");
        var player = player(helper, new ItemStack(Items.COAL, 64));
        var pos = absolute(helper, 0);
        player.setPos(pos.getX() + 100, pos.getY(), pos.getZ());
        distribute(helper, player, 1);
        helper.assertTrue(stored(furnace) == 0 && player.getMainHandItem().getCount() == 64, "Generator cannot bypass distance");
        var far = pos.offset(10000, 0, 10000);
        helper.assertFalse(helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4), "unloaded destination");
        var scan = FurnaceArea.scan(helper.getLevel(), far, far);
        // Moving a real ServerPlayer to 'far' would itself load the chunk. Widen only the test's range
        // to reach the chunk guard with the player still nearby, then restore the actual config.
        int range = com.thonyy.furnacedistributor.config.ServerConfig.get().maxDistance;
        try {
            com.thonyy.furnacedistributor.config.ServerConfig.get().maxDistance = 20000;
            DistributionManager.distribute(player, far, far);
        } finally {
            com.thonyy.furnacedistributor.config.ServerConfig.get().maxDistance = range;
        }
        helper.assertTrue(!scan.valid() && "area_unloaded".equals(scan.error())
                && !helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4)
                && stored(furnace) == 0 && player.getMainHandItem().getCount() == 64, "unloaded target rejected without force loading");
        helper.succeed();
    }

    public static void realGeneratorProducesEnergyWithoutOutputOrXp(GameTestHelper helper) {
        var furnace = generator(helper, 0, "diamond");
        var player = player(helper, new ItemStack(Items.COAL, 8));
        distribute(helper, player, 1);
        helper.assertTrue(furnace.recipes.isEmpty() && helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "inserting fuel grants no XP");
        tick(furnace);
        helper.assertTrue(stored(furnace) == 7 && furnace.getEnergy() > 0 && furnace.recipes.isEmpty(), "real fuel consumption produces FE only");
        collect(helper, player, 1); collect(helper, player, 1);
        helper.assertTrue(stored(furnace) == 7 && player.getInventory().isEmpty()
                && Arrays.stream(FurnaceAccessRegistry.find(furnace).access().outputSlots(furnace)).allMatch(slot -> furnace.getItem(slot).isEmpty())
                && helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "no output, fuel collection or XP");
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()).orElseThrow();
        furnace.setRecipeUsed(recipe); // A historical cooking ledger must not be unlocked in Generator mode.
        FurnaceAccessRegistry.find(furnace).access().awardExperience(furnace, player);
        helper.assertTrue(!furnace.recipes.isEmpty() && helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "Generator never calls unlockRecipes");
        helper.succeed();
    }
}
