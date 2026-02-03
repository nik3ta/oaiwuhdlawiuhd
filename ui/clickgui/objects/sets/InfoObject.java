package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.settings.imp.InfoSetting;
import nuclear.ui.clickgui.Panel;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;

public class InfoObject extends Object {

    public InfoSetting set;
    public ModuleObject object;

    public InfoObject(ModuleObject object, InfoSetting set) {
        this.object = object;
        this.set = set;
        setting = set;
    }

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();

        y -= 4f;
        height -= 6;
        x -= 4;
        width += 12f;
        super.draw(stack, mouseX, mouseY);

        int infoColor = Panel.getColorByName("infoColor");
        int finalColor = ColorUtils.setAlpha(infoColor, (int) (180 * windowAlpha));

        Fonts.newcode[14].drawCenteredString(stack, set.getName(), x + 52, y + 5, finalColor);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
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