package com.thonyy.furnacedistributor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thonyy.furnacedistributor.logic.FurnaceArea;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RenderHandler {
    private record Outline(AABB box, int color) { }
    private record Frame(Vec3 camera, Quaternionf rotation, List<Outline> outlines,
                         List<BlockPos> furnaces, ItemStackRenderState item, float bobbing) { }
    private static final Map<LevelRenderState, Frame> FRAMES = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    /** Read live world/player data only during extraction; submissions use the captured frame. */
    public static void extract(Camera camera, LevelRenderState levelState, Frustum frustum) {
        Minecraft mc = Minecraft.getInstance();
        FRAMES.remove(levelState);
        if (mc.level == null || mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null
                || !mc.player.isAlive() || mc.player.isSpectator()) return;
        if (FurnaceSelectionHandler.getFirstPos() == null && Collector.getFirstCollectPos() == null) return;
        List<Outline> outlines = new ArrayList<>();
        BlockPos first = FurnaceSelectionHandler.getFirstPos();
        BlockPos second = FurnaceSelectionHandler.getSecondPos();
        BlockPos preview = FurnaceSelectionHandler.isInSelectionMode()
                ? FurnaceSelectionHandler.getLookingAtPos(mc) : null;
        if (!SelectionPreview.isSupportedFurnace(mc, preview)) preview = null;
        if (first != null) {
            block(outlines, first, 0xCC00FF00);
            if (second != null) {
                area(outlines, first, second, 0xCC0080FF, 0.02);
                block(outlines, second, 0xCC00FFFF);
            } else if (preview != null) {
                area(outlines, first, preview, 0x80FFFF00, 0.01);
                block(outlines, preview, 0xFFFFFF00);
            }
        }
        BlockPos collectFirst = Collector.getFirstCollectPos();
        BlockPos collectSecond = Collector.getSecondCollectPos();
        if (collectFirst != null) {
            block(outlines, collectFirst, 0xCCB233FF);
            if (collectSecond == null && Collector.isInCollectionMode()) {
                BlockPos target = FurnaceSelectionHandler.getLookingAtPos(mc);
                if (SelectionPreview.isSupportedFurnace(mc, target)) collectSecond = target;
            }
            if (collectSecond != null) {
                area(outlines, collectFirst, collectSecond, 0xCCB233FF, 0.02);
                block(outlines, collectSecond, 0xCCE566FF);
            }
        }
        List<BlockPos> positions = first != null && second == null && preview != null
                && ClientConfig.get().showItemPreview && !mc.player.getMainHandItem().isEmpty()
                ? SelectionPreview.get(mc, first, preview).positions() : List.of();
        List<BlockPos> furnaces = List.of();
        ItemStackRenderState item = null;
        if (!positions.isEmpty()) {
            item = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(item, mc.player.getMainHandItem(),
                    ItemDisplayContext.GROUND, mc.level, mc.player, 0);
            furnaces = visiblePreview(positions, frustum, ClientConfig.get().maxPreviewItems,
                    previewRadius(item.getModelBoundingBox()));
        }
        float bobbing = (float) Math.sin((System.currentTimeMillis() % 3000L) / 3000.0 * Math.PI * 2) * 0.1f;
        FRAMES.put(levelState, new Frame(camera.position(), new Quaternionf(camera.rotation()), List.copyOf(outlines), furnaces, item, bobbing));
    }

    private static double previewRadius(AABB model) {
        double x = Math.max(Math.abs(model.minX), Math.abs(model.maxX));
        double y = Math.max(Math.abs(model.minY), Math.abs(model.maxY));
        double z = Math.max(Math.abs(model.minZ), Math.abs(model.maxZ));
        // A sphere includes every billboard rotation, including larger resource-pack models.
        return Math.max(0.5, Math.sqrt(x * x + y * y + z * z) * 0.5);
    }

    private static List<BlockPos> visiblePreview(List<BlockPos> positions, Frustum frustum, int limit, double radius) {
        List<BlockPos> visible = new ArrayList<>();
        for (BlockPos pos : positions) {
            double x = pos.getX() + 0.5, y = pos.getY() + 1.3, z = pos.getZ() + 0.5;
            if (!frustum.isVisible(new AABB(x - radius, y - radius - 0.1, z - radius,
                    x + radius, y + radius + 0.1, z + radius))) continue;
            visible.add(pos);
            if (visible.size() == limit) break;
        }
        return List.copyOf(visible);
    }

    public static void submit(LevelRenderState levelState, SubmitNodeCollector collector) {
        Frame captured = FRAMES.get(levelState);
        if (captured == null) return;
        PoseStack pose = new PoseStack();
        pose.translate(-captured.camera.x, -captured.camera.y, -captured.camera.z);
        for (Outline outline : captured.outlines) {
            collector.submitShapeOutline(pose, Shapes.create(outline.box), RenderTypes.lines(), outline.color, 1.0f, true);
        }
        for (BlockPos pos : captured.furnaces) {
            Vec3 location = Vec3.atCenterOf(pos).add(0, 0.8 + captured.bobbing, 0);
            pose.pushPose();
            pose.translate(location.x, location.y, location.z);
            pose.mulPose(captured.rotation);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(0.5f, 0.5f, 0.5f);
            captured.item.submit(pose, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static void block(List<Outline> outlines, BlockPos pos, int color) {
        if (!ClientConfig.get().showOutlines) return;
        outlines.add(new Outline(new AABB(pos).inflate(0.003), glow(color)));
        outlines.add(new Outline(new AABB(pos).inflate(0.001), color));
    }

    private static void area(List<Outline> outlines, BlockPos first, BlockPos second, int color, double glowSize) {
        if (!ClientConfig.get().showOutlines) return;
        var bounds = FurnaceArea.bounds(first, second);
        AABB box = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0);
        outlines.add(new Outline(box.inflate(glowSize), glow(color)));
        outlines.add(new Outline(box, color));
    }

    private static int glow(int color) { return ((int) ((color >>> 24) * 0.35f) << 24) | (color & 0xFFFFFF); }
    private RenderHandler() { }
}
