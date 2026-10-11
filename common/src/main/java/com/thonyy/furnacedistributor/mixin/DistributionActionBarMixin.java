package com.thonyy.furnacedistributor.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(Hud.class)
public abstract class DistributionActionBarMixin {
    @WrapOperation(method = "extractOverlayMessage", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"))
    private void wrapDistributionResult(GuiGraphicsExtractor graphics, Font font, Component message,
                                        int x, int y, int width, int color, Operation<Void> original) {
        int available = Math.max(1, graphics.guiWidth() - 40);
        if (!isDistributionResult(message) || width <= available) {
            original.call(graphics, font, message, x, y, width, color);
            return;
        }
        // Keep vanilla's lifetime, fade, styling and backdrop; extend upward above the hotbar.
        List<Component> lines = lines(font, message, available);
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lineWidth = font.width(line);
            original.call(graphics, font, line, -lineWidth / 2, y - (lines.size() - 1 - i) * 12, lineWidth, color);
        }
    }

    private static boolean isDistributionResult(Component message) {
        return message.getContents() instanceof TranslatableContents text
                && (text.getKey().equals("message.furnacedistributor.distributed_items")
                    || text.getKey().equals("message.furnacedistributor.distributed_none"));
    }

    private static List<Component> lines(Font font, Component message, int width) {
        return font.getSplitter().splitLines(message, width, Style.EMPTY).stream().<Component>map(part -> {
            var line = Component.empty();
            part.visit((style, text) -> {
                line.append(Component.literal(text).setStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            return line;
        }).toList();
    }
}
