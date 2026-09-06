package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.logic.CollectionManager;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record CollectPacket(
        BlockPos pos1,
        BlockPos pos2
) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
    }

    public static CollectPacket decode(FriendlyByteBuf buf) {
        return new CollectPacket(
                buf.readBlockPos(),
                buf.readBlockPos()
        );
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