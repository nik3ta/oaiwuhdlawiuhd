package nuclear.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.math.MathUtil;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.MathHelper.lerp;
import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "Trails", type = TypeList.Render, desc = "Создаёт плавную линию ходьбы")
public class Trails extends Module {

    public List<Point> points = new ArrayList<>();
    private static final float BLINK_SPEED = 0.002f;
    private final SliderSetting trailLength = new SliderSetting("Длина", 2, 2, 4, 0.5f);

    public Trails() {
        addSettings(trailLength);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render && render.isRender3D()) {
            if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
                return false;
            }
            long currentTime = System.currentTimeMillis();

            points.removeIf(p -> (currentTime - p.time) > trailLength.getValue().floatValue() * 100);

            Vector3d playerPos = interpolatePlayerPosition(render.partialTicks);

            points.add(new Point(playerPos));

            render3DPoints();
            RenderSystem.color4f(1,1,1,1);
        }
        return false;
    }

    private Vector3d interpolatePlayerPosition(float partialTicks) {
        return new Vector3d(
                MathUtil.interpolate(mc.player.getPosX(), mc.player.prevPosX, partialTicks),
                MathUtil.interpolate(mc.player.getPosY(), mc.player.prevPosY, partialTicks),
                MathUtil.interpolate(mc.player.getPosZ(), mc.player.prevPosZ, partialTicks)
        );
    }

    private void render3DPoints() {
        startRendering();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int index = 0;
        float blinkFactor = (float) (Math.sin(System.currentTimeMillis() * BLINK_SPEED) * 0.5 + 0.5);
        for (Point p : points) {
            int c = getColorByName("primaryColor");
            float red = (float) (c >> 16 & 255) / 255.0F;
            float green = (float) (c >> 8 & 255) / 255.0F;
            float blue = (float) (c & 255) / 255.0F;
            red = lerp(blinkFactor, red, Math.min(red + 0.3f, 1.0f));
            green = lerp(blinkFactor, green, Math.min(green + 0.3f, 1.0f));
            blue = lerp(blinkFactor, blue, Math.min(blue + 0.3f, 1.0f));
            float alpha = (float) index / (float) points.size() * 0.7f;
            Vector3d pos = p.pos.subtract(mc.getRenderManager().info.getProjectedView());
            buffer.pos(pos.x, pos.y + mc.player.getHeight(), pos.z).color(red, green, blue, alpha).endVertex();
            buffer.pos(pos.x, pos.y, pos.z).color(red, green, blue, alpha).endVertex();
            index++;
        }
        TESSELLATOR.draw();

        RenderSystem.lineWidth(1);

        renderLineStrip(points, true);
        renderLineStrip(points, false);

        stopRendering();
    }

    private void renderLineStrip(List<Point> points, boolean withHeight) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        Vector3d projectedView = mc.getRenderManager().info.getProjectedView();

        int index = 0;
        float blinkFactor = (float) (Math.sin(System.currentTimeMillis() * BLINK_SPEED) * 0.5 + 0.5);
        for (Point p : points) {
            int c = getColorByName("primaryColor");
            float red = (float) (c >> 16 & 255) / 255.0F;
            float green = (float) (c >> 8 & 255) / 255.0F;
            float blue = (float) (c & 255) / 255.0F;
            red = lerp(blinkFactor, red, Math.min(red + 0.3f, 1.0f));
            green = lerp(blinkFactor, green, Math.min(green + 0.3f, 1.0f));
            blue = lerp(blinkFactor, blue, Math.min(blue + 0.3f, 1.0f));
            float alpha = (float) index / (float) points.size() * 1.5f;
            alpha = Math.min(alpha, 1f);
            Vector3d pos = p.pos.subtract(projectedView);
            if (withHeight)
                buffer.pos(pos.x, pos.y + mc.player.getHeight(), pos.z).color(red, green, blue, alpha).endVertex();
            else
                buffer.pos(pos.x, pos.y, pos.z).color(red, green, blue, alpha).endVertex();
            index++;
        }

        TESSELLATOR.draw();
    }

    class Point {
        public Vector3d pos;
        public long time;

        public Point(Vector3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }

    private void startRendering() {
        RenderSystem.pushMatrix();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1, 1, 1, 0.5f);
    }

    private void stopRendering() {
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.popMatrix();
    }

    @Override
    public void onDisable() {
        points.clear();
    }
}