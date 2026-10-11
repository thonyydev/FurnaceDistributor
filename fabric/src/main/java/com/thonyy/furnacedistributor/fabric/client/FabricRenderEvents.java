package com.thonyy.furnacedistributor.fabric.client;

import com.thonyy.furnacedistributor.client.RenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;

public final class FabricRenderEvents {

    public static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(context -> RenderHandler.extract(context.camera(), context.levelState()));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> RenderHandler.submit(context.levelState(), context.submitNodeCollector()));
    }

    private FabricRenderEvents() {
    }
}
