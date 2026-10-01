package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class SelectionFeedback {
    private static int confirmationTicks;
    private static int furnaceCount;
    private static boolean collection;

    public static void show(Component message) {
        clear();
        Minecraft minecraft = Minecraft.getInstance();
        // The HUD already describes selection states; avoid a second copy in the action bar.
        if (minecraft.player != null && (!ClientConfig.get().showSelectionHud || minecraft.options.hideGui)) {
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

    public static void tick() {
        if (confirmationTicks > 0) confirmationTicks--;
    }

    public static void clear() {
        confirmationTicks = 0;
        furnaceCount = 0;
    }

    private SelectionFeedback() { }
}
