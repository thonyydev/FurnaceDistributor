package com.thonyy.furnacedistributor.feedback;

import net.minecraft.network.chat.Component;

/** Suppresses identical feedback briefly; changed results and errors are always shown immediately. */
public final class FeedbackThrottle {
    private static final int REPEAT_INTERVAL_TICKS = 20;
    private Component previous;
    private long shownAt;
    private boolean previousOverlay;

    public boolean shouldShow(Component message, long tick, boolean overlay) {
        if (overlay == previousOverlay && message.equals(previous) && tick >= shownAt
                && tick - shownAt < REPEAT_INTERVAL_TICKS) return false;
        previous = message.copy();
        previousOverlay = overlay;
        shownAt = tick;
        return true;
    }
}
