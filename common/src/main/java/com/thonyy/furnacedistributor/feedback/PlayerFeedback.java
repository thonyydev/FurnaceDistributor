package com.thonyy.furnacedistributor.feedback;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/** Uses Minecraft's own action-bar/chat transport, on both loaders and both logical sides. */
public final class PlayerFeedback {
    private static final Map<Player, FeedbackThrottle> RECENT = new WeakHashMap<>();

    public static void actionBar(Player player, Component message) {
        show(player, message, true);
    }

    public static void important(Player player, Component message) {
        show(player, message, false);
    }

    private static void show(Player player, Component message, boolean overlay) {
        // Integrated servers call from both threads. Weak keys release feedback with the player/session.
        synchronized (RECENT) {
            if (!RECENT.computeIfAbsent(player, ignored -> new FeedbackThrottle())
                    .shouldShow(message, player.tickCount, overlay)) return;
        }
        player.displayClientMessage(message, overlay);
    }

    private PlayerFeedback() { }
}
