package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.settings.imp.BindSetting;
import nuclear.ui.clickgui.Panel;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.ClientUtils;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.animations.DecelerateAnimation;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;

import java.awt.*;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class BindObject extends Object {

    public BindSetting set;
    public ModuleObject object;
    public boolean bind;
    private static final int MOUSE_BUTTON_3 = 3;
    private static final int MOUSE_BUTTON_4 = 4;
    private static final int MOUSE_BUTTON_RIGHT = 1;
    private static final int MOUSE_BUTTON_MIDDLE = 2;
    public boolean isBinding;

    public BindObject(ModuleObject object, BindSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        float alpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        y -= 6f;
        height -= 6;
        x -= 4;
        width += 8;

        int textColor = ColorUtils.setAlpha(Panel.getColorByName("textColor"), (int)(255 * alpha));
        int warnColor = ColorUtils.setAlpha(new Color(214, 67, 67).getRGB(), (int)(202 * alpha));
        int iconnoColor = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int)(10 * alpha));

        String bindString = bind ? "..." : (set.getKey() == 0 ? "n/a" : ClientUtils.getKey(set.getKey()));
        if (bindString == null) bindString = "";

        bindString = bindString.replace("MOUSE", "M")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("CONTROL", "C")
                .replace("SHIFT", "S")
                .replace("_", "");

        String shortBindString = bindString.substring(0, Math.min(bindString.length(), 4));
        shortBindString = shortBindString.equals("n/a") ? shortBindString : shortBindString.toUpperCase();

        boolean isDuplicate = isDuplicateBind(shortBindString);
        float widthtext = Math.max(5f, Fonts.blod[11].getWidth(shortBindString));

        Fonts.newcode[12].drawString(matrixStack, set.getName(), x + 16.5f, y + 8.5f, textColor);

        RenderUtils.Render2D.drawRoundedRect(x + width - 28 - 0.25f - widthtext + 11 - 1.8f,
                y + 5.5f, widthtext + 4, 9, 2f, iconnoColor);

        int bindKeyColor = isDuplicate ? ColorUtils.setAlpha(warnColor, (int)(184 * alpha))
                : ColorUtils.setAlpha(textColor, (int)(122 * alpha));

        Fonts.blod[11].drawCenteredString(matrixStack, shortBindString,
                x + width - 26 - 0.25f - (widthtext / 2) + 11 - 1.8f,
                y + 8.7f, bindKeyColor);
    }
    private boolean isDuplicateBind(String currentBind) {
        if (currentBind.equals("n/a")) {
            return false;
        }

        for (Object obj : object.object) {
            if (obj instanceof BindObject && obj != this && obj.setting.visible()) {
                BindObject otherBindObject = (BindObject) obj;
                String otherBindString = otherBindObject.set.getKey() == 0 ? "n/a" : ClientUtils.getKey(otherBindObject.set.getKey());
                if (otherBindString != null) {
                    otherBindString = otherBindString.replace("MOUSE", "M")
                            .replace("LEFT", "L")
                            .replace("RIGHT", "R")
                            .replace("CONTROL", "C")
                            .replace("SHIFT", "S")
                            .replace("_", "");
                    String otherShortBindString = otherBindString.substring(0, Math.min(otherBindString.length(), 4));
                    otherShortBindString = otherShortBindString.equals("n/a") ? otherShortBindString : otherShortBindString.toUpperCase();
                    if (currentBind.equals(otherShortBindString) && !otherShortBindString.equals("n/a")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!setting.visible() || !object.module.expanded) return;

        if (isHovered(mouseX, mouseY) && mouseButton == 0) {
            bind = true;
            isBinding = true;
        }

        if (bind) {
            if (mouseButton == MOUSE_BUTTON_RIGHT || mouseButton == MOUSE_BUTTON_MIDDLE ||
                    mouseButton == MOUSE_BUTTON_3 || mouseButton == MOUSE_BUTTON_4) {
                set.setKey(-100 + mouseButton);
                bind = false;
                isBinding = false;
            }
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (!setting.visible()) return;
        if (bind) {
            if (keyCode == 261 || keyCode == 259) {
                set.setKey(0);
                bind = false;
                isBinding = false;
                return;
            }

            set.setKey(keyCode);
            bind = false;
            isBinding = false;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
    }

    public boolean isHovered(int mouseX, int mouseY) {

        String bindString = bind ? "..." : (set.getKey() == 0 ? "n/a" : ClientUtils.getKey(set.getKey()));
        if (bindString == null) bindString = "";

        bindString = bindString.replace("MOUSE", "M")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("CONTROL", "C")
                .replace("SHIFT", "S")
                .replace("_", "");

        String shortBindString = bindString.substring(0, Math.min(bindString.length(), 4));
        shortBindString = shortBindString.equals("n/a") ? shortBindString : shortBindString.toUpperCase();

        float textWidth = Fonts.blod[11].getWidth(shortBindString);

        float textX = x + width - 25 - (textWidth / 2) + 11 - 1.8f;

        float padding = 8f;
        float left = textX - textWidth / 2 - padding;
        float right = textX + textWidth / 2 + padding;
        float top = y + 1;
        float bottom = y + 17;

        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }
}