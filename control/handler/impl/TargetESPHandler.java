package nuclear.control.handler.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.impl.combat.AttackAura;
import nuclear.module.impl.combat.ProjectileHelper;
import nuclear.module.impl.render.TargetEsp;
import nuclear.utils.IMinecraft;
import nuclear.utils.anim.animations.Easing;
import nuclear.utils.anim.animations.TimeAnim;
import nuclear.utils.math.MathUtil;
import nuclear.utils.render.AnimationUtil;
import nuclear.utils.render.ColorUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.Easings;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.CopyOnWriteArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class TargetESPHandler implements IMinecraft {

    private static final ResourceLocation GLOW_TEXTURE = new ResourceLocation("nuclear/images/targetesp/glow.png");

    private static ResourceLocation getTargetTexture(String type) {
        return new ResourceLocation("nuclear/images/targetesp/target" + type.replace("Тип ", "") + ".png");
    }

    private final MatrixStack reusableStack = new MatrixStack();
    private final AnimationUtil ghostAlphaAnimation = new AnimationUtil(0f, 10f, Easings.CUBIC_IN_OUT);
    private final AnimationUtil ghostScaleAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

    private final AnimationUtil alphaAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

    private final AnimationUtil hurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

    private final AnimationUtil ghostHurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);
    private final AnimationUtil circleHurtColorAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);
    private final AnimationUtil crystalFadeAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);

    private Entity lastGhostEntity;
    private Entity lastTargetEntity;
    private Entity lastCrystalTarget;

    private Vector3d lastPosition;
    private final int redFactor = 200;

    private float ghostAnimationTime;
    private long lastGhostUpdateTimestamp;

    private float lastHalfHeight;
    private boolean lastHadTarget;
    private double rotationPhase;
    private long lastRotationUpdateMs;
    private float crystalRotate;

    // Для режима Тонкий призраков
    private float animationNurik = 0;
    private long startTime = System.currentTimeMillis();
    private final TimeAnim test = new TimeAnim(Easing.LINEAR, 300);
    private Entity lastThinGhostEntity;

    // Для режима Тянущиеся призраков
    private final CopyOnWriteArrayList<GlowPoint> bmwPoints = new CopyOnWriteArrayList<>();
    private Entity lastBMWEntity;
    private long lastBMWAddTime = 0;

    public boolean onEvent(Event event) {
        if (event instanceof EventRender render && render.isRender3D()) {
            onRender3D(render);
        }
        return false;
    }

    private void onRender3D(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state) return;

        String mode = TargetEsp.targetesp.get();
        switch (mode) {
            case "Ромб" -> handleRhombus(event);
            case "Призраки" -> handleGhost(event);
            case "Кольцо" -> handleCircle(event);
            case "Кристаллы" -> handleCrystals(event);
        }
    }

    private LivingEntity getCurrentTarget() {
        LivingEntity auraTarget = AttackAura.getTarget();
        if (auraTarget != null && auraTarget.isAlive()) {
            return auraTarget;
        }

        ProjectileHelper projectileHelper = (ProjectileHelper) Manager.FUNCTION_MANAGER.get("ProjectileHelper");
        if (projectileHelper != null && projectileHelper.target != null && projectileHelper.target.isAlive()) {
            return projectileHelper.target;
        }

        return null;
    }

    private void handleRhombus(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !targetEsp.targetesp.is("Ромб")) return;
        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();

        Entity target = getCurrentTarget();
        boolean hasTarget = target != null;

        alphaAnimation.update(hasTarget ? 1f : 0f);

        if (!hasTarget && alphaAnimation.getValue() <= 0.01f) {
            lastHadTarget = false;
            lastTargetEntity = null;
            return;
        }

        Vector3d entityPos = lastPosition;
        float halfHeight = lastHalfHeight;
        Entity currentEntity = hasTarget ? target : lastTargetEntity;

        if (currentEntity != null && currentEntity.isAlive()) {
            entityPos = MathUtil.interpolate(currentEntity, event.getPartialTicks());
            halfHeight = currentEntity.getHeight() / 2f;
        } else if (lastPosition == null || !lastHadTarget) return;

        if (hasTarget) {
            lastPosition = entityPos;
            lastHalfHeight = halfHeight;
            lastHadTarget = true;
            lastTargetEntity = target;
        }

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(entityPos.x - cameraPosition.x, entityPos.y + halfHeight - cameraPosition.y, entityPos.z - cameraPosition.z);

        float hurtFactor = 0f;
        if (targetEsp.hitred.get()) {
            hurtFactor = hasTarget && target instanceof LivingEntity living && living.maxHurtTime > 0
                    ? MathHelper.clamp((float) living.hurtTime / living.maxHurtTime, 0f, 1f) : 0f;
        }
        hurtColorAnimation.update(hurtFactor);

        float rawSize = targetEsp.size.getValue().floatValue() / 65;
        float displayedSize = MathUtil.lerp(1f, rawSize, alphaAnimation.getValue());

        String typeValue = TargetEsp.type.get();
        if ("Тип 2".equals(typeValue)) {
            displayedSize *= 1.5f;
        }

        float halfSize = displayedSize / 2f;

        double deltaTime = lastRotationUpdateMs == 0 ? 0 : (System.currentTimeMillis() - lastRotationUpdateMs) / 1000f;
        lastRotationUpdateMs = System.currentTimeMillis();
        rotationPhase += 2 * (hasTarget ? 1 : 1.5) * deltaTime;

        matrixStack.push();
        matrixStack.rotate(mc.getRenderManager().info.getRotation().copy());
        matrixStack.rotate(Vector3f.ZP.rotationDegrees((float) (Math.sin(rotationPhase) * 180)));
        Matrix4f rotationMatrix = matrixStack.getLast().getMatrix();
        matrixStack.pop();

        RenderSystem.pushMatrix();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

        int baseColor = getColorByName("primaryColor");
        int color = ColorUtil.boostColor(ColorUtil.getColor(
                MathHelper.clamp((int) (ColorUtil.red(baseColor) * (1f - hurtColorAnimation.getValue()) + redFactor * hurtColorAnimation.getValue()), 0, 255),
                MathHelper.clamp((int) (ColorUtil.green(baseColor) * (1f - hurtColorAnimation.getValue())), 0, 255),
                MathHelper.clamp((int) (ColorUtil.blue(baseColor) * (1f - hurtColorAnimation.getValue())), 0, 255),
                (int) (100 * MathHelper.clamp(alphaAnimation.getValue(), 0f, 1f))), 45);

        int r = ColorUtil.red(color), g = ColorUtil.green(color), b = ColorUtil.blue(color), a = ColorUtil.alpha(color);

        ResourceLocation targetTexture = getTargetTexture(typeValue);
        mc.getTextureManager().bindTexture(targetTexture);

        BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        BUFFER.pos(rotationMatrix, -halfSize, -halfSize + displayedSize, 0).tex(0, 1).color(r, g, b, a).endVertex();
        BUFFER.pos(rotationMatrix, halfSize, -halfSize + displayedSize, 0).tex(1, 1).color(r, g, b, a).endVertex();
        BUFFER.pos(rotationMatrix, halfSize, -halfSize, 0).tex(1, 0).color(r, g, b, a).endVertex();
        BUFFER.pos(rotationMatrix, -halfSize, -halfSize, 0).tex(0, 0).color(r, g, b, a).endVertex();
        TESSELLATOR.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private void handleGhost(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Призраки")) return;

        String ghostType = TargetEsp.ghostType.get();
        if ("Тонкий".equals(ghostType)) {
            handleThinGhost(event);
            // Очищаем состояние других режимов
            lastGhostEntity = null;
            ghostAnimationTime = 0;
            lastGhostUpdateTimestamp = 0;
            lastBMWEntity = null;
            bmwPoints.clear();
            return;
        }

        if ("Тянущуюся".equals(ghostType)) {
            handleStretchingGhost(event);
            // Очищаем состояние других режимов
            lastGhostEntity = null;
            ghostAnimationTime = 0;
            lastGhostUpdateTimestamp = 0;
            lastThinGhostEntity = null;
            animationNurik = 0;
            startTime = System.currentTimeMillis();
            test.run(0);
            return;
        }

        // Режим "Кастом" (по умолчанию)
        handleCustomGhost(event);
        // Очищаем состояние других режимов
        lastThinGhostEntity = null;
        animationNurik = 0;
        startTime = System.currentTimeMillis();
        test.run(0);
        lastBMWEntity = null;
        bmwPoints.clear();
        return;
    }

    private void handleCustomGhost(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Призраки")) return;

        // Проверяем, что выбран именно режим "Кастом"
        String ghostType = TargetEsp.ghostType.get();
        if ("Тонкий".equals(ghostType) || "Тянущуюся".equals(ghostType)) {
            return;
        }

        LivingEntity target = getCurrentTarget();
        boolean alive = target != null && target.isAlive();

        ghostAlphaAnimation.update(alive ? 1f : 0f);
        ghostScaleAnimation.update(alive ? 1f : 0f);

        if (ghostAlphaAnimation.getValue() <= 0.01f && ghostScaleAnimation.getValue() <= 0.01f) {
            lastGhostEntity = null;
            ghostAnimationTime = 0;
            lastGhostUpdateTimestamp = 0;
            return;
        }

        if (alive) {
            if (lastGhostEntity == null) lastGhostUpdateTimestamp = System.currentTimeMillis();
            lastGhostEntity = target;
        }
        if (lastGhostEntity == null) return;

        float speedMul       = targetEsp.speed.getValue().floatValue() / 40f;
        float spriteSize     = targetEsp.sizee.getValue().floatValue() * 0.01f;
        float radiusMod      = targetEsp.distancee.getValue().floatValue();
        float alphaStep      = targetEsp.alpha.getValue().floatValue();
        int totalSprites     = (int) (targetEsp.distance.getValue().floatValue() * 1.1f);

        long currentTime = System.currentTimeMillis();
        if (lastGhostUpdateTimestamp > 0) {
            ghostAnimationTime += (4f * (currentTime - lastGhostUpdateTimestamp) / 600f) * speedMul;
        }
        lastGhostUpdateTimestamp = currentTime;

        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        Vector3d entityPos = MathUtil.interpolate(lastGhostEntity, event.getPartialTicks());
        double x = entityPos.x - cameraPosition.x;
        double y = entityPos.y - cameraPosition.y - 0.5f;
        double z = entityPos.z - cameraPosition.z;

        float rawHurt = 0f;
        if (targetEsp.hitred.get() && alive && target.maxHurtTime > 0) {
            rawHurt = MathHelper.clamp((float) target.hurtTime / target.maxHurtTime, 0f, 1f);
        }
        ghostHurtColorAnimation.update(rawHurt);

        RenderSystem.pushMatrix();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

        mc.getTextureManager().bindTexture(GLOW_TEXTURE);

        BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        int baseColor = getColorByName("primaryColor");
        int color = ColorUtil.getColor(
                MathHelper.clamp((int) (ColorUtil.red(baseColor) * (1f - ghostHurtColorAnimation.getValue()) + redFactor * ghostHurtColorAnimation.getValue()), 0, 255),
                MathHelper.clamp((int) (ColorUtil.green(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                MathHelper.clamp((int) (ColorUtil.blue(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                ColorUtil.alpha(baseColor)
        );

        float halfHeight = lastGhostEntity.getHeight() / 2f;
        for (int ringLayer = 0; ringLayer < 9; ringLayer += 3) {
            float layerAngleOffset = ringLayer * 0.5f;
            float forwardOffset = ringLayer >= 6 ? 1.2f : 0f;
            boolean reverseDirection = ringLayer == 3;
            for (int spriteIndex = 0; spriteIndex < totalSprites; ++spriteIndex) {
                float basePhase = ghostAnimationTime + spriteIndex * 0.095f + layerAngleOffset + forwardOffset;
                float spritePhase = reverseDirection ? -basePhase : basePhase;

                reusableStack.push();
                reusableStack.translate(
                        x + (radiusMod * 0.8f * MathHelper.sin(spritePhase)),
                        y + halfHeight + (radiusMod * 0.3f * MathHelper.sin(spritePhase * 0.7f)) + (0.2f * ringLayer),
                        z + (radiusMod * 0.8f * MathHelper.cos(spritePhase))
                );

                float spriteScale = ghostScaleAnimation.getValue() * (spriteSize + spriteIndex / 2000.0f);
                reusableStack.scale(spriteScale, spriteScale, spriteScale);
                reusableStack.rotate(mc.getRenderManager().info.getRotation().copy());

                float distAlpha = Math.max(0f, 1f - (spriteIndex / (float) totalSprites * alphaStep / 20f));
                int finalAlpha = (int) (ghostAlphaAnimation.getValue() * 255 * distAlpha);
                int spriteColor = ColorUtil.applyOpacity(color, finalAlpha);

                int r = ColorUtil.red(spriteColor), g = ColorUtil.green(spriteColor),
                        b = ColorUtil.blue(spriteColor), a = ColorUtil.alpha(spriteColor);

                Matrix4f matrix = reusableStack.getLast().getMatrix();
                BUFFER.pos(matrix, -25, 25, 0.0f).tex(0.0f, 1.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, 25, 25, 0.0f).tex(1.0f, 1.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, 25, -25, 0.0f).tex(1.0f, 0.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, -25, -25, 0.0f).tex(0.0f, 0.0f).color(r, g, b, a).endVertex();
                reusableStack.pop();
            }
        }

        TESSELLATOR.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private void handleThinGhost(EventRender event3D) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Призраки")) return;

        // Проверяем, что выбран именно режим "Тонкий"
        String ghostType = TargetEsp.ghostType.get();
        if (!"Тонкий".equals(ghostType)) {
            // Если режим не "Тонкий", очищаем состояние и выходим
            lastThinGhostEntity = null;
            animationNurik = 0;
            startTime = System.currentTimeMillis();
            test.run(0);
            return;
        }

        LivingEntity target = getCurrentTarget();
        boolean alive = target != null && target.isAlive();

        // Обновляем анимацию появления/исчезновения
        this.test.run(alive ? 1 : 0);

        // Если анимация закончилась и значение близко к нулю, не рендерим
        if (this.test.getValue() <= 0.01) {
            lastThinGhostEntity = null;
            animationNurik = 0;
            startTime = System.currentTimeMillis();
            return;
        }

        if (alive) {
            if (lastThinGhostEntity == null || lastThinGhostEntity != target) {
                this.startTime = System.currentTimeMillis();
            }
            lastThinGhostEntity = target;
        }

        // Если нет последней сущности, не рендерим
        if (lastThinGhostEntity == null) return;

        // Используем настройки из TargetEsp
        float speedMul = targetEsp.speed.getValue().floatValue() / 40f;
        float spriteSize = targetEsp.sizee.getValue().floatValue();
        float radiusMod = targetEsp.distancee.getValue().floatValue();
        float alphaStep = targetEsp.alpha.getValue().floatValue();
        int totalSprites = (int) targetEsp.distance.getValue().floatValue();

        MatrixStack e = new MatrixStack();
        long currentTime = System.currentTimeMillis();
        this.animationNurik = (this.animationNurik + (5 * (currentTime - this.startTime) / 600.0f) * speedMul);
        this.startTime = currentTime;

        RenderSystem.disableDepthTest();
        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        final double x = interpolate(lastThinGhostEntity.getPosX(), lastThinGhostEntity.lastTickPosX, event3D.getPartialTicks()) - cameraPos.x;
        final double y = interpolate(lastThinGhostEntity.getPosY(), lastThinGhostEntity.lastTickPosY, event3D.getPartialTicks()) - cameraPos.y;
        final double z = interpolate(lastThinGhostEntity.getPosZ(), lastThinGhostEntity.lastTickPosZ, event3D.getPartialTicks()) - cameraPos.z;

        int n2 = 3;
        int n3 = totalSprites; // Используем настройку distance
        int n4 = 3 * n2;
        final float hurtPercent = (lastThinGhostEntity instanceof LivingEntity living)
                ? (float) Math.sin(living.hurtTime * (18F * Math.PI / 180F))
                : 0f;

        e.push();
        BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        int baseColor = ColorUtils.setAlpha(getColorByName("primaryColor"), 150);
        for (int i = 0; i < n4; i += n2) {
            for (int j = 0; j < n3; ++j) {
                int color;
                if (targetEsp.hitred.get())
                    color = ColorUtils.interpolateColor(ColorUtils.reAlphaInt(baseColor, 90), ColorUtils.rgba(255, 50, 50, 255), hurtPercent);
                else color = ColorUtils.reAlphaInt(baseColor, 90);

                float f2 = animationNurik + j * 0.1f;
                float f3 = radiusMod; // Используем настройку distancee
                float f4 = 0.5f;
                int n5 = (int) Math.pow(i, 2.0);
                e.push();
                e.translate((x + (f3 * MathHelper.sin(f2 + n5))), y + f4 + (0.3f * MathHelper.sin(this.animationNurik + j * 0.2f)) + (0.2f * i), (z + (f3 * MathHelper.cos(f2 - n5))));

                // Используем настройку sizee для размера с анимацией исчезновения
                float baseScale = spriteSize * 0.01f;
                float animationValue = (float) test.getValue();
                float scaleValue = animationValue * (baseScale + j / 1800.0f);
                e.scale(scaleValue, scaleValue, scaleValue);
                e.rotate(mc.getRenderManager().info.getRotation().copy());

                int n7 = -25;
                int n8 = 50;

                // Используем настройку alpha для прозрачности с анимацией исчезновения
                float distAlpha = Math.max(0f, 1f - (j / (float) n3 * alphaStep / 20f));
                int finalAlpha = (int) (animationValue * 255F * distAlpha);
                int finalColor = ColorUtils.reAlphaInt(color, finalAlpha);
                int r = ColorUtils.red(finalColor), g = ColorUtils.green(finalColor), b = ColorUtils.blue(finalColor), a = ColorUtils.getAlpha(finalColor);
                Matrix4f matrix = e.getLast().getMatrix();
                BUFFER.pos(matrix, n7, (n7 + n8), 0.0f).tex(0.0f, 1.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, (n7 + n8), (n7 + n8), 0.0f).tex(1.0f, 1.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, (n7 + n8), n7, 0.0f).tex(1.0f, 0.0f).color(r, g, b, a).endVertex();
                BUFFER.pos(matrix, n7, n7, 0.0f).tex(0.0f, 0.0f).color(r, g, b, a).endVertex();
                e.pop();
            }
        }

        TESSELLATOR.draw();
        e.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    private void handleStretchingGhost(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Призраки")) return;

        // Проверяем, что выбран именно режим "Тянущуюся"
        String ghostType = TargetEsp.ghostType.get();
        if (!"Тянущуюся".equals(ghostType)) {
            lastBMWEntity = null;
            bmwPoints.clear();
            return;
        }

        LivingEntity target = getCurrentTarget();
        boolean alive = target != null && target.isAlive();

        ghostAlphaAnimation.update(alive ? 1f : 0f);
        ghostScaleAnimation.update(alive ? 1f : 0f);

        if (ghostAlphaAnimation.getValue() <= 0.01f && ghostScaleAnimation.getValue() <= 0.01f) {
            lastBMWEntity = null;
            bmwPoints.clear();
            return;
        }

        if (alive) {
            if (lastBMWEntity == null || lastBMWEntity != target) {
                lastBMWEntity = target;
                lastBMWAddTime = 0;
            }
            // Добавляем новые точки призраков с задержкой
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBMWAddTime >= 15) {
                addGhosts(target, targetEsp.bmwGhostCount.getValue().intValue(), event.getPartialTicks(),
                        targetEsp.bmwGhostTimer.getValue().intValue(), getColorByName("primaryColor"));
                lastBMWAddTime = currentTime;
            }
        }

        // Удаляем старые точки
        oldGhostsRemoverPreRender();

        if (lastBMWEntity == null || bmwPoints.isEmpty()) return;

        float rawHurt = 0f;
        if (targetEsp.hitred.get() && alive && target != null && target.maxHurtTime > 0) {
            rawHurt = MathHelper.clamp((float) target.hurtTime / target.maxHurtTime, 0f, 1f);
        }
        ghostHurtColorAnimation.update(rawHurt);

        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        float alphaPC = ghostAlphaAnimation.getValue();
        if (alphaPC <= 0.01f) return;

        // Используем те же настройки, что и в других режимах
        float spriteSize = targetEsp.sizee.getValue().floatValue() * 0.01f;
        float alphaStep = targetEsp.alpha.getValue().floatValue();

        RenderSystem.pushMatrix();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        BUFFER.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        // Используем тот же цвет, что и в режиме "Кастом"
        int baseColor = getColorByName("primaryColor");
        int color = ColorUtil.getColor(
                MathHelper.clamp((int) (ColorUtil.red(baseColor) * (1f - ghostHurtColorAnimation.getValue()) + redFactor * ghostHurtColorAnimation.getValue()), 0, 255),
                MathHelper.clamp((int) (ColorUtil.green(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                MathHelper.clamp((int) (ColorUtil.blue(baseColor) * (1f - ghostHurtColorAnimation.getValue())), 0, 255),
                ColorUtil.alpha(baseColor)
        );

        // Подсчитываем общее количество точек для расчета distAlpha
        int totalPoints = bmwPoints.size() * 2;
        int pointIndex = 0;

        for (GlowPoint point : bmwPoints) {
            final float timePC = point.getTimePCUpdated();
            final float scaleOfTime = (1.0f - timePC);

            Vector3d particlePos = new Vector3d(point.getX(), point.getY(), point.getZ());
            reusableStack.push();
            reusableStack.translate(
                    particlePos.x - cameraPosition.x,
                    particlePos.y - cameraPosition.y,
                    particlePos.z - cameraPosition.z
            );

            // Используем те же принципы масштабирования, что и в режиме "Кастом"
            float scaleValue = ghostScaleAnimation.getValue() * spriteSize * scaleOfTime * 2;
            reusableStack.scale(scaleValue, scaleValue, scaleValue);
            reusableStack.rotate(mc.getRenderManager().info.getRotation().copy());

            // Используем те же принципы альфа-канала, что и в режиме "Кастом"
            // Применяем alphaStep для плавного затухания по индексу точки
            float distAlpha = totalPoints > 0 ? Math.max(0f, 1f - (pointIndex / (float) totalPoints * alphaStep / 20f)) : 1f;
            // Комбинируем с временем жизни точки
            distAlpha *= (1.0f - timePC);
            int finalAlpha = (int) (ghostAlphaAnimation.getValue() * 155 * distAlpha);
            int spriteColor = ColorUtil.applyOpacity(color, finalAlpha);

            int r = ColorUtil.red(spriteColor), g = ColorUtil.green(spriteColor),
                    b = ColorUtil.blue(spriteColor), a = ColorUtil.alpha(spriteColor);

            // Используем тот же формат рендеринга текстуры, что и в других режимах
            Matrix4f matrix = reusableStack.getLast().getMatrix();
            BUFFER.pos(matrix, -25, 25, 0.0f).tex(0.0f, 1.0f).color(r, g, b, a).endVertex();
            BUFFER.pos(matrix, 25, 25, 0.0f).tex(1.0f, 1.0f).color(r, g, b, a).endVertex();
            BUFFER.pos(matrix, 25, -25, 0.0f).tex(1.0f, 0.0f).color(r, g, b, a).endVertex();
            BUFFER.pos(matrix, -25, -25, 0.0f).tex(0.0f, 0.0f).color(r, g, b, a).endVertex();
            reusableStack.pop();

            pointIndex++;
        }

        TESSELLATOR.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private void addGhosts(LivingEntity entity, int cornersCount, float partialTicks, int maxTime, int colorBase) {
        final float x = (float) (entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks);
        final float y = (float) (entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks);
        final float z = (float) (entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks);
        final float xzRange = entity.getWidth() * 0.7f;
        final float yRange = entity.getHeight();

        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null) return;

        int delayXZ = targetEsp.bmwStrengthXZ.getValue().intValue();
        int delayY = targetEsp.bmwStrengthY.getValue().intValue();
        long time = System.currentTimeMillis();

        for (int corner = 0; corner < cornersCount; corner++) {
            float cornersPC = corner / (float) cornersCount;
            float xzRotate = ((time + (int) (delayXZ * cornersPC)) % delayXZ) / (float) delayXZ * 360.0f;
            float yLrpPC = ((time + (int) (delayY * cornersPC)) % delayY) / (float) delayY;
            yLrpPC = (yLrpPC > 0.5f ? 1.0f - yLrpPC : yLrpPC) * 2.0f;
            yLrpPC = (float) Easings.QUAD_IN_OUT.ease(yLrpPC);
            double yawRad = Math.toRadians(MathHelper.wrapDegrees(cornersPC * 360.0f + xzRotate));
            final float xPos = x - (float) Math.sin(yawRad) * xzRange;
            final float yPos = y + yRange * yLrpPC;
            final float zPos = z + (float) Math.cos(yawRad) * xzRange;
            this.bmwPoints.add(new GlowPoint(xPos, yPos, zPos, maxTime, colorBase));
        }
    }

    private void oldGhostsRemoverPreRender() {
        this.bmwPoints.removeIf(GlowPoint::removeIfUpdateValueTimePC);
    }

    private static class GlowPoint {
        private final float x, y, z;
        private final long startTime;
        private final int maxTime;
        private final int baseColor;
        private float currentTimePC;

        public GlowPoint(float x, float y, float z, int maxTime, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.startTime = System.currentTimeMillis();
            this.maxTime = maxTime;
            this.baseColor = color;
        }

        public float getTimePC() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(elapsed / (float) maxTime, 1.0f);
        }

        public float getTimePCUpdated() {
            return this.currentTimePC;
        }

        public int getColor(float timePC) {
            timePC = (timePC > 0.5f ? 1.0f - timePC : timePC) * 2.0f;
            return ColorUtils.reAlphaInt(this.baseColor, (int)(timePC * 255));
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public float getZ() {
            return this.z;
        }

        public boolean removeIfUpdateValueTimePC() {
            final float timePC = this.getTimePC();
            this.currentTimePC = timePC;
            return timePC >= 1.0f;
        }
    }

    private void handleCircle(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (!targetEsp.state || !TargetEsp.targetesp.is("Кольцо")) return;

        LivingEntity target = getCurrentTarget();
        if (target == null) return;

        circleHurtColorAnimation.update(0f);

        float radius = target.getWidth() * 0.8F;
        Vector3d targetPosition = MathUtil.interpolate(target, event.getPartialTicks());

        double duration = 2000;
        double elapsedMillis = (System.currentTimeMillis() % duration);
        double progress = elapsedMillis / (duration / 2);

        progress = elapsedMillis > duration / 2 ? progress - 1 : 1 - progress;
        progress = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;

        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();

        RenderSystem.pushMatrix();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2f);
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        BUFFER.begin(8, DefaultVertexFormats.POSITION_COLOR);
        for (int angleDegree = 0; angleDegree <= 360; ++angleDegree) {
            int gradientColor = ColorUtil.gradient(10, angleDegree * 5, getColorByName("primaryColor"), ColorUtil.darken(ColorUtils.getColorStyle(0.0F), 0.5f));

            int ringColor = gradientColor;

            double angleRadians = Math.toRadians(angleDegree);
            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);
            double heightOffset = (target.getHeight() / 2) * (progress > 0.5 ? 1 - progress : progress) * (elapsedMillis > duration / 2 ? -1 : 1);

            BUFFER.pos((float) (targetPosition.x + cos * radius - cameraPosition.x),
                    (float) (targetPosition.y + target.getHeight() * progress - cameraPosition.y),
                    (float) (targetPosition.z + sin * radius - cameraPosition.z)).color(ringColor).endVertex();
            BUFFER.pos((float) (targetPosition.x + cos * radius - cameraPosition.x),
                    (float) (targetPosition.y + target.getHeight() * progress + heightOffset - cameraPosition.y),
                    (float) (targetPosition.z + sin * radius - cameraPosition.z)).color(ColorUtil.applyOpacity(ringColor, 0)).endVertex();
        }
        TESSELLATOR.draw();

        BUFFER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        for (int angleDegree = 0; angleDegree <= 360; ++angleDegree) {
            int gradientColor = ColorUtil.gradient(10, angleDegree * 5, getColorByName("primaryColor"), ColorUtil.darken(ColorUtils.getColorStyle(0.0F), 0.5f));
            int ringColor = gradientColor;

            double angleRadians = Math.toRadians(angleDegree);
            BUFFER.pos((float) (targetPosition.x + Math.cos(angleRadians) * radius - cameraPosition.x),
                    (float) (targetPosition.y + target.getHeight() * progress - cameraPosition.y),
                    (float) (targetPosition.z + Math.sin(angleRadians) * radius - cameraPosition.z)).color(ringColor).endVertex();
        }
        TESSELLATOR.draw();

        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    private void handleCrystals(EventRender event) {
        TargetEsp targetEsp = Manager.FUNCTION_MANAGER.targetEsp;
        if (targetEsp == null || !targetEsp.state || !TargetEsp.targetesp.is("Кристаллы")) return;

        LivingEntity target = getCurrentTarget();
        boolean alive = target != null && target.isAlive();

        crystalFadeAnimation.update(alive ? targetEsp.crystalFade.getValue().floatValue() : 0f);

        if (alive) {
            lastCrystalTarget = target;
        }

        if (lastCrystalTarget == null || crystalFadeAnimation.getValue() <= 0.01f) {
            if (!alive) {
                lastCrystalTarget = null;
            }
            return;
        }

        float alpha = crystalFadeAnimation.getValue();
        if (alpha <= 0.01f) return;

        crystalRotate = (System.currentTimeMillis() % 360000L) / 7.25f;
        float timeSec = System.currentTimeMillis() * 0.001f;

        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        Vector3d targetPosition = MathUtil.interpolate(lastCrystalTarget, event.getPartialTicks());

        double x = targetPosition.x - cameraPosition.x;
        double y = targetPosition.y - cameraPosition.y;
        double z = targetPosition.z - cameraPosition.z;

        float width = lastCrystalTarget.getWidth() * 1.5f;
        float height = lastCrystalTarget.getHeight();

        float alphaMul = alpha * (255 / 255f);
        if (alphaMul <= 0.01f) return;

        int baseColor = getColorByName("primaryColor");
        int baseArgb = ColorUtils.setAlpha(baseColor, (int)(255 * alphaMul));

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);

        java.util.List<Vector3f> points = new java.util.ArrayList<>();

        long seed = (long) lastCrystalTarget.getEntityId() * 133769420L;
        java.util.Random rng = new java.util.Random(seed);

        int count = (int) (width + height * 12);
        int candidatesPerPoint = 15;

        for (int i = 0; i < count; i++) {
            Vector3f bestCandidate = null;
            float bestDistSq = -1.0f;

            for (int attempt = 0; attempt < candidatesPerPoint; attempt++) {
                float angle = rng.nextFloat() * 360f;
                float h = rng.nextFloat() * height;
                float rad = (float) Math.toRadians(angle);
                float px = (float) (Math.sin(rad) * width);
                float pz = (float) (Math.cos(rad) * width);
                float py = h;

                if (points.isEmpty()) {
                    bestCandidate = new Vector3f(px, py, pz);
                    break;
                }

                float nearestDistSq = Float.MAX_VALUE;
                for (Vector3f existing : points) {
                    float dSq = (existing.getX() - px)*(existing.getX() - px) +
                            (existing.getY() - py)*(existing.getY() - py) +
                            (existing.getZ() - pz)*(existing.getZ() - pz);
                    if (dSq < nearestDistSq) {
                        nearestDistSq = dSq;
                    }
                }

                if (nearestDistSq > bestDistSq) {
                    bestDistSq = nearestDistSq;
                    bestCandidate = new Vector3f(px, py, pz);
                }
            }

            if (bestCandidate != null) {
                points.add(bestCandidate);
            }
        }

        RenderSystem.disableTexture();
        BUFFER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        double cx = x;
        double cy = y + height * 0.5;
        double cz = z;

        float meshScale = 0.10f * targetEsp.crystalSize.getValue().floatValue();
        float radiusAnimFactor = 1.25f - 0.5f * alpha;
        float rotationRad = (float) Math.toRadians(crystalRotate);
        float sinRot = (float) Math.sin(rotationRad);
        float cosRot = (float) Math.cos(rotationRad);

        MatrixStack ms = new MatrixStack();

        for (int i = 0; i < points.size(); i++) {
            Vector3f p = points.get(i);

            float rotX = p.getX() * cosRot - p.getZ() * sinRot;
            float rotZ = p.getX() * sinRot + p.getZ() * cosRot;
            float rotY = p.getY();

            float finalX = rotX * radiusAnimFactor;
            float finalZ = rotZ * radiusAnimFactor;
            float bob = (float) (0.05f * Math.sin(timeSec * 2.0f + i * 1337.0f));
            float finalY = rotY + bob;

            ms.push();
            ms.translate(x + finalX, y + finalY, z + finalZ);

            double wx = x + finalX;
            double wy = y + finalY;
            double wz = z + finalZ;

            Vector3f dir = new Vector3f((float)(cx - wx), (float)(cy - wy), (float)(cz - wz));
            float len = (float) Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY() + dir.getZ() * dir.getZ());
            if (len > 0.001f) {
                dir.set(dir.getX() / len, dir.getY() / len, dir.getZ() / len);
                ms.rotate(quatFromTo(Vector3f.YP, dir));
            }

            ms.scale(meshScale, meshScale, meshScale);
            Matrix4f mat = ms.getLast().getMatrix();
            putCrystalMesh(BUFFER, mat, baseArgb);

            ms.pop();
        }

        TESSELLATOR.draw();
        RenderSystem.enableTexture();

        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < points.size(); i++) {
            Vector3f p = points.get(i);

            float rotX = p.getX() * cosRot - p.getZ() * sinRot;
            float rotZ = p.getX() * sinRot + p.getZ() * cosRot;
            float rotY = p.getY();

            float finalX = rotX * radiusAnimFactor;
            float finalZ = rotZ * radiusAnimFactor;
            float bob = (float) (0.05f * Math.sin(timeSec * 2.0f + i * 1337.0f));
            float finalY = rotY + bob;

            float sSmall = 0.18f * 0.85f;
            float sBig = 0.55f * 0.85f;

            ms.push();
            ms.translate(x + finalX, y + finalY, z + finalZ);
            ms.rotate(mc.getRenderManager().info.getRotation().copy());

            Matrix4f mat = ms.getLast().getMatrix();

            int bigCol = ColorUtils.setAlpha(baseColor, (int)(255 * alphaMul * 0.20f));
            int smallCol = ColorUtils.setAlpha(baseColor, (int)(255 * alphaMul));

            drawTextureQuad(BUFFER, mat, sBig, bigCol);
            drawTextureQuad(BUFFER, mat, sSmall, smallCol);

            ms.pop();
        }

        TESSELLATOR.draw();

        RenderSystem.shadeModel(7424);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private static final float[] CRYSTAL_FACE_MUL = new float[]{1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f};

    private void putCrystalMesh(BufferBuilder bb, Matrix4f mat, int color) {
        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);
        int a = ColorUtil.alpha(color);

        // Crystal mesh vertices (simplified octahedron-like shape)
        float[] vertices = {
                // Top point
                0, 0.5f, 0,
                // Bottom point
                0, -0.5f, 0,
                // Front
                0.3f, 0, 0.3f,
                // Back
                -0.3f, 0, -0.3f,
                // Right
                0.3f, 0, -0.3f,
                // Left
                -0.3f, 0, 0.3f
        };

        // Draw faces
        for (int face = 0; face < 8; face++) {
            float mul = CRYSTAL_FACE_MUL[face % CRYSTAL_FACE_MUL.length];
            int faceR = (int)(r * mul);
            int faceG = (int)(g * mul);
            int faceB = (int)(b * mul);

            // Top faces
            if (face < 4) {
                int v1 = 0; // top
                int v2 = 2 + (face % 4);
                int v3 = 2 + ((face + 1) % 4);
                bb.pos(mat, vertices[v1*3], vertices[v1*3+1], vertices[v1*3+2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v2*3], vertices[v2*3+1], vertices[v2*3+2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v3*3], vertices[v3*3+1], vertices[v3*3+2]).color(faceR, faceG, faceB, a).endVertex();
            } else {
                // Bottom faces
                int v1 = 1; // bottom
                int v2 = 2 + ((face - 4) % 4);
                int v3 = 2 + ((face - 3) % 4);
                bb.pos(mat, vertices[v1*3], vertices[v1*3+1], vertices[v1*3+2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v3*3], vertices[v3*3+1], vertices[v3*3+2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v2*3], vertices[v2*3+1], vertices[v2*3+2]).color(faceR, faceG, faceB, a).endVertex();
            }
        }
    }

    private void drawTextureQuad(BufferBuilder bb, Matrix4f mat, float size, int color) {
        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);
        int a = ColorUtil.alpha(color);

        float halfSize = size / 2f;
        bb.pos(mat, -halfSize, halfSize, 0).tex(0, 1).color(r, g, b, a).endVertex();
        bb.pos(mat, halfSize, halfSize, 0).tex(1, 1).color(r, g, b, a).endVertex();
        bb.pos(mat, halfSize, -halfSize, 0).tex(1, 0).color(r, g, b, a).endVertex();
        bb.pos(mat, -halfSize, -halfSize, 0).tex(0, 0).color(r, g, b, a).endVertex();
    }

    private net.minecraft.util.math.vector.Quaternion quatFromTo(Vector3f from, Vector3f to) {
        Vector3f cross = new Vector3f(
                from.getY() * to.getZ() - from.getZ() * to.getY(),
                from.getZ() * to.getX() - from.getX() * to.getZ(),
                from.getX() * to.getY() - from.getY() * to.getX()
        );
        float dot = from.getX() * to.getX() + from.getY() * to.getY() + from.getZ() * to.getZ();
        float len = (float) Math.sqrt(cross.getX() * cross.getX() + cross.getY() * cross.getY() + cross.getZ() * cross.getZ());

        if (len < 0.001f) {
            if (dot > 0) {
                return net.minecraft.util.math.vector.Quaternion.ONE;
            } else {
                Vector3f axis = Math.abs(from.getX()) < 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
                return new net.minecraft.util.math.vector.Quaternion(axis, 180f, true);
            }
        }

        float angle = (float) Math.acos(MathHelper.clamp(dot, -1f, 1f));
        cross.set(cross.getX() / len, cross.getY() / len, cross.getZ() / len);
        return new net.minecraft.util.math.vector.Quaternion(cross, (float) Math.toDegrees(angle), true);
    }
}
