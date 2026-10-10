package com.thonyy.furnacedistributor.neoforge.tests;

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
    static final net.neoforged.neoforge.registries.DeferredItem<Item> GENERATOR_FUEL = ITEMS.registerSimpleItem("generator_fuel");
    static final net.neoforged.neoforge.registries.DeferredItem<Item> REMAINDER_FUEL = ITEMS.registerSimpleItem(
            "remainder_fuel", new Item.Properties().craftRemainder(Items.BUCKET));

    public IntegrationTestMod(IEventBus bus) {
        ITEMS.register(bus);
        NeoForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent event) -> {
            if (event.getItemStack().is(GENERATOR_FUEL.get()) || event.getItemStack().is(REMAINDER_FUEL.get())) {
                event.setBurnTime(1600);
            }
        });
        bus.addListener((RegisterGameTestsEvent event) -> {
            event.register(VanillaGameTests.class);
            if (ModList.get().isLoaded("ironfurnaces")) event.register(IronGameTests.class);
            if (ModList.get().isLoaded("ironfurnaces")) event.register(GeneratorGameTests.class);
        });
    }
}
