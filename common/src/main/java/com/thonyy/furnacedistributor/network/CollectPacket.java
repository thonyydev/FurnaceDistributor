package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.logic.CollectionManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record CollectPacket(
        BlockPos pos1,
        BlockPos pos2
) implements CustomPacketPayload {

    public static final Type<CollectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FurnaceDistributor.MOD_ID, "collect")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CollectPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CollectPacket::pos1,
                    BlockPos.STREAM_CODEC, CollectPacket::pos2,
                    CollectPacket::new
            );

    @Override
    public Type<CollectPacket> type() {
        return TYPE;
    }

    public static void handle(
            CollectPacket packet,
            NetworkManager.PacketContext context
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        CollectionManager.collect(
                player,
                packet.pos1(),
                packet.pos2()
        );
    }
}
