package com.thonyy.furnacedistributor.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {

    public static final String CATEGORY =
            "key.categories.furnacedistributor";

    public static final KeyMapping DISTRIBUTE_KEY =
            new KeyMapping(
                    "key.furnacedistributor.distribute",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    CATEGORY
            );

    public static final KeyMapping COLLECT_KEY =
            new KeyMapping(
                    "key.furnacedistributor.collect",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_C,
                    CATEGORY
            );

    public static void register() {
        KeyMappingRegistry.register(DISTRIBUTE_KEY);
        KeyMappingRegistry.register(COLLECT_KEY);
    }

    private KeyBindings() {
    }
}