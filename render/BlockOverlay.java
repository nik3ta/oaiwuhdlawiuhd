package nuclear.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.render.shader.ShaderUtil;
import nuclear.ui.clickgui.Panel;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@Annotation(name = "BlockOverlay", type = TypeList.Render, desc = "Шейдерный оверлей с плавной интерполяцией")
public class BlockOverlay extends Module {

    private final SliderSetting speedSetting = new SliderSetting("Скорость", 2.0f, 0.1f, 2.0f, 0.1f);
    private final BooleanSetting interpolationSetting = new BooleanSetting("Интерполяция движения", true);
    private AxisAlignedBB currentBB = null;
    private BlockPos lastTargetPos = null;
    private List<AxisAlignedBB> currentStairsBBs = null;

    public BlockOverlay() {
        addSettings(speedSetting, interpolationSetting);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render && render.isRender3D()) {
            if (mc.world == null || mc.player == null) return false;

            RayTraceResult result = mc.objectMouseOver;

            if (result == null || result.getType() != RayTraceResult.Type.BLOCK) {
                lastTargetPos = null;
                currentBB = null;
                currentStairsBBs = null;
                return false;
            }

            BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
            BlockPos targetPos = blockResult.getPos();
            Vector3d view = mc.getRenderManager().info.getProjectedView();
            BlockState blockState = mc.world.getBlockState(targetPos);

            if (blockState.getBlock() instanceof StairsBlock) {
                VoxelShape shape = blockState.getShape(mc.world, targetPos, ISelectionContext.dummy());
                List<AxisAlignedBB> targetBBs = shape.toBoundingBoxList();

                if (targetBBs.isEmpty()) {
                    return false;
                }

                if (currentStairsBBs == null || currentStairsBBs.size() != targetBBs.size()) {
                    currentStairsBBs = new java.util.ArrayList<>();
                    for (AxisAlignedBB bb : targetBBs) {
                        currentStairsBBs.add(bb.offset(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
                    }
                }

                List<AxisAlignedBB> renderBBs = new java.util.ArrayList<>();

                if (interpolationSetting.get()) {
                    double speed = 0.15 * (mc.getRenderPartialTicks() * 2.0);

                    for (int i = 0; i < targetBBs.size(); i++) {
                        AxisAlignedBB targetBB = targetBBs.get(i).offset(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                        AxisAlignedBB currentPartBB = currentStairsBBs.get(i);

                        currentStairsBBs.set(i, new AxisAlignedBB(
                                lerp(currentPartBB.minX, targetBB.minX, speed),
                                lerp(currentPartBB.minY, targetBB.minY, speed),
                                lerp(currentPartBB.minZ, targetBB.minZ, speed),
                                lerp(currentPartBB.maxX, targetBB.maxX, speed),
                                lerp(currentPartBB.maxY, targetBB.maxY, speed),
                                lerp(currentPartBB.maxZ, targetBB.maxZ, speed)
                        ));

                        renderBBs.add(currentStairsBBs.get(i).offset(-view.x, -view.y, -view.z).grow(0.002));
                    }
                } else {
                    for (AxisAlignedBB bb : targetBBs) {
                        renderBBs.add(bb.offset(targetPos.getX() - view.x, targetPos.getY() - view.y, targetPos.getZ() - view.z).grow(0.002));
                    }
                    currentStairsBBs = new java.util.ArrayList<>();
                    for (AxisAlignedBB bb : targetBBs) {
                        currentStairsBBs.add(bb.offset(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
                    }
                }

                renderStairsOverlay(renderBBs);

                currentBB = null;
            } else {
                currentStairsBBs = null;

                AxisAlignedBB targetBB = blockState.getShape(mc.world, targetPos).getBoundingBox()
                        .offset(targetPos.getX(), targetPos.getY(), targetPos.getZ());

                AxisAlignedBB renderBB;

                if (interpolationSetting.get()) {
                    if (currentBB == null) {
                        currentBB = targetBB;
                    }

                    double speed = 0.15 * (mc.getRenderPartialTicks() * 2.0);

                    currentBB = new AxisAlignedBB(
                            lerp(currentBB.minX, targetBB.minX, speed),
                            lerp(currentBB.minY, targetBB.minY, speed),
                            lerp(currentBB.minZ, targetBB.minZ, speed),
                            lerp(currentBB.maxX, targetBB.maxX, speed),
                            lerp(currentBB.maxY, targetBB.maxY, speed),
                            lerp(currentBB.maxZ, targetBB.maxZ, speed)
                    );

                    renderBB = currentBB.offset(-view.x, -view.y, -view.z).grow(0.002);
                } else {
                    renderBB = targetBB.offset(-view.x, -view.y, -view.z).grow(0.002);
                    currentBB = targetBB;
                }

                renderOverlay(renderBB);
            }
        }
        return false;
    }

    private double lerp(double start, double end, double step) {
        return start + (end - start) * Math.min(1.0, step);
    }

    private void renderOverlay(AxisAlignedBB bb) {
        if (ShaderUtil.blockoverlay == null) return;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderUtil.blockoverlay.attach();

        float time = (System.currentTimeMillis() % 100000) * 0.001f;

        int themeColorInt = Panel.getColorByName("primaryColor");
        Color themeColor = new Color(themeColorInt);

        ShaderUtil.blockoverlay.setUniformf("time", time);
        ShaderUtil.blockoverlay.setUniformf("speed", (float) speedSetting.getValue(), (float) speedSetting.getValue());
        ShaderUtil.blockoverlay.setUniformf("themeColor", themeColor.getRed() / 255.0f, themeColor.getGreen() / 255.0f, themeColor.getBlue() / 255.0f);
        ShaderUtil.blockoverlay.setUniformf("alpha", 0.5f);

        drawBox(bb);

        ShaderUtil.blockoverlay.detach();

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void renderStairsOverlay(List<AxisAlignedBB> bbs) {
        if (ShaderUtil.blockoverlay == null || bbs.isEmpty()) return;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderUtil.blockoverlay.attach();

        float time = (System.currentTimeMillis() % 100000) * 0.001f;

        int themeColorInt = Panel.getColorByName("primaryColor");
        Color themeColor = new Color(themeColorInt);

        ShaderUtil.blockoverlay.setUniformf("time", time);
        ShaderUtil.blockoverlay.setUniformf("speed", (float) speedSetting.getValue(), (float) speedSetting.getValue());
        ShaderUtil.blockoverlay.setUniformf("themeColor", themeColor.getRed() / 255.0f, themeColor.getGreen() / 255.0f, themeColor.getBlue() / 255.0f);
        ShaderUtil.blockoverlay.setUniformf("alpha", 0.5f);

        for (AxisAlignedBB bb : bbs) {
            drawBox(bb);
        }

        ShaderUtil.blockoverlay.detach();

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void drawBox(AxisAlignedBB bb) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        tessellator.draw();
    }
}