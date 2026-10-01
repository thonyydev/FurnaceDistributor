package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.logic.AreaBounds;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class SelectionHud {
    public static void register() {
        ClientGuiEvent.RENDER_HUD.register((graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientConfig.get().showSelectionHud || mc.player == null || mc.level == null
                || mc.options.hideGui || mc.screen != null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        boolean distributing = FurnaceSelectionHandler.isInSelectionMode();
        boolean collecting = Collector.isInCollectionMode();
        if (distributing || collecting) {
            int nextY = line(graphics, mc, Component.translatable(distributing
                            ? "hud.furnacedistributor.select_controls" : "hud.furnacedistributor.collect_select_controls",
                    (distributing ? KeyBindings.DISTRIBUTE_KEY : KeyBindings.COLLECT_KEY).getTranslatedKeyMessage(),
                    KeyBindings.CANCEL_KEY.getTranslatedKeyMessage()), 6, collecting ? 0xAA55FF : 0xFFFFFF);
            BlockPos first = distributing ? FurnaceSelectionHandler.getFirstPos() : Collector.getFirstCollectPos();
            BlockPos target = FurnaceSelectionHandler.getLookingAtPos(mc);
            // Keep the first furnace/count visible when the crosshair leaves a valid target.
            if (collecting && !SelectionPreview.isSupportedFurnace(mc, target)) target = first;
            if (SelectionPreview.isSupportedFurnace(mc, target)) {
                FurnaceArea.Scan scan = SelectionPreview.get(mc, first, target);
                Component info = scan.valid()
                        ? Component.translatable(distributing ? "hud.furnacedistributor.estimate" : "hud.furnacedistributor.furnaces",
                                scan.positions().size(), mc.player.getMainHandItem().getCount())
                        : Component.translatable("message.furnacedistributor." + scan.error(),
                                scan.error().equals("area_too_large") ? AreaBounds.MAX_VOLUME : AreaBounds.MAX_FURNACES);
                line(graphics, mc, info, nextY, scan.valid() ? 0xFFFF55 : 0xFF5555);
            }
        } else if (SelectionFeedback.isConfirmed(false)) {
            int nextY = line(graphics, mc, Component.translatable("hud.furnacedistributor.area_saved",
                    SelectionFeedback.getFurnaceCount()), 6, 0x55FFFF);
            line(graphics, mc, Component.translatable("hud.furnacedistributor.area_controls",
                    KeyBindings.COLLECT_KEY.getTranslatedKeyMessage(), mc.options.keyShift.getTranslatedKeyMessage(),
                    KeyBindings.DISTRIBUTE_KEY.getTranslatedKeyMessage(), KeyBindings.CANCEL_KEY.getTranslatedKeyMessage()), nextY, 0xAAAAAA);
        } else if (SelectionFeedback.isConfirmed(true)) {
            line(graphics, mc, Component.translatable("hud.furnacedistributor.collect_area",
                    SelectionFeedback.getFurnaceCount()), 6, 0xAA55FF);
        }
    }

    private static int line(GuiGraphics graphics, Minecraft mc, Component text, int y, int color) {
        for (var part : mc.font.split(text, Math.max(40, graphics.guiWidth() - 12))) {
            graphics.fill(4, y - 2, 8 + mc.font.width(part), y + 10, 0x80000000);
            graphics.drawString(mc.font, part, 6, y, color, true);
            y += 12;
        }
        return y;
    }

    private SelectionHud() { }
}
