package nuclear.ui.clickgui.objects.sets;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.settings.imp.ColorSetting;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.StencilUtils;
import nuclear.utils.render.animation.AnimationMath;
import org.joml.Vector4i;

import java.awt.*;

import static nuclear.utils.IMinecraft.mc;

public class ColorObject extends Object {
    public ModuleObject object;
    public ColorSetting setting;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;
    private boolean colorSelectorDragging;
    private boolean hueSelectorDragging;
    private boolean alphaSelectorDragging;
    private float copyButtonHoverAnim;
    private float copyButtonClickAnim;
    private float pasteButtonHoverAnim;
    private float pasteButtonClickAnim;
    private float expandAnimation;
    private boolean firstRender = true;

    public ColorObject(ModuleObject object, ColorSetting setting) {
        height = 11;
        this.object = object;
        this.setting = setting;
        float[] hsb = RGBtoHSB(setting.get());
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = (setting.get() >> 24 & 0xFF) / 255F;
        this.copyButtonHoverAnim = 0.0f;
        this.copyButtonClickAnim = 0.0f;
        this.pasteButtonHoverAnim = 0.0f;
        this.pasteButtonClickAnim = 0.0f;
        this.expandAnimation = 0.0f;
    }

    protected boolean extended;

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        if (!setting.visible()) return;

        // Получаем прозрачность из твоего Window
        float windowAlpha = (float) nuclear.ui.clickgui.Window.alphaAnimation.getOutput();
        if (windowAlpha <= 0.001f) return;

        y += 1f;
        width += 7.5f;

        if (firstRender) {
            this.expandAnimation = extended ? 1.0f : 0.0f;
            firstRender = false;
        } else {
            this.expandAnimation = AnimationMath.ultraSmooth(expandAnimation, extended ? 1.0f : 0.0f, 12.0f);
        }

        height = 5F + (95F - 6F) * expandAnimation;

        // Применяем windowAlpha ко всем базовым цветам
        int textColor = ColorUtils.setAlpha(nuclear.ui.clickgui.Panel.getColorByName("textColor"), (int) (255 * windowAlpha));
        int iconnoColor = ColorUtils.setAlpha(nuclear.ui.clickgui.Panel.getColorByName("iconnoColor"), (int) (30 * windowAlpha));
        int settingPreviewColor = ColorUtils.setAlpha(setting.get(), (int) (200 * windowAlpha));

        Fonts.newcode[12].drawScissorString(stack, setting.getName(), x + 12.5f, y + 1.5f, textColor, 55);
        RenderUtils.Render2D.drawRoundedRect(x + width - 24.5f, y + 0.5f, 5f, 5f, 2F, settingPreviewColor);

        if (expandAnimation > 0.01f) {
            float combinedAlpha = windowAlpha * expandAnimation;
            float animatedHeight = 87f * expandAnimation;

            StencilUtils.initStencilToWrite();
            RenderUtils.Render2D.drawRoundedRect(x + 12, y + 9, 71.5f, animatedHeight, 3F, ColorUtils.setAlpha(iconnoColor, (int) (30 * combinedAlpha)));
            StencilUtils.readStencilBuffer(1);

            RenderUtils.Render2D.drawRoundedRect(x + 12, y + 9, 71.5f, 87f, 3F, ColorUtils.setAlpha(iconnoColor, (int) (30 * combinedAlpha)));

            if (colorSelectorDragging && RenderUtils.isInRegion(mouseX, mouseY, x + 15, y + 12.5f, 65, 54)) {
                saturation = Math.max(0, Math.min(1, (mouseX - (x + 15)) / 65));
                brightness = Math.max(0, Math.min(1, 1 - (mouseY - (y + 12.5f)) / 54));
                setting.setValue(ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(hue, saturation, brightness)), alpha).getRGB());
            }

            int selectorAlphaInt = (int) (255 * combinedAlpha);

            // Палитра
            RenderUtils.Render2D.drawGradientRoundedRect(stack, x + 15, y + 12.5f, 65, 54, 3.5f,
                    ColorUtils.setAlpha(-1, selectorAlphaInt),
                    ColorUtils.setAlpha(Color.BLACK.getRGB(), selectorAlphaInt),
                    ColorUtils.setAlpha(Color.HSBtoRGB(hue, 1, 1), selectorAlphaInt),
                    ColorUtils.setAlpha(Color.BLACK.getRGB(), selectorAlphaInt));

            float selectorX = Math.max(x + 15, Math.min(x + 15 + 65 - 4, x + 15 + saturation * 65));
            float selectorY = Math.max(y + 12.5f, Math.min(y + 12.5f + 54 - 4, y + 12.5f + (1 - brightness) * 54));
            RenderUtils.Render2D.drawCircle2(selectorX, selectorY, 2.25f, ColorUtils.rgba(255, 255, 255, selectorAlphaInt));

            // Hue slider
            if (hueSelectorDragging && RenderUtils.isInRegion(mouseX, mouseY, x + 15, y + 68, 65, 4.5f)) {
                hue = Math.max(0, Math.min(1, (mouseX - (x + 15)) / 65));
                setting.setValue(ColorUtils.applyOpacity(new Color(Color.HSBtoRGB(hue, saturation, brightness)), alpha).getRGB());
            }

            float times = 5;
            float size = 65f / times;
            float sliderX = x + 15;
            float sliderY = y + 68;

            for (int i = 0; i < times; i++) {
                int color1 = ColorUtils.setAlpha(Color.HSBtoRGB(0.2F * i, 1, 1), selectorAlphaInt);
                int color2 = ColorUtils.setAlpha(Color.HSBtoRGB(0.2F * (i + 1), 1, 1), selectorAlphaInt);
                if (i == 0) RenderUtils.Render2D.drawCustomGradientRoundedRect(stack, sliderX, sliderY, size + 0.4f, 4.5f, 4, 4, 0, 0, color1, color1, color2, color2);
                else if (i == times - 1) RenderUtils.Render2D.drawCustomGradientRoundedRect(stack, sliderX, sliderY, size, 4.5f, 0, 0, 4, 4, color1, color1, color2, color2);
                else RenderUtils.Render2D.drawGradientRectCustom(stack, sliderX + 0.001f, sliderY, size + 0.4f, 4.5f, color1, color1, color2, color2);
                sliderX += size;
            }

            // Тот самый белый кружок (обводка) на Hue
            float hueSelectorX = Math.max(0, Math.min(65 - 4.5f, hue * 65));
            int whiteOutline = ColorUtils.rgba(255, 255, 255, selectorAlphaInt);
            RenderUtils.Render2D.drawRoundOutline(x + 15 + hueSelectorX, sliderY, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(whiteOutline, whiteOutline, whiteOutline, whiteOutline));

            // Alpha slider
            int color = Color.HSBtoRGB(hue, saturation, brightness);
            if (alphaSelectorDragging && RenderUtils.isInRegion(mouseX, mouseY, x + 15, y + 74.5f, 65, 4.5f)) {
                alpha = Math.max(0, Math.min(1, (mouseX - (x + 15)) / 65));
                setting.setValue(ColorUtils.applyOpacity(new Color(color), alpha).getRGB());
            }

            int alphaColor = ColorUtils.setAlpha(color, selectorAlphaInt);
            RenderUtils.Render2D.drawGradientRoundedRect(stack, x + 15, y + 74.5f, 65, 4.5f, 2, -1, -1, alphaColor, alphaColor);

            float alphaSelectorX = Math.max(0, Math.min(65 - 4.5f, alpha * 65));
            RenderUtils.Render2D.drawRoundOutline(x + 15 + alphaSelectorX, y + 74.5f, 4.5f, 4.5f, 2f, 0.4f, ColorUtils.rgba(25, 26, 33, 0),
                    new Vector4i(whiteOutline, whiteOutline, whiteOutline, whiteOutline));

            // Кнопки Copy и Paste
            drawButtons(stack, mouseX, mouseY, combinedAlpha, textColor);

            StencilUtils.uninitStencilBuffer();
        }
    }

    private void drawButtons(MatrixStack stack, int mouseX, int mouseY, float combinedAlpha, int textColor) {
        float copyButtonX = x + 15f;
        float copyButtonY = y + 81f;
        float pasteButtonX = x + 16f + 32;

        copyButtonHoverAnim = AnimationMath.fast(copyButtonHoverAnim, RenderUtils.isInRegion(mouseX, mouseY, copyButtonX, copyButtonY, 32, 11) ? 1.0f : 0.0f, 12.0f);
        pasteButtonHoverAnim = AnimationMath.fast(pasteButtonHoverAnim, RenderUtils.isInRegion(mouseX, mouseY, pasteButtonX, copyButtonY, 32, 11) ? 1.0f : 0.0f, 12.0f);

        int fonColorButton = ColorUtils.setAlpha(nuclear.ui.clickgui.Panel.getColorByName("fonColor"), (int) (150 * combinedAlpha));
        int primaryColor = ColorUtils.setAlpha(nuclear.ui.clickgui.Panel.getColorByName("primaryColor"), (int) (100 * combinedAlpha));

        int finalCopyColor = ColorUtils.interpolateColor(fonColorButton, primaryColor, copyButtonHoverAnim);
        int finalPasteColor = ColorUtils.interpolateColor(fonColorButton, primaryColor, pasteButtonHoverAnim);

        RenderUtils.Render2D.drawRoundedRect(copyButtonX, copyButtonY, 32, 11, 2f, finalCopyColor);
        RenderUtils.Render2D.drawRoundedRect(pasteButtonX, copyButtonY, 32, 11, 2f, finalPasteColor);

        int tAlphaC = (int) ((180 + 75 * copyButtonHoverAnim) * combinedAlpha);
        int tAlphaP = (int) ((180 + 75 * pasteButtonHoverAnim) * combinedAlpha);

        Fonts.newcode[12].drawCenteredString(stack, "Копир", x + 32f, y + 85.5f, ColorUtils.setAlpha(textColor, tAlphaC));
        Fonts.newcode[12].drawCenteredString(stack, "Встав", x + 64f, y + 85.5f, ColorUtils.setAlpha(textColor, tAlphaP));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (nuclear.ui.clickgui.Window.alphaAnimation.getOutput() < 0.5) return;
        if (!setting.visible()) return;

        float drawY = y + 1f;
        if (RenderUtils.isInRegion(mouseX, mouseY, x, drawY, width + 7.5f, 11) && mouseButton == 1) {
            extended = !extended;
        }

        if (expandAnimation > 0.5f) {
            if (mouseButton == 0) {
                if (RenderUtils.isInRegion(mouseX, mouseY, x + 15, drawY + 12.5f, 65, 54)) colorSelectorDragging = true;
                if (RenderUtils.isInRegion(mouseX, mouseY, x + 15, drawY + 68, 65, 4.5f)) hueSelectorDragging = true;
                if (RenderUtils.isInRegion(mouseX, mouseY, x + 15, drawY + 74.5f, 65, 4.5f)) alphaSelectorDragging = true;

                if (RenderUtils.isInRegion(mouseX, mouseY, x + 15, drawY + 81, 32, 11)) {
                    mc.keyboardListener.setClipboardString(colorToHex(setting.get()));
                }
                if (RenderUtils.isInRegion(mouseX, mouseY, x + 48, drawY + 81, 32, 11)) {
                    try {
                        String cb = mc.keyboardListener.getClipboardString();
                        int pastedColor = hexToColor(cb.trim());
                        if (pastedColor != -1) {
                            setting.setValue(pastedColor);
                            float[] hsb = RGBtoHSB(pastedColor);
                            this.hue = hsb[0]; this.saturation = hsb[1]; this.brightness = hsb[2];
                            this.alpha = ((pastedColor >> 24) & 0xFF) / 255.0f;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private float[] RGBtoHSB(int color) {
        return Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null);
    }

    private String colorToHex(int color) {
        int r = (color >> 16) & 0xFF; int g = (color >> 8) & 0xFF; int b = color & 0xFF; int a = (color >> 24) & 0xFF;
        return a == 255 ? String.format("#%02X%02X%02X", r, g, b) : String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    private int hexToColor(String hex) {
        try { return ColorUtils.hex(hex); } catch (Exception e) { return -1; }
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        colorSelectorDragging = hueSelectorDragging = alphaSelectorDragging = false;
    }
    @Override public void drawComponent(MatrixStack ms, int mx, int my) {}
    @Override public void keyTyped(int k, int s, int m) {}
    @Override public void charTyped(char c, int m) {}
}