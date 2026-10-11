package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class SelectionFeedback {
    private static int confirmationTicks;
    private static int furnaceCount;
    private static boolean collection;
    private static int tipTicks;

    public static void show(Component message) {
        clear();
        Minecraft minecraft = Minecraft.getInstance();
        // The HUD already describes selection states; avoid a second copy in the action bar.
        if (minecraft.player != null && (!ClientConfig.get().showSelectionHud || minecraft.gui.hud.isHidden())) {
            PlayerFeedback.actionBar(minecraft.player, message);
        }
    }

    public static void confirmed(boolean collecting, int count) {
        collection = collecting;
        furnaceCount = count;
        // A short notice is independent of the configurable world-outline duration (including zero).
        confirmationTicks = 60;
    }

    public static boolean isConfirmed(boolean collecting) {
        return confirmationTicks > 0 && collection == collecting;
    }

    public static int getFurnaceCount() { return furnaceCount; }

    public static void manualAreaConfirmed() {
        Minecraft mc = Minecraft.getInstance();
        startTip(mc.player != null && mc.level != null && mc.gui.screen() == null
                && !mc.gui.hud.isHidden() && !KeyBindings.COLLECT_KEY.isUnbound());
    }

    private static void startTip(boolean visibleHud) {
        ClientConfig config = ClientConfig.get();
        if (!visibleHud || !config.showSelectionHud || !config.showContextualTips
                || config.collectionTipLearned || config.collectionTipSelections >= 3) return;
        tipTicks = 100;
        config.collectionTipSelections++;
        ClientConfig.save();
    }

    public static boolean hasTip() { return tipTicks > 0; }

    public static boolean isTipEnding() { return tipTicks > 0 && tipTicks <= 5; }

    public static Component tipDescription(KeyMapping key) {
        if (key.isUnbound()) return null;
        Component keycap = Component.literal("[").append(key.getTranslatedKeyMessage()).append("]")
                .withStyle(style -> style.withBold(true).withColor(0xFFFF55));
        return Component.translatable("hud.furnacedistributor.collect_tip", keycap);
    }

    public static void hideTip() { tipTicks = 0; }

    public static void collectedSavedArea() {
        hideTip();
        ClientConfig config = ClientConfig.get();
        if (!config.collectionTipLearned) {
            config.collectionTipLearned = true;
            ClientConfig.save();
        }
    }

    public static void tick() {
        if (confirmationTicks > 0) confirmationTicks--;
        if (!hasTip()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean valid = mc.player != null && mc.level != null && mc.gui.screen() == null
                && !mc.gui.hud.isHidden() && mc.isWindowActive() && mc.player.isAlive() && !mc.player.isSpectator()
                && ClientConfig.get().showSelectionHud && ClientConfig.get().showContextualTips
                && !KeyBindings.COLLECT_KEY.isUnbound() && FurnaceSelectionHandler.hasConfirmedArea()
                && !FurnaceSelectionHandler.isInSelectionMode() && !Collector.isInCollectionMode();
        if (valid) {
            // Reuse the bounded preview cache; no inventory access or extra network requests.
            var scan = SelectionPreview.get(mc, FurnaceSelectionHandler.getLastConfirmedFirstPos(),
                    FurnaceSelectionHandler.getLastConfirmedSecondPos());
            valid = scan.valid() && !scan.positions().isEmpty();
        }
        tickTip(valid);
    }

    private static void tickTip(boolean valid) {
        if (!valid) hideTip();
        else if (tipTicks > 0) tipTicks--;
    }

    public static void clear() {
        confirmationTicks = 0;
        furnaceCount = 0;
        hideTip();
    }

    private SelectionFeedback() { }
}
