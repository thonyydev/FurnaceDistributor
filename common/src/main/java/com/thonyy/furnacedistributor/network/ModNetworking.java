package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ModNetworking {

    public static final ResourceLocation DISTRIBUTE =
            new ResourceLocation(FurnaceDistributor.MOD_ID, "distribute");

    public static final ResourceLocation COLLECT =
            new ResourceLocation(FurnaceDistributor.MOD_ID, "collect");

    public static void register() {

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                DISTRIBUTE,
                (buf, context) -> {
                    DistributePacket packet = DistributePacket.decode(buf);

                    context.queue(() ->
                            DistributePacket.handle(packet, context)
                    );
                }
        );

        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                COLLECT,
                (buf, context) -> {
                    CollectPacket packet = CollectPacket.decode(buf);

                    context.queue(() ->
                            CollectPacket.handle(packet, context)
                    );
                }
        );
    }

    public static void sendDistribute(DistributePacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        packet.encode(buf);

        NetworkManager.sendToServer(DISTRIBUTE, buf);
    }

    public static void sendCollect(CollectPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        packet.encode(buf);

        NetworkManager.sendToServer(COLLECT, buf);
    }

    private ModNetworking() {
    }
}