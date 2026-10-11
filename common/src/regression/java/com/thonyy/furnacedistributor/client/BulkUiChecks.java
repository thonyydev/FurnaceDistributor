package com.thonyy.furnacedistributor.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;

/** Real screen widgets/callbacks and layout, without starting a GPU or submitting a network operation. */
public final class BulkUiChecks {
    private static int checks;

    public static int run(Path root) throws Exception {
        checks = 0;
        var access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) access.get(null);
        // The screen itself uses its normal constructor; only the game/window/player fixtures bypass startup.
        Minecraft mc = (Minecraft) unsafe.allocateInstance(Minecraft.class);
        set(Minecraft.class, mc, "font", UiAnimationChecks.FONT);
        LocalPlayer player = (LocalPlayer) unsafe.allocateInstance(LocalPlayer.class);
        var inventory = new Inventory(player, new net.minecraft.world.entity.EntityEquipment());
        set(net.minecraft.world.entity.player.Player.class, player, "inventory", inventory);
        mc.player = player;
        Minecraft original = Minecraft.getInstance();
        Language language = Language.getInstance();
        var constructor = BulkDistributionScreen.class.getDeclaredConstructor(Minecraft.class, BlockPos.class, BlockPos.class, int.class);
        constructor.setAccessible(true);
        try {
            set(Minecraft.class, null, "instance", mc);
            try (var paths = Files.list(root.resolve("common/src/main/resources/assets/furnacedistributor/lang"))) {
                for (Path path : paths.toList()) {
                    var strings = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    Language.inject(new Language() {
                        @Override public String getOrDefault(String key, String fallback) { return strings.has(key) ? strings.get(key).getAsString() : fallback; }
                        @Override public boolean has(String key) { return strings.has(key); }
                        @Override public boolean isDefaultRightToLeft() { return false; }
                        @Override public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text) {
                            return net.minecraft.util.FormattedCharSequence.forward(text.getString(), net.minecraft.network.chat.Style.EMPTY);
                        }
                    });
                    for (int[] size : new int[][]{{320, 240}, {360, 240}, {854, 480}}) {
                        var screen = constructor.newInstance(mc, BlockPos.ZERO, BlockPos.ZERO, 32);
                        screen.width = size[0]; screen.height = size[1];
                        screen.init();
                        int left = (int) get(screen, "left"), top = (int) get(screen, "top");
                        int width = (int) get(screen, "panelWidth"), height = (int) get(screen, "panelHeight");
                        check(left >= 0 && top >= 0 && left + width <= size[0] && top + height <= size[1], "localized panel fits GUI: " + path.getFileName());
                        for (var child : screen.children()) {
                            var button = (Button) child;
                            check(button.getX() >= left && button.getRight() <= left + width
                                    && button.getY() >= top && button.getBottom() <= top + height, "all localized controls inside panel");
                        }
                        check(!screen.isPauseScreen(), "inventory view keeps the server running");
                    }
                }
            }
            inventory.setItem(0, new ItemStack(Items.RAW_IRON, 64));
            inventory.setItem(9, new ItemStack(Items.RAW_IRON, 32));
            ItemStack named = new ItemStack(Items.RAW_IRON, 8);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("separate"));
            inventory.setItem(1, named);
            var screen = constructor.newInstance(mc, BlockPos.ZERO, BlockPos.ZERO, 32);
            screen.width = 320; screen.height = 240; screen.init();
            Button[] slots = (Button[]) get(screen, "slots");
            Button confirm = (Button) get(screen, "confirm");
            check(!confirm.active && !slots[35].active, "empty selection cannot submit; empty slots disabled");
            slots[0].onPress(null);
            check((long) get(screen, "selected") == 1 && confirm.active, "real slot callback selects and enables confirmation");
            slots[0].onPress(null);
            check((long) get(screen, "selected") == 0 && !confirm.active, "real slot callback deselects");
            slots[0].onPress(null);
            ((Button) get(screen, "matching")).onPress(null);
            check((long) get(screen, "selected") == (1 | (1L << 9)), "matching callback includes only equivalent components");
            var summary = (TranslatableContents) ((Component) get(screen, "summary")).getContents();
            check(java.util.Arrays.equals(summary.getArgs(), new Object[]{96L, 2, 32}), "selected item/stack/furnace totals are exact");
            check(inventory.getItem(0).getCount() == 64 && inventory.getItem(9).getCount() == 32
                    && inventory.getItem(1).getCount() == 8, "UI callbacks never mutate inventory");
            inventory.setItem(0, ItemStack.EMPTY);
            inventory.setItem(9, new ItemStack(Items.COAL, 12));
            var refresh = BulkDistributionScreen.class.getDeclaredMethod("refresh");
            refresh.setAccessible(true); refresh.invoke(screen);
            check((long) get(screen, "selected") == (1L << 9), "removed slot deselected, replaced slot remains the explicit selected slot");
            summary = (TranslatableContents) ((Component) get(screen, "summary")).getContents();
            check(java.util.Arrays.equals(summary.getArgs(), new Object[]{12L, 1, 32}) && slots[9].getMessage().getString().contains("12"),
                    "live inventory changes update the visible quantity and narration");
            check(KeyBindings.BULK_KEY.getDefaultKey().getValue() == org.lwjgl.glfw.GLFW.GLFW_KEY_B, "bulk default key is B");
            var key = KeyBindings.BULK_KEY;
            try {
                key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_G));
                check(key.matches(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_G, 0, 0))
                        && !key.matches(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_B, 0, 0)),
                        "bulk binding supports another keyboard key without retaining B");
                key.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN);
                check(key.isUnbound(), "bulk binding can be unassigned");
            } finally { key.setKey(key.getDefaultKey()); }
        } finally {
            set(Minecraft.class, null, "instance", original);
            Language.inject(language);
        }
        return checks;
    }

    private static Object get(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private BulkUiChecks() { }
}
