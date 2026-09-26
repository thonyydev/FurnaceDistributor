package com.thonyy.furnacedistributor.neoforge.client;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.RenderHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = FurnaceDistributor.MOD_ID,
        value = Dist.CLIENT
)
public final class NeoForgeRenderEvents {

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

    private NeoForgeRenderEvents() {
    }
}
