package nuclear.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.render.ColorUtil;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static nuclear.module.TypeList.Render;
import static nuclear.ui.clickgui.Panel.getColorByName;
import static nuclear.utils.render.ColorUtils.rgba;

@Annotation(name = "Trajectories", type = Render, desc = "Предикт выстрела/броска предмета в руке")
public class Trajectories extends Module {

    private final MultiBoxSetting projectileSettings = new MultiBoxSetting("Траектории",
            new BooleanSetting("Стрела", true),
            new BooleanSetting("Эндер Пёрл", true),
            new BooleanSetting("Снежок", true),
            new BooleanSetting("Зелья", true),
            new BooleanSetting("Трезубец", true)
    );

    private final List<LandingInfo> landings = new ArrayList<>();
    private final BufferBuilder buffer = Tessellator.getInstance().getBuffer();

    private static class LandingInfo {
        Vector3d impact;
        float time;
        ItemStack stack;
        Entity hitEntity;
        Direction face;

        LandingInfo(Vector3d impact, float time, ItemStack stack, Entity hitEntity, Direction face) {
            this.impact = impact;
            this.time = time;
            this.stack = stack;
            this.hitEntity = hitEntity;
            this.face = face;
        }
    }

    public Trajectories() {
        addSettings(projectileSettings);
    }

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventRender render)) return false;

        if (render.isRender3D()) {
            landings.clear();
            render3D(render.getPartialTicks());
        }

        return false;
    }

    private void render3D(float partialTicks) {
        RenderSystem.pushMatrix();
        RenderSystem.translated(
                -mc.getRenderManager().renderPosX(),
                -mc.getRenderManager().renderPosY(),
                -mc.getRenderManager().renderPosZ()
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(2.0f);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        simulateAllTrajectories(partialTicks);

        Tessellator.getInstance().draw();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (LandingInfo info : landings) {
            drawCrossWithCircle(info.impact, info.hitEntity != null, info.face);
        }
        Tessellator.getInstance().draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void simulateAllTrajectories(float partialTicks) {
        ItemStack mainHand = mc.player.getHeldItemMainhand();
        ItemStack activeStack = mc.player.getActiveItemStack();
        Item item = mainHand.getItem();

        float pitch = mc.player.prevRotationPitch + (mc.player.rotationPitch - mc.player.prevRotationPitch) * partialTicks;
        float yaw = mc.player.prevRotationYaw + (mc.player.rotationYaw - mc.player.prevRotationYaw) * partialTicks;

        double eyeX = mc.player.prevPosX + (mc.player.getPosX() - mc.player.prevPosX) * partialTicks;
        double eyeY = mc.player.prevPosY + (mc.player.getPosY() - mc.player.prevPosY) * partialTicks + mc.player.getEyeHeight();
        double eyeZ = mc.player.prevPosZ + (mc.player.getPosZ() - mc.player.prevPosZ) * partialTicks;
        Vector3d eye = new Vector3d(eyeX, eyeY, eyeZ);

        if (item instanceof CrossbowItem && projectileSettings.get("Стрела") && CrossbowItem.isCharged(mainHand)) {
            simulate(eye, computeCrossbowVelocity(yaw, pitch, 0f), new ItemStack(Items.ARROW));
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.MULTISHOT, mainHand) > 0) {
                simulate(eye, computeCrossbowVelocity(yaw, pitch, -10f), new ItemStack(Items.ARROW));
                simulate(eye, computeCrossbowVelocity(yaw, pitch, 10f), new ItemStack(Items.ARROW));
            }
        }

        if (mc.player.isHandActive() && activeStack.getItem() instanceof BowItem && projectileSettings.get("Стрела")) {
            float drawTime = activeStack.getUseDuration() - mc.player.getItemInUseCount();
            float velocity = BowItem.getArrowVelocity((int) drawTime) * 3.0f;
            if (velocity > 0.1f) {
                simulate(eye, computeThrowVelocity(yaw, pitch, velocity), new ItemStack(Items.ARROW));
            }
        }

        if (item == Items.SNOWBALL && projectileSettings.get("Снежок")) {
            simulate(eye, computeThrowVelocity(yaw, pitch, 1.5f), new ItemStack(Items.SNOWBALL));
        }

        if ((item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) && projectileSettings.get("Зелья")) {
            simulate(eye, computeThrowVelocity(yaw, pitch - 20f, 0.5f), mainHand.copy());
        }

        if (item == Items.ENDER_PEARL && projectileSettings.get("Эндер Пёрл")) {
            simulate(eye, computeThrowVelocity(yaw, pitch, 1.5f), new ItemStack(Items.ENDER_PEARL));
        }

        if (mc.player.isHandActive() && activeStack.getItem() instanceof TridentItem && projectileSettings.get("Трезубец")) {
            if (activeStack.getUseDuration() - mc.player.getItemInUseCount() >= 10) {
                simulate(eye, computeThrowVelocity(yaw, pitch, 2.5f), new ItemStack(Items.TRIDENT));
            }
        }
    }

    private Vector3d computeThrowVelocity(float yaw, float pitch, float velocity) {
        float yawRad = yaw * 0.017453292f;
        float pitchRad = pitch * 0.017453292f;

        float xDir = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        float yDir = -MathHelper.sin(pitchRad);
        float zDir = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        Vector3d dir = new Vector3d(xDir, yDir, zDir).normalize().scale(velocity);
        Vector3d motion = mc.player.getMotion();
        return dir.add(-motion.x, mc.player.isOnGround() ? 0 : 0, -motion.z);
    }

    private Vector3d computeCrossbowVelocity(float yaw, float pitch, float angleDeg) {
        float yawRad = yaw * 0.017453292f;
        float pitchRad = pitch * 0.017453292f;

        Vector3d look = new Vector3d(
                -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad),
                -MathHelper.sin(pitchRad),
                MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)
        );

        Vector3d up = new Vector3d(0, 1, 0);
        Vector3f axis = new Vector3f((float) up.x, (float) up.y, (float) up.z);
        axis.normalize();

        Quaternion rotation = new Quaternion(axis, angleDeg, true);
        Vector3f direction = new Vector3f((float) look.x, (float) look.y, (float) look.z);
        direction.normalize();
        direction.transform(rotation);

        Vector3d dir = new Vector3d(direction.getX(), direction.getY(), direction.getZ()).normalize().scale(3.15f);
        Vector3d motion = mc.player.getMotion();
        return dir.add(-motion.x, 0, -motion.z);
    }

    private void simulate(Vector3d startPosition, Vector3d startVelocity, ItemStack projectile) {
        Item item = projectile.getItem();
        float gravity = 0.05f;
        float airDrag = 0.99f;
        float waterDrag = 0.6f;
        int maxSteps = 300;

        if (item instanceof TridentItem) {
            waterDrag = 0.99f;
            maxSteps = 350;
        } else if (item == Items.SNOWBALL || item == Items.ENDER_PEARL) {
            gravity = 0.03f;
            waterDrag = 0.8f;
        } else if (item instanceof PotionItem) {
            gravity = 0.05f;
            waterDrag = 0.8f;
        }

        Vector3d position = startPosition;
        Vector3d velocity = startVelocity;

        int baseColor = getColorByName("primaryColor");
        int base = baseColor;
        int shade = ColorUtil.darken(base, 0.5f);
        final int fadeSteps = 6;

        boolean willHitEntity = false;
        Entity finalHitEntity = null;

        for (int step = 0; step < maxSteps; step++) {
            Vector3d previous = position;
            position = position.add(velocity);
            float time = step * 0.05f;

            BlockPos blockPos = new BlockPos(previous);
            boolean water = mc.world.getFluidState(blockPos).isTagged(FluidTags.WATER)
                    || mc.world.getBlockState(blockPos).getBlock() == Blocks.WATER;

            float drag = water ? waterDrag : airDrag;
            velocity = velocity.scale(drag).add(0, -gravity, 0);

            RayTraceContext context = new RayTraceContext(
                    previous, position,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    mc.player
            );
            BlockRayTraceResult blockResult = mc.world.rayTraceBlocks(context);

            AxisAlignedBB sweep = new AxisAlignedBB(
                    Math.min(previous.x, position.x) - 1,
                    Math.min(previous.y, position.y) - 1,
                    Math.min(previous.z, position.z) - 1,
                    Math.max(previous.x, position.x) + 1,
                    Math.max(previous.y, position.y) + 1,
                    Math.max(previous.z, position.z) + 1
            );
            List<Entity> hits = mc.world.getEntitiesWithinAABBExcludingEntity(mc.player, sweep);

            Entity entityHit = null;
            Vector3d entityPoint = null;
            double entityDistance = Double.MAX_VALUE;

            for (Entity entity : hits) {
                if (!entity.canBeCollidedWith()) continue;
                AxisAlignedBB entityBox = entity.getBoundingBox().grow(0.3);
                Vector3d impact = entityBox.rayTrace(previous, position).orElse(null);
                if (impact == null) continue;
                double distance = previous.distanceTo(impact);
                if (distance < entityDistance) {
                    entityDistance = distance;
                    entityHit = entity;
                    entityPoint = impact;
                }
            }

            double blockDistance = Double.MAX_VALUE;
            Vector3d blockPoint = null;
            Direction blockFace = null;
            if (blockResult.getType() == RayTraceResult.Type.BLOCK) {
                blockPoint = blockResult.getHitVec();
                blockDistance = previous.distanceTo(blockPoint);
                blockFace = ((BlockRayTraceResult) blockResult).getFace();
            }

            Vector3d finalPoint = null;
            double minDist = Double.MAX_VALUE;
            Entity hitEntityResult = null;
            Direction finalFace = null;

            if (entityHit != null && entityDistance < minDist) {
                minDist = entityDistance;
                finalPoint = entityPoint;
                hitEntityResult = entityHit;
                willHitEntity = true;
                finalHitEntity = entityHit;
                Vector3d dir = entityPoint.subtract(previous).normalize();
                double dotX = Math.abs(dir.x);
                double dotY = Math.abs(dir.y);
                double dotZ = Math.abs(dir.z);
                if (dotX > dotY && dotX > dotZ) {
                    finalFace = dir.x > 0 ? Direction.EAST : Direction.WEST;
                } else if (dotY > dotZ) {
                    finalFace = dir.y > 0 ? Direction.UP : Direction.DOWN;
                } else {
                    finalFace = dir.z > 0 ? Direction.SOUTH : Direction.NORTH;
                }
            }

            if (blockPoint != null && blockDistance < minDist) {
                minDist = blockDistance;
                finalPoint = blockPoint;
                hitEntityResult = null;
                finalFace = blockFace;
            }

            if (position.y <= 0) {
                double t = Math.max(0, Math.min(1, -previous.y / (position.y - previous.y)));
                Vector3d groundImpact = previous.add((position.subtract(previous)).scale(t));
                double groundDist = previous.distanceTo(groundImpact);
                if (groundDist < minDist) {
                    finalPoint = groundImpact;
                    hitEntityResult = null;
                    finalFace = Direction.UP;
                }
            }

            int red, green, blue, alpha;
            if (willHitEntity || hitEntityResult != null) {
                red = 255;
                green = 50;
                blue = 50;
                alpha = 100;
            } else {
                int gradientColor = ColorUtil.gradient(3, step * 8, base, shade);
                red = (gradientColor >> 16) & 0xFF;
                green = (gradientColor >> 8) & 0xFF;
                blue = gradientColor & 0xFF;
                alpha = 200;
                if (step < fadeSteps) {
                    alpha = (int) ((step / (float) fadeSteps) * 200);
                }
            }
            int color = (alpha << 24) | (red << 16) | (green << 8) | blue;

            if (finalPoint != null) {
                buffer.pos(previous.x, previous.y, previous.z).color(color).endVertex();
                buffer.pos(finalPoint.x, finalPoint.y, finalPoint.z).color(color).endVertex();
                landings.add(new LandingInfo(finalPoint, time, projectile, finalHitEntity != null ? finalHitEntity : hitEntityResult, finalFace != null ? finalFace : Direction.UP));
                break;
            }

            buffer.pos(previous.x, previous.y, previous.z).color(color).endVertex();
            buffer.pos(position.x, position.y, position.z).color(color).endVertex();
        }
    }

    private void drawCrossWithCircle(Vector3d center, boolean hitEntity, Direction face) {
        float size = 0.2f;
        float circleRadius = 0.4f;
        int segments = 32;

        int color;
        if (hitEntity) {
            color = rgba(255, 50, 50, 120);
        } else {
            int baseColor = getColorByName("primaryColor");
            color = baseColor;
        }

        Vector3d right, up;
        if (face == Direction.UP || face == Direction.DOWN) {
            right = new Vector3d(1, 0, 0);
            up = new Vector3d(0, 0, 1);
        } else if (face == Direction.NORTH || face == Direction.SOUTH) {
            right = new Vector3d(1, 0, 0);
            up = new Vector3d(0, 1, 0);
        } else {
            right = new Vector3d(0, 0, 1);
            up = new Vector3d(0, 1, 0);
        }

        right = right.normalize();
        up = up.normalize();

        Vector3d p1 = center.add(right.scale(-size));
        Vector3d p2 = center.add(right.scale(size));
        buffer.pos(p1.x, p1.y, p1.z).color(color).endVertex();
        buffer.pos(p2.x, p2.y, p2.z).color(color).endVertex();

        Vector3d p3 = center.add(up.scale(-size));
        Vector3d p4 = center.add(up.scale(size));
        buffer.pos(p3.x, p3.y, p3.z).color(color).endVertex();
        buffer.pos(p4.x, p4.y, p4.z).color(color).endVertex();

        double step = 2 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * step;
            double angle2 = (i + 1) * step;

            Vector3d offset1 = right.scale(Math.cos(angle1) * circleRadius).add(up.scale(Math.sin(angle1) * circleRadius));
            Vector3d offset2 = right.scale(Math.cos(angle2) * circleRadius).add(up.scale(Math.sin(angle2) * circleRadius));

            Vector3d point1 = center.add(offset1);
            Vector3d point2 = center.add(offset2);

            buffer.pos(point1.x, point1.y, point1.z).color(color).endVertex();
            buffer.pos(point2.x, point2.y, point2.z).color(color).endVertex();
        }
    }


}
