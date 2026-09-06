package com.thonyy.furnacedistributor.forge.client;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.RenderHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = FurnaceDistributor.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ForgeRenderEvents {

    @SubscribeEvent
    public static void onRenderLevel(
            RenderLevelStageEvent event
    ) {
        if (
                event.getStage()
                        != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
        ) {
            return;
        }

        RenderHandler.render(
                event.getPoseStack(),
                event.getCamera()
        );
    }

    private ForgeRenderEvents() {
    }
}