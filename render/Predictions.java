//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.ProjectionUtils;
import nuclear.utils.render.RenderUtils;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static nuclear.ui.clickgui.Panel.getColorByName;
import static nuclear.utils.misc.HudUtil.drawItemStack;
import static nuclear.utils.render.ColorUtils.rgba;

@Annotation(name = "Predictions", type = TypeList.Render, desc = "Показывает траекторию падения снарядов")
public class Predictions extends Module {

    public MultiBoxSetting projectiles = new MultiBoxSetting("Предсказывать для",
            new BooleanSetting("Эндер Жемчуг", true),
            new BooleanSetting("Стрела", false),
            new BooleanSetting("Трезубец", false),
            new BooleanSetting("Предметы", true),
            new BooleanSetting("Снежок", false));

    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0");

    private static final ItemStack ENDER_PEARL_STACK = new ItemStack(Items.ENDER_PEARL);
    private static final ItemStack ARROW_STACK = new ItemStack(Items.ARROW);
    private static final ItemStack TRIDENT_STACK = new ItemStack(Items.TRIDENT);
    private static final ItemStack SNOWBALL_STACK = new ItemStack(Items.SNOWBALL);

    private final List<Entity> projectileCache = new ArrayList<>();

    private static class TrajectoryData {
        Entity entity;
        Vector3d finalPosition;
        int steps;
    }
    private final List<TrajectoryData> trajectoryCache = new ArrayList<>();
    private final MatrixStack reusableMatrix = new MatrixStack();

    public Predictions() {
        addSettings(projectiles);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render) {
            if (render.isRender3D()) {
                collectProjectiles();
                renderTrajectories3D(render);
            }
            if (render.isRender2D()) {
                renderTrajectories2D(render.matrixStack);
            }
        }
        return false;
    }

    private void collectProjectiles() {
        projectileCache.clear();
        trajectoryCache.clear();

        int itemCount = 0;

        for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class,
                mc.player.getBoundingBox().grow(128),
                e -> e instanceof EnderPearlEntity || e instanceof ArrowEntity ||
                        e instanceof ItemEntity || e instanceof TridentEntity ||
                        e instanceof SnowballEntity)) {

            if (entity instanceof ItemEntity) {
                if (entity.isInWater() || entity.isInLava() || entity.isOnGround()) {
                    continue;
                }
                if (!projectiles.get("Предметы")) continue;
                itemCount++;
                if (itemCount > 33) continue;
            } else if (entity instanceof EnderPearlEntity && !projectiles.get("Эндер Жемчуг")) {
                continue;
            } else if (entity instanceof ArrowEntity && !projectiles.get("Стрела")) {
                continue;
            } else if (entity instanceof TridentEntity && !projectiles.get("Трезубец")) {
                continue;
            } else if (entity instanceof SnowballEntity && !projectiles.get("Снежок")) {
                continue;
            }

            if (entity.prevPosY == entity.getPosY() && entity.prevPosX == entity.getPosX() && entity.prevPosZ == entity.getPosZ()) {
                continue;
            }

            projectileCache.add(entity);
        }
    }

    private void renderTrajectories2D(MatrixStack e) {
        if (trajectoryCache.isEmpty()) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int i = 0, size = trajectoryCache.size(); i < size; i++) {
            TrajectoryData data = trajectoryCache.get(i);
            Entity entity = data.entity;

            Vector2d finalProjectedPosition = ProjectionUtils.project(
                    data.finalPosition.x, data.finalPosition.y, data.finalPosition.z);

            if (finalProjectedPosition == null ||
                    finalProjectedPosition.x == Float.MAX_VALUE ||
                    finalProjectedPosition.y == Float.MAX_VALUE) {
                continue;
            }

            float timeInSeconds = data.steps * 0.05f;
            float x = (float) finalProjectedPosition.x;
            float y = (float) finalProjectedPosition.y + 3;

            String timeText = TIME_FORMAT.format(timeInSeconds) + " сек";
            float timeWidth = Fonts.blod[11].getWidth(timeText);

            if (entity instanceof ItemEntity itemEntity) {
                RenderUtils.Render2D.drawRoundedRect(x - 12.5f, y - 2, timeWidth + 5f, 8.5f, 0, rgba(0, 0, 0, 128));
                Fonts.blod[11].drawString(e, timeText, x - (timeWidth / 2), y + 1.5f, -1);

                ITextComponent displayName = itemEntity.getItem().getDisplayName();
                String itemName = displayName.getString();
                float itemNameWidth = Fonts.blod[11].getWidth(itemName);
                RenderUtils.Render2D.drawRoundedRect(x - (itemNameWidth / 2) - 2.5f, y - 2 + 8f, itemNameWidth + 5f, 8.5f, 0, rgba(0, 0, 0, 128));
                Fonts.blod[11].drawText(e, displayName, x - (itemNameWidth / 2), y + 1.5f + 8f);
            } else {
                RenderUtils.Render2D.drawRoundedRect(x - 17f, y - 2, timeWidth + 12.5f, 8.5f, 0, rgba(0, 0, 0, 128));
                drawItemStack(getProjectileItemStack(entity), x - 16f, y - 1.5f, 0.45f, null, true);
                Fonts.blod[11].drawString(e, timeText, x - (timeWidth / 2) + 3.5f, y + 1.5f, -1);
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private ItemStack getProjectileItemStack(Entity entity) {
        if (entity instanceof EnderPearlEntity) {
            return ENDER_PEARL_STACK;
        } else if (entity instanceof ArrowEntity) {
            return ARROW_STACK;
        } else if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem();
        } else if (entity instanceof TridentEntity) {
            return TRIDENT_STACK;
        } else if (entity instanceof SnowballEntity) {
            return SNOWBALL_STACK;
        }
        return ItemStack.EMPTY;
    }

    private void renderTrajectories3D(EventRender e) {
        if (projectileCache.isEmpty()) return;

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(reusableMatrix.getLast().getMatrix());
        RenderSystem.translated(-mc.getRenderManager().renderPosX(), -mc.getRenderManager().renderPosY(), -mc.getRenderManager().renderPosZ());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.6F);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        BUFFER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0, size = projectileCache.size(); i < size; i++) {
            Entity entity = projectileCache.get(i);
            renderLine(entity);
        }

        TESSELLATOR.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.translated(mc.getRenderManager().renderPosX(), mc.getRenderManager().renderPosY(), mc.getRenderManager().renderPosZ());
        RenderSystem.popMatrix();
    }

    private void renderLine(Entity entity) {
        Vector3d entityPosition = entity.getPositionVec();
        Vector3d entityMotion = entity.getMotion();
        Vector3d lastPosition = entityPosition;
        Vector3d finalPos = entityPosition;
        int steps = 0;

        int base = getColorByName("primaryColor");
        int shade = ColorUtils.darken(base, 0.5f);
        final int fadeSteps = 4;

        boolean isTrident = entity instanceof TridentEntity;
        boolean isEnderPearl = entity instanceof EnderPearlEntity;
        boolean isSnowball = entity instanceof SnowballEntity;
        boolean isItem = entity instanceof ItemEntity;
        double gravityValue = (isEnderPearl || isSnowball) ? 0.03 : (isItem ? 0.04 : 0.05);
        float waterScale = (isEnderPearl || isItem || isSnowball) ? 0.8f : 0.6f;
        boolean hasGravity = !entity.hasNoGravity();

        for (int i = 0; i <= 300; i++) {
            steps++;
            lastPosition = entityPosition;
            entityPosition = entityPosition.add(entityMotion);

            boolean inWater = entity.isInWater() || mc.world.getBlockState(new BlockPos(lastPosition)).getBlock() == Blocks.WATER;
            double scale = (inWater && !isTrident) ? waterScale : 0.99;

            double newMotionX = entityMotion.x * scale;
            double newMotionY = entityMotion.y * scale - (hasGravity ? gravityValue : 0);
            double newMotionZ = entityMotion.z * scale;
            entityMotion = new Vector3d(newMotionX, newMotionY, newMotionZ);

            BlockRayTraceResult blockHitResult = mc.world.rayTraceBlocks(
                    new RayTraceContext(lastPosition, entityPosition,
                            RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));

            if (blockHitResult.getType() == RayTraceResult.Type.BLOCK || entityPosition.y <= 0) {
                finalPos = lastPosition;
                break;
            }

            finalPos = entityPosition;

            int gradientColor = ColorUtils.gradient(3, i * 8, base, shade);

            int alpha = i < fadeSteps ? (int) ((i / (float) fadeSteps) * 200) : 200;

            int color = (alpha << 24) | (gradientColor & 0x00FFFFFF);

            BUFFER.pos(lastPosition.x, lastPosition.y, lastPosition.z).color(color).endVertex();
            BUFFER.pos(entityPosition.x, entityPosition.y, entityPosition.z).color(color).endVertex();
        }

        TrajectoryData data = new TrajectoryData();
        data.entity = entity;
        data.finalPosition = finalPos;
        data.steps = steps;
        trajectoryCache.add(data);
    }
}