package com.thonyy.furnacedistributor.network;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.logic.DistributionManager;
import com.thonyy.furnacedistributor.logic.InventorySelection;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Intent only: dimension, area endpoints and a bounded, duplicate-free slot bitset. */
public record BulkDistributePacket(BlockPos pos1, BlockPos pos2, long slots, Identifier dimension) implements CustomPacketPayload {
    public static final Type<BulkDistributePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FurnaceDistributor.MOD_ID, "bulk_distribute"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkDistributePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                validate(packet.slots());
                buffer.writeBlockPos(packet.pos1());
                buffer.writeBlockPos(packet.pos2());
                buffer.writeLong(packet.slots());
                buffer.writeUtf(packet.dimension().toString(), 256);
            }, buffer -> {
                BlockPos first = buffer.readBlockPos();
                BlockPos second = buffer.readBlockPos();
                long slots = buffer.readLong();
                validate(slots);
                return new BulkDistributePacket(first, second, slots, Identifier.parse(buffer.readUtf(256)));
            });

    private static void validate(long slots) {
        if (!InventorySelection.valid(slots, InventorySelection.SLOTS)) {
            throw new IllegalArgumentException("Invalid bulk inventory slot bitset");
        }
    }

    @Override public Type<BulkDistributePacket> type() { return TYPE; }

    public static void handle(BulkDistributePacket packet, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            DistributionManager.distributeBulk(player, packet.pos1(), packet.pos2(), packet.slots(), packet.dimension());
        }
    }
}
