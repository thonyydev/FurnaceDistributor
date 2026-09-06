package com.thonyy.furnacedistributor.client;

import com.thonyy.furnacedistributor.network.DistributePacket;
import com.thonyy.furnacedistributor.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Distributor {

    public static void distributeItems(
            BlockPos pos1,
            BlockPos pos2
    ) {
        Minecraft mc = Minecraft.getInstance();

        Player player = mc.player;

        if (player == null || mc.level == null) {
            return;
        }

        if (player.getMainHandItem().isEmpty()) {

            player.displayClientMessage(
                    Component.translatable(
                                    "message.furnacedistributor.no_item"
                            )
                            .withStyle(ChatFormatting.RED),
                    false
            );

            return;
        }

        ModNetworking.sendDistribute(
                new DistributePacket(
                        pos1,
                        pos2
                )
        );
    }

    private Distributor() {
    }
}