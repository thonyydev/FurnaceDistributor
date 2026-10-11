package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class DistributorSettingsScreen extends Screen {
    private final Screen parent;
    private boolean saveFailed;
    private boolean closeHandled;

    public DistributorSettingsScreen() {
        this(null);
    }

    public DistributorSettingsScreen(Screen parent) {
        super(Component.translatable("config.furnacedistributor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientConfig config = ClientConfig.get();
        int top = Math.max(24, height / 2 - 100);
        toggle("outlines", top, () -> config.showOutlines, value -> config.showOutlines = value);
        toggle("preview", top + 22, () -> config.showItemPreview, value -> config.showItemPreview = value);
        toggle("hud", top + 44, () -> config.showSelectionHud, value -> config.showSelectionHud = value);
        toggle("tips", top + 66, () -> config.showContextualTips, value -> config.showContextualTips = value);
        toggle("animations", top + 88, () -> config.enableUiAnimations, value -> config.enableUiAnimations = value);
        addRenderableWidget(Button.builder(durationLabel(), button -> {
            config.selectionDisplayTicks = config.selectionDisplayTicks < 60 ? 60
                    : config.selectionDisplayTicks < 200 ? 200 : config.selectionDisplayTicks < 1200 ? 1200 : 0;
            button.setMessage(durationLabel());
        }).bounds(width / 2 - 150, top + 110, 300, 20)
                .tooltip(Tooltip.create(Component.translatable("config.furnacedistributor.duration.tooltip"))).build());
        addRenderableWidget(Button.builder(previewLimitLabel(), button -> {
            config.maxPreviewItems = config.maxPreviewItems < 8 ? 8 : config.maxPreviewItems < 16 ? 16
                    : config.maxPreviewItems < 32 ? 32 : config.maxPreviewItems < 64 ? 64 : 8;
            button.setMessage(previewLimitLabel());
        }).bounds(width / 2 - 150, top + 132, 300, 20)
                .tooltip(Tooltip.create(Component.translatable("config.furnacedistributor.preview_limit.tooltip"))).build());
        if (saveFailed) {
            addRenderableWidget(Button.builder(Component.translatable("config.furnacedistributor.retry_save"), button -> onClose())
                    .bounds(width / 2 - 150, top + 160, 146, 20)
                    .tooltip(Tooltip.create(Component.translatable("message.furnacedistributor.config_save_failed"))).build());
            addRenderableWidget(Button.builder(Component.translatable("config.furnacedistributor.close_without_saving"), button -> {
                reportSaveFailure();
                closeHandled = true;
                minecraft.gui.setScreen(parent);
            }).bounds(width / 2 + 4, top + 160, 146, 20)
                    .tooltip(Tooltip.create(Component.translatable("config.furnacedistributor.unsaved.tooltip"))).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(width / 2 - 100, top + 160, 200, 20).build());
        }
    }

    private void toggle(String key, int y, BooleanSupplier getter, Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(toggleLabel(key, getter.getAsBoolean()), button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }).bounds(width / 2 - 150, y, 300, 20)
                .tooltip(Tooltip.create(Component.translatable("config.furnacedistributor." + key + ".tooltip"))).build());
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable("config.furnacedistributor." + key,
                Component.translatable(value ? "options.on" : "options.off"));
    }

    private Component durationLabel() {
        return Component.translatable("config.furnacedistributor.duration", ClientConfig.get().selectionDisplayTicks / 20.0);
    }

    private Component previewLimitLabel() {
        return Component.translatable("config.furnacedistributor.preview_limit", ClientConfig.get().maxPreviewItems);
    }

    @Override
    public void onClose() {
        if (!ClientConfig.save()) {
            saveFailed = true;
            rebuildWidgets();
            minecraft.getNarrator().saySystemNow(Component.translatable("config.furnacedistributor.save_error"));
            return;
        }
        closeHandled = true;
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        // Abrupt screen changes (e.g. disconnect) still attempt to persist the edits once.
        if (!closeHandled && !ClientConfig.save()) reportSaveFailure();
        super.removed();
    }

    private void reportSaveFailure() {
        if (minecraft.player != null) {
            PlayerFeedback.important(minecraft.player, Component.translatable("message.furnacedistributor.config_save_failed")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, Math.max(8, height / 2 - 128), 0xFFFFFFFF);
        if (saveFailed) {
            graphics.textWithWordWrap(font, Component.translatable("config.furnacedistributor.save_error"),
                    width / 2 - 150, Math.max(24, height / 2 - 100) + 184, 300, 0xFFFF5555);
        }
    }
}
