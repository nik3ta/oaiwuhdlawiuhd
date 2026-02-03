package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.control.Manager;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.ui.clickgui.Panel;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.SoundUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.font.styled.StyledFont;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiObject extends Object {

    public MultiBoxSetting set;
    public ModuleObject object;

    private final List<Float> optionAnimations = new ArrayList<>();
    private boolean firstRender = true;

    public MultiObject(ModuleObject object, MultiBoxSetting set) {
        this.object = object;
        this.set = set;
        setting = set;

        for (int i = 0; i < set.options.size(); i++) {
            optionAnimations.add(set.get(i) ? 8.5f : 0f);
        }
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        super.draw(stack, mouseX, mouseY);
        int offsetY = 0;
        y += 3;
        x -= 3;
        height += 3f;

        int textColor = Panel.getColorByName("textColor");
        int iconnoColor = Panel.getColorByName("iconnoColor");

        int totalOptions = set.options.size();
        int enabledOptions = 0;
        for (BooleanSetting option : set.options) {
            if (option.get()) enabledOptions++;
        }

        String enabledPart = String.valueOf(enabledOptions);
        String separator = "/";
        String totalPart = String.valueOf(totalOptions);

        StyledFont fontmono = Fonts.newcode[11];
        float enabledWidth = fontmono.getWidth(enabledPart);
        float separatorWidth = fontmono.getWidth(separator);
        float totalWidth = fontmono.getWidth(totalPart);
        float totalTitleWidth = enabledWidth + separatorWidth + totalWidth;

        float baseX = x + 86f - totalTitleWidth;
        float baseY = y + height / 2f - 6.5f;

        fontmono.drawString(stack, enabledPart, baseX, baseY, ColorUtils.setAlpha(textColor, (int) (220 * windowAlpha)));
        fontmono.drawString(stack, separator, baseX + enabledWidth, baseY, ColorUtils.setAlpha(textColor, (int) (122 * windowAlpha)));
        fontmono.drawString(stack, totalPart, baseX + enabledWidth + separatorWidth, baseY, ColorUtils.setAlpha(textColor, (int) (220 * windowAlpha)));

        Fonts.newcode[12].drawString(stack, set.getName(), x + 15.5f, y + height / 2f - 7f, ColorUtils.setAlpha(textColor, (int) (255 * windowAlpha)));

        height += 8;
        float maxWidth = 71.5f;
        float currentX = x + 17f;
        float currentY = y + 12f;
        float lineHeight = 11.2f;

        float tempX = x + 17f;
        int lineCount = 1;
        for (BooleanSetting mode : set.options) {
            float textWidth = Fonts.blod[11].getWidth(mode.getName());
            float paddedWidth = textWidth + 5.0f;
            if (tempX + paddedWidth > x + 17f + maxWidth) {
                tempX = x + 17f;
                lineCount++;
            }
            tempX += paddedWidth;
        }

        float rectHeight = lineHeight * lineCount + 6f;

        int i = 0;
        currentX = x + 17f;
        currentY = y + 12f;

        for (BooleanSetting mode : set.options) {
            String modeName = mode.getName();
            float textWidth = Fonts.blod[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (currentX + paddedWidth > x + 17f + maxWidth) {
                currentX = x + 17f;
                currentY += lineHeight;
            }

            boolean isEnabled = set.get(i);
            float target = isEnabled ? 8.5f : 0f;
            float currentAnim;
            if (firstRender) {
                currentAnim = target;
                optionAnimations.set(i, target);
            } else {
                currentAnim = AnimationMath.fast(optionAnimations.get(i), target, 12f);
                optionAnimations.set(i, currentAnim);
            }

            float itemAlphaFactor = (currentAnim / 8.5f);

            int backgroundAlpha = (int) ((10 + (20 * itemAlphaFactor)) * windowAlpha);
            int bgColor = ColorUtils.setAlpha(iconnoColor, backgroundAlpha);
            RenderUtils.Render2D.drawRoundedRect(currentX - 2f, currentY - 4.5f, textWidth + 4, 10, 1.5f, bgColor);

            int textAlpha = (int) ((150 + (105 * itemAlphaFactor)) * windowAlpha);
            int txtColor = ColorUtils.setAlpha(textColor, textAlpha);
            Fonts.blod[11].drawString(stack, modeName, currentX, currentY - 0.5f, txtColor);

            currentX += paddedWidth;
            i++;
        }

        if (firstRender) firstRender = false;

        height += rectHeight - 21.5f;
        y -= 10;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (nuclear.ui.clickgui.Window.alphaAnimation.getOutput() < 0.5f) return;

        if (!object.module.expanded) return;

        float currentX = x + 17f;
        float currentY = y + 23f;
        float maxWidth = 71.5f;
        float lineHeight = 11.2f;
        int i = 0;

        for (BooleanSetting mode : set.options) {
            String modeName = mode.getName();
            float textWidth = Fonts.blod[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (currentX + paddedWidth > x + 17f + maxWidth) {
                currentX = x + 17f;
                currentY += lineHeight;
            }

            if (RenderUtils.isInRegion(mouseX, mouseY, currentX - 2f, currentY - 4.5f, textWidth + 4f, 10f)) {
                if (mouseButton == 0) {
                    set.set(i, !set.get(i));
                    if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                        int volume = new Random().nextInt(13) + 59;
                        SoundUtils.playSound("select.wav", volume, false);
                    }
                }
            }
            currentX += paddedWidth;
            i++;
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {}
    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {}
    @Override
    public void charTyped(char codePoint, int modifiers) {}
}