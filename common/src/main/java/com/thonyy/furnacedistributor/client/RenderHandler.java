package com.thonyy.furnacedistributor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class RenderHandler {

    public static void render(
            PoseStack poseStack,
            Camera camera
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource =
                mc.renderBuffers().bufferSource();

        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();

        poseStack.translate(
                -cameraPosition.x,
                -cameraPosition.y,
                -cameraPosition.z
        );

        renderDistributionMode(
                mc,
                poseStack,
                bufferSource
        );

        renderCollectionMode(
                mc,
                poseStack,
                bufferSource
        );

        poseStack.popPose();

        /*
         * Desenha os buffers utilizados pelo nosso render.
         */
        bufferSource.endBatch();
    }

    private static void renderDistributionMode(
            Minecraft mc,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource
    ) {
        BlockPos firstPos =
                FurnaceSelectionHandler.getFirstPos();

        BlockPos secondPos =
                FurnaceSelectionHandler.getSecondPos();

        if (firstPos == null && secondPos == null) {
            return;
        }

        /*
         * Primeira fornalha selecionada.
         * Verde.
         */
        if (firstPos != null) {
            renderBlockOutline(
                    poseStack,
                    bufferSource,
                    firstPos,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.8f
            );
        }

        /*
         * Enquanto o jogador está escolhendo a
         * segunda posição:
         *
         * - área amarela
         * - outline amarelo
         * - preview dos itens
         */
        if (
                FurnaceSelectionHandler.isInSelectionMode()
                        && firstPos != null
                        && secondPos == null
        ) {
            BlockPos lookingAt =
                    FurnaceSelectionHandler.getLookingAtPos(mc);

            if (
                    lookingAt != null
                            && mc.level
                            .getBlockState(lookingAt)
                            .getBlock()
                            instanceof AbstractFurnaceBlock
            ) {
                renderArea(
                        poseStack,
                        bufferSource,
                        firstPos,
                        lookingAt,
                        1.0f,
                        1.0f,
                        0.0f,
                        0.5f,
                        0.01
                );

                renderBlockOutline(
                        poseStack,
                        bufferSource,
                        lookingAt,
                        1.0f,
                        1.0f,
                        0.0f,
                        1.0f
                );

                renderItemsOnFurnaces(
                        mc,
                        poseStack,
                        bufferSource,
                        firstPos,
                        lookingAt
                );
            }
        }

        /*
         * Seleção final confirmada.
         * Azul.
         */
        if (firstPos != null && secondPos != null) {
            renderArea(
                    poseStack,
                    bufferSource,
                    firstPos,
                    secondPos,
                    0.0f,
                    0.5f,
                    1.0f,
                    0.8f,
                    0.02
            );

            renderBlockOutline(
                    poseStack,
                    bufferSource,
                    secondPos,
                    0.0f,
                    1.0f,
                    1.0f,
                    0.8f
            );
        }
    }

    private static void renderCollectionMode(
            Minecraft mc,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource
    ) {
        BlockPos firstPos =
                Collector.getFirstCollectPos();

        BlockPos secondPos =
                Collector.getSecondCollectPos();

        if (firstPos == null && secondPos == null) {
            return;
        }

        /*
         * Primeira posição da coleta.
         */
        if (firstPos != null) {
            renderBlockOutline(
                    poseStack,
                    bufferSource,
                    firstPos,
                    0.7f,
                    0.2f,
                    1.0f,
                    0.8f
            );
        }

        /*
         * Preview enquanto escolhe a segunda posição.
         */
        if (
                Collector.isInCollectionMode()
                        && firstPos != null
                        && secondPos == null
        ) {
            BlockPos lookingAt =
                    FurnaceSelectionHandler.getLookingAtPos(mc);

            if (
                    lookingAt != null
                            && mc.level
                            .getBlockState(lookingAt)
                            .getBlock()
                            instanceof AbstractFurnaceBlock
            ) {
                renderArea(
                        poseStack,
                        bufferSource,
                        firstPos,
                        lookingAt,
                        0.7f,
                        0.2f,
                        1.0f,
                        0.5f,
                        0.01
                );

                renderBlockOutline(
                        poseStack,
                        bufferSource,
                        lookingAt,
                        0.9f,
                        0.4f,
                        1.0f,
                        1.0f
                );
            }
        }

        /*
         * Área final confirmada.
         */
        if (firstPos != null && secondPos != null) {
            renderArea(
                    poseStack,
                    bufferSource,
                    firstPos,
                    secondPos,
                    0.7f,
                    0.2f,
                    1.0f,
                    0.8f,
                    0.02
            );

            renderBlockOutline(
                    poseStack,
                    bufferSource,
                    secondPos,
                    0.9f,
                    0.4f,
                    1.0f,
                    0.8f
            );
        }
    }

    private static void renderItemsOnFurnaces(
            Minecraft mc,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            BlockPos pos1,
            BlockPos pos2
    ) {
        if (mc.player == null) {
            return;
        }

        ItemStack heldItem =
                mc.player.getMainHandItem();

        if (heldItem.isEmpty()) {
            return;
        }

        List<BlockPos> furnaces =
                getFurnacesInArea(
                        mc,
                        pos1,
                        pos2
                );

        if (furnaces.isEmpty()) {
            return;
        }

        int totalItems =
                heldItem.getCount();

        int itemsPerFurnace =
                totalItems / furnaces.size();

        int remainder =
                totalItems % furnaces.size();

        if (itemsPerFurnace == 0) {
            return;
        }

        ItemRenderer itemRenderer =
                mc.getItemRenderer();

        Font font =
                mc.font;

        float time =
                (System.currentTimeMillis() % 3000L)
                        / 3000.0f;

        float bobbing =
                (float) Math.sin(
                        time * Math.PI * 2.0
                ) * 0.1f;

        for (int i = 0; i < furnaces.size(); i++) {
            BlockPos furnacePos =
                    furnaces.get(i);

            int amount =
                    itemsPerFurnace;

            if (i < remainder) {
                amount++;
            }

            Vec3 itemPos =
                    Vec3.atCenterOf(furnacePos)
                            .add(
                                    0,
                                    0.8 + bobbing,
                                    0
                            );

            /*
             * Item flutuando.
             */
            poseStack.pushPose();

            poseStack.translate(
                    itemPos.x,
                    itemPos.y,
                    itemPos.z
            );

            poseStack.mulPose(
                    mc.getEntityRenderDispatcher()
                            .cameraOrientation()
            );

            poseStack.mulPose(
                    Axis.YP.rotationDegrees(180.0f)
            );

            float scale = 0.5f;

            poseStack.scale(
                    scale,
                    scale,
                    scale
            );

            itemRenderer.renderStatic(
                    heldItem,
                    ItemDisplayContext.GROUND,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    mc.level,
                    0
            );

            poseStack.popPose();

            /*
             * Quantidade abaixo do item.
             */
            renderFloatingText(
                    poseStack,
                    bufferSource,
                    font,
                    String.valueOf(amount),
                    itemPos.add(
                            0,
                            -0.35,
                            0
                    ),
                    mc
            );
        }
    }

    private static void renderFloatingText(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            Font font,
            String text,
            Vec3 pos,
            Minecraft mc
    ) {
        poseStack.pushPose();

        poseStack.translate(
                pos.x,
                pos.y,
                pos.z
        );

        poseStack.mulPose(
                mc.getEntityRenderDispatcher()
                        .cameraOrientation()
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(180.0f)
        );

        float scale = 0.02f;

        poseStack.scale(
                -scale,
                -scale,
                scale
        );

        Matrix4f matrix =
                poseStack.last().pose();

        Component textComponent =
                Component.literal(text);

        float textWidth =
                font.width(textComponent);

        float x =
                -textWidth / 2.0f;

        /*
         * Fundo.
         */
        int backgroundColor =
                0x40000000;

        /*
         * Texto.
         */
        font.drawInBatch(
                textComponent,
                x,
                0,
                0xFF55FF55,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                backgroundColor,
                15728880
        );

        poseStack.popPose();
    }

    private static List<BlockPos> getFurnacesInArea(
            Minecraft mc,
            BlockPos pos1,
            BlockPos pos2
    ) {
        List<BlockPos> furnaces =
                new ArrayList<>();

        if (mc.level == null) {
            return furnaces;
        }

        int minX =
                Math.min(
                        pos1.getX(),
                        pos2.getX()
                );

        int minY =
                Math.min(
                        pos1.getY(),
                        pos2.getY()
                );

        int minZ =
                Math.min(
                        pos1.getZ(),
                        pos2.getZ()
                );

        int maxX =
                Math.max(
                        pos1.getX(),
                        pos2.getX()
                );

        int maxY =
                Math.max(
                        pos1.getY(),
                        pos2.getY()
                );

        int maxZ =
                Math.max(
                        pos1.getZ(),
                        pos2.getZ()
                );

        for (
                BlockPos pos :
                BlockPos.betweenClosed(
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ
                )
        ) {
            if (
                    mc.level
                            .getBlockState(pos)
                            .getBlock()
                            instanceof AbstractFurnaceBlock
            ) {
                furnaces.add(
                        pos.immutable()
                );
            }
        }

        return furnaces;
    }

    private static void renderBlockOutline(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            BlockPos pos,
            float r,
            float g,
            float b,
            float alpha
    ) {
        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.lines()
                );

        /*
         * Glow externo.
         */
        AABB glowBox =
                new AABB(pos)
                        .inflate(0.003);

        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                glowBox,
                r,
                g,
                b,
                alpha * 0.35f
        );

        /*
         * Outline principal.
         */
        AABB mainBox =
                new AABB(pos)
                        .inflate(0.001);

        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                mainBox,
                r,
                g,
                b,
                alpha
        );
    }

    private static void renderArea(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            BlockPos pos1,
            BlockPos pos2,
            float r,
            float g,
            float b,
            float alpha,
            double glowSize
    ) {
        int minX =
                Math.min(
                        pos1.getX(),
                        pos2.getX()
                );

        int minY =
                Math.min(
                        pos1.getY(),
                        pos2.getY()
                );

        int minZ =
                Math.min(
                        pos1.getZ(),
                        pos2.getZ()
                );

        int maxX =
                Math.max(
                        pos1.getX(),
                        pos2.getX()
                );

        int maxY =
                Math.max(
                        pos1.getY(),
                        pos2.getY()
                );

        int maxZ =
                Math.max(
                        pos1.getZ(),
                        pos2.getZ()
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.lines()
                );

        /*
         * Glow externo.
         */
        AABB glowBox =
                new AABB(
                        minX,
                        minY,
                        minZ,
                        maxX + 1,
                        maxY + 1,
                        maxZ + 1
                ).inflate(glowSize);

        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                glowBox,
                r,
                g,
                b,
                alpha * 0.35f
        );

        /*
         * Caixa principal.
         */
        AABB mainBox =
                new AABB(
                        minX,
                        minY,
                        minZ,
                        maxX + 1,
                        maxY + 1,
                        maxZ + 1
                );

        LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                mainBox,
                r,
                g,
                b,
                alpha
        );
    }

    private RenderHandler() {
    }
}