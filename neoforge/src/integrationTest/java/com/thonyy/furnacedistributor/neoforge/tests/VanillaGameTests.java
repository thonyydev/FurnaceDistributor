package com.thonyy.furnacedistributor.neoforge.tests;

import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.furnace.FurnaceAccessRegistry;
import com.thonyy.furnacedistributor.logic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.fml.ModList;

public class VanillaGameTests {
    static final String NAMESPACE = "furnacedistributor_tests";
    static final BlockPos FIRST = new BlockPos(1, 1, 1);

    static ServerPlayer player(GameTestHelper helper, ItemStack stack) {
        ServerConfig.get().requestCooldownTicks = 0;
        ServerConfig.get().smartDistribution = true;
        ServerConfig.get().allowPartialDistribution = false;
        ServerConfig.get().collectExperience = true;
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = helper.absolutePos(FIRST);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        player.getInventory().getNonEquipmentItems().set(0, stack);
        player.getInventory().setSelectedSlot(0);
        return player;
    }

    static BlockPos absolute(GameTestHelper helper, int index) { return helper.absolutePos(FIRST.offset(index, 0, 0)); }
    static void distribute(GameTestHelper helper, ServerPlayer player, int count) {
        DistributionManager.distribute(player, absolute(helper, 0), absolute(helper, count - 1));
    }
    static void collect(GameTestHelper helper, ServerPlayer player, int count) {
        CollectionManager.collect(player, absolute(helper, 0), absolute(helper, count - 1));
    }
    static AbstractFurnaceBlockEntity furnace(GameTestHelper helper, int index, Block block) {
        helper.setBlock(FIRST.offset(index, 0, 0), block);
        return helper.getBlockEntity(FIRST.offset(index, 0, 0), AbstractFurnaceBlockEntity.class);
    }

    private static int experience(GameTestHelper helper) {
        return helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).stream()
                .mapToInt(orb -> orb.getValue() * orbCount(orb)).sum();
    }

    private static int orbCount(net.minecraft.world.entity.ExperienceOrb orb) {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
        orb.saveWithoutId(output);
        return output.buildResult().getIntOr("Count", 1);
    }

    static void lock(GameTestHelper helper, AbstractFurnaceBlockEntity furnace) {
        var predicate = net.minecraft.advancements.predicates.ItemPredicate.Builder.item()
                .of(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), Items.TRIPWIRE_HOOK).build();
        furnace.applyComponents(net.minecraft.core.component.DataComponentMap.EMPTY,
                net.minecraft.core.component.DataComponentPatch.builder().set(DataComponents.LOCK, new net.minecraft.world.LockCode(predicate)).build());
    }

    public static void startupAndVanilla128(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("ironfurnaces") == Boolean.getBoolean("furnacedistributor.tests.expectIron"),
                "actual mod presence must match test run");
        if (!ModList.get().isLoaded("ironfurnaces")) {
            helper.assertTrue(VanillaGameTests.class.getClassLoader().getResource(
                    "ironfurnaces/tileentity/furnaces/BlockIronFurnaceTileBase.class") == null,
                    "Iron API classes must also be absent from the runtime classpath");
        }
        var a = furnace(helper, 0, Blocks.FURNACE);
        var b = furnace(helper, 1, Blocks.FURNACE);
        var c = furnace(helper, 2, Blocks.FURNACE);
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 128));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 43 && b.getItem(0).getCount() == 43 && c.getItem(0).getCount() == 42
                && player.getMainHandItem().isEmpty(), "case 1: vanilla 43/43/42");
        helper.assertTrue(FurnaceArea.scan(helper.getLevel(), absolute(helper, 0), absolute(helper, 2)).positions().size() == 3,
                "vanilla detection");
        helper.succeed();
    }

    public static void blastSmokerAndInvalidRecipes(GameTestHelper helper) {
        var a = furnace(helper, 0, Blocks.FURNACE);
        var b = furnace(helper, 1, Blocks.BLAST_FURNACE);
        var c = furnace(helper, 2, Blocks.SMOKER);
        ServerPlayer player = player(helper, new ItemStack(Items.RAW_IRON, 64));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 32 && b.getItem(0).getCount() == 32 && c.getItem(0).isEmpty(), "blast recipe");
        a.clearContent(); b.clearContent();
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.BEEF, 64));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 32 && c.getItem(0).getCount() == 32 && b.getItem(0).isEmpty(), "smoking recipe");
        a.clearContent(); c.clearContent();
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.DIAMOND, 64));
        distribute(helper, player, 3);
        helper.assertTrue(a.isEmpty() && b.isEmpty() && c.isEmpty() && player.getMainHandItem().getCount() == 64, "invalid recipe");
        helper.succeed();
    }

    public static void partialFullComponentsAndFuel(GameTestHelper helper) {
        var a = furnace(helper, 0, Blocks.FURNACE);
        var b = furnace(helper, 1, Blocks.FURNACE);
        var c = furnace(helper, 2, Blocks.FURNACE);
        a.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        b.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        c.setItem(0, new ItemStack(Items.IRON_ORE, 1));
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 64));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 64 && player.getMainHandItem().getCount() == 32
                && c.getItem(0).is(Items.IRON_ORE), "partial/full/incompatible inputs");
        ItemStack named = new ItemStack(Items.COBBLESTONE, 8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("preserved"));
        a.clearContent(); b.clearContent();
        player.getInventory().getNonEquipmentItems().set(0, named);
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(0).getCount() == 4 && b.getItem(0).getCount() == 4
                && a.getItem(0).has(DataComponents.CUSTOM_NAME), "component copies");
        a.getItem(0).shrink(1);
        helper.assertTrue(b.getItem(0).getCount() == 4, "independent mutable stacks");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 12));
        distribute(helper, player, 3);
        helper.assertTrue(a.getItem(1).getCount() == 4 && b.getItem(1).getCount() == 4 && c.getItem(1).getCount() == 4, "fuel priority");
        helper.succeed();
    }

    public static void collectionFullPartialAndXp(GameTestHelper helper) {
        var furnace = furnace(helper, 0, Blocks.FURNACE);
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()).orElseThrow();
        furnace.setRecipeUsed(recipe);
        // Two iron recipes guarantee at least one XP; one recipe can legitimately round down to zero.
        furnace.setRecipeUsed(recipe);
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 10));
        ServerPlayer player = player(helper, ItemStack.EMPTY);
        for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) player.getInventory().getNonEquipmentItems().set(i, new ItemStack(Items.STONE, 64));
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(2).getCount() == 10 && helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "full: no loss or XP");
        player.getInventory().getNonEquipmentItems().set(1, new ItemStack(Items.IRON_INGOT, 60));
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(2).getCount() == 6 && player.getInventory().getNonEquipmentItems().get(1).getCount() == 64, "partial output");
        helper.assertTrue(!helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(), "vanilla XP delivered");
        int experience = experience(helper);
        player.getInventory().getNonEquipmentItems().set(1, ItemStack.EMPTY);
        collect(helper, player, 1);
        collect(helper, player, 1);
        helper.assertTrue(furnace.getItem(2).isEmpty() && player.getInventory().getNonEquipmentItems().get(1).getCount() == 6
                && experience(helper) == experience, "repeat: no item or XP duplication");
        helper.succeed();
    }

    public static void distributionAndCollectionShareCooldown(GameTestHelper helper) {
        var furnace = furnace(helper, 0, Blocks.FURNACE);
        var player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        int cooldown = ServerConfig.get().requestCooldownTicks;
        try {
            ServerConfig.get().requestCooldownTicks = 5;
            distribute(helper, player, 1);
            helper.assertTrue(furnace.getItem(0).getCount() == 16 && player.getMainHandItem().isEmpty(), "first request accepted");
            furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 3));
            collect(helper, player, 1);
            helper.assertTrue(furnace.getItem(2).getCount() == 3 && player.getInventory().isEmpty(),
                    "distribution prevents collection in the same tick");
            ServerConfig.get().requestCooldownTicks = 0;
            collect(helper, player, 1);
            helper.assertTrue(furnace.getItem(2).isEmpty() && player.getMainHandItem().getCount() == 3, "collection accepted with cooldown disabled");
            ServerConfig.get().requestCooldownTicks = 5;
            player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COBBLESTONE, 8));
            distribute(helper, player, 1);
            helper.assertTrue(furnace.getItem(0).getCount() == 16 && player.getMainHandItem().getCount() == 8,
                    "collection prevents distribution in the same tick");
        } finally {
            ServerConfig.get().requestCooldownTicks = cooldown;
        }
        helper.runAfterDelay(4, () -> {
            int previous = ServerConfig.get().requestCooldownTicks;
            try {
                ServerConfig.get().requestCooldownTicks = 5;
                distribute(helper, player, 1);
                helper.assertTrue(furnace.getItem(0).getCount() == 16 && player.getMainHandItem().getCount() == 8, "cooldown still active at tick four");
            } finally {
                ServerConfig.get().requestCooldownTicks = previous;
            }
        });
        helper.runAfterDelay(5, () -> {
            int previous = ServerConfig.get().requestCooldownTicks;
            try {
                ServerConfig.get().requestCooldownTicks = 5;
                distribute(helper, player, 1);
                helper.assertTrue(furnace.getItem(0).getCount() == 24 && player.getMainHandItem().isEmpty(),
                        "cooldown expires at tick five; refusals do not postpone it");
                helper.succeed();
            } finally {
                ServerConfig.get().requestCooldownTicks = previous;
            }
        });
    }

    public static void rangeAndAccess(GameTestHelper helper) {
        var furnace = furnace(helper, 0, Blocks.FURNACE);
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        var pos = absolute(helper, 0);
        player.setPos(pos.getX() + 100, pos.getY(), pos.getZ());
        distribute(helper, player, 1);
        helper.assertTrue(furnace.isEmpty() && player.getMainHandItem().getCount() == 16, "distance rejection");
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.containerMenu = new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                1, player.getInventory(), new net.minecraft.world.SimpleContainer(9), 1);
        distribute(helper, player, 1);
        helper.assertTrue(furnace.isEmpty(), "active menu prevents conflicting operation");
        player.containerMenu = player.inventoryMenu;
        furnace.setRemoved();
        helper.assertTrue(FurnaceAccessRegistry.find(furnace) == null, "removed entity rejected");
        helper.succeed();
    }

    public static void unloadedChunksAndVanillaLock(GameTestHelper helper) {
        var furnace = furnace(helper, 0, Blocks.FURNACE);
        ServerPlayer player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        var far = absolute(helper, 0).offset(10000, 0, 10000);
        helper.assertFalse(helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4), "test chunk initially unloaded");
        var scan = FurnaceArea.scan(helper.getLevel(), far, far);
        helper.assertTrue(!scan.valid() && "area_unloaded".equals(scan.error())
                && !helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4), "scan never loads a chunk");
        lock(helper, furnace);
        distribute(helper, player, 1);
        helper.assertTrue(furnace.getItem(0).isEmpty() && player.getMainHandItem().getCount() == 16, "vanilla lock respected");
        helper.succeed();
    }

    public static void secondLockedFurnaceRejectsDistributionAndCollection(GameTestHelper helper) {
        var first = furnace(helper, 0, Blocks.FURNACE);
        var second = furnace(helper, 1, Blocks.FURNACE);
        lock(helper, second);
        var player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        boolean partial = ServerConfig.get().allowPartialDistribution;
        try {
            // Partial item allocation must never turn an authorization refusal into a partial transfer.
            ServerConfig.get().allowPartialDistribution = true;
            distribute(helper, player, 2);
            helper.assertTrue(first.getItem(0).isEmpty() && second.getItem(0).isEmpty()
                    && player.getMainHandItem().getCount() == 16, "second lock rejects all distribution before any debit");
            first.setItem(2, new ItemStack(Items.IRON_INGOT, 10));
            second.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
            var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING,
                    new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()).orElseThrow();
            first.setRecipeUsed(recipe);
            first.setRecipeUsed(recipe);
            collect(helper, player, 2);
            helper.assertTrue(first.getItem(2).getCount() == 10 && second.getItem(2).getCount() == 5
                    && player.getInventory().getNonEquipmentItems().stream().noneMatch(stack -> stack.is(Items.IRON_INGOT))
                    && player.getMainHandItem().getCount() == 16
                    && helper.getEntities(net.minecraft.world.entity.EntityTypes.EXPERIENCE_ORB, FIRST, 8).isEmpty(),
                    "second lock rejects all collection and XP before any extraction");
        } finally {
            ServerConfig.get().allowPartialDistribution = partial;
        }
        helper.succeed();
    }

    public static void distributionSkippedSummary(GameTestHelper helper) {
        var available = furnace(helper, 0, Blocks.FURNACE);
        var incompatible = furnace(helper, 1, Blocks.SMOKER);
        var full = furnace(helper, 2, Blocks.FURNACE);
        full.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        var player = player(helper, new ItemStack(Items.COBBLESTONE, 16));
        distribute(helper, player, 3);
        Component result = lastFeedback(player);
        var quantities = (net.minecraft.network.chat.contents.TranslatableContents) result.getContents();
        var reasons = (net.minecraft.network.chat.contents.TranslatableContents) result.getSiblings().getLast().getContents();
        helper.assertTrue(quantities.getKey().equals("message.furnacedistributor.distributed_items")
                && java.util.Arrays.equals(quantities.getArgs(), new Object[]{16, 1, 3})
                && reasons.getKey().equals("message.furnacedistributor.skipped_mixed")
                && java.util.Arrays.equals(reasons.getArgs(), new Object[]{1, 1}),
                "real server feedback reports committed quantities, incompatible smoker and full input separately");
        helper.assertTrue(available.getItem(0).getCount() == 16 && incompatible.isEmpty()
                && full.getItem(0).getCount() == 64 && player.getMainHandItem().isEmpty(),
                "ignored feedback does not change inventory transfers");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.DIAMOND, 16));
        distribute(helper, player, 3);
        result = lastFeedback(player);
        reasons = (net.minecraft.network.chat.contents.TranslatableContents) result.getSiblings().getLast().getContents();
        helper.assertTrue(quantities(result).getKey().equals("message.furnacedistributor.distributed_none")
                && reasons.getKey().equals("message.furnacedistributor.skipped_item")
                && java.util.Arrays.equals(reasons.getArgs(), new Object[]{3})
                && player.getMainHandItem().getCount() == 16, "all rejected inputs report three incompatibilities without item loss");
        helper.succeed();
    }

    public static void distribution64CoalFeedback(GameTestHelper helper) {
        var furnaces = new java.util.ArrayList<AbstractFurnaceBlockEntity>();
        for (int i = 0; i < 64; i++) {
            var pos = FIRST.offset(i % 4, i / 16, i / 4 % 4);
            helper.setBlock(pos, Blocks.FURNACE);
            var furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
            if (i >= 4) furnace.setItem(1, new ItemStack(Items.COAL, 64));
            furnaces.add(furnace);
        }
        var player = player(helper, new ItemStack(Items.COAL, 64));
        DistributionManager.distribute(player, helper.absolutePos(FIRST), helper.absolutePos(FIRST.offset(3, 3, 3)));
        Component result = lastFeedback(player);
        helper.assertTrue(java.util.Arrays.equals(quantities(result).getArgs(), new Object[]{64, 4, 64}),
                "64 coal distributed to four of 64 real furnaces");
        var skipped = quantities(result.getSiblings().getLast());
        helper.assertTrue(skipped.getKey().equals("message.furnacedistributor.skipped_space")
                && java.util.Arrays.equals(skipped.getArgs(), new Object[]{60}), "60 full fuel slots reported without zero categories");
        for (int i = 0; i < 64; i++) helper.assertTrue(furnaces.get(i).getItem(1).getCount() == (i < 4 ? 16 : 64),
                "exact real fuel transfer at furnace " + i);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "all 64 held coal transferred with no loss");
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 64));
        for (var furnace : furnaces) furnace.setItem(1, new ItemStack(Items.COAL, 64));
        DistributionManager.distribute(player, helper.absolutePos(FIRST), helper.absolutePos(FIRST.offset(3, 3, 3)));
        result = lastFeedback(player);
        helper.assertTrue(quantities(result).getKey().equals("message.furnacedistributor.distributed_none")
                && java.util.Arrays.equals(quantities(result).getArgs(), new Object[]{64})
                && java.util.Arrays.equals(quantities(result.getSiblings().getLast()).getArgs(), new Object[]{64})
                && player.getMainHandItem().getCount() == 64, "all full: zero moved, exact exclusion count, held items intact");
        helper.succeed();
    }

    public static void distributionAvailableItemsFeedback(GameTestHelper helper) {
        for (int i = 0; i < 8; i++) furnace(helper, i, Blocks.FURNACE);
        var player = player(helper, new ItemStack(Items.COBBLESTONE, 64));
        distribute(helper, player, 8);
        helper.assertTrue(java.util.Arrays.equals(quantities(lastFeedback(player)).getArgs(), new Object[]{64, 8, 8})
                && lastFeedback(player).getSiblings().isEmpty(), "all accepted: 8/8 without zero skipped");
        for (int i = 0; i < 8; i++) furnace(helper, i, Blocks.FURNACE).clearContent();
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COBBLESTONE, 2));
        distribute(helper, player, 8);
        helper.assertTrue(quantities(lastFeedback(player)).getKey().equals("message.furnacedistributor.not_enough_items")
                && player.getMainHandItem().getCount() == 2, "disallowed partial allocation remains a refusal");
        ServerConfig.get().allowPartialDistribution = true;
        try {
            distribute(helper, player, 8);
            helper.assertTrue(java.util.Arrays.equals(quantities(lastFeedback(player)).getArgs(), new Object[]{2, 2, 8})
                    && lastFeedback(player).getSiblings().isEmpty() && player.getMainHandItem().isEmpty(),
                    "six eligible destinations without items are never labelled full or incompatible");
            int stored = 0;
            for (int i = 0; i < 8; i++) stored += helper.getBlockEntity(FIRST.offset(i, 0, 0), AbstractFurnaceBlockEntity.class).getItem(0).getCount();
            helper.assertTrue(stored == 2, "feedback matches actual inventory credit");
        } finally {
            ServerConfig.get().allowPartialDistribution = false;
        }
        helper.succeed();
    }

    private static net.minecraft.network.chat.contents.TranslatableContents quantities(Component component) {
        return (net.minecraft.network.chat.contents.TranslatableContents) component.getContents();
    }

    private static Component lastFeedback(ServerPlayer player) {
        // Inspect the existing throttle snapshot; the headless mock player has no HUD to draw.
        try {
            var recent = com.thonyy.furnacedistributor.feedback.PlayerFeedback.class.getDeclaredField("RECENT");
            recent.setAccessible(true);
            Object throttle = ((java.util.Map<?, ?>) recent.get(null)).get(player);
            var previous = com.thonyy.furnacedistributor.feedback.FeedbackThrottle.class.getDeclaredField("previous");
            previous.setAccessible(true);
            return (Component) previous.get(throttle);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot inspect recorded action-bar result", exception);
        }
    }
}
