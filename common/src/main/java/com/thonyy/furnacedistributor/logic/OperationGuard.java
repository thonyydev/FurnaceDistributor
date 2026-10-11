package com.thonyy.furnacedistributor.logic;

import com.thonyy.furnacedistributor.feedback.PlayerFeedback;

import com.thonyy.furnacedistributor.config.ServerConfig;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.thonyy.furnacedistributor.furnace.FurnaceAccessRegistry;
import com.thonyy.furnacedistributor.furnace.FurnaceTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperationGuard {
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> LAST_REQUEST.remove(player.getUUID()));
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> LAST_REQUEST.remove(player.getUUID()));
        LifecycleEvent.SERVER_STOPPED.register(server -> LAST_REQUEST.clear());
    }

    /** null means rejected; all validation finishes before any inventory changes. Called on the server thread. */
    public static List<FurnaceTarget> find(ServerPlayer player, BlockPos first, BlockPos second) {
        if (!player.isAlive() || player.isSpectator() || player.hasDisconnected()
                || player.containerMenu != player.inventoryMenu) return null;
        long now = player.level().getGameTime();
        Long previous = LAST_REQUEST.get(player.getUUID());
        if (previous != null && now >= previous && now - previous < ServerConfig.get().requestCooldownTicks) {
            return null; // Silent rejection prevents chat spam as well as repeated area scans.
        }
        LAST_REQUEST.put(player.getUUID(), now);
        AreaBounds area = FurnaceArea.bounds(first, second);
        if (!area.isWithinVolumeLimit()) {
            error(player, "area_too_large", AreaBounds.MAX_VOLUME);
            return null;
        }
        if (!area.isWithinDistance(player.getX(), player.getY(), player.getZ(), ServerConfig.get().maxDistance)) {
            error(player, "area_too_far", ServerConfig.get().maxDistance);
            return null;
        }
        FurnaceArea.Scan scan = FurnaceArea.scan(player.level(), first, second);
        if (!scan.valid()) {
            error(player, scan.error(), AreaBounds.MAX_FURNACES);
            return null;
        }
        List<FurnaceTarget> furnaces = new ArrayList<>();
        for (BlockPos pos : scan.positions()) {
            FurnaceTarget furnace = FurnaceAccessRegistry.find(player.level().getBlockEntity(pos));
            if (furnace != null) {
                // shortcut: mayInteract covers spawn/border, add read-only authorization for the installed claim provider.
                if (!player.level().mayInteract(player, pos)) {
                    error(player, "area_protected");
                    return null;
                }
                if (!furnace.access().canAccess(furnace.entity(), player)) return null;
                furnaces.add(furnace);
            }
        }
        return furnaces;
    }

    public static void error(ServerPlayer player, String key, Object... arguments) {
        PlayerFeedback.actionBar(player, Component.translatable("message.furnacedistributor." + key, arguments)
                .withStyle(ChatFormatting.RED));
    }

    private OperationGuard() { }
}
