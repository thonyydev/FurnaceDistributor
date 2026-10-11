package com.thonyy.furnacedistributor.neoforge.client;

import com.thonyy.furnacedistributor.FurnaceDistributor;
import com.thonyy.furnacedistributor.client.RenderHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(
        modid = FurnaceDistributor.MOD_ID,
        value = Dist.CLIENT
)
public final class NeoForgeRenderEvents {

    @SubscribeEvent
    public static void onExtractLevel(ExtractLevelRenderStateEvent event) {
        RenderHandler.extract(event.getCamera(), event.getRenderState(), event.getFrustum());
    }

    @SubscribeEvent
    public static void onSubmit(SubmitCustomGeometryEvent event) { RenderHandler.submit(event.getLevelRenderState(), event.getSubmitNodeCollector()); }

    private NeoForgeRenderEvents() {
    }
}
