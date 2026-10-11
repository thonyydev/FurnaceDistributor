package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.logic.AreaBounds;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;

public final class SelectionHud {
    private record Row(Component text, int color) { }
    private record Line(FormattedCharSequence text, int color) { }
    private static final UiTransition opacity = new UiTransition();
    private static final UiTransition tipOpacity = new UiTransition();
    private static final UiTransition panelWidth = new UiTransition();
    private static final UiTransition panelHeight = new UiTransition();
    private static final UiTransition accentBlend = new UiTransition();
    private static List<Row> heldRows = List.of();
    private static List<Row> wrappedRows = List.of();
    private static List<Line> cachedLines = List.of();
    private static Font cachedFont;
    private static Language cachedLanguage;
    private static int cachedGuiWidth, cachedGuiHeight, contentWidth, contentHeight, tipLine;
    private static int fromAccent, targetAccent;
    public static void register() {
        ClientGuiEvent.RENDER_HUD.register((graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!canShow(mc)) {
            reset();
            SelectionFeedback.hideTip();
            return;
        }
        boolean distributing = FurnaceSelectionHandler.isInSelectionMode();
        boolean collecting = Collector.isInCollectionMode();
        List<Row> rows = new ArrayList<>();
        if (distributing || collecting) {
            BlockPos first = distributing ? FurnaceSelectionHandler.getFirstPos() : Collector.getFirstCollectPos();
            BlockPos target = FurnaceSelectionHandler.getLookingAtPos(mc);
            // Keep the first furnace/count visible when the crosshair leaves a valid target.
            if (!SelectionPreview.isSupportedFurnace(mc, target)) target = first;
            if (SelectionPreview.isSupportedFurnace(mc, target)) {
                FurnaceArea.Scan scan = SelectionPreview.get(mc, first, target);
                Component info = scan.valid()
                        ? Component.translatable(distributing ? "hud.furnacedistributor.estimate" : "hud.furnacedistributor.furnaces",
                                scan.positions().size(), mc.player.getMainHandItem().getCount())
                        : Component.translatable("message.furnacedistributor." + scan.error(),
                                scan.error().equals("area_too_large") ? AreaBounds.MAX_VOLUME : AreaBounds.MAX_FURNACES);
                rows.add(new Row(info.copy().withStyle(style -> style.withBold(true)), scan.valid()
                        ? collecting ? 0xCC88FF : 0xFFFF55 : 0xFF5555));
            }
            rows.add(new Row(Component.translatable(distributing
                            ? "hud.furnacedistributor.select_controls" : "hud.furnacedistributor.collect_select_controls",
                    (distributing ? KeyBindings.DISTRIBUTE_KEY : KeyBindings.COLLECT_KEY).getTranslatedKeyMessage(),
                    KeyBindings.CANCEL_KEY.getTranslatedKeyMessage()), 0xCCCCCC));
        } else if (SelectionFeedback.isConfirmed(false) || SelectionFeedback.hasTip()) {
            rows.add(new Row(Component.translatable("hud.furnacedistributor.area_saved",
                    SelectionFeedback.getFurnaceCount()).withStyle(style -> style.withBold(true)), 0x55FFFF));
            rows.add(new Row(Component.translatable("hud.furnacedistributor.area_controls",
                    KeyBindings.COLLECT_KEY.getTranslatedKeyMessage(), mc.options.keyShift.getTranslatedKeyMessage(),
                    KeyBindings.DISTRIBUTE_KEY.getTranslatedKeyMessage(), KeyBindings.CANCEL_KEY.getTranslatedKeyMessage()), 0xCCCCCC));
        } else if (SelectionFeedback.isConfirmed(true)) {
            rows.add(new Row(Component.translatable("hud.furnacedistributor.collect_area",
                    SelectionFeedback.getFurnaceCount()).withStyle(style -> style.withBold(true)), 0xCC88FF));
        }
        List<Row> tips = new ArrayList<>(2);
        if (SelectionFeedback.hasTip() && ClientConfig.get().showContextualTips) {
            Component description = SelectionFeedback.tipDescription(KeyBindings.COLLECT_KEY);
            if (description != null) {
                tips.add(new Row(Component.translatable("hud.furnacedistributor.tip_title")
                        .withStyle(style -> style.withBold(true)), 0x55FFFF));
                tips.add(new Row(description, 0xCCCCCC));
            }
        }
        panel(graphics, mc.font, rows, tips, Util.getMillis(), ClientConfig.get().enableUiAnimations);
    }

    private static void panel(GuiGraphicsExtractor graphics, Font font, List<Row> rows, List<Row> tips,
                              long now, boolean animated) {
        if (rows.isEmpty() && heldRows.isEmpty()) return;
        if (!rows.isEmpty() && !rows.equals(heldRows)) heldRows = List.copyOf(rows);
        float alpha = opacity.update(rows.isEmpty() ? 0 : 1, now,
                animated ? rows.isEmpty() ? 240 : 180 : 0, !rows.isEmpty());
        if (rows.isEmpty() && alpha == 0) {
            reset();
            return;
        }
        // Expiry fades within the existing five seconds; collecting/invalidating still hides the tip immediately.
        if (tips.isEmpty()) tipOpacity.snap(0);
        float tipAlpha = tipOpacity.update(tips.isEmpty() || animated && SelectionFeedback.isTipEnding() ? 0 : 1,
                now, animated ? SelectionFeedback.isTipEnding() ? 250 : 180 : 0, !SelectionFeedback.isTipEnding());
        List<Row> displayed = new ArrayList<>(heldRows);
        displayed.addAll(tips);
        boolean resized = graphics.guiWidth() != cachedGuiWidth || graphics.guiHeight() != cachedGuiHeight
                || font != cachedFont || Language.getInstance() != cachedLanguage;
        boolean firstFrame = cachedLines.isEmpty();
        if (resized || !displayed.equals(wrappedRows)) {
            int limit = Math.max(1, Math.min(320, graphics.guiWidth() - 20));
            cachedLines = wrap(displayed, font, limit);
            tipLine = wrap(heldRows, font, limit).size();
            contentWidth = 12;
            for (Line line : cachedLines) contentWidth = Math.max(contentWidth, font.width(line.text) + 12);
            contentHeight = 6 + cachedLines.size() * 12;
            wrappedRows = List.copyOf(displayed);
            cachedFont = font;
            cachedLanguage = Language.getInstance();
            cachedGuiWidth = graphics.guiWidth();
            cachedGuiHeight = graphics.guiHeight();
        }
        // Resizing/reloading is immediate so old GUI bounds never clip the new layout.
        if (resized || firstFrame) {
            panelWidth.snap(contentWidth);
            panelHeight.snap(contentHeight);
        }
        int color = heldRows.getFirst().color | 0xFF000000;
        int accent = ARGB.linearLerp(accentBlend.value(now), fromAccent, targetAccent);
        if (color != targetAccent) {
            fromAccent = firstFrame ? color : accent;
            targetAccent = color;
            accentBlend.snap(0);
        }
        accent = ARGB.linearLerp(accentBlend.update(1, now, animated ? 160 : 0, false), fromAccent, targetAccent);
        // Never tween the backdrop smaller than exact counts/controls; only tips may be revealed progressively.
        if (panelWidth.value(now) < contentWidth) panelWidth.snap(contentWidth);
        int mainHeight = 6 + tipLine * 12;
        if (panelHeight.value(now) < mainHeight) panelHeight.snap(mainHeight);
        int right = Math.min(graphics.guiWidth() - 4,
                4 + Math.round(panelWidth.update(contentWidth, now, animated ? 160 : 0, false)));
        int bottom = Math.min(graphics.guiHeight() - 4,
                4 + Math.round(panelHeight.update(contentHeight, now, animated ? 160 : 0, false)));
        if (alpha == 0) return;
        // Clip to the viewport, not the animated backdrop: counts and controls remain exact and readable.
        graphics.enableScissor(4, 4, Math.max(4, graphics.guiWidth() - 4), Math.max(4, graphics.guiHeight() - 4));
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, (1 - alpha) * 6);
        graphics.fill(4, 4, right, bottom, ARGB.multiplyAlpha(0xB0202020, alpha));
        graphics.fill(4, 4, 6, bottom, ARGB.multiplyAlpha(accent, alpha));
        int y = 8;
        boolean tipClipped = false;
        for (int i = 0; i < cachedLines.size(); i++) {
            Line line = cachedLines.get(i);
            boolean tip = i >= tipLine;
            if (i == tipLine) {
                graphics.enableScissor(4, 4, right, bottom);
                graphics.pose().pushMatrix();
                graphics.pose().translate(0, (1 - tipAlpha) * 3);
                tipClipped = true;
            }
            graphics.text(font, line.text, 10, y,
                    ARGB.multiplyAlpha(line.color | 0xFF000000, alpha * (tip ? tipAlpha : 1)), true);
            y += 12;
        }
        if (tipClipped) {
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    static void tick(Minecraft mc) {
        // F1 may suppress the render callback altogether, so suspension also clears captured GUI state here.
        if (!canShow(mc)) reset();
    }

    private static boolean canShow(Minecraft mc) {
        return ClientConfig.get().showSelectionHud && mc.player != null && mc.level != null
                && !mc.gui.hud.isHidden() && mc.gui.screen() == null && mc.isWindowActive()
                && !mc.isPaused() && mc.player.isAlive() && !mc.player.isSpectator();
    }

    static void reset() {
        opacity.snap(0);
        tipOpacity.snap(0);
        panelWidth.snap(0);
        panelHeight.snap(0);
        accentBlend.snap(0);
        heldRows = wrappedRows = List.of();
        cachedLines = List.of();
        cachedFont = null;
        cachedLanguage = null;
        targetAccent = fromAccent = 0;
    }

    private static List<Line> wrap(List<Row> rows, Font font, int width) {
        List<Line> lines = new ArrayList<>();
        for (Row row : rows) {
            for (var part : font.split(row.text, width)) lines.add(new Line(part, row.color));
        }
        return lines;
    }

    private SelectionHud() { }
}
