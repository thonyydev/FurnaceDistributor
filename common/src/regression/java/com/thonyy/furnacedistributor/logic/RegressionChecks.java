package com.thonyy.furnacedistributor.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thonyy.furnacedistributor.client.ClientConfig;
import com.thonyy.furnacedistributor.client.CollectionShortcut;
import com.thonyy.furnacedistributor.config.ConfigFile;
import com.thonyy.furnacedistributor.config.ServerConfig;
import com.thonyy.furnacedistributor.feedback.FeedbackThrottle;
import com.thonyy.furnacedistributor.network.CollectPacket;
import com.thonyy.furnacedistributor.network.DistributePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

/** Executable regression suite: assertions throw even when the JVM has assertions disabled. */
public final class RegressionChecks {
    private static int checks;

    public static void main(String[] arguments) throws Exception {
        areaBounds();
        collectionShortcut();
        distribution();
        configuration();
        translations(Path.of(arguments[0]));
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        inventory();
        packets();
        collectionArea();
        feedback();
        System.out.println("Furnace Distributor: " + checks + " regression checks passed.");
    }

    private static void areaBounds() {
        check(AreaBounds.between(0, 0, 0, 0, 0, 0).isWithinVolumeLimit(), "one block");
        check(AreaBounds.between(31, 31, 31, 0, 0, 0).isWithinVolumeLimit(), "inclusive boundary/reversed corners");
        check(!AreaBounds.between(0, 0, 0, 32, 31, 31).isWithinVolumeLimit(), "volume above limit");
        check(!AreaBounds.between(Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, 0, 0).isWithinVolumeLimit(), "coordinate overflow");
        check(!AreaBounds.between(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).isWithinVolumeLimit(), "product overflow");
        check(AreaBounds.between(0, 0, 0, 0, 0, 0).isWithinDistance(0.5, 0.5, 0.5, 8), "near area");
        check(!AreaBounds.between(0, 0, 0, 10, 0, 10).isWithinDistance(0.5, 0.5, 0.5, 10), "farthest crossed corner");
        check(AreaBounds.between(0, 0, 0, 8, 0, 0).isWithinDistance(0.5, 0.5, 0.5, 8), "distance boundary");
        check(!AreaBounds.between(0, 0, 0, 8, 0, 0).isWithinDistance(0.49, 0.5, 0.5, 8), "distance above boundary");
    }

    private static void distribution() {
        equal(DistributionPlan.allocate(64, new int[]{64, 64, 64}, true), new int[]{22, 21, 21}, "even split");
        equal(DistributionPlan.allocate(64, new int[]{0, 1, 64}, true), new int[]{0, 1, 63}, "redistribute unavailable capacity");
        equal(DistributionPlan.allocate(2, new int[]{64, 64, 64}, true), new int[]{1, 1, 0}, "partial distribution");
        equal(DistributionPlan.allocate(64, new int[]{1, 2, 3}, true), new int[]{1, 2, 3}, "retain overflow");
        equal(DistributionPlan.allocate(13, new int[]{64, 64, 64, 1}, true), new int[]{4, 4, 4, 1}, "fair remainder after saturation");
        equal(DistributionPlan.allocate(64, new int[]{0, 64}, false), new int[]{0, 32}, "legacy split");
        Random random = new Random(0xF012ACE);
        for (int iteration = 0; iteration < 10000; iteration++) {
            int[] capacities = new int[random.nextInt(65)];
            for (int i = 0; i < capacities.length; i++) capacities[i] = random.nextInt(100);
            int total = random.nextInt(1000);
            int[] amounts = DistributionPlan.allocate(total, capacities, true);
            check(Arrays.stream(amounts).sum() == Math.min(total, Arrays.stream(capacities).sum()), "conservation at " + iteration);
            for (int i = 0; i < amounts.length; i++) {
                check(amounts[i] >= 0 && amounts[i] <= capacities[i], "capacity at " + iteration);
            }
            // Among destinations that remain unsaturated, shares differ by at most one.
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < amounts.length; i++) {
                if (amounts[i] < capacities[i]) { min = Math.min(min, amounts[i]); max = Math.max(max, amounts[i]); }
            }
            check(min == Integer.MAX_VALUE || max - min <= 1, "fairness at " + iteration);
        }
    }

    private static void collectionShortcut() {
        for (int state = 0; state < 16; state++) {
            boolean selecting = (state & 1) != 0;
            boolean saved = (state & 2) != 0;
            boolean sneaking = (state & 4) != 0;
            boolean target = (state & 8) != 0;
            CollectionShortcut.Action actual = CollectionShortcut.resolve(selecting, saved, sneaking, target);
            if (selecting) {
                check(actual == CollectionShortcut.Action.SELECT_SECOND, "pending collection wins, including after releasing sneak: " + state);
            } else if (sneaking && target) {
                check(actual == CollectionShortcut.Action.SELECT_FIRST, "contextual selection overrides saved area: " + state);
            } else {
                check(actual == (saved ? CollectionShortcut.Action.COLLECT_SAVED : CollectionShortcut.Action.SELECT_FIRST),
                        "C retains previous behavior outside the new context: " + state);
            }
        }
    }

    private static void collectionArea() {
        BlockPos first = new BlockPos(3, 64, 5);
        AreaBounds single = FurnaceArea.bounds(first, first);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(single.minX(), single.minY(), single.minZ(),
                single.maxX(), single.maxY(), single.maxZ())) {
            check(pos.equals(first), "same furnace twice selects only that block");
            count++;
        }
        check(count == 1, "single furnace selection");
        BlockPos second = first.offset(-2, 0, 0);
        AreaBounds multiple = FurnaceArea.bounds(first, second);
        boolean includesFirst = false;
        boolean includesSecond = false;
        count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(multiple.minX(), multiple.minY(), multiple.minZ(),
                multiple.maxX(), multiple.maxY(), multiple.maxZ())) {
            includesFirst |= pos.equals(first);
            includesSecond |= pos.equals(second);
            count++;
        }
        check(includesFirst && includesSecond && count == 3, "area includes both furnaces and intermediate blocks, reversed corners");
    }

    private static void configuration() throws Exception {
        Path directory = Files.createTempDirectory("furnacedistributor-regression-");
        try {
            Path path = directory.resolve("server.json");
            ConfigFile<ServerConfig> file = new ConfigFile<>(path, ServerConfig.class, ServerConfig::validate);
            ServerConfig defaults = file.load(ServerConfig::new);
            check(Files.exists(path) && defaults.maxDistance == 32 && !defaults.allowPartialDistribution, "default file");
            Files.writeString(path, "{\"maxDistance\":-5,\"requestCooldownTicks\":500,\"futureOption\":123}");
            ServerConfig old = file.load(ServerConfig::new);
            check(old.maxDistance == 8 && old.requestCooldownTicks == 100 && old.smartDistribution && old.collectExperience,
                    "clamping and missing fields");
            check(file.save(old), "save validated config");
            check(JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("futureOption").getAsInt() == 123,
                    "preserve unknown fields");
            String broken = "{broken json";
            Files.writeString(path, broken);
            ServerConfig fallback = file.load(ServerConfig::new);
            check(fallback.maxDistance == 32 && Files.readString(path).equals(broken), "preserve malformed file on load");
            check(file.save(fallback), "recover config on explicit save");
            try (var files = Files.list(directory)) {
                check(files.anyMatch(candidate -> candidate.getFileName().toString().startsWith("server.json.invalid-")), "invalid backup");
            }
            Files.writeString(path, "null");
            check(file.load(ServerConfig::new).maxDistance == 32, "null config");
            ClientConfig client = new ClientConfig();
            client.selectionDisplayTicks = -1;
            client.validate();
            check(client.selectionDisplayTicks == 0, "client lower bound");
            client.selectionDisplayTicks = Integer.MAX_VALUE;
            client.validate();
            check(client.selectionDisplayTicks == 1200, "client upper bound");
        } finally {
            try (var files = Files.list(directory)) {
                for (Path path : files.toList()) Files.delete(path);
            }
            Files.delete(directory);
        }
    }

    private static void translations(Path root) throws Exception {
        Path languages = root.resolve("common/src/main/resources/assets/furnacedistributor/lang");
        JsonObject english = JsonParser.parseString(Files.readString(languages.resolve("en_us.json"))).getAsJsonObject();
        for (String locale : new String[]{"en_us", "pt_br", "es_es", "fr_fr", "de_de", "it_it", "pl_pl", "ru_ru",
                "zh_cn", "zh_tw", "ja_jp", "ko_kr"}) {
            check(Files.isRegularFile(languages.resolve(locale + ".json")), "required language " + locale);
        }
        Pattern placeholder = Pattern.compile("%(?:[0-9]+\\$)?[-#+ 0,(<]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z%]|§[0-9a-fk-or]",
                Pattern.CASE_INSENSITIVE);
        try (var files = Files.list(languages)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                JsonObject translated = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                check(english.keySet().equals(translated.keySet()), "translation key parity " + file.getFileName());
                for (String key : english.keySet()) {
                    String label = file.getFileName() + ": " + key;
                    check(translated.get(key).isJsonPrimitive() && translated.get(key).getAsJsonPrimitive().isString(),
                            "string translation " + label);
                    String value = translated.get(key).getAsString();
                    String original = english.get(key).getAsString();
                    check(!value.isBlank() && !value.contains("\uFFFD"), "nonempty Unicode translation " + label);
                    check(placeholder.matcher(original).results().map(result -> result.group()).toList()
                            .equals(placeholder.matcher(value).results().map(result -> result.group()).toList()),
                            "exact format tokens " + label);
                    check(value.contains("Furnace Distributor") == original.contains("Furnace Distributor"),
                            "mod name preserved " + label);
                    for (char symbol : new char[]{'✓', '·', '+'}) {
                        check(value.chars().filter(character -> character == symbol).count()
                                == original.chars().filter(character -> character == symbol).count(), "formatting symbol " + label);
                    }
                }
            }
        }
        // Literal keys in client/server source must exist; dynamic error/option keys are checked below.
        Pattern literal = Pattern.compile("\"((?:message|key|hud|config)\\.furnacedistributor\\.[a-z_]+)\"");
        try (var paths = Files.walk(root.resolve("common/src/main/java"))) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                var matcher = literal.matcher(Files.readString(path));
                while (matcher.find()) check(english.has(matcher.group(1)), "literal key " + matcher.group(1));
            }
        }
        for (String key : new String[]{"area_too_large", "area_too_far", "area_outside_world", "area_unloaded",
                "area_protected", "too_many_furnaces", "no_item", "no_furnaces", "no_capacity", "invalid_item", "not_enough_items"}) {
            check(english.has("message.furnacedistributor." + key), "dynamic error " + key);
        }
        for (String key : new String[]{"outlines", "preview", "hud"}) {
            check(english.has("config.furnacedistributor." + key) && english.has("config.furnacedistributor." + key + ".tooltip"),
                    "dynamic setting " + key);
        }
    }

    private static void inventory() {
        Inventory inventory = new Inventory(null);
        ItemStack emptyInventoryOutput = new ItemStack(Items.IRON_INGOT, 32);
        InventoryTransfer.insert(inventory, emptyInventoryOutput);
        check(emptyInventoryOutput.isEmpty() && inventory.items.get(0).getCount() == 32, "empty inventory collection");
        InventoryTransfer.insert(inventory, ItemStack.EMPTY);
        check(inventory.items.get(0).getCount() == 32 && inventory.items.get(1).isEmpty(), "empty furnace output changes nothing");
        inventory = new Inventory(null);
        inventory.items.set(0, new ItemStack(Items.IRON_INGOT, 60));
        ItemStack remaining = new ItemStack(Items.IRON_INGOT, 10);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.isEmpty() && inventory.items.get(0).getCount() == 64 && inventory.items.get(1).getCount() == 6,
                "merge before empty slots");
        inventory = new Inventory(null);
        for (int i = 0; i < inventory.items.size(); i++) inventory.items.set(i, new ItemStack(Items.STONE, 64));
        inventory.offhand.set(0, new ItemStack(Items.IRON_INGOT, 63));
        remaining = new ItemStack(Items.IRON_INGOT, 10);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.offhand.get(0).getCount() == 64 && inventory.armor.get(0).isEmpty(),
                "partial offhand merge; full inventory preserves remainder and armour");
        inventory.offhand.set(0, ItemStack.EMPTY);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.offhand.get(0).isEmpty(), "empty offhand is not filled");
        inventory.items.set(0, new ItemStack(Items.IRON_INGOT, 61));
        ItemStack firstOutput = new ItemStack(Items.IRON_INGOT, 2);
        ItemStack secondOutput = new ItemStack(Items.IRON_INGOT, 5);
        InventoryTransfer.insert(inventory, firstOutput);
        InventoryTransfer.insert(inventory, secondOutput);
        check(firstOutput.isEmpty() && secondOutput.getCount() == 4 && inventory.items.get(0).getCount() == 64,
                "multiple furnace outputs share available capacity and preserve partial remainder");
        ItemStack fullOutput = new ItemStack(Items.IRON_INGOT, 7);
        InventoryTransfer.insert(inventory, fullOutput);
        check(fullOutput.getCount() == 7 && inventory.items.get(0).getCount() == 64, "full inventory leaves output untouched");
        inventory.items.set(0, new ItemStack(Items.IRON_INGOT, 50));
        inventory.items.get(0).set(DataComponents.CUSTOM_NAME, Component.literal("different components"));
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.items.get(0).getCount() == 50, "different components do not merge");
        inventory = new Inventory(null);
        remaining = new ItemStack(Items.ENDER_PEARL, 20);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.isEmpty() && inventory.items.get(0).getCount() == 16 && inventory.items.get(1).getCount() == 4,
                "item stack limits");
    }

    private static void packets() {
        BlockPos first = new BlockPos(-30000000, -64, 30000000);
        BlockPos second = new BlockPos(30000000, 319, -30000000);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            DistributePacket distribution = new DistributePacket(first, second);
            DistributePacket.STREAM_CODEC.encode(buffer, distribution);
            check(buffer.readableBytes() == 16, "distribution wire format preserved");
            check(DistributePacket.STREAM_CODEC.decode(buffer).equals(distribution), "distribution packet round trip");
            buffer.clear();
            CollectPacket collection = new CollectPacket(second, first);
            CollectPacket.STREAM_CODEC.encode(buffer, collection);
            check(buffer.readableBytes() == 16, "collection wire format preserved");
            check(CollectPacket.STREAM_CODEC.decode(buffer).equals(collection), "collection packet round trip");
            check(!buffer.isReadable(), "no unexpected payload data");
        } finally {
            buffer.release();
        }
    }

    private static void feedback() {
        FeedbackThrottle throttle = new FeedbackThrottle();
        Component empty = Component.translatable("message.furnacedistributor.no_smelted_items");
        check(throttle.shouldShow(empty, 100, true), "first action-bar feedback");
        check(!throttle.shouldShow(empty.copy(), 100, true), "duplicate in the same tick");
        check(!throttle.shouldShow(empty.copy(), 119, true), "repeated feedback during cooldown");
        check(throttle.shouldShow(empty.copy(), 120, true), "suppression does not postpone refresh indefinitely");
        check(throttle.shouldShow(Component.translatable("message.furnacedistributor.inventory_full"), 120, true),
                "changed error is immediate");
        check(throttle.shouldShow(Component.translatable("message.furnacedistributor.collected_items", 10, 2), 120, true),
                "changed result is immediate");
        check(throttle.shouldShow(Component.translatable("message.furnacedistributor.collected_items", 11, 2), 120, true),
                "changed counts are not hidden");
        check(throttle.shouldShow(empty, 121, false), "important chat remains separate");
        check(throttle.shouldShow(empty, 121, true), "changing presentation channel is immediate");
        check(throttle.shouldShow(empty, 0, true), "clock reset cannot suppress feedback");
        Component mutable = Component.literal("test");
        check(throttle.shouldShow(mutable, 1, true), "mutable message initial state");
        mutable = mutable.copy().withStyle(ChatFormatting.RED);
        check(throttle.shouldShow(mutable, 2, true), "changed severity is immediate");
        check(new FeedbackThrottle().shouldShow(mutable, 2, true), "fresh session does not inherit suppression");
    }

    private static void equal(int[] actual, int[] expected, String label) {
        check(Arrays.equals(actual, expected), label + ": " + Arrays.toString(actual));
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
