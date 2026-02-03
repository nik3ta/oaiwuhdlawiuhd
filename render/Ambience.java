package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.gen.Heightmap;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ColorSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.shader.ShaderUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.time.LocalTime;

@Annotation(name = "Ambience", type = TypeList.Render, desc = "Изменяет ваш мир")
public class Ambience extends Module {
    private final ModeSetting timeMode = new ModeSetting("Время", "Не менять", "Рассвет", "Утро", "День", "Заход", "Вечер", "Ночь", "Время из жизни", "Не менять");

    private final BooleanSetting snow = new BooleanSetting("Снег", true);
    private final SliderSetting sizesnow = new SliderSetting("Сила снегопада", 2f, 0.5f, 5f, 0.5f).setVisible(() -> snow.get());

    private final ModeSetting fogMode = new ModeSetting("Туман", "Ничего не делать", "Ничего не делать", "Очистить", "Переопределить");
    public ColorSetting color = new ColorSetting("Цвет тумана", ColorUtils.rgba(128, 115, 225, 255)).setVisible(() -> fogMode.is("Переопределить"));
    private final SliderSetting fogStart = new SliderSetting("Начало тумана", 0.5f, 0.1f, 1.5f, 0.1f).setVisible(() -> fogMode.is("Переопределить"));
    private final SliderSetting fogEnd = new SliderSetting("Конец тумана", 1.0f, 0.1f, 1.5f, 0.1f).setVisible(() -> fogMode.is("Переопределить"));

    public final BooleanSetting aurora = new BooleanSetting("Северное сияние", false);
    private final SliderSetting auroraIntensity = new SliderSetting("Интенсивность", 1.5f, 0.1f, 2.0f, 0.1f).setVisible(() -> aurora.get());
    private final SliderSetting auroraSpeed = new SliderSetting("Скорость", 1.5f, 0.1f, 3.0f, 0.1f).setVisible(() -> aurora.get());
    private final ColorSetting auroraColor1 = new ColorSetting("Цвет сияние 1", ColorUtils.rgba(51, 204, 255, 255)).setVisible(() -> aurora.get());
    private final ColorSetting auroraColor2 = new ColorSetting("Цвет сияние 2", ColorUtils.rgba(26, 230, 128, 255)).setVisible(() -> aurora.get());

    private float auroraTime = 0.0f;
    private static AuroraShader auroraShader;

    public Ambience() {
        addSettings(timeMode, snow, sizesnow, fogMode, color, fogStart, fogEnd, aurora, auroraIntensity, auroraSpeed, auroraColor1, auroraColor2);
        if (auroraShader == null) {
            try {
                auroraShader = new AuroraShader();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class AuroraShader extends ShaderUtil {
        public AuroraShader() throws IOException {
            super("aurora", "aurora_vertex");
        }
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket eventPacket && ((EventPacket) event).isReceivePacket()) {
            if (eventPacket.getPacket() instanceof SUpdateTimePacket) {
                eventPacket.setCancel(true);
            }
        }
        if (event instanceof EventUpdate) {
            if (aurora.get()) {
                auroraTime += 0.02f * auroraSpeed.getValue().floatValue();
                if (auroraTime > 1000.0f) {
                    auroraTime = 0.0f;
                }
            }

            if (snow.get()) {
                int particlesPerTick = (int) (50 * sizesnow.getValue().floatValue());
                if (mc.world.getGameTime() % 2 != 0) return false;

                for (int i = 0; i < particlesPerTick; i++) {
                    double offsetX = (Math.random() - 0.5) * 130;
                    double offsetZ = (Math.random() - 0.5) * 130;
                    double x = mc.player.getPosX() + offsetX;
                    double z = mc.player.getPosZ() + offsetZ;

                    int surfaceY = mc.world.getHeight(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));

                    double minHeightAboveGround = 5.0;
                    double maxHeightAboveGround = 40.0;
                    double y = surfaceY + minHeightAboveGround + Math.random() * (maxHeightAboveGround - minHeightAboveGround);


                    if (y < mc.player.getPosY() - 20) {
                        y = mc.player.getPosY() + 5.0 + Math.random() * 30.0;
                    }

                    mc.world.addParticle(
                            new BlockParticleData(ParticleTypes.FALLING_DUST, Blocks.SNOW.getDefaultState()),
                            x, y, z,
                            0.0D,
                            -0.03D - Math.random() * 0.03D,
                            0.0D
                    );
                }
            }

            if (!timeMode.get().equals("Не менять")) {
                long time;
                if (timeMode.get().equals("Время из жизни")) {
                    time = getRealWorldTime();
                } else {
                    time = switch (timeMode.get()) {
                        case "Рассвет" -> 23000L;
                        case "Утро" -> 1000L;
                        case "День" -> 6000L;
                        case "Вечер" -> 12000L;
                        case "Заход" -> 13000L;
                        case "Ночь" -> 18000L;
                        default -> mc.world.getDayTime();
                    };
                }
                mc.world.setDayTime(time);
            }
        }
        return false;
    }

    private long getRealWorldTime() {
        LocalTime now = LocalTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        int seconds = now.getSecond();

        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        int offsetSeconds = (totalSeconds - 6 * 3600) % (24 * 3600);
        if (offsetSeconds < 0) offsetSeconds += 24 * 3600;

        return (long) ((offsetSeconds / 86400.0) * 24000);
    }

    public String getFogMode() {
        return fogMode.get();
    }

    public float getFogStart() {
        return fogStart.getValue().floatValue();
    }

    public float getFogEnd() {
        return fogEnd.getValue().floatValue();
    }

    public Color getFogColor() {
        return new Color(color.get());
    }

    public boolean shouldRenderAurora() {
        return aurora.get() && state;
    }

    public void renderAurora(MatrixStack matrixStack, float partialTicks) {
        if (!shouldRenderAurora() || auroraShader == null) return;

        float intensity = auroraIntensity.getValue().floatValue();
        float time = auroraTime + partialTicks * 0.01f * auroraSpeed.getValue().floatValue();

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        matrixStack.push();

        matrixStack.rotate(Vector3f.XP.rotationDegrees(-mc.world.func_242415_f(partialTicks) * 360.0F));

        auroraShader.attach();

        Vector3d cameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();

        Color color1 = new Color(auroraColor1.get());
        Color color2 = new Color(auroraColor2.get());
        Color color3 = new Color(auroraColor1.get());

        auroraShader.setUniform("time", time);
        auroraShader.setUniform("intensity", intensity);
        auroraShader.setUniform("speed", auroraSpeed.getValue().floatValue());
        auroraShader.setUniform("cameraPos", (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        auroraShader.setUniform("resolution",
            (float) mc.getMainWindow().getFramebufferWidth(),
            (float) mc.getMainWindow().getFramebufferHeight());
        auroraShader.setUniform("color1", color1.getRed() / 255.0f, color1.getGreen() / 255.0f, color1.getBlue() / 255.0f);
        auroraShader.setUniform("color2", color2.getRed() / 255.0f, color2.getGreen() / 255.0f, color2.getBlue() / 255.0f);
        auroraShader.setUniform("color3", color3.getRed() / 255.0f, color3.getGreen() / 255.0f, color3.getBlue() / 255.0f);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        float radius = 100.0F;
        int segments = 64;
        int rings = 48;

        for (int ring = 0; ring < rings; ring++) {
            float phi1 = (float) ring / rings * (float) Math.PI;
            float phi2 = (float) (ring + 1) / rings * (float) Math.PI;

            float y1 = (float) Math.cos(phi1) * radius;
            float r1 = (float) Math.sin(phi1) * radius;
            float y2 = (float) Math.cos(phi2) * radius;
            float r2 = (float) Math.sin(phi2) * radius;

            for (int seg = 0; seg < segments; seg++) {
                float theta1 = (float) seg / segments * (float) Math.PI * 2.0f;
                float theta2 = (float) (seg + 1) / segments * (float) Math.PI * 2.0f;

                float x1_1 = (float) Math.sin(phi1) * (float) Math.cos(theta1) * radius;
                float z1_1 = (float) Math.sin(phi1) * (float) Math.sin(theta1) * radius;
                float x1_2 = (float) Math.sin(phi1) * (float) Math.cos(theta2) * radius;
                float z1_2 = (float) Math.sin(phi1) * (float) Math.sin(theta2) * radius;
                float x2_1 = (float) Math.sin(phi2) * (float) Math.cos(theta1) * radius;
                float z2_1 = (float) Math.sin(phi2) * (float) Math.sin(theta1) * radius;
                float x2_2 = (float) Math.sin(phi2) * (float) Math.cos(theta2) * radius;
                float z2_2 = (float) Math.sin(phi2) * (float) Math.sin(theta2) * radius;

                float u1 = (float) seg / segments;
                float u2 = (float) (seg + 1) / segments;
                float v1 = (float) ring / rings;
                float v2 = (float) (ring + 1) / rings;

                buffer.pos(matrix, x1_1, y1, z1_1).tex(u1, v1).endVertex();
                buffer.pos(matrix, x1_2, y1, z1_2).tex(u2, v1).endVertex();
                buffer.pos(matrix, x2_2, y2, z2_2).tex(u2, v2).endVertex();
                buffer.pos(matrix, x2_1, y2, z2_1).tex(u1, v2).endVertex();
            }
        }

        buffer.finishDrawing();
        org.lwjgl.opengl.GL11.glEnable(GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        net.minecraft.client.renderer.WorldVertexBufferUploader.draw(buffer);
        org.lwjgl.opengl.GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        auroraShader.detach();

        matrixStack.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }
}
