package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.font.Fonts;
import nuclear.utils.math.MathUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;

import static nuclear.ui.clickgui.Panel.getColorByName;
import static org.joml.Math.lerp;

public class SliderObject extends Object {

    public ModuleObject object;
    public SliderSetting set;
    public boolean sliding;

    public float animatedVal;
    public float animatedThumbX;
    private float slider = 0;
    private boolean firstRender = true;

    public SliderObject(ModuleObject object, SliderSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;
        super.draw(stack, mouseX, mouseY);

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        x -= 2f;
        y += 2f;
        width += 4.5f;
        height -= 2.5f;

        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), (int)(255 * windowAlpha));
        int fonColor2 = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int)(30 * windowAlpha));
        int primaryColor = ColorUtils.setAlpha(getColorByName("primaryColor"), (int)(200 * windowAlpha));
        int thumbColor = ColorUtils.rgba(225, 226, 226, (int)(255 * windowAlpha));
        int backgroundSliderColor = ColorUtils.rgba(255, 255, 255, (int)(15 * windowAlpha));

        if (sliding && windowAlpha > 0.5f) {
            float value = (float) ((mouseX - (x + 10 + 3 + 1.75f)) / (width - 30 - 4) * (set.getMax() - set.getMin()) + set.getMin());
            value = MathUtil.round(value, set.getIncrement());
            set.setValue(value);
        }

        float sliderWidth = ((set.getValue().floatValue() - set.getMin()) / (set.getMax() - set.getMin())) * (width - 27 - 4) + 3f;

        if (firstRender) {
            animatedVal = sliderWidth;
            animatedThumbX = x + 10 + 3 + 1.75f + animatedVal;
            firstRender = false;
        } else {
            animatedVal = AnimationMath.fast(animatedVal, sliderWidth, 11);
            float targetThumbX = x + 10 + 3 + 1.75f + animatedVal;
            animatedThumbX = AnimationMath.fast(animatedThumbX, targetThumbX, 11);
        }

        RenderUtils.Render2D.drawRoundedRect(x + 10 + 3 + 1.75f, y + height / 2f + 2.5f, width - 34 + 9 - 4, 2.5f, 0.5f, backgroundSliderColor);

        RenderUtils.Render2D.drawRoundedRect(x + 10 + 3 + 1.75f, y + height / 2f + 2.5f, animatedVal - 5 + 2, 2.5f, 0.5f, primaryColor);

        RenderUtils.Render2D.drawRoundedRect(x + 10 + 3 + 1.75f + animatedVal - 5 + 1 - 0.5f, y + height / 2f + 2.5f - 0.5f, 3.5f, 3.5f, 1.25f, thumbColor);

        float currentDisplayValue = set.getValue().floatValue();
        slider = lerp(slider, currentDisplayValue, 0.15f);

        Fonts.newcode[12].drawScissorString(stack, set.getName(), x + 14.5f, y + height / 2f - 4, textColor, 42);

        String valueStr = String.valueOf(currentDisplayValue);
        float valueWidth = Fonts.newcode[11].getWidth(valueStr);
        RenderUtils.Render2D.drawRoundedRect(x + width - valueWidth - 17 - 1, y + height / 2f - 6.5f, valueWidth + 4, 7.5f, 1.5f, fonColor2);

        Fonts.newcode[11].drawString(stack, valueStr, x + width - 15f - 1 - valueWidth, y + height / 2f - 3.5f, ColorUtils.setAlpha(textColor, (int)(220 * windowAlpha)));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (nuclear.ui.clickgui.Window.alphaAnimation.getOutput() < 0.5f) return;

        if (!setting.visible() || !object.module.expanded) return;

        if (isHovered(mouseX, mouseY - 4)) {
            if (mouseButton == 0) {
                sliding = true;
            } else if (mouseButton == 2) {
                set.resetToDefault();
            }
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {
    }

    @Override
    public void exit() {
        super.exit();
        sliding = false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        sliding = false;
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }
}