package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.logic.DistributionManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record DistributePacket(
        BlockPos pos1,
        BlockPos pos2
) implements CustomPacketPayload {

    public static final Type<DistributePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FurnaceDistributor.MOD_ID, "distribute")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DistributePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DistributePacket::pos1,
                    BlockPos.STREAM_CODEC, DistributePacket::pos2,
                    DistributePacket::new
            );

    @Override
    public Type<DistributePacket> type() {
        return TYPE;
    }

    public static void handle(
            DistributePacket packet,
            NetworkManager.PacketContext context
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        DistributionManager.distribute(
                player,
                packet.pos1(),
                packet.pos2()
        );
    }
}
