package com.thonyy.furnacedistributor.client;

/** A single GUI property; retargeting starts at its current value, not the previous endpoint. */
final class UiTransition {
    private float from;
    private float target;
    private long started;
    private int duration;
    private boolean easeOut;

    void snap(float value) {
        from = target = value;
        duration = 0;
    }

    float update(float value, long now, int millis, boolean out) {
        if (millis == 0) snap(value);
        else if (value != target) {
            from = value(now);
            target = value;
            started = now;
            duration = millis;
            easeOut = out;
        }
        return value(now);
    }

    float value(long now) {
        if (duration == 0) return target;
        float t = Math.clamp((float) (now - started) / duration, 0, 1);
        float remaining = 2 - 2 * t;
        float eased = easeOut ? 1 - (1 - t) * (1 - t) * (1 - t)
                : t < 0.5f ? 4 * t * t * t : 1 - remaining * remaining * remaining / 2;
        return from + (target - from) * eased;
    }
}
