package nuclear.module.impl.render;

import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;

@Annotation(name = "Crosshair", type = TypeList.Render)
public class Crosshair extends Module {
    private final SliderSetting length = new SliderSetting("Длина", 5, 1, 20, 1);
    private final SliderSetting gap = new SliderSetting("Зазор", 2, 0, 10, 1);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1, 1f, 5, 1f);
    private final BooleanSetting dynamic = new BooleanSetting("Динамика", true);
    private final BooleanSetting targetIndicator = new BooleanSetting("Индикатор цели", true);
    private final BooleanSetting outline = new BooleanSetting("Обводка", true);
    private final BooleanSetting centerDot = new BooleanSetting("Точка в центре", true);

    private float dynamicOffset = 0;
    private float prevDynamicOffset = 0;

    public Crosshair() {
        addSettings(length, gap, thickness, dynamic, targetIndicator, outline, centerDot);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender e) {
            if (e.isRender2D()) {
                if (mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON) return false;

                float centerX = (float) e.scaledResolution.scaledWidth() / 2F;
                float centerY = (float) e.scaledResolution.scaledHeight() / 2F;

                if (dynamic.getValue()) {
                    final float calculateCooldown = mc.player.getCooledAttackStrength(1.0F);
                    float targetOffset = 6 * (1 - calculateCooldown);
                    float animationSpeed = getWeaponAnimationSpeed();
                    dynamicOffset = AnimationMath.lerp(prevDynamicOffset, targetOffset, animationSpeed);
                    prevDynamicOffset = dynamicOffset;
                } else {
                    dynamicOffset = 0;
                    prevDynamicOffset = 0;
                }

                drawRegularCrosshair(centerX, centerY);
            }
        }
        return false;
    }

    private float getWeaponAnimationSpeed() {
        if (mc.player == null || mc.player.getHeldItemMainhand().isEmpty()) return 8;

        Item item = mc.player.getHeldItemMainhand().getItem();
        if (item instanceof SwordItem) return 12;
        if (item instanceof AxeItem) return 5;
        if (item instanceof ShovelItem) return 10;
        return 8;
    }

    private boolean isLookingAtEntity() {
        if (mc.player == null || mc.world == null) return false;
        Entity target = mc.pointedEntity;
        return target != null && target != mc.player && target instanceof LivingEntity;
    }

    private void drawRegularCrosshair(float centerX, float centerY) {
        float len = length.getValue().floatValue();
        float gapSize = gap.getValue().floatValue() + dynamicOffset;
        float thick = thickness.getValue().floatValue();

        int color = getCurrentColor();

        if (outline.getValue()) {
            int outlineColor = ColorUtils.getColor(0, 0, 0, 180);
            drawCrosshairLines(centerX, centerY, len + 1, gapSize - 0.5f, thick + 1, outlineColor);
        }

        drawCrosshairLines(centerX, centerY, len, gapSize, thick, color);

        if (centerDot.getValue()) {
            float pixelX = Math.round(centerX);
            float pixelY = Math.round(centerY);
            float dotSize = Math.max(1, thick * 0.8f);
            
            if (outline.getValue()) {
                int outlineColor = ColorUtils.getColor(0, 0, 0, 180);
                float outlineSize = dotSize + 1;
                RenderUtils.Render2D.drawRect(pixelX - outlineSize/2, pixelY - outlineSize/2, outlineSize, outlineSize, outlineColor);
            }
            
            RenderUtils.Render2D.drawRect(pixelX - dotSize/2, pixelY - dotSize/2, dotSize, dotSize, color);
        }
    }

    private void drawCrosshairLines(float centerX, float centerY, float length, float gap, float thickness, int color) {
        float pixelX = Math.round(centerX);
        float pixelY = Math.round(centerY);

        float halfThickness = thickness / 2;
        float startX = pixelX - halfThickness;
        float startY = pixelY - halfThickness;

        RenderUtils.Render2D.drawRect(startX, pixelY - gap - length, thickness, length, color);
        RenderUtils.Render2D.drawRect(startX, pixelY + gap, thickness, length, color);
        RenderUtils.Render2D.drawRect(pixelX - gap - length, startY, length, thickness, color);
        RenderUtils.Render2D.drawRect(pixelX + gap, startY, length, thickness, color);
    }

    private int getCurrentColor() {
        if (targetIndicator.getValue() && isLookingAtEntity()) {
            return ColorUtils.getColor(255, 0, 0, 255);
        }
        return ColorUtils.getColor(255, 255, 255, 255);
    }
}