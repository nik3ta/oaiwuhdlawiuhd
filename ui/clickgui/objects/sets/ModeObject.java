package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.control.Manager;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.SoundUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;

import java.util.Random;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class ModeObject extends Object {

    public ModeSetting set;
    public ModuleObject object;

    private float expandAnimation = 0f;
    private boolean firstRender = true;

    public ModeObject(ModuleObject object, ModeSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        super.draw(stack, mouseX, mouseY);
        y += 3;
        x -= 3;
        height += 3f;

        int textColorBase = getColorByName("textColor");
        int iconnoColor = getColorByName("iconnoColor");

        int currentIndex = set.getIndex();

        int nameTextAlpha = (int) ((150 + 105 * (expandAnimation / 8.5f)) * windowAlpha);
        int nameTextColor = ColorUtils.setAlpha(textColorBase, nameTextAlpha);
        Fonts.newcode[12].drawString(stack, set.getName(), x + 15.5f, y + height / 2f - 7f, nameTextColor);

        height += 8;

        float target = object.module.expanded ? 8.5f : 0f;

        if (firstRender) {
            expandAnimation = target;
            firstRender = false;
        } else {
            expandAnimation = AnimationMath.fast(expandAnimation, target, 12f);
        }

        if (expandAnimation < 0.01f) {
            height += 6f - 21.5f;
            y -= 10;
            return;
        }

        float maxWidth = 71.5f;
        float currentX = x + 17f;
        float currentY = y + 12f;
        float lineHeight = 11.2f;

        float tempX = x + 17f;
        int lineCount = 1;
        for (String mode : set.modes) {
            float textWidth = Fonts.blod[11].getWidth(mode);
            float paddedWidth = textWidth + 5.0f;
            if (tempX + paddedWidth > x + 17f + maxWidth) {
                tempX = x + 17f;
                lineCount++;
            }
            tempX += paddedWidth;
        }

        float rectHeight = lineHeight * lineCount + 6f;

        float combinedAlpha = (expandAnimation / 8.5f) * windowAlpha;

        currentX = x + 17f;
        currentY = y + 12f;

        for (int i = 0; i < set.modes.length; i++) {
            String modeName = set.modes[i];
            float textWidth = Fonts.blod[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (currentX + paddedWidth > x + 17f + maxWidth) {
                currentX = x + 17f;
                currentY += lineHeight;
            }

            int baseBgAlpha = i == currentIndex ? 30 : 10;
            int modeBgColor = ColorUtils.setAlpha(iconnoColor, (int) (baseBgAlpha * combinedAlpha));

            RenderUtils.Render2D.drawRoundedRect(currentX - 2f, currentY - 4.5f, textWidth + 4, 10, 1.5f, modeBgColor);

            int baseTxtAlpha = i == currentIndex ? 255 : 150;
            int modeTxtColor = ColorUtils.setAlpha(textColorBase, (int) (baseTxtAlpha * windowAlpha));

            Fonts.blod[11].drawString(stack, modeName, currentX, currentY - 0.5f, modeTxtColor);

            currentX += paddedWidth;
        }

        height += rectHeight - 21.5f;
        y -= 10;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (nuclear.ui.clickgui.Window.alphaAnimation.getOutput() < 0.5f) return;

        if (!object.module.expanded) return;
        if (mouseButton != 0) return;
        if (expandAnimation < 3f) return;

        float currentX = x + 17f;
        float currentY = y + 23f;
        float maxWidth = 71.5f;
        float lineHeight = 11.2f;

        for (int i = 0; i < set.modes.length; i++) {
            String modeName = set.modes[i];
            float textWidth = Fonts.blod[11].getWidth(modeName);
            float paddedWidth = textWidth + 5.0f;

            if (currentX + paddedWidth > x + 17f + maxWidth) {
                currentX = x + 17f;
                currentY += lineHeight;
            }

            if (RenderUtils.isInRegion(mouseX, mouseY, currentX - 2f, currentY - 4.5f, textWidth + 4f, 10f)) {
                set.setIndex(i);
                if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                    int volume = new Random().nextInt(13) + 59;
                    SoundUtils.playSound("select.wav", volume, false);
                }
                return;
            }
            currentX += paddedWidth;
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