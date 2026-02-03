package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.settings.imp.ButtonSetting;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class ButtonObject extends Object {

    public ButtonSetting set;
    public ModuleObject object;

    public ButtonObject(ModuleObject object, ButtonSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;
        super.draw(stack, mouseX, mouseY);

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (150 * windowAlpha));
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), (int) (255 * windowAlpha));

        float wwidth = 72.25f;
        y -= 4;
        height -= 4;

        RenderUtils.Render2D.drawRoundedRect(x + 12, y + 1, wwidth, 12, 2f, fonColor);

        Fonts.newcode[14].drawCenteredString(stack, set.getName(),
                x + (Fonts.newcode[14].getWidth(set.getName()) / 2) + 33,
                y + 5f, textColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (nuclear.ui.clickgui.Window.alphaAnimation.getOutput() < 0.5) return;

        if (object.module.expanded) {
            if (RenderUtils.isInRegion(mouseX, mouseY, x + 12, y + 1, 72.25f, 12)) {
                set.getRun().run();
            }
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