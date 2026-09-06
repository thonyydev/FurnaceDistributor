package com.thonyy.furnacedistributor.fabric.client;

import com.thonyy.furnacedistributor.client.RenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class FabricRenderEvents {

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(
                context -> {
                    if (context.matrixStack() == null) {
                        return;
                    }

                    RenderHandler.render(
                            context.matrixStack(),
                            context.camera()
                    );
                }
        );
    }

    private FabricRenderEvents() {
    }
}