package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.optifine.util.TextureUtils;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.*;
import nuclear.utils.math.MathUtil;
import nuclear.utils.math.TargetUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.ProjectUtil;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.shader.CustomFramebuffer;
import nuclear.utils.render.shader.ShaderUtil;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nuclear.ui.clickgui.Panel.getColorByName;
import static org.lwjgl.opengl.GL11.*;

@Annotation(name = "Esp", type = TypeList.Render, desc = "Отображает ентити или подсвечивает")
public class Esp extends Module {
    public MultiBoxSetting targets = new MultiBoxSetting("Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Монстров", false),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Животных", false),
            new BooleanSetting("Себя", true),
            new BooleanSetting("Жителей", false),
            new BooleanSetting("Голых", true),
            new BooleanSetting("Предметы", false)
    );

    public InfoSetting box = new InfoSetting("Бокс есп", () -> {});
    private final ModeSetting mode = new ModeSetting("Режим", "Отключено", "Бокс", "Квадрат", "Углы", "Отключено");

    private final BooleanSetting fill = new BooleanSetting("Заливка", true).setVisible(() -> mode.is("Бокс"));
    private final SliderSetting widthline = new SliderSetting("Размер линий", 1.2f, 0.5f, 3.0f, 0.1f).setVisible(() -> mode.is("Бокс"));

    private final ModeSetting color = new ModeSetting("Режим цвета", "Клиентский", "Клиентский", "Статичный").setVisible(() -> !mode.is("Отключено"));
    private final ColorSetting friend_color = new ColorSetting("Цвет друзей", ColorUtils.getColor(0, 255, 0, 255))
            .setVisible(() -> targets.get("Друзей") && !mode.is("Отключено"));
    private final ColorSetting box_color = new ColorSetting("Цвет", -1).setVisible(() -> color.is("Статичный"));

    public InfoSetting health = new InfoSetting("Здоровье", () -> {});
    private final ModeSetting hp = new ModeSetting("Бар здоровья", "Отключен", "Состояние здоровья", "Индивидуальный", "Клиентский", "Отключен");
    private final ColorSetting up = new ColorSetting("Верхний цвет", ColorUtils.getColor(0, 255, 0, 255)).setVisible(() -> hp.is("Индивидуальный"));
    private final ColorSetting down = new ColorSetting("Нижний цвет", ColorUtils.getColor(255, 0, 0, 255)).setVisible(() -> hp.is("Индивидуальный"));
    public InfoSetting other = new InfoSetting("Прочее", () -> {});
    private final BooleanSetting eat = new BooleanSetting("Кд поедание", true);

    private final Map<LivingEntity, EatingData> eatingEntities = new ConcurrentHashMap<>();

    private static class EatingData {
        final ItemStack itemStack;
        final int maxDuration;
        final long startTime;

        EatingData(ItemStack itemStack, int maxDuration, long startTime) {
            this.itemStack = itemStack;
            this.maxDuration = maxDuration;
            this.startTime = startTime;
        }
    }

    public Esp() {
        addSettings(targets, box, mode, fill, widthline, color, friend_color, box_color, health, hp, up, down, other, eat);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render) {
            if (render.isRender3D()) {
                onRender3D(render);
            } else if (render.isRender2D()) {
                onRender2D(render);
            }
        }
        return false;
    }

    public void onRender3D(EventRender e) {
        if (eat.get()) {
            updateEatingEntities();
        }

        if (mode.is("Бокс")) {
            render3DBoxes(e.matrixStack, e.getPartialTicks());
        }
    }

    public void onRender2D(EventRender e) {
        renderHpBars(e);
        render2DBoxes(e);

        if (eat.get()) {
            renderEatingProgress(e);
        }
    }

    private void render3DBoxes(MatrixStack matrixStack, float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        matrixStack.push();

        for (Entity entity : mc.world.getAllEntities()) {
            if (!entity.isAlive() || !ProjectUtil.isInView(entity) || isTarget(entity)) continue;

            if (entity instanceof PlayerEntity player) {
                if (!player.botEntity) continue;
                if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON && player == mc.player) continue;
            }

            Vector3d interp = MathUtil.interpolate(entity, partialTicks);
            Vector3d proj = interp.subtract(mc.getRenderManager().info.getProjectedView());

            double height = entity.getHeight();
            double width = entity.getWidth() * 0.5;

            AxisAlignedBB bb = new AxisAlignedBB(
                    proj.x - width, proj.y, proj.z - width,
                    proj.x + width, proj.y + height, proj.z + width
            );

            int colorInt = resolveBoxColor(entity);
            float r = ((colorInt >> 16) & 255) / 255.0f;
            float g = ((colorInt >> 8) & 255) / 255.0f;
            float b = (colorInt & 255) / 255.0f;

            if (fill.get()) {
                RenderSystem.color4f(r, g, b, 0.25f);
                buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION);
                drawFilledBox(buffer, bb);
                tessellator.draw();
            }

            glLineWidth(widthline.getValue().floatValue());
            RenderSystem.color4f(r, g, b, 1.0f);
            buffer.begin(GL_LINES, DefaultVertexFormats.POSITION);
            drawOutlineBox(buffer, bb);
            tessellator.draw();
        }

        matrixStack.pop();

        glDisable(GL_LINE_SMOOTH);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void drawFilledBox(BufferBuilder buffer, AxisAlignedBB bb) {
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
    }

    private void drawOutlineBox(BufferBuilder buffer, AxisAlignedBB bb) {
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
    }

    private void render2DBoxes(EventRender event) {
        if (!(mode.is("Квадрат") || mode.is("Углы") || mode.is("Бокс"))) return;
        if (mc.world == null || mc.player == null) return;

        for (Entity base : mc.world.getAllEntities()) {
            if (!base.isAlive()) continue;
            if (!ProjectUtil.isInView(base)) continue;
            if (isTarget(base)) continue;

            if (base instanceof PlayerEntity player) {
                if (!player.botEntity) continue;
                if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON && player == mc.player) continue;
            }

            Vector3d interpolated = MathUtil.interpolate(base, mc.getRenderPartialTicks());
            AxisAlignedBB aabb = ProjectUtil.getEntityBox(base, interpolated);

            double extra = 0.2 - (base.isSneaking() && !mc.player.abilities.isFlying ? 0.1 : 0.0);
            AxisAlignedBB adjusted = new AxisAlignedBB(
                    aabb.minX, aabb.minY, aabb.minZ,
                    aabb.maxX, aabb.maxY - extra, aabb.maxZ
            );

            Vector4f b = computeScreenBounds(adjusted);
            if (b == null) continue;

            float minX = b.getX();
            float minY = b.getY();
            float maxX = b.getZ();
            float maxY = b.getW();

            if (maxX <= minX || maxY <= minY) continue;

            int colorInt = resolveBoxColor(base);

            if (mode.is("Квадрат")) {
                drawConnectedBox(event.matrixStack, minX, minY, maxX, maxY, colorInt);
            } else if (mode.is("Углы")) {
                drawCornerBox(event.matrixStack, minX, minY, maxX, maxY, colorInt);
            }
        }
    }

    private int resolveBoxColor(Entity entity) {
        int firstColor2 = getColorByName("primaryColor");
        if (entity instanceof PlayerEntity p) {
            boolean isFriend = Manager.FRIEND_MANAGER.isFriend(entity.getName().getString());
            if (isFriend && targets.get("Друзей")) {
                return friend_color.get();
            }
        }

        if (color.is("Статичный")) {
            return box_color.get();
        }

        return firstColor2;
    }

    private void drawConnectedBox(MatrixStack matrixStack, float x, float y, float right, float bottom, int colorInt) {
        float outlineThickness = 0.5F;
        int firstColor = ColorUtils.gradient(5, 0, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int secondColor = ColorUtils.gradient(5, 90, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int thirdColor = ColorUtils.gradient(5, 180, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int fourthColor = ColorUtils.gradient(5, 270, colorInt, ColorUtils.darken(colorInt, 0.5F));

        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, y, 0.5f, bottom - y, secondColor, firstColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, bottom, right - x, 0.5f, fourthColor, firstColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, right, y, 0.5f, bottom - y + 0.5f, thirdColor, fourthColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, y, right - x, 0.5f, thirdColor, secondColor);

        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - 0.5f, y - outlineThickness, right - x + 1.5F, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - outlineThickness, y, outlineThickness, bottom - y + 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - 0.5f, bottom + 0.5f, right - x + 1.5F, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right + 0.5f, y, outlineThickness, bottom - y + 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, y + 0.5f, right - x - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, y + 0.5f, outlineThickness, bottom - y - 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, bottom - outlineThickness, right - x - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - outlineThickness, y + 0.5f, outlineThickness, bottom - y - 0.5f, ColorUtils.getColor(0, 0, 0, 255));
    }

    private void drawCornerBox(MatrixStack matrixStack, float x, float y, float right, float bottom, int colorInt) {
        float cornerLength = Math.min((right - x) * 0.25f, (bottom - y) * 0.25f);
        float lineThickness = 0.5F;
        float outlineThickness = 0.5F;
        int firstColor = ColorUtils.gradient(5, 0, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int secondColor = ColorUtils.gradient(5, 90, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int thirdColor = ColorUtils.gradient(5, 180, colorInt, ColorUtils.darken(colorInt, 0.5F));
        int fourthColor = ColorUtils.gradient(5, 270, colorInt, ColorUtils.darken(colorInt, 0.5F));

        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, y, cornerLength, lineThickness, thirdColor, secondColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, y, lineThickness, cornerLength, secondColor, firstColor);

        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, right - cornerLength + 0.5f, y, cornerLength - 0.5f, lineThickness, thirdColor, secondColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, right, y, lineThickness, cornerLength, thirdColor, fourthColor);

        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, bottom, cornerLength, lineThickness, fourthColor, firstColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, x, bottom - cornerLength + 0.5f, lineThickness, cornerLength - 0.5f, secondColor, firstColor);

        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, right - cornerLength + lineThickness, bottom, cornerLength, lineThickness, fourthColor, firstColor);
        RenderUtils.Render2D.drawMinecraftGradientRectangle(matrixStack, right, bottom - cornerLength + 0.5f, lineThickness, cornerLength - 0.5f, thirdColor, fourthColor);

        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - 0.5f, y - outlineThickness, cornerLength + 0.5F, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - outlineThickness, y, outlineThickness, cornerLength, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, y + 0.5f, cornerLength - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, y + 0.5f, outlineThickness, cornerLength - 0.5f, ColorUtils.getColor(0, 0, 0, 255));

        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - outlineThickness, y + 0.5f, outlineThickness, cornerLength - 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right + 0.5f, y, outlineThickness, cornerLength, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - cornerLength + 0.5f, y - outlineThickness, cornerLength + 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - cornerLength + 0.5f, y + 0.5f, cornerLength - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));

        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - outlineThickness, bottom - cornerLength + 0.5f, outlineThickness, cornerLength, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, bottom - cornerLength + 0.5f, outlineThickness, cornerLength - 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x + 0.5f, bottom - outlineThickness, cornerLength - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x - 0.5f, bottom + 0.5f, cornerLength + 0.5F, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));

        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - outlineThickness, bottom - cornerLength + 0.5f, outlineThickness, cornerLength - 0.5f, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right + 0.5f, bottom - cornerLength + 0.5f, outlineThickness, cornerLength, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - cornerLength + 0.5f, bottom - outlineThickness, cornerLength - 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
        RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, right - cornerLength + 0.5f, bottom + 0.5f, cornerLength + 0.5f, outlineThickness, ColorUtils.getColor(0, 0, 0, 255));
    }

    private void renderHpBars(EventRender event) {
        if (!hp.is("Состояние здоровья") && !hp.is("Индивидуальный") && !hp.is("Клиентский")) return;
        if (mc.world == null) return;

        for (Entity base : mc.world.getAllEntities()) {
            if (!(base instanceof LivingEntity entity)) continue;
            if (!entity.isAlive()) continue;
            if (!ProjectUtil.isInView(entity)) continue;
            if (isTarget(entity)) continue;

            if (entity instanceof PlayerEntity player) {
                if (!player.botEntity) continue;
                if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON && player == mc.player) continue;
            }

            Vector3d interpolated = MathUtil.interpolate(entity, mc.getRenderPartialTicks());
            AxisAlignedBB aabb = ProjectUtil.getEntityBox(entity, interpolated);
            double extra = 0.2 - (entity.isSneaking() && !mc.player.abilities.isFlying ? 0.1 : 0.0);
            AxisAlignedBB adjusted = new AxisAlignedBB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY - extra, aabb.maxZ);
            Vector4f bounds = computeScreenBounds(adjusted);
            if (bounds == null) continue;

            float minX = bounds.getX();
            float minY = bounds.getY();
            float maxY = bounds.getW();

            float barWidth = 0.5f;
            float barHeight = maxY - minY;
            float barX = minX - 2.0f - barWidth;

            float currentHP = Math.max(0f, entity.getHealth());
            float maxHP = Math.max(1f, entity.getMaxHealth());
            float percent = Math.min(currentHP / maxHP, 1.0f);
            float fillHeight = barHeight * percent;
            float fillY = minY + (barHeight - fillHeight);
            int firstColor2 = getColorByName("primaryColor");

            int topColor;
            int bottomColor;

            if (hp.is("Индивидуальный")) {
                topColor = up.get();
                bottomColor = down.get();
            } else if (hp.is("Клиентский")) {
                int active = firstColor2;
                int darker = ColorUtils.darken(active, 0.6f);
                topColor = active;
                bottomColor = darker;
            } else {
                int red = ColorUtils.getColor(255, 0, 0, 255);
                int green = ColorUtils.getColor(0, 255, 0, 255);
                int mixed = ColorUtils.interpolate(red, green, percent);
                topColor = mixed;
                bottomColor = ColorUtils.darken(mixed, 0.6f);
            }
            RenderUtils.Render2D.drawMinecraftRectangle(event.matrixStack, barX - 0.5f, minY - 0.5f, barWidth + 1.0f, barHeight + 1.5f, ColorUtils.getColor(0, 0, 0, 255));
            RenderUtils.Render2D.drawMinecraftGradientRectangle(event.matrixStack, barX, fillY, barWidth, fillHeight + 0.5F, topColor, bottomColor);
        }
    }

    private boolean isTarget(Entity entity) {
        return !TargetUtil.isEntityTarget(entity, targets);
    }

    private Vector4f computeScreenBounds(AxisAlignedBB aabb) {
        Vector3d[] corners = ProjectUtil.getCorners(aabb);
        Vector2f min = new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
        Vector2f max = new Vector2f(Float.MIN_VALUE, Float.MIN_VALUE);
        for (Vector3d corner : corners) {
            Vector2f projected = ProjectUtil.project2D(corner);
            if (projected == null) continue;
            if (projected.x < min.x) min.x = projected.x;
            if (projected.y < min.y) min.y = projected.y;
            if (projected.x > max.x) max.x = projected.x;
            if (projected.y > max.y) max.y = projected.y;
        }
        if (min.x == Float.MAX_VALUE) return null;
        return new Vector4f(min.x, min.y, max.x, max.y);
    }

    private void updateEatingEntities() {
        if (mc.world == null || mc.player == null) return;

        eatingEntities.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getKey();
            if (!entity.isAlive() || !entity.isHandActive()) return true;
            UseAction action = entity.getActiveItemStack().getUseAction();
            return action != UseAction.EAT && action != UseAction.DRINK;
        });

        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (!livingEntity.isAlive()) continue;
            if (!ProjectUtil.isInView(livingEntity)) continue;
            if (isTarget(livingEntity)) continue;

            if (livingEntity instanceof PlayerEntity player) {
                if (!player.botEntity) continue;
                if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON && player == mc.player) continue;
            }

            if (livingEntity.isHandActive()) {
                ItemStack activeStack = livingEntity.getActiveItemStack();
                if (!activeStack.isEmpty()) {
                    UseAction action = activeStack.getUseAction();
                    if (action == UseAction.EAT || action == UseAction.DRINK) {
                        int maxDuration = activeStack.getUseDuration();
                        if (!eatingEntities.containsKey(livingEntity)) {
                            eatingEntities.put(livingEntity, new EatingData(activeStack.copy(), maxDuration, System.currentTimeMillis()));
                        } else {
                            EatingData existing = eatingEntities.get(livingEntity);
                            if (!existing.itemStack.getItem().equals(activeStack.getItem())) {
                                eatingEntities.put(livingEntity, new EatingData(activeStack.copy(), maxDuration, System.currentTimeMillis()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderEatingProgress(EventRender event) {
        if (mc.world == null || mc.player == null) return;

        int primaryColor = getColorByName("primaryColor");

        for (Map.Entry<LivingEntity, EatingData> entry : eatingEntities.entrySet()) {
            LivingEntity entity = entry.getKey();

            if (!entity.isAlive() || !entity.isHandActive()) continue;
            if (!ProjectUtil.isInView(entity)) continue;

            Vector3d interpolated = MathUtil.interpolate(entity, event.getPartialTicks());
            Vector3d centerPos = new Vector3d(interpolated.x, interpolated.y + entity.getHeight() / 2.0, interpolated.z);
            Vector2f screenPos = ProjectUtil.project2D(centerPos);

            if (screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) continue;

            ItemStack currentStack = entity.getActiveItemStack();
            if (currentStack.isEmpty()) continue;

            UseAction action = currentStack.getUseAction();
            if (action != UseAction.EAT && action != UseAction.DRINK) continue;

            int currentUseCount = entity.getItemInUseCount();
            int maxDuration = currentStack.getUseDuration();
            if (maxDuration <= 0) continue;

            float progress = 1.0f - ((float) currentUseCount / (float) maxDuration);
            progress = Math.max(0.0f, Math.min(1.0f, progress));

            try {
                float centerX = screenPos.x;
                float centerY = screenPos.y;

                float bgSize = 20.0f;
                float circleRadius = 9.0f;
                float itemScale = 0.85f;
                float itemSize = 16.0f * itemScale;

                float bgX = centerX - bgSize / 2.0f;
                float bgY = centerY - bgSize / 2.0f;

                RenderUtils.Render2D.drawRoundedRect(
                        (int) Math.round(bgX),
                        (int) Math.round(bgY),
                        (int) Math.round(bgSize),
                        (int) Math.round(bgSize),
                        10f,
                        new Color(6, 6, 6, 210).getRGB()
                );

                RenderUtils.Render2D.drawCircle(centerX, centerY, 0, 360, circleRadius, 2.2f, false, new Color(20, 20, 20, 255).getRGB());

                float angle = 360f * progress;
                RenderUtils.Render2D.drawCircle(centerX, centerY, 0, angle, circleRadius, 2.2f, false, primaryColor);

                float itemX = centerX - itemSize / 2.0f - 1f;
                float itemY = centerY - itemSize / 2.0f - 1.2f; // лёгкий сдвиг вверх

                GL11.glPushMatrix();
                GL11.glTranslatef(centerX, centerY, 0);
                GL11.glScalef(itemScale, itemScale, itemScale);
                GL11.glTranslatef(-centerX, -centerY, 0);

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

                mc.getItemRenderer().renderItemAndEffectIntoGUI(
                        currentStack,
                        (int) Math.round(itemX),
                        (int) Math.round(itemY)
                );

                RenderSystem.disableDepthTest();
                RenderSystem.disableBlend();

                GL11.glPopMatrix();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}