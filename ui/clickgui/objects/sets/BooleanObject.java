package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.control.Manager;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.ui.clickgui.Panel;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.SoundUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;
import org.joml.Vector4i;

import java.util.Random;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class BooleanObject extends Object {

    public ModuleObject object;
    public BooleanSetting set;
    public float enabledAnimation;
    private boolean firstRender = true;

    public BooleanObject(ModuleObject object, BooleanSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        super.draw(stack, mouseX, mouseY);

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        int iconnoColor = Panel.getColorByName("iconnoColor");
        double max = set.get() ? 8.5f : 0;

        y -= 4.5f;
        height -= 5;

        if (firstRender) {
            this.enabledAnimation = (float) max;
            firstRender = false;
        } else {
            this.enabledAnimation = AnimationMath.fast(enabledAnimation, (float) max, 12);
        }

        int textColor = getColorByName("textColor");
        int colorfont = ColorUtils.interpolateColor(ColorUtils.setAlpha(textColor, 150), textColor, enabledAnimation / 8.5f);
        colorfont = ColorUtils.setAlpha(colorfont, (int) (ColorUtils.getAlpha(colorfont) * windowAlpha));

        Fonts.newcode[12].drawScissorString(stack, set.getName(), x + 12.5f, y + 7f, colorfont, 54);

        int enabledColor = getColorByName("yesColor");
        int disabledColor = getColorByName("crossColor");

        int backgroundAlpha = (int)(20 * windowAlpha);
        int backgroundColor = ColorUtils.interpolateColor(
                ColorUtils.setAlpha(disabledColor, backgroundAlpha),
                ColorUtils.setAlpha(enabledColor, backgroundAlpha),
                enabledAnimation / 8.5f
        );

        RenderUtils.Render2D.drawRoundedRect(x + 75.25f, y + 3.5f, 8.5f, 8.5f, 2f, backgroundColor);

        RenderUtils.Render2D.drawRoundOutline(x + 75.25f, y + 3.5f, 8.5f, 8.5f, 2f, -0.5f, ColorUtils.rgba(25, 26, 33, 0),
                new Vector4i(backgroundColor, backgroundColor, backgroundColor, backgroundColor));

        int iconColor = ColorUtils.interpolateColor(disabledColor, enabledColor, enabledAnimation / 8.5f);
        float baseOpacity = set.get() ? 0.75f : 0.65f;

        int finalIconColor = ColorUtils.setAlpha(iconColor, (int) (255 * baseOpacity * windowAlpha));

        Fonts.icon[11].drawString(stack, set.get() ? "2" : "1", x + 76.7f, y + 7.5f, finalIconColor);
    }
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (object.module.expanded) {
            if (mouseButton == 0) {
                if (isHovered(mouseX, mouseY - 5)) {
                    set.toggle();
                    int volume = new Random().nextInt(13) + 59;
                    if (Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                        SoundUtils.playSound("select.wav", volume, false);
                    }
                }
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

    }

    @Override
    public void charTyped(char codePoint, int modifiers) {

    }


}
