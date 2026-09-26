package com.thonyy.furnacedistributor.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public final class ModNetworking {

    public static void register() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                DistributePacket.TYPE,
                DistributePacket.STREAM_CODEC,
                (packet, context) -> context.queue(() ->
                        DistributePacket.handle(packet, context)
                )
        );

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                CollectPacket.TYPE,
                CollectPacket.STREAM_CODEC,
                (packet, context) -> context.queue(() ->
                        CollectPacket.handle(packet, context)
                )
        );
    }

    @Environment(EnvType.CLIENT)
    public static void sendDistribute(DistributePacket packet) {
        NetworkManager.sendToServer(packet);
    }

    @Environment(EnvType.CLIENT)
    public static void sendCollect(CollectPacket packet) {
        NetworkManager.sendToServer(packet);
    }

    private ModNetworking() {
    }
}
