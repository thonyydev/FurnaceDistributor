package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.logic.DistributionManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record DistributePacket(
        BlockPos pos1,
        BlockPos pos2
) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
    }

    public static DistributePacket decode(FriendlyByteBuf buf) {
        return new DistributePacket(
                buf.readBlockPos(),
                buf.readBlockPos()
        );
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