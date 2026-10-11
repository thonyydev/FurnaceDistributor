package com.thonyy.furnacedistributor.neoforge.tests;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

@Mod("furnacedistributor_tests")
public final class IntegrationTestMod {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("furnacedistributor_tests");
    static final DeferredItem<Item> GENERATOR_FUEL = ITEMS.registerSimpleItem("generator_fuel");
    static final DeferredItem<Item> REMAINDER_FUEL = ITEMS.registerSimpleItem(
            "remainder_fuel", properties -> properties.craftRemainder(new ItemStackTemplate(Items.BUCKET)));

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, "furnacedistributor_tests");
    private static final List<Identifier> TESTS = new ArrayList<>();

    private static void add(String name, Consumer<GameTestHelper> test) {
        FUNCTIONS.register(name, () -> test);
        TESTS.add(Identifier.fromNamespaceAndPath("furnacedistributor_tests", name));
    }
    public IntegrationTestMod(IEventBus bus) {
        ITEMS.register(bus);
        add("vanillagametests.startupandvanilla128", VanillaGameTests::startupAndVanilla128);
        add("vanillagametests.blastsmokerandinvalidrecipes", VanillaGameTests::blastSmokerAndInvalidRecipes);
        add("vanillagametests.partialfullcomponentsandfuel", VanillaGameTests::partialFullComponentsAndFuel);
        add("vanillagametests.collectionfullpartialandxp", VanillaGameTests::collectionFullPartialAndXp);
        add("vanillagametests.distributionandcollectionsharecooldown", VanillaGameTests::distributionAndCollectionShareCooldown);
        add("vanillagametests.rangeandaccess", VanillaGameTests::rangeAndAccess);
        add("vanillagametests.unloadedchunksandvanillalock", VanillaGameTests::unloadedChunksAndVanillaLock);
        add("vanillagametests.distributionskippedsummary", VanillaGameTests::distributionSkippedSummary);
        add("vanillagametests.distribution64coalfeedback", VanillaGameTests::distribution64CoalFeedback);
        add("vanillagametests.distributionavailableitemsfeedback", VanillaGameTests::distributionAvailableItemsFeedback);
        add("vanillagametests.secondlockedfurnacerejectsdistributionandcollection", VanillaGameTests::secondLockedFurnaceRejectsDistributionAndCollection);
        if (ModList.get().isLoaded("ironfurnaces")) {
            add("irongametests.normal128andmixed256", IronGameTests::normal128AndMixed256);
            add("irongametests.normaloccupiedcomponentsandaugments", IronGameTests::normalOccupiedComponentsAndAugments);
            add("irongametests.factoryactivetiersandcapacity", IronGameTests::factoryActiveTiersAndCapacity);
            add("irongametests.factorypartialandglobalbalance", IronGameTests::factoryPartialAndGlobalBalance);
            add("irongametests.factoryandvanillabalance", IronGameTests::factoryAndVanillaBalance);
            add("irongametests.factoryoutputsfullpartialcomponentsandxp", IronGameTests::factoryOutputsFullPartialComponentsAndXp);
            add("irongametests.xpdisabledandgeneratorexcluded", IronGameTests::xpDisabledAndGeneratorExcluded);
            add("irongametests.factoryprocessesrealrecipesandxp", IronGameTests::factoryProcessesRealRecipesAndXp);
            add("irongametests.everyregisteredfurnacetier", IronGameTests::everyRegisteredFurnaceTier);
            add("irongametests.lockedsecondfurnacepreservesfactoryinputsoutputsandxp", IronGameTests::lockedSecondFurnacePreservesFactoryInputsOutputsAndXp);
            add("generatorgametests.balanced192and100", GeneratorGameTests::balanced192And100);
            add("generatorgametests.partialandredistribution", GeneratorGameTests::partialAndRedistribution);
            add("generatorgametests.invalidandincompatiblefuel", GeneratorGameTests::invalidAndIncompatibleFuel);
            add("generatorgametests.componentsandlimits", GeneratorGameTests::componentsAndLimits);
            add("generatorgametests.moddedfuelandcraftingremainder", GeneratorGameTests::moddedFuelAndCraftingRemainder);
            add("generatorgametests.mixedallmodes", GeneratorGameTests::mixedAllModes);
            add("generatorgametests.dynamicmodetransitions", GeneratorGameTests::dynamicModeTransitions);
            add("generatorgametests.augmentchangesbetweenplanandwrite", GeneratorGameTests::augmentChangesBetweenPlanAndWrite);
            add("generatorgametests.smokingblastingandheaterrules", GeneratorGameTests::smokingBlastingAndHeaterRules);
            add("generatorgametests.changedstacksbetweenplanandwrite", GeneratorGameTests::changedStacksBetweenPlanAndWrite);
            add("generatorgametests.everyregisteredgeneratortier", GeneratorGameTests::everyRegisteredGeneratorTier);
            add("generatorgametests.generatoroperationguard", GeneratorGameTests::generatorOperationGuard);
            add("generatorgametests.realgeneratorproducesenergywithoutoutputorxp", GeneratorGameTests::realGeneratorProducesEnergyWithoutOutputOrXp);
        }
        FUNCTIONS.register(bus);
        NeoForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent event) -> {
            if (event.getItemStack().is(GENERATOR_FUEL.get()) || event.getItemStack().is(REMAINDER_FUEL.get())) {
                event.setBurnTime(1600);
            }
        });
        bus.addListener((RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath("furnacedistributor_tests", "default"),
                    new TestEnvironmentDefinition.AllOf());
            for (var id : TESTS) event.registerTest(id, new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id),
                    new TestData<>(environment, Identifier.fromNamespaceAndPath("furnacedistributor_tests", "empty"), 200, 0, true)));
        });
    }
}
