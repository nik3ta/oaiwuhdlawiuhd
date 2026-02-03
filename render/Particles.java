package nuclear.module.impl.render;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.*;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.math.MathUtil;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.render.RenderUtils;

import java.util.ArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "Particles", type = TypeList.Render)
public class Particles extends Module {
    private final ModeSetting type = new ModeSetting("Режим", "Звездочки", "Сердечки", "Крестики", "Молнии", "Линии", "Сияние", "Тыковки", "Снежинки", "Взрыв");
    public final MultiBoxSetting reason = new MultiBoxSetting("Добавлять при",
            new BooleanSetting("Бездействии", false),
            new BooleanSetting("Беге", false),
            new BooleanSetting("Ударе", true),
            new BooleanSetting("Падении перла", true),
            new BooleanSetting("Падении трезубца", true),
            new BooleanSetting("Падении стрелы", true),
            new BooleanSetting("Сносе тотема", true)
    );
    private final SliderSetting count = new SliderSetting("Количество", 10, 2, 40, 1).setVisible(() -> reason.get("Бездействии"));

    private final ArrayList<Particle> particles = new ArrayList<>();

    public Particles() {
        addSettings(type, reason, count);
    }

    private static String getTexturePath(String displayName) {
        return switch (displayName) {
            case "Сердечки" -> "heart.png";
            case "Крестики" -> "cross.png";
            case "Молнии" -> "lightning.png";
            case "Линии" -> "line.png";
            case "Сияние" -> "firefly.png";
            case "Тыковки" -> "pumpkin.png";
            case "Снежинки" -> "snow.png";
            case "Взрыв" -> "boom.png";
            default -> "glowstar.png";
        };
    }

    private boolean isPositionInBlock(Vector3d position) {
        BlockPos blockPos = new BlockPos(position.x, position.y, position.z);
        if (mc.world.getBlockState(blockPos).isSolid()) {
            return true;
        }
        RayTraceContext context = new RayTraceContext(
                new Vector3d(mc.player.getPosX(), mc.player.getPosY() + mc.player.getEyeHeight(), mc.player.getPosZ()),
                position,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );
        BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
        return result.getType() == RayTraceResult.Type.BLOCK;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender) {
            particles.removeIf(particle -> particle.time.hasTimeElapsed(particle.lifeTime));
            if (particles.isEmpty()) return false;

            if (mc.getRenderManager() == null || mc.getRenderManager().info == null) {
                return false;
            }

            Quaternion rotation = new Quaternion(Vector3f.YP, -mc.getRenderManager().info.getYaw(), true);
            rotation.multiply(new Quaternion(Vector3f.XP, mc.getRenderManager().info.getPitch(), true));
            rotation.multiply(new Quaternion(Vector3f.ZP, 180f, true));

            Vector3f right3f = new Vector3f(1f, 0f, 0f), up3f = new Vector3f(0f, 1f, 0f), forward3f = new Vector3f(0f, 0f, 1f);
            right3f.transform(rotation);
            up3f.transform(rotation);
            forward3f.transform(rotation);

            ResourceLocation texture = new ResourceLocation("nuclear/images/particle/" + getTexturePath(type.get()));
            Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
            Vector3d forward = new Vector3d(forward3f.getX(), forward3f.getY(), forward3f.getZ());

            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            RenderUtils.beginImage3DBatch(texture, true);
            for (Particle particle : particles) {
                particle.update();

                Vector3d toCenter = particle.position.subtract(cameraPos);
                if (forward.dotProduct(toCenter) <= 0.0) continue;

                double halfSize = particle.size * 0.5f;

                Vector3d halfRight = new Vector3d(right3f.getX(), right3f.getY(), right3f.getZ()).scale(halfSize);
                Vector3d halfUp = new Vector3d(up3f.getX(), up3f.getY(), up3f.getZ()).scale(halfSize);

                Vector3d p0 = toCenter.subtract(halfRight).subtract(halfUp);
                Vector3d p1 = toCenter.add(halfRight).subtract(halfUp);
                Vector3d p2 = toCenter.add(halfRight).add(halfUp);
                Vector3d p3 = toCenter.subtract(halfRight).add(halfUp);

                int color = (particle.color & 0x00FFFFFF) | ((int) (particle.alpha * 255) << 24);
                RenderUtils.addImage3DQuad(
                        (float) p0.x, (float) p0.y, (float) p0.z,
                        (float) p1.x, (float) p1.y, (float) p1.z,
                        (float) p2.x, (float) p2.y, (float) p2.z,
                        (float) p3.x, (float) p3.y, (float) p3.z,
                        color
                );
            }
            RenderUtils.endImage3DBatch();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        } else if (event instanceof EventMotion) {
            if (reason.get("Беге") && MoveUtil.isMoving()) {
                double speed = Math.sqrt(mc.player.motion.x * mc.player.motion.x + mc.player.motion.z * mc.player.motion.z);
                Vector3d direction;
                if (speed < 0.01) direction = mc.player.getLookVec().scale(-1);
                else if (mc.player.isElytraFlying()) direction = mc.player.motion.normalize().scale(-1);
                else direction = new Vector3d(-mc.player.motion.x / speed, 0, -mc.player.motion.z / speed);

                double distanceBehind = (mc.player.isElytraFlying() ? 1.2 : 0.5) + (speed > 0.1 ? speed * 1.5 : 0);
                double offsetX = MathUtil.random(-0.35f, 0.35f);
                double offsetZ = MathUtil.random(-0.35f, 0.35f);

                double posX = mc.player.getPosX() + direction.x * distanceBehind + offsetX;
                double posY = mc.player.isElytraFlying() ? mc.player.getPosY() + mc.player.getHeight() / 2.0 + direction.y * distanceBehind + MathUtil.random(-0.35f, 0.35f) : mc.player.getPosY() + MathUtil.random(0.2f, mc.player.getHeight() + 0.1f);
                double posZ = mc.player.getPosZ() + direction.z * distanceBehind + offsetZ;

                if (!isPositionInBlock(new Vector3d(posX, posY, posZ))) {
                    double baseSpeed = 0.075;
                    Vector3d velocity = direction.scale(baseSpeed).add(new Vector3d(MathUtil.random(-0.01f, 0.01f), MathUtil.random(-0.05f, 0.01f), MathUtil.random(-0.01f, 0.01f))).scale(0.1f);
                    addParticles(posX, posY, posZ, velocity, getColorByName("primaryColor"), 0.6f, (long) MathUtil.random(1500, 2000), 3, 0.00005f);
                }
            }

            for (Entity entity : mc.world.getAllEntities()) {
                if (reason.get("Падении перла") && entity instanceof EnderPearlEntity) {
                    EnderPearlEntity pearl = (EnderPearlEntity) entity;
                    if (!pearl.isOnGround()) {
                        createParticles(pearl.getPositionVec(), 1);
                    }
                }
                if (reason.get("Падении трезубца") && entity instanceof TridentEntity) {
                    TridentEntity trident = (TridentEntity) entity;
                    if (!trident.isInGround()) {
                        createParticles(trident.getPositionVec(), 1);
                    }
                }
                if (reason.get("Падении стрелы") && entity instanceof ArrowEntity) {
                    ArrowEntity arrow = (ArrowEntity) entity;
                    if (!arrow.isInGround()) {
                        createParticles(arrow.getPositionVec(), 1);
                    }
                }
            }

        } else if (event instanceof EventUpdate) {
            if (reason.get("Бездействии")) {
                Vector3d base = new Vector3d(mc.player.getPosX(), mc.player.getPosY() + mc.player.getHeight() / 2.0, mc.player.getPosZ());
                particles.ensureCapacity(particles.size() + count.getValue().intValue());
                for (int i = 0; i < count.getValue().intValue(); i++) {
                    double distance = MathUtil.random(7, 35);
                    double angle = Math.toRadians(MathUtil.random(0, 360));
                    double height = MathUtil.random(-7, 25);

                    Vector3d offset = new Vector3d(Math.cos(angle) * distance, height, Math.sin(angle) * distance);
                    Vector3d spawnPos = base.add(offset);

                    if (isPositionInBlock(spawnPos)) continue;

                    Vector3d originalPosition = base.add(offset);
                    long life = (long) MathUtil.random(1500, 2000);
                    double speed = Math.random() < 0.8 ? MathUtil.random(0.015f, 0.03f) : 0.125f;
                    double phi = Math.toRadians(MathUtil.random(0, 360));
                    float smooth = 3;

                    Vector3d velocity = new Vector3d(Math.cos(phi) * speed, MathUtil.random((float) (-speed * 0.1f), (float) (speed * 0.1f)), Math.sin(phi) * speed);
                    addParticles(originalPosition.x, originalPosition.y, originalPosition.z, velocity, getColorByName("primaryColor"), 0.6f, life, smooth, 0.00005f);
                }
            }
        } else if (event instanceof EventAttack) {
            if (reason.get("Ударе")) {
                Entity target = ((EventAttack) event).getTarget();
                if (target != null) {
                    for (int i = 0; i < 35; i++) {
                        double targetX = target.getPosX() + MathUtil.random(-0.4f, 0.4f);
                        double targetY = target.getPosY() + MathUtil.random(-0.4f, target.getHeight() + 0.4f);
                        double targetZ = target.getPosZ() + MathUtil.random(-0.4f, 0.4f);

                        if (isPositionInBlock(new Vector3d(targetX, targetY, targetZ))) continue;

                        float baseMx = (float) (MathUtil.random(-0.8f, 0.8f) * 2.0f);
                        float baseMy = (float) MathUtil.random(-0.25f, 1.4f);
                        float baseMz = (float) (MathUtil.random(-0.8f, 0.8f) * 2.0f);

                        float smooth = 0.5f;
                        long life = (long) MathUtil.random(1000, 1200);

                        Vector3d velocity = new Vector3d(baseMx * 0.075f, baseMy * 0.075f, baseMz * 0.075f);
                        addParticles(targetX, targetY, targetZ, velocity, getColorByName("primaryColor"), 0.6f, life, smooth, 0.0007f);
                    }
                }
            }
        } else if (event instanceof EventDestroyTotem) {
            if (reason.get("Сносе тотема")) {
                Entity entity = ((EventDestroyTotem) event).entity;
                if (entity != null) {
                    double centerX = entity.getPosX();
                    double centerY = entity.getPosY() + entity.getHeight() / 2.0;
                    double centerZ = entity.getPosZ();

                    for (int i = 0; i < 50; i++) {
                        double theta = Math.random() * 2.0 * Math.PI;
                        double phi = Math.random() * Math.PI;
                        double speed = (Math.random() * 0.5 + 0.5) * 0.1;

                        double vx = Math.sin(phi) * Math.cos(theta) * speed;
                        double vy = Math.sin(phi) * Math.sin(theta) * speed;
                        double vz = Math.cos(phi) * speed;

                        double spawnX = centerX + MathUtil.random(-0.3f, 0.3f);
                        double spawnY = centerY + MathUtil.random(-0.3f, 0.3f);
                        double spawnZ = centerZ + MathUtil.random(-0.3f, 0.3f);

                        if (isPositionInBlock(new Vector3d(spawnX, spawnY, spawnZ))) continue;

                        int color = Math.random() < 0.7 ? 0xFF00FF00 : 0xFFFFFF00;
                        float smooth = 2.0f;
                        long life = (long) MathUtil.random(1500, 2000);

                        addParticles(spawnX, spawnY, spawnZ, new Vector3d(vx, vy, vz), color, 0.6f, life, smooth, 0.00005f);
                    }
                }
            }

        } else if (event instanceof EventWorldChanged) {
            particles.clear();
        }
        return false;
    }

    private void createParticles(Vector3d position, int count) {
        final int particleColor = getColorByName("primaryColor");
        particles.ensureCapacity(particles.size() + count * 2);
        for (int i = 0; i < count * 2.5; i++) {
            final double distance = 0f;
            final double angle = Math.toRadians(MathUtil.random(0, 360));
            final double cosAngle = Math.cos(angle);
            final double sinAngle = Math.sin(angle);

            final double dx = cosAngle * distance;
            final double dz = sinAngle * distance;
            final double dy = MathUtil.random(0.1f, 0.35f);

            final Vector3d particlePos = new Vector3d(position.x + dx, position.y + dy, position.z + dz);
            if (isPositionInBlock(particlePos)) continue;

            final float life = (float) MathUtil.random(2400, 2800);
            final float speedMin = (float) MathUtil.random(0.015f, 0.0375f);
            final float speedMax = (float) MathUtil.random(0.05f, 0.075f);
            final double speedFinal = MathUtil.random(speedMin, speedMax);
            final double speedFinalY = speedFinal * 0.4;

            final double angleVel = Math.toRadians(MathUtil.random(0, 360));
            final double cosVel = Math.cos(angleVel);
            final double sinVel = Math.sin(angleVel);

            final double velX = cosVel * speedFinal;
            final double velZ = sinVel * speedFinal;
            final double velY = MathUtil.random(-speedFinalY, speedFinalY);

            addParticles(particlePos.x, particlePos.y, particlePos.z, new Vector3d(velX, velY, velZ), particleColor, 0.375f, (long) life, 2, 0.00005f);
        }
    }

    private void addParticles(double x, double y, double z, Vector3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
        Vector3d safePos = Particle.checkCollision(x, y, z, size);
        if (safePos != null) {
            float rotation = 0f;
            particles.add(new Particle(new Vector3d(x, y, z), velocity, color, size, lifeTime, smooth, gravity, rotation));
        }
    }

    @Override
    public void onDisable() {
        particles.clear();
        super.onDisable();
    }

    @Getter
    public static class Particle {
        Vector3d position;
        Vector3d velocity;
        int color;
        float size;
        long lifeTime;
        TimerUtil time = new TimerUtil();
        float alpha = 1.0f;
        float smoothFactor;
        private long lastUpdateNs;
        private final double gravity;
        float rotation;

        public Particle(Vector3d position, Vector3d velocity, int color, float size, long lifeTime, float smooth, double gravity, float rotation) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.lifeTime = lifeTime;
            this.time.reset();
            this.lastUpdateNs = System.nanoTime();
            this.smoothFactor = smooth;
            this.gravity = gravity;
            this.rotation = rotation;
        }

        public void update() {
            long nowNs = System.nanoTime();
            double deltaSec = (nowNs - lastUpdateNs) / 1_000_000_000.0;
            lastUpdateNs = nowNs;

            float progress = Math.min(1.0f, (float) time.getTimeElapsed() / lifeTime);
            double factor = Math.pow(1.0 - progress, smoothFactor);

            double vx = velocity.x;
            double vy = velocity.y;
            double vz = velocity.z;

            double newX = position.x;
            double newY = position.y;
            double newZ = position.z;

            newX += vx * factor * (deltaSec * 60);
            if (checkCollision(newX, position.y, position.z, size) == null) {
                vx = -vx * 0.8;
                newX = position.x;
            }

            newY += vy * factor * (deltaSec * 60);
            if (checkCollision(newX, newY, position.z, size) == null) {
                vy = -vy * 1.5;
                newY = position.y;
            }

            newZ += vz * factor * (deltaSec * 60);
            if (checkCollision(newX, newY, newZ, size) == null) {
                vz = -vz * 0.8;
                newZ = position.z;
            }

            position = new Vector3d(newX, newY, newZ);
            velocity = new Vector3d(vx * 0.9999, vy * 0.9999 - gravity, vz * 0.9999);
            alpha = 1.0f - progress;
        }

        private static Vector3d checkCollision(double x, double y, double z, float size) {
            double half = size * 0.5;
            int minX = net.minecraft.util.math.MathHelper.floor(x - half);
            int maxX = net.minecraft.util.math.MathHelper.floor(x + half);
            int minY = net.minecraft.util.math.MathHelper.floor(y - half);
            int maxY = net.minecraft.util.math.MathHelper.floor(y + half);
            int minZ = net.minecraft.util.math.MathHelper.floor(z - half);
            int maxZ = net.minecraft.util.math.MathHelper.floor(z + half);

            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int bx = minX; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        net.minecraft.block.BlockState state = mc.world.getBlockState(pos.setPos(bx, by, bz));
                        if (!state.isAir() && state.isSolid()) {
                            return null;
                        }
                    }
                }
            }
            return new Vector3d(x, y, z);
        }
    }
}