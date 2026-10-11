package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;
import com.thonyy.furnacedistributor.logic.InventorySelection;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import com.thonyy.furnacedistributor.network.BulkDistributePacket;
import com.thonyy.furnacedistributor.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/** A read-only inventory view: clicks change only the local slot bitset. */
public final class BulkDistributionScreen extends net.minecraft.client.gui.screens.Screen {
    private final BlockPos first;
    private final BlockPos second;
    private final ClientLevel level;
    private final net.minecraft.client.player.LocalPlayer player;
    private final SlotButton[] slots = new SlotButton[InventorySelection.SLOTS];
    private final UiTransition entrance = new UiTransition();
    private long selected;
    private int anchor = -1;
    private int furnaceCount;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private Button matching;
    private Button confirm;
    private Component summary = Component.empty();
    private long lastTotal = -1;
    private int lastStacks = -1;

    public static void open(Minecraft mc) {
        if (!FurnaceSelectionHandler.hasConfirmedArea() || FurnaceSelectionHandler.isInSelectionMode()) {
            PlayerFeedback.actionBar(mc.player, Component.translatable("message.furnacedistributor.no_saved_area")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        BlockPos first = FurnaceSelectionHandler.getLastConfirmedFirstPos();
        BlockPos second = FurnaceSelectionHandler.getLastConfirmedSecondPos();
        if (!FurnaceArea.bounds(first, second).isWithinDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 128)) {
            PlayerFeedback.actionBar(mc.player, Component.translatable("message.furnacedistributor.area_too_far", 128));
            return;
        }
        int count = SelectionPreview.validate(mc, first, second);
        if (count <= 0) {
            if (count == 0) PlayerFeedback.actionBar(mc.player, Component.translatable("message.furnacedistributor.no_furnaces"));
            return;
        }
        mc.gui.setScreen(new BulkDistributionScreen(mc, first, second, count));
    }

    private BulkDistributionScreen(Minecraft mc, BlockPos first, BlockPos second, int count) {
        super(Component.translatable("bulk.furnacedistributor.title"));
        this.first = first.immutable();
        this.second = second.immutable();
        this.level = mc.level;
        this.player = mc.player;
        furnaceCount = count;
    }

    @Override protected void init() {
        panelWidth = Math.min(300, width - 16);
        int hintHeight = font.split(Component.translatable("bulk.furnacedistributor.hint"), panelWidth - 16).size() * 9;
        panelHeight = 198 + hintHeight;
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - panelHeight) / 2);
        int gridTop = top + 46 + hintHeight;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = row == 3 ? column : 9 + row * 9 + column;
                slots[slot] = addRenderableWidget(new SlotButton(slot, width / 2 - 90 + column * 20,
                        gridTop + row * 20 + (row == 3 ? 4 : 0)));
            }
        }
        matching = addRenderableWidget(Button.builder(Component.translatable("bulk.furnacedistributor.matching"), button -> {
            selected = InventorySelection.matching(selected, anchor, player.getInventory().getNonEquipmentItems()::get);
            refresh();
        }).bounds(left + 8, gridTop + 90, panelWidth - 16, 20).build());
        int half = (panelWidth - 20) / 2;
        confirm = addRenderableWidget(Button.builder(Component.translatable("bulk.furnacedistributor.confirm"), button -> submit())
                .bounds(left + 8, gridTop + 114, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 12 + half, gridTop + 114, half, 20).build());
        refresh();
    }

    private boolean validContext() {
        return minecraft.level == level && minecraft.player == player && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu && FurnaceSelectionHandler.hasConfirmedArea()
                && first.equals(FurnaceSelectionHandler.getLastConfirmedFirstPos())
                && second.equals(FurnaceSelectionHandler.getLastConfirmedSecondPos());
    }

    private void submit() {
        if (!validContext()) { onClose(); return; }
        refresh();
        int count = SelectionPreview.validate(minecraft, first, second);
        if (count <= 0) { onClose(); return; }
        // Keep only currently nonempty selected slots; the server independently reads them again.
        long intent = selected;
        for (int slot = 0; slot < slots.length; slot++) if (slots[slot].stack.isEmpty()) intent &= ~(1L << slot);
        if (intent == 0) { refresh(); return; }
        confirm.active = false;
        ModNetworking.sendBulk(new BulkDistributePacket(first, second, intent, level.dimension().identifier()));
        onClose();
    }

    private void refresh() {
        long total = 0;
        int stackCount = 0;
        for (int slot = 0; slot < slots.length; slot++) {
            ItemStack stack = player.getInventory().getNonEquipmentItems().get(slot);
            if (stack.isEmpty()) selected &= ~(1L << slot);
            SlotButton button = slots[slot];
            boolean chosen = (selected & (1L << slot)) != 0;
            if (!button.initialized || !ItemStack.matches(stack, button.stack) || chosen != button.chosen) {
                if (!button.initialized) button.highlight.snap(chosen ? 1 : 0);
                button.initialized = true;
                button.stack = stack.copy();
                button.chosen = chosen;
                button.setMessage(Component.translatable("bulk.furnacedistributor.slot", slot + 1,
                        stack.isEmpty() ? Component.translatable("bulk.furnacedistributor.empty") : stack.getHoverName(),
                        stack.getCount(), Component.translatable(chosen ? "bulk.furnacedistributor.selected" : "bulk.furnacedistributor.unselected")));
            }
            button.active = !stack.isEmpty();
            if (chosen && !stack.isEmpty()) { total += stack.getCount(); stackCount++; }
        }
        if (total != lastTotal || stackCount != lastStacks) {
            summary = Component.translatable("bulk.furnacedistributor.summary", total, stackCount, furnaceCount);
            lastTotal = total;
            lastStacks = stackCount;
        }
        confirm.active = total > 0;
        matching.active = anchor >= 0 && !slots[anchor].stack.isEmpty();
    }

    @Override public void tick() {
        if (!validContext()) { onClose(); return; }
        refresh();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float alpha = entrance.update(1, Util.getMillis(), ClientConfig.get().enableUiAnimations ? 180 : 0, true);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, ARGB.multiplyAlpha(0xE0202020, alpha));
        graphics.fill(left, top, left + 2, top + panelHeight, ARGB.multiplyAlpha(0xFF55FF55, alpha));
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);
        graphics.textWithWordWrap(font, summary, left + 8, top + 23, panelWidth - 16, 0xFFDDDDDD);
        graphics.textWithWordWrap(font, Component.translatable("bulk.furnacedistributor.hint"),
                left + 8, top + 44, panelWidth - 16, 0xFFAAAAAA);
        if (!confirm.active) graphics.centeredText(font, Component.translatable("bulk.furnacedistributor.no_selection"),
                width / 2, top + panelHeight - 13, 0xFFAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private final class SlotButton extends Button {
        private ItemStack stack = ItemStack.EMPTY;
        private boolean chosen;
        private boolean initialized;
        private final UiTransition highlight = new UiTransition();

        SlotButton(int slot, int x, int y) {
            super(x, y, 20, 20, Component.empty(), button -> {
                selected ^= 1L << slot;
                anchor = slot;
                refresh();
            }, DEFAULT_NARRATION);
        }

        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            extractDefaultSprite(graphics);
            float alpha = highlight.update(chosen ? 1 : 0, Util.getMillis(), ClientConfig.get().enableUiAnimations ? 160 : 0, false);
            if (alpha > 0) graphics.fill(getX() + 1, getY() + 1, getX() + 19, getY() + 19,
                    ARGB.multiplyAlpha(0x8055FF55, alpha));
            if (!stack.isEmpty()) {
                graphics.item(stack, getX() + 2, getY() + 2);
                graphics.itemDecorations(font, stack, getX() + 2, getY() + 2);
                if (isHovered() || isFocused()) graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
            // A persistent corner mark makes selection clear even when motion is disabled or just starting.
            if (chosen) graphics.fill(getX() + 1, getY() + 1, getX() + 4, getY() + 4, 0xFF55FF55);
        }
    }
}
