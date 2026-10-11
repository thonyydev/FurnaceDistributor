package com.thonyy.furnacedistributor.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thonyy.furnacedistributor.client.ClientConfig;
import com.thonyy.furnacedistributor.client.CollectionShortcut;
import com.thonyy.furnacedistributor.client.ClientEvents;
import com.thonyy.furnacedistributor.client.FurnaceSelectionHandler;
import com.thonyy.furnacedistributor.client.SelectionFeedback;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
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
import java.util.Map;
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
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup())
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        clientInputAndSelection();
        contextualTips();
        checks += com.thonyy.furnacedistributor.client.UiAnimationChecks.run();
        inventory();
        checks += FurnaceTransferChecks.run();
        packets();
        checks += BulkChecks.run();
        checks += com.thonyy.furnacedistributor.client.BulkUiChecks.run(Path.of(arguments[0]));
        collectionArea();
        feedback();
        distributionFeedback(Path.of(arguments[0]));
        renderSubmissions();
        previewCulling();
        hudWrapping(Path.of(arguments[0]));
        distributionActionBarTarget();
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

    private static void clientInputAndSelection() throws ReflectiveOperationException {
        // Exercise client state without launching Minecraft or exposing test-only public methods.
        var consume = ClientEvents.class.getDeclaredMethod("consumeClicks", KeyMapping.class);
        consume.setAccessible(true);
        var key = new KeyMapping("regression.clicks", InputConstants.Type.MOUSE, 7, KeyMapping.Category.MISC);
        check(!(boolean) consume.invoke(null, key), "empty key queue does nothing");
        for (int clicks : new int[]{1, 2, 20}) {
            for (int i = 0; i < clicks; i++) KeyMapping.click(key.getDefaultKey());
            int actions = 0;
            if ((boolean) consume.invoke(null, key)) actions++;
            check(actions == 1 && !key.consumeClick(), "one action and no queued repeat for " + clicks + " clicks");
            check(!(boolean) consume.invoke(null, key), "next tick does not inherit clicks");
        }
        var tick = FurnaceSelectionHandler.class.getDeclaredMethod("tick", boolean.class);
        tick.setAccessible(true);
        BlockPos first = new BlockPos(3, 64, 5);
        try {
            for (var entry : Map.<String, Object>of("firstPos", first, "selectionMode", true,
                    "lastConfirmedFirstPos", first, "lastConfirmedSecondPos", first.offset(2, 0, 0)).entrySet()) {
                var field = FurnaceSelectionHandler.class.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(null, entry.getValue());
            }
            tick.invoke(null, true);
            check(FurnaceSelectionHandler.isInSelectionMode() && first.equals(FurnaceSelectionHandler.getFirstPos()),
                    "valid first furnace retains pending selection");
            SelectionFeedback.confirmed(false, 2);
            tick.invoke(null, false);
            check(!FurnaceSelectionHandler.isInSelectionMode() && FurnaceSelectionHandler.getFirstPos() == null
                    && FurnaceSelectionHandler.getSecondPos() == null && !SelectionFeedback.isConfirmed(false),
                    "removed first furnace cancels selection and feedback");
            check(FurnaceSelectionHandler.hasConfirmedArea()
                    && first.equals(FurnaceSelectionHandler.getLastConfirmedFirstPos()), "cancel preserves saved area");
        } finally {
            FurnaceSelectionHandler.resetSelection();
            FurnaceSelectionHandler.clearConfirmedArea();
            SelectionFeedback.clear();
        }
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
            check(old.enableBulkDistribution && old.maxBulkSlots == 36, "old server configs enable bulk with a bounded slot budget");
            old.enableBulkDistribution = false;
            old.maxBulkSlots = 1000;
            check(file.save(old), "save validated config");
            ServerConfig reloaded = file.load(ServerConfig::new);
            check(!reloaded.enableBulkDistribution && reloaded.maxBulkSlots == 36, "bulk server settings survive restart and clamp upper bound");
            reloaded.maxBulkSlots = -1;
            reloaded.validate();
            check(reloaded.maxBulkSlots == 1, "bulk server slot lower bound");
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
            Path clientPath = directory.resolve("client.json");
            Files.writeString(clientPath, "{\"showItemPreview\":false,\"selectionDisplayTicks\":200,\"futureOption\":123}");
            ConfigFile<ClientConfig> clientFile = new ConfigFile<>(clientPath, ClientConfig.class, ClientConfig::validate);
            ClientConfig existing = clientFile.load(ClientConfig::new);
            check(!existing.showItemPreview && existing.selectionDisplayTicks == 200 && existing.maxPreviewItems == 64,
                    "old client config retains settings and full preview budget");
            check(existing.showContextualTips && existing.collectionTipSelections == 0 && !existing.collectionTipLearned,
                    "old config enables onboarding without losing existing settings");
            check(existing.enableUiAnimations, "old config enables UI animations by default");
            existing.collectionTipSelections = 2;
            existing.showContextualTips = false;
            existing.enableUiAnimations = false;
            check(clientFile.save(existing), "save disabled tips and onboarding progress");
            existing = new ConfigFile<>(clientPath, ClientConfig.class, ClientConfig::validate).load(ClientConfig::new);
            check(!existing.showContextualTips && existing.collectionTipSelections == 2 && !existing.collectionTipLearned,
                    "restart preserves onboarding progress while disabled");
            check(!existing.enableUiAnimations, "disabled UI animations survive restart");
            existing.showContextualTips = true;
            existing.collectionTipLearned = true;
            check(clientFile.save(existing), "save learned shortcut");
            existing = new ConfigFile<>(clientPath, ClientConfig.class, ClientConfig::validate).load(ClientConfig::new);
            check(existing.showContextualTips && existing.collectionTipSelections == 2 && existing.collectionTipLearned,
                    "learned shortcut survives restart and re-enabling tips");
            existing.collectionTipSelections = Integer.MAX_VALUE;
            existing.validate();
            check(existing.collectionTipSelections == 3, "onboarding upper bound");
            existing.collectionTipSelections = -1;
            existing.validate();
            check(existing.collectionTipSelections == 0, "onboarding lower bound");
            existing.maxPreviewItems = -1;
            existing.validate();
            check(existing.maxPreviewItems == 1, "preview budget lower bound");
            existing.maxPreviewItems = Integer.MAX_VALUE;
            check(clientFile.save(existing) && existing.maxPreviewItems == 64, "preview budget upper bound on save");
            check(JsonParser.parseString(Files.readString(clientPath)).getAsJsonObject().get("futureOption").getAsInt() == 123,
                    "client config save preserves unknown fields");
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
                String hud = translated.get("hud.furnacedistributor.estimate").getAsString();
                check(!hud.matches(".*[（(][^()（）]*[)）]\\s*$"), "HUD has no trailing quantity note " + file.getFileName());
                String counts = String.format(hud, 24, 64);
                check(counts.contains("24") && counts.contains("64"), "HUD preserves both counts " + file.getFileName());
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
        Pattern literal = Pattern.compile("\"((?:message|key|hud|config|bulk)\\.furnacedistributor\\.[a-z_]+)\"");
        try (var paths = Files.walk(root.resolve("common/src/main/java"))) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                var matcher = literal.matcher(Files.readString(path));
                while (matcher.find()) check(english.has(matcher.group(1)), "literal key " + matcher.group(1));
            }
        }
        for (String key : new String[]{"area_too_large", "area_too_far", "area_outside_world", "area_unloaded",
                "area_protected", "too_many_furnaces", "no_item", "no_furnaces", "no_capacity", "invalid_item", "not_enough_items",
                "bulk_disabled", "bulk_invalid_slots", "bulk_empty"}) {
            check(english.has("message.furnacedistributor." + key), "dynamic error " + key);
        }
        for (String key : new String[]{"outlines", "preview", "hud", "tips", "animations"}) {
            check(english.has("config.furnacedistributor." + key) && english.has("config.furnacedistributor." + key + ".tooltip"),
                    "dynamic setting " + key);
        }
    }

    private static void inventory() {
        Inventory inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        ItemStack emptyInventoryOutput = new ItemStack(Items.IRON_INGOT, 32);
        InventoryTransfer.insert(inventory, emptyInventoryOutput);
        check(emptyInventoryOutput.isEmpty() && inventory.getNonEquipmentItems().get(0).getCount() == 32, "empty inventory collection");
        InventoryTransfer.insert(inventory, ItemStack.EMPTY);
        check(inventory.getNonEquipmentItems().get(0).getCount() == 32 && inventory.getNonEquipmentItems().get(1).isEmpty(), "empty furnace output changes nothing");
        inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        inventory.getNonEquipmentItems().set(0, new ItemStack(Items.IRON_INGOT, 60));
        ItemStack remaining = new ItemStack(Items.IRON_INGOT, 10);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.isEmpty() && inventory.getNonEquipmentItems().get(0).getCount() == 64 && inventory.getNonEquipmentItems().get(1).getCount() == 6,
                "merge before empty slots");
        inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) inventory.getNonEquipmentItems().set(i, new ItemStack(Items.STONE, 64));
        inventory.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.IRON_INGOT, 63));
        remaining = new ItemStack(Items.IRON_INGOT, 10);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.getItem(Inventory.SLOT_OFFHAND).getCount() == 64 && inventory.getItem(36).isEmpty(),
                "partial offhand merge; full inventory preserves remainder and armour");
        inventory.setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.getItem(Inventory.SLOT_OFFHAND).isEmpty(), "empty offhand is not filled");
        inventory.getNonEquipmentItems().set(0, new ItemStack(Items.IRON_INGOT, 61));
        ItemStack firstOutput = new ItemStack(Items.IRON_INGOT, 2);
        ItemStack secondOutput = new ItemStack(Items.IRON_INGOT, 5);
        InventoryTransfer.insert(inventory, firstOutput);
        InventoryTransfer.insert(inventory, secondOutput);
        check(firstOutput.isEmpty() && secondOutput.getCount() == 4 && inventory.getNonEquipmentItems().get(0).getCount() == 64,
                "multiple furnace outputs share available capacity and preserve partial remainder");
        ItemStack fullOutput = new ItemStack(Items.IRON_INGOT, 7);
        InventoryTransfer.insert(inventory, fullOutput);
        check(fullOutput.getCount() == 7 && inventory.getNonEquipmentItems().get(0).getCount() == 64, "full inventory leaves output untouched");
        inventory.getNonEquipmentItems().set(0, new ItemStack(Items.IRON_INGOT, 50));
        inventory.getNonEquipmentItems().get(0).set(DataComponents.CUSTOM_NAME, Component.literal("different components"));
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.getCount() == 9 && inventory.getNonEquipmentItems().get(0).getCount() == 50, "different components do not merge");
        inventory = new Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        remaining = new ItemStack(Items.ENDER_PEARL, 20);
        InventoryTransfer.insert(inventory, remaining);
        check(remaining.isEmpty() && inventory.getNonEquipmentItems().get(0).getCount() == 16 && inventory.getNonEquipmentItems().get(1).getCount() == 4,
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

    private static void renderSubmissions() throws ReflectiveOperationException {
        var state = new net.minecraft.client.renderer.state.level.LevelRenderState();
        int[] submissions = new int[3];
        var collector = (net.minecraft.client.renderer.SubmitNodeCollector) java.lang.reflect.Proxy.newProxyInstance(
                RegressionChecks.class.getClassLoader(), new Class<?>[]{net.minecraft.client.renderer.SubmitNodeCollector.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("submitShapeOutline")) {
                        submissions[0]++;
                        var pose = (com.mojang.blaze3d.vertex.PoseStack) args[0];
                        check(pose.last().pose().m30() == -5 && pose.last().pose().m31() == -6,
                                "outline submission uses the captured camera");
                        check((int) args[3] == 0xCC00FF00, "outline color including alpha preserved");
                    } else if (method.getName().equals("submitText")) {
                        submissions[1]++;
                    }
                    return null;
                });
        com.thonyy.furnacedistributor.client.RenderHandler.submit(state, collector);
        check(submissions[0] == 0 && submissions[1] == 0, "uncaptured frame submits nothing");
        var outlineConstructor = Class.forName("com.thonyy.furnacedistributor.client.RenderHandler$Outline").getDeclaredConstructors()[0];
        outlineConstructor.setAccessible(true);
        var outline = outlineConstructor.newInstance(new net.minecraft.world.phys.AABB(BlockPos.ZERO), 0xCC00FF00);
        var frameConstructor = Class.forName("com.thonyy.furnacedistributor.client.RenderHandler$Frame").getDeclaredConstructors()[0];
        frameConstructor.setAccessible(true);
        var item = new net.minecraft.client.renderer.item.ItemStackRenderState() {
            @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose,
                                         net.minecraft.client.renderer.SubmitNodeCollector nodes, int light, int overlay, int outline) {
                submissions[2]++;
            }
        };
        var frame = frameConstructor.newInstance(new net.minecraft.world.phys.Vec3(5, 6, 7), new org.joml.Quaternionf(),
                java.util.List.of(outline), java.util.List.of(BlockPos.ZERO, new BlockPos(2, 0, 0)), item, 0f);
        var field = com.thonyy.furnacedistributor.client.RenderHandler.class.getDeclaredField("FRAMES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") var frames = (Map<Object, Object>) field.get(null);
        frames.put(state, frame);
        try {
            com.thonyy.furnacedistributor.client.RenderHandler.submit(state, collector);
            check(submissions[0] == 1 && submissions[2] == 2, "outline and each floating item preview are preserved");
            check(submissions[1] == 0, "unknown per-furnace quantities submit no text or symbols");
        } finally {
            frames.remove(state);
        }
    }

    private static void hudWrapping(Path root) throws Exception {
        var actionBarLines = com.thonyy.furnacedistributor.mixin.DistributionActionBarMixin.class
                .getDeclaredMethod("lines", net.minecraft.client.gui.Font.class, Component.class, int.class);
        actionBarLines.setAccessible(true);
        var rowConstructor = Class.forName("com.thonyy.furnacedistributor.client.SelectionHud$Row").getDeclaredConstructors()[0];
        rowConstructor.setAccessible(true);
        var wrap = com.thonyy.furnacedistributor.client.SelectionHud.class.getDeclaredMethod("wrap", java.util.List.class,
                net.minecraft.client.gui.Font.class, int.class);
        wrap.setAccessible(true);
        var textAccessor = Class.forName("com.thonyy.furnacedistributor.client.SelectionHud$Line").getDeclaredMethod("text");
        var colorAccessor = textAccessor.getDeclaringClass().getDeclaredMethod("color");
        textAccessor.setAccessible(true);
        colorAccessor.setAccessible(true);
        var splitter = new net.minecraft.client.StringSplitter((point, style) -> 6f);
        var font = new net.minecraft.client.gui.Font(new net.minecraft.client.gui.Font.Provider() {
            @Override public net.minecraft.client.gui.GlyphSource glyphs(net.minecraft.network.chat.FontDescription description) { return null; }
            @Override public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() { return null; }
        }) {
            @Override public java.util.List<net.minecraft.util.FormattedCharSequence> split(net.minecraft.network.chat.FormattedText text, int width) {
                return splitter.splitLines(text, width, net.minecraft.network.chat.Style.EMPTY).stream()
                        .map(net.minecraft.locale.Language.getInstance()::getVisualOrder).toList();
            }
            @Override public int width(net.minecraft.util.FormattedCharSequence text) { return (int) splitter.stringWidth(text); }
            @Override public net.minecraft.client.StringSplitter getSplitter() { return splitter; }
        };
        try (var files = Files.list(root.resolve("common/src/main/resources/assets/furnacedistributor/lang"))) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                var translated = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                String count = String.format(translated.get("hud.furnacedistributor.estimate").getAsString(), 24, 64);
                String controls = String.format(translated.get("hud.furnacedistributor.select_controls").getAsString(), "R", "X");
                String title = translated.get("hud.furnacedistributor.tip_title").getAsString();
                String tip = String.format(translated.get("hud.furnacedistributor.collect_tip").getAsString(), "[Mouse Button 4]");
                var rows = java.util.List.of(rowConstructor.newInstance(Component.literal(count).withStyle(style -> style.withBold(true)), 0xFFFF55),
                        rowConstructor.newInstance(Component.literal(controls), 0xCCCCCC),
                        rowConstructor.newInstance(Component.literal(title).withStyle(style -> style.withBold(true)), 0x55FFFF),
                        rowConstructor.newInstance(Component.literal(tip), 0xCCCCCC));
                for (int width : new int[]{120, 220, 320}) {
                    var lines = (java.util.List<?>) wrap.invoke(null, rows, font, width);
                    StringBuilder contents = new StringBuilder();
                    for (Object line : lines) {
                        var text = (net.minecraft.util.FormattedCharSequence) textAccessor.invoke(line);
                        check(font.width(text) <= width, "HUD lines fit narrow and wide GUI: " + path.getFileName());
                        int color = (int) colorAccessor.invoke(line);
                        text.accept((index, style, point) -> {
                            check(style.isBold() == (color == 0xFFFF55 || color == 0x55FFFF), "HUD emphasizes counts and tip title");
                            contents.appendCodePoint(point);
                            return true;
                        });
                    }
                    check(contents.toString().replaceAll("\\s", "").equals((count + controls + title + tip).replaceAll("\\s", "")),
                            "HUD wrapping preserves counts, controls and tip in " + path.getFileName());
                    String result = String.format(translated.get("message.furnacedistributor.distributed_items").getAsString(), 32, 4, 8)
                            + " · " + String.format(translated.get("message.furnacedistributor.skipped_mixed").getAsString(), 2, 2)
                            + " · " + String.format(translated.get("message.furnacedistributor.remaining_items").getAsString(), 32);
                    @SuppressWarnings("unchecked") var resultLines = (java.util.List<Component>) actionBarLines.invoke(null, font,
                            Component.literal(result).withStyle(ChatFormatting.YELLOW), width);
                    StringBuilder resultText = new StringBuilder();
                    for (Component line : resultLines) {
                        check(splitter.stringWidth(line) <= width, "distribution result fits available Action Bar width");
                        line.visit((style, part) -> {
                            check(style.getColor().getValue() == 0xFFFF55, "Action Bar preserves result severity");
                            resultText.append(part);
                            return java.util.Optional.empty();
                        }, net.minecraft.network.chat.Style.EMPTY);
                    }
                    check(resultText.toString().replaceAll("\\s", "").equals(result.replaceAll("\\s", "")),
                            "adaptive Action Bar preserves quantities, fraction, reasons and remainder in " + path.getFileName());
                }
            }
        }
    }

    private static void distributionActionBarTarget() throws Exception {
        var target = new org.objectweb.asm.tree.ClassNode();
        try (var input = RegressionChecks.class.getClassLoader().getResourceAsStream("net/minecraft/client/gui/Hud.class")) {
            new org.objectweb.asm.ClassReader(input).accept(target, 0);
        }
        var method = target.methods.stream().filter(value -> value.name.equals("extractOverlayMessage")
                && value.desc.equals("(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
                .findFirst().orElseThrow();
        int calls = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                    && call.owner.equals("net/minecraft/client/gui/GuiGraphicsExtractor") && call.name.equals("textWithBackdrop")
                    && call.desc.equals("(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V")) calls++;
        }
        check(calls == 1, "Action Bar mixin invocation matches the actual Minecraft 26.2 bytecode");
        var isResult = com.thonyy.furnacedistributor.mixin.DistributionActionBarMixin.class
                .getDeclaredMethod("isDistributionResult", Component.class);
        isResult.setAccessible(true);
        check((boolean) isResult.invoke(null, Component.translatable("message.furnacedistributor.distributed_items", 64, 4, 64))
                && (boolean) isResult.invoke(null, Component.translatable("message.furnacedistributor.distributed_none")),
                "only distribution results receive adaptive rendering");
        check(!(boolean) isResult.invoke(null, Component.literal("external message"))
                && !(boolean) isResult.invoke(null, Component.translatable("message.furnacedistributor.area_protected")),
                "other Action Bar messages and rejection errors retain vanilla rendering");
    }

    private static void distributionFeedback(Path root) throws Exception {
        var method = DistributionManager.class.getDeclaredMethod("result", int.class, int.class, int.class,
                int.class, int.class, int.class);
        method.setAccessible(true);
        int[][] cases = {{64,8,8,0,0,0}, {64,4,64,0,0,60}, {32,4,8,0,4,0}, {32,4,8,0,2,2},
                {0,0,8,64,0,8}, {0,0,8,64,8,0}, {2,2,8,0,0,0}, {4,1,8,60,2,5}, {0,0,8,64,0,0}};
        String[] expected = {"Distributed 64 items across 8/8 furnaces",
                "Distributed 64 items across 4/64 furnaces · 60 skipped (no space)",
                "Distributed 32 items across 4/8 furnaces · 4 skipped (incompatible item)",
                "Distributed 32 items across 4/8 furnaces · 2 no space · 2 incompatible",
                "No items distributed · 0/8 furnaces · 8 skipped (no space)", "No items distributed · 0/8 furnaces · 8 skipped (incompatible item)",
                "Distributed 2 items across 2/8 furnaces",
                "Distributed 4 items across 1/8 furnaces · 5 no space · 2 incompatible · 60 left in hand",
                "No items distributed · 0/8 furnaces"};
        try (var files = Files.list(root.resolve("common/src/main/resources/assets/furnacedistributor/lang"))) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                var language = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                for (int i = 0; i < cases.length; i++) {
                    int[] c = cases[i];
                    Component result = (Component) method.invoke(null, c[0], c[1], c[2], c[3], c[4], c[5]);
                    String text = translated(result, language);
                    check(!text.contains("%s"), "all distribution placeholders resolved in " + path.getFileName());
                    check(text.contains(c[1] + "/" + c[2]), "actual receiver/considered fraction, including zero moved");
                    if (path.getFileName().toString().equals("en_us.json")) check(text.equals(expected[i]), "exact English result " + i);
                    if (c[4] + c[5] == 0 && c[3] == 0) check(result.getSiblings().isEmpty(), "no zero exclusion categories");
                }
            }
        }
    }

    private static String translated(Component component, JsonObject language) {
        String text;
        if (component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents) {
            Object[] args = Arrays.stream(contents.getArgs())
                    .map(arg -> arg instanceof Component child ? translated(child, language) : arg).toArray();
            text = String.format(language.get(contents.getKey()).getAsString(), args);
        } else {
            var copy = component.copy();
            copy.getSiblings().clear();
            text = copy.getString();
        }
        for (Component child : component.getSiblings()) text += translated(child, language);
        return text;
    }

    private static void contextualTips() throws Exception {
        Path directory = Files.createTempDirectory("furnacedistributor-onboarding-");
        Path path = directory.resolve("client.json");
        var configFile = ClientConfig.class.getDeclaredField("file");
        configFile.setAccessible(true);
        Object previousFile = configFile.get(null);
        configFile.set(null, new ConfigFile<>(path, ClientConfig.class, ClientConfig::validate));
        try {
            var start = SelectionFeedback.class.getDeclaredMethod("startTip", boolean.class);
            var tick = SelectionFeedback.class.getDeclaredMethod("tickTip", boolean.class);
            start.setAccessible(true);
            tick.setAccessible(true);
            ClientConfig config = ClientConfig.get();
            SelectionFeedback.clear();
            start.invoke(null, false);
            check(!SelectionFeedback.hasTip() && config.collectionTipSelections == 0, "hidden HUD/unbound shortcut does not consume tip");
            config.showContextualTips = false;
            start.invoke(null, true);
            check(!SelectionFeedback.hasTip() && config.collectionTipSelections == 0, "disabled tips preserve progress");
            config.showContextualTips = true;
            for (int selection = 1; selection <= 3; selection++) {
                start.invoke(null, true);
                check(SelectionFeedback.hasTip() && config.collectionTipSelections == selection, "first three valid selections teach collection");
                ClientConfig persisted = new ConfigFile<>(path, ClientConfig.class, ClientConfig::validate).load(ClientConfig::new);
                check(persisted.collectionTipSelections == selection && !persisted.collectionTipLearned,
                        "showing a tip automatically persists its count before shutdown");
                for (int i = 0; i < 99; i++) tick.invoke(null, true);
                check(SelectionFeedback.hasTip(), "tip lasts just under five seconds without timer reset");
                tick.invoke(null, true);
                check(!SelectionFeedback.hasTip() && config.collectionTipSelections == selection, "tip expires at five seconds");
                SelectionFeedback.clear();
                check(config.collectionTipSelections == selection, "cancellation does not reset or advance onboarding");
            }
            start.invoke(null, true);
            check(!SelectionFeedback.hasTip(), "fourth completed selection shows no tip");
            config.collectionTipSelections = 0;
            start.invoke(null, true);
            tick.invoke(null, false);
            check(!SelectionFeedback.hasTip() && config.collectionTipSelections == 1, "invalidated area hides tip");
            start.invoke(null, true);
            SelectionFeedback.hideTip();
            check(!config.collectionTipLearned, "collection press/new collection selection only dismisses tip");
            check(CollectionShortcut.resolve(false, true, true, true) == CollectionShortcut.Action.SELECT_FIRST,
                    "Sneak+collect on furnace never marks saved-area collection");
            start.invoke(null, true);
            SelectionFeedback.collectedSavedArea();
            check(!SelectionFeedback.hasTip() && config.collectionTipLearned, "saved-area command teaches shortcut without ready items");
            ClientConfig persisted = new ConfigFile<>(path, ClientConfig.class, ClientConfig::validate).load(ClientConfig::new);
            check(persisted.collectionTipSelections == 3 && persisted.collectionTipLearned,
                    "using saved-area collection automatically persists learning");
            config.collectionTipSelections = 0;
            start.invoke(null, true);
            check(!SelectionFeedback.hasTip(), "learned shortcut suppresses all future tips");
            var key = new KeyMapping("regression.tip", InputConstants.Type.KEYSYM, 67, KeyMapping.Category.MISC);
            for (var binding : new InputConstants.Key[]{InputConstants.getKey("key.keyboard.c"), InputConstants.getKey("key.keyboard.g"),
                    InputConstants.getKey("key.mouse.left"), InputConstants.Type.SCANCODE.getOrCreate(30)}) {
                key.setKey(binding);
                Component tip = SelectionFeedback.tipDescription(key);
                var contents = (net.minecraft.network.chat.contents.TranslatableContents) tip.getContents();
                Component keycap = (Component) contents.getArgs()[0];
                check(keycap.getString().equals("[" + key.getTranslatedKeyMessage().getString() + "]")
                        && keycap.getStyle().isBold(), "tip tracks current keyboard/mouse binding " + binding);
            }
            key.setKey(InputConstants.UNKNOWN);
            check(SelectionFeedback.tipDescription(key) == null, "unassigned key offers no incorrect instruction");
            config.collectionTipSelections = 0;
            config.collectionTipLearned = false;
            SelectionFeedback.clear();
        } finally {
            configFile.set(null, previousFile);
            Files.deleteIfExists(path);
            Files.delete(directory);
        }
    }

    private static void previewCulling() throws ReflectiveOperationException {
        var visible = com.thonyy.furnacedistributor.client.RenderHandler.class.getDeclaredMethod("visiblePreview",
                java.util.List.class, net.minecraft.client.renderer.culling.Frustum.class, int.class, double.class);
        visible.setAccessible(true);
        var positions = java.util.stream.IntStream.range(0, 64).mapToObj(x -> new BlockPos(x, 0, 0)).toList();
        int[] visited = {0};
        var frustum = new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(), new org.joml.Matrix4f()) {
            @Override public boolean isVisible(net.minecraft.world.phys.AABB box) {
                visited[0]++;
                check(box.minY <= 0.71 && box.maxY >= 1.89, "culling includes floating preview and bobbing range");
                return box.minX >= 32;
            }
        };
        @SuppressWarnings("unchecked") var limited = (java.util.List<BlockPos>) visible.invoke(null, positions, frustum, 8, 0.5);
        check(limited.size() == 8 && limited.getFirst().getX() == 32 && visited[0] == 40,
                "offscreen positions do not consume budget; stop searching once budget is filled");
        check(positions.size() == 64, "render budget does not truncate selected furnace count");
        @SuppressWarnings("unchecked") var full = (java.util.List<BlockPos>) visible.invoke(null, positions, frustum, 64, 0.5);
        check(full.size() == 32 && full.getLast().getX() == 63, "full budget preserves all visible positions");
        check(((java.util.List<?>) visible.invoke(null, positions.subList(0, 16), frustum, 64, 0.5)).isEmpty(),
                "entirely offscreen selection submits no item previews");
        var actualFrustum = new net.minecraft.client.renderer.culling.Frustum(new org.joml.Matrix4f(), new org.joml.Matrix4f());
        actualFrustum.prepare(0, 0, 0);
        check(((java.util.List<?>) visible.invoke(null, java.util.List.of(BlockPos.ZERO), actualFrustum, 64, 0.5)).size() == 1,
                "real vanilla frustum retains an item overlapping the edge of view");
        var radius = com.thonyy.furnacedistributor.client.RenderHandler.class.getDeclaredMethod("previewRadius", net.minecraft.world.phys.AABB.class);
        radius.setAccessible(true);
        var oversized = new net.minecraft.world.phys.AABB(-4, -2, -1, 2, 3, 5);
        double size = (double) radius.invoke(null, oversized);
        for (int angle = 0; angle < 360; angle += 15) {
            var rotation = new org.joml.Quaternionf().rotationXYZ(angle * 0.01f, angle * 0.03f, angle * 0.02f);
            for (double x : new double[]{oversized.minX, oversized.maxX}) {
                for (double y : new double[]{oversized.minY, oversized.maxY}) {
                    for (double z : new double[]{oversized.minZ, oversized.maxZ}) {
                        var corner = new org.joml.Vector3f((float) x, (float) y, (float) z).mul(0.5f).rotate(rotation);
                        check(corner.length() <= size + 0.00001, "culling bound includes oversized model in every rotation");
                    }
                }
            }
        }
    }

    private static void equal(int[] actual, int[] expected, String label) {
        check(Arrays.equals(actual, expected), label + ": " + Arrays.toString(actual));
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
