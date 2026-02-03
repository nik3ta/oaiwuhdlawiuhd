package nuclear.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventJump;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.render.animation.AnimationMath;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR_TEX;
import static nuclear.ui.clickgui.Panel.getColorByName;
import static org.lwjgl.opengl.GL11.GL_QUAD_STRIP;

@Annotation(name = "JumpCircle", type = TypeList.Render)
public class JumpCircle extends Module {
    private final List<Circle> circles = new ArrayList<>();
    private final ResourceLocation texture = new ResourceLocation("nuclear/images/jump/circlekrug.png");

    public SliderSetting radius = new SliderSetting("Радиус", 2.0f, 0.5f, 5.0f, 0.1f);
    public SliderSetting speed = new SliderSetting("Скорость", 1.5f, 0.1f, 5.0f, 0.1f);

    public JumpCircle() {
        addSettings(radius, speed);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventJump) {
            addCircle();
        } else if (event instanceof EventRender render && render.isRender3D()) {
            updateCircles();
            renderCircles();
        }
        return false;
    }

    private void addCircle() {
        circles.add(new Circle((float) mc.player.getPosX(), (float) (mc.player.getPosY() + 0.02), (float) mc.player.getPosZ()));
    }

    private void updateCircles() {
        float rSpeed = speed.getValue().floatValue();
        float rMax = radius.getValue().floatValue();

        for (Circle circle : circles) {
            circle.factor = AnimationMath.fast(circle.factor, rMax, rSpeed);
            circle.alpha -= 0.001f;
        }
        circles.removeIf(circle -> circle.alpha <= 0);
    }

    private void renderCircles() {
        setupRenderSettings();
        int themeColor = getColorByName("primaryColor");
        for (Circle circle : circles) {
            if (circle.alpha > 0) {
                drawJumpCircle(circle, circle.factor, circle.alpha, themeColor);
            }
        }
        restoreRenderSettings();
    }

    private void setupRenderSettings() {
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 1, 0);

        double x = mc.getRenderManager().info.getProjectedView().getX();
        double y = mc.getRenderManager().info.getProjectedView().getY();
        double z = mc.getRenderManager().info.getProjectedView().getZ();
        GlStateManager.translated(-x, -y, -z);
    }

    private void restoreRenderSettings() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    private void drawJumpCircle(Circle circle, float radius, float alpha, int themeColor) {
        GlStateManager.pushMatrix();
        GlStateManager.translated(circle.spawnX, circle.spawnY, circle.spawnZ);
        GlStateManager.rotatef(circle.factor * 50, 0, -1, 0);

        mc.getTextureManager().bindTexture(texture);

        float r = (themeColor >> 16 & 0xFF) / 255f;
        float g = (themeColor >> 8 & 0xFF) / 255f;
        float b = (themeColor & 0xFF) / 255f;

        BUFFER.begin(GL_QUAD_STRIP, POSITION_COLOR_TEX);
        for (int i = 0; i <= 360; i += 5) {
            double rad = Math.toRadians(i);
            double sin = MathHelper.sin(rad) * radius;
            double cos = MathHelper.cos(rad) * radius;

            BUFFER.pos(0, 0, 0).color(r, g, b, alpha).tex(0.5f, 0.5f).endVertex();
            BUFFER.pos(sin, 0, cos).color(r, g, b, alpha).tex((float) (sin / (2 * radius) + 0.5f), (float) (cos / (2 * radius) + 0.5f)).endVertex();
        }
        TESSELLATOR.draw();
        GlStateManager.popMatrix();
    }

    private static class Circle {
        public final float spawnX, spawnY, spawnZ;
        public float factor = 0;
        public float alpha = 1.0f;

        public Circle(float spawnX, float spawnY, float spawnZ) {
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
        }
    }
}