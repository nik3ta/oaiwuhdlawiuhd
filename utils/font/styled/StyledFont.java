package nuclear.utils.font.styled;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import nuclear.utils.font.ReplaceUtil;
import nuclear.utils.font.common.AbstractFont;
import nuclear.utils.font.common.Lang;
import nuclear.utils.math.MathUtil;
import nuclear.utils.render.ColorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Locale;

public final class StyledFont extends AbstractFont {

    private final GlyphPage regular;

    public StyledFont(String fileName, int size, float stretching, float spacing, float lifting, boolean antialiasing, Lang lang) {
        int[] codes = lang.getCharCodes();
        char[] chars = new char[(codes[1] - codes[0] + codes[3] - codes[2])];

        int c = 0;
        for (int d = 0; d <= 2; d += 2) {
            for (int i = codes[d]; i <= codes[d + 1] - 1; i++) {
                chars[c] = (char) i;
                c++;
            }
        }

        this.regular = new GlyphPage(AbstractFont.getFont(fileName, Font.PLAIN, size), chars, stretching, spacing, lifting, antialiasing);
    }

    public StyledFont(String fileName, int size, float stretching, float spacing, float lifting, boolean antialiasing, Lang lang, boolean wind) {
        int[] codes = lang.getCharCodes();
        char[] chars = new char[(codes[1] - codes[0] + codes[3] - codes[2])];

        int c = 0;
        for (int d = 0; d <= 2; d += 2) {
            for (int i = codes[d]; i <= codes[d + 1] - 1; i++) {
                chars[c] = (char) i;
                c++;
            }
        }

        this.regular = new GlyphPage(AbstractFont.getFontWindows(fileName, Font.PLAIN, size), chars, stretching, spacing, lifting, antialiasing);
    }

    public float drawText(MatrixStack matrixStack, ITextComponent component, double x, double y) {
        return renderText(matrixStack, component, x, y, -1);
    }

    public float drawText(MatrixStack matrixStack, ITextComponent component, double x, double y, int color) {
        return renderText(matrixStack, component, x, y, color);
    }

    public float renderGlyph(Matrix4f matrix, char c, float x, float y, boolean bold, boolean italic, float red, float green, float blue, float alpha) {
        return getGlyphPage().renderGlyph(matrix, c, x, y, red, green, blue, alpha);
    }
    public float renderGlyph(final Matrix4f matrix, final char c, final float x, final float y, final float red, final float green, final float blue, final float alpha) {
        return this.getGlyphPage().renderGlyph(matrix, c, x, y, red, green, blue, alpha);
    }
    public void drawStringWithShadow(MatrixStack matrixStack, ITextComponent text, double x, double y, int color) {
        StyledFontRenderer.drawShadowedString(matrixStack, this, text, x, y, color);
    }

    public void drawString(MatrixStack matrixStack, String text, double x, double y, int color) {
        StyledFontRenderer.drawString(matrixStack, this, text, x, y, color);
    }

    public void drawStringTest(MatrixStack matrixStack, ITextComponent text, double x, double y, int color) {
        StyledFontRenderer.renderStringGradient(matrixStack, this, text, x, y, false, color);
    }
    public void drawScissorString(final MatrixStack matrixStack, final String text, final double x, final double y, final int color, final int width) {
        StyledFontRenderer.drawScissorString(matrixStack, this, text, x, y, color, width);
    }

    public void drawString(MatrixStack matrixStack, ITextComponent text, double x, double y, int color) {
        StyledFontRenderer.drawString(matrixStack, this, text, x, y, color);
    }

    public void drawStringWithShadow(MatrixStack matrixStack, String text, double x, double y, int color) {
        StyledFontRenderer.drawShadowedString(matrixStack, this, text, x, y, color);
    }

    public void drawCenteredString(MatrixStack matrixStack, String text, double x, double y, int color) {
        StyledFontRenderer.drawCenteredXString(matrixStack, this, text, x, y, color);
    }

    public void drawCenteredString(MatrixStack matrixStack, ITextComponent text, double x, double y, int color) {
        StyledFontRenderer.drawCenteredString(matrixStack, this, text, x, y, color);
    }

    public void drawStringWithOutline(MatrixStack stack, String text, double x, double y, int color) {
        Color c = new Color(0, 0, 0, 128);
        x = MathUtil.round((float) x, 0.5F);
        y = MathUtil.round((float) y, 0.5F);
        StyledFontRenderer.drawString(stack, this, text, x - 0.5, y, c.getRGB());
        StyledFontRenderer.drawString(stack, this, text, x + 0.5, y, c.getRGB());
        StyledFontRenderer.drawString(stack, this, text, x, y - 0.5f, c.getRGB());
        StyledFontRenderer.drawString(stack, this, text, x, y + 0.5f, c.getRGB());

        drawString(stack, text, x, y, color);
    }

    public float renderText(MatrixStack matrixStack, ITextComponent component, double x, double y, int defaultColor) {
        float startX = (float) x;
        final float[] posX = new float[]{startX};
        IReorderingProcessor processor = component.func_241878_f();
        processor.accept((index, style, codePoint) -> {
            net.minecraft.util.text.Color styleColor = style.getColor();
            int color = defaultColor;
            if (styleColor != null) {
                int rgb = styleColor.getColor();
                int alpha = (defaultColor >> 24) & 0xFF;
                color = (alpha << 24) | (rgb & 0x00FFFFFF);
            }
            String str = ReplaceUtil.replaceCustomFonts(new String(Character.toChars(codePoint)));
            float advance = renderString(matrixStack, this, str, posX[0], y, color);
            posX[0] += advance;
            return true;
        });
        return posX[0] - startX;
    }

    private static float renderString(MatrixStack matrices, StyledFont font, String text, double x, double y, int color) {
        y -= 3;
        GL11.glColor4f(1, 1, 1, 1);
        float startPos = (float) x * 2.0f;
        float posX = startPos;
        float posY = (float) y * 2.0f;
        float[] rgb = ColorUtils.rgba(color);
        float red = Math.min(rgb[0] + 0.05f, 1.0f);
        float green = Math.min(rgb[1] + 0.05f, 1.0f);
        float blue = Math.min(rgb[2] + 0.05f, 1.0f);
        float alpha = rgb[3];
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        font.bindTex();
        matrices.push();
        matrices.scale(0.5f, 0.5f, 1f);
        Matrix4f matrix = matrices.getLast().getMatrix();
        int length = text.length();
        String upperText = text.toUpperCase(Locale.ENGLISH);
        boolean inPlayer = upperText.contains("PLAYER");
        boolean inHero = upperText.contains("HERO");
        boolean inImperaotr = upperText.contains("IMPERATOR");
        boolean inDragon = upperText.contains("DRAGON");
        boolean inBull = upperText.contains("BULL");
        boolean inTitan = upperText.contains("TITAN");
        boolean inAvenger = upperText.contains("AVENGER");
        boolean inOverlord = upperText.contains("OVERLORD");
        boolean inMagister = upperText.contains("MAGISTER");
        boolean inCobra = upperText.contains("COBRA");
        boolean inHydra = upperText.contains("HYDRA");
        boolean inRabbit = upperText.contains("RABBIT");
        boolean inGod = upperText.contains("GOD");
        boolean inDhelper = upperText.contains("D.HELPER");
        boolean inHelper = upperText.contains("HELPER");
        boolean inTiger = upperText.contains("TIGER");
        boolean inBunny = upperText.contains("BUNNY");
        boolean inDracula = upperText.contains("DRACULA");
        boolean inVampire = upperText.contains("VAMPIRE");
        boolean inMlModer = upperText.contains("ML.MODER");
        boolean inModer = upperText.contains("MODER");
        boolean inStModer = upperText.contains("ST.MODER");
        boolean inDStModer = upperText.contains("D.ST.MODER");
        boolean inGlModer = upperText.contains("GL.MODER");
        boolean inModerPlus = upperText.contains("MODER+");
        boolean inYt = upperText.contains("YT");
        boolean inMedia = upperText.contains("MEDIA");
        boolean inAdmin = upperText.contains("ADMIN");
        boolean inMlAdmin = upperText.contains("ML.ADMIN");
        boolean inSponsor = upperText.contains("SPONSOR");
        boolean inCat = upperText.contains("КОШКА");
        boolean inBravo = upperText.contains("BRAVO");
        boolean inPegas = upperText.contains("PEGAS");

        for (int i = 0; i < length; i++) {
            char c0 = text.charAt(i);
            if (inPlayer && i + 6 <= length && upperText.substring(i, i + 6).equals("PLAYER")) {
                float endRed = 0.45f;
                float endGreen = 0.45f;
                float endBlue = 0.45f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 6 && i + j < length; j++) {
                    float t = j < 5 ? (float) j / 4.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 5;
                continue;
            }
            else if (inBravo && i + 5 <= length && upperText.substring(i, i + 5).equals("BRAVO")) {
                float endRed = 0.7f;
                float endGreen = 0.1f;
                float endBlue = 0.1f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inSponsor && i + 7 <= length && upperText.substring(i, i + 7).equals("SPONSOR")) {
                float endRed = 0.9f;
                float endGreen = 0.7f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 7 && i + j < length; j++) {
                    float t = j < 6 ? (float) j / 5.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 6;
                continue;
            }
            else if (inPegas && i + 5 <= length && upperText.substring(i, i + 5).equals("PEGAS")) {
                float endRed = 0.9f;
                float endGreen = 0.7f;
                float endBlue = 0.1f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 5.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inCat && i + 5 <= length && upperText.substring(i, i + 5).equals("КОШКА")) {
                float endRed = 40 / 255.0f;
                float endGreen = 130 / 255.0f;
                float endBlue = 215 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inGod && i + 3 <= length && upperText.substring(i, i + 3).equals("GOD")) {
                float endRed = 0.95f;    // Darker pale yellow: red
                float endGreen = 0.85f;  // Darker pale yellow: green
                float endBlue = 0.5f;   // Darker pale yellow: blue
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 3 && i + j < length; j++) {
                    float t = j < 2 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 2;
                continue;
            }
            else if (inHero && i + 4 <= length && upperText.substring(i, i + 4).equals("HERO")) {
                float endRed = 34 / 255.0f;
                float endGreen = 60 / 255.0f;
                float endBlue = 227 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 4 && i + j < length; j++) {
                    float t = j < 3 ? (float) j / 2.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 3;
                continue;
            }
            else if (inBull && i + 4 <= length && upperText.substring(i, i + 4).equals("BULL")) {
                float endRed = 0.7f;
                float endGreen = 0.15f;
                float endBlue = 0.7f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 4 && i + j < length; j++) {
                    float t = j < 3 ? (float) j / 2.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 3;
                continue;
            }
            else if (inImperaotr && i + 9 <= length && upperText.substring(i, i + 9).equals("IMPERATOR")) {
                float endRed = 1.0f;
                float endGreen = 0.3f;
                float endBlue = 0.3f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 9 && i + j < length; j++) {
                    float t = j < 8 ? (float) j / 7.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 8;
                continue;
            }
            else if (inDracula && i + 7 <= length && upperText.substring(i, i + 7).equals("DRACULA")) {
                float endRed = 0.549f;
                float endGreen = 0.1f;
                float endBlue = 0.1f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 7 && i + j < length; j++) {
                    float t = j < 6 ? (float) j / 5.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 6;
                continue;
            } else if (inVampire && i + 7 <= length && upperText.substring(i, i + 7).equals("VAMPIRE")) {
                float endRed = 0.549f;
                float endGreen = 0.1f;
                float endBlue = 0.1f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 7 && i + j < length; j++) {
                    float t = j < 6 ? (float) j / 5.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 6;
                continue;
            }
            else if (inDragon && i + 6 <= length && upperText.substring(i, i + 6).equals("DRAGON")) {
                float endRed = 1.0f;
                float endGreen = 0.4f;
                float endBlue = 1.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 6 && i + j < length; j++) {
                    float t = j < 5 ? (float) j / 4.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 5;
                continue;
            }
            else if (inTitan && i + 5 <= length && upperText.substring(i, i + 5).equals("TITAN")) {
                float endRed = 1.0f;
                float endGreen = 1.0f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inAvenger && i + 7 <= length && upperText.substring(i, i + 7).equals("AVENGER")) {
                float endRed = 0.15f;
                float endGreen = 1.0f;
                float endBlue = 0.15f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 7 && i + j < length; j++) {
                    float t = j < 6 ? (float) j / 5.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 6;
                continue;
            }
            else if (inOverlord && i + 8 <= length && upperText.substring(i, i + 8).equals("OVERLORD")) {
                float endRed = 0.0f;
                float endGreen = 1.0f;
                float endBlue = 1.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inMagister && i + 8 <= length && upperText.substring(i, i + 8).equals("MAGISTER")) {
                float endRed = 0.95f;
                float endGreen = 0.6f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inCobra && i + 5 <= length && upperText.substring(i, i + 5).equals("COBRA")) {
                float endRed = 0.15f;
                float endGreen = 1.0f;
                float endBlue = 0.15f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inHydra && i + 5 <= length && upperText.substring(i, i + 5).equals("HYDRA")) {
                float endRed = 0.156f;
                float endGreen = 0.365f;
                float endBlue = 0.012f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inRabbit && i + 6 <= length && upperText.substring(i, i + 6).equals("RABBIT")) {
                float endRed = 0.8f;
                float endGreen = 0.8f;
                float endBlue = 0.8f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 6 && i + j < length; j++) {
                    float t = j < 5 ? (float) j / 4.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 5;
                continue;
            }
            else if (inDhelper && i + 8 <= length && upperText.substring(i, i + 8).equals("D.HELPER")) {
                float endRed = 0.95f;
                float endGreen = 0.6f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inHelper && i + 6 <= length && upperText.substring(i, i + 6).equals("HELPER")) {
                float endRed = 0.95f;
                float endGreen = 0.6f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 6 && i + j < length; j++) {
                    float t = j < 5 ? (float) j / 4.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 5;
                continue;
            }
            else if (inTiger && i + 5 <= length && upperText.substring(i, i + 5).equals("TIGER")) {
                float endRed = 0.9f;
                float endGreen = 0.6f;
                float endBlue = 0.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inBunny && i + 5 <= length && upperText.substring(i, i + 5).equals("BUNNY")) {
                float endRed = 0.2f;
                float endGreen = 0.2f;
                float endBlue = 0.2f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inModerPlus && i + 6 <= length && upperText.substring(i, i + 6).equals("MODER+")) {
                float endRed = 0.275f;
                float endGreen = 0.235f;
                float endBlue = 0.569f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 6 && i + j < length; j++) {
                    float t = j < 5 ? (float) j / 4.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 5;
                continue;
            }
            else if (inMlModer && i + 8 <= length && upperText.substring(i, i + 8).equals("ML.MODER")) {
                float endRed = 44 / 255.0f;
                float endGreen = 58 / 255.0f;
                float endBlue = 227 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inStModer && i + 10 <= length && upperText.substring(i, i + 10).equals("D.ST.MODER")) {
                float endRed = 44 / 255.0f;
                float endGreen = 58 / 255.0f;
                float endBlue = 227 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 10 && i + j < length; j++) {
                    float t = j < 9 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 9;
                continue;
            }
            else if (inStModer && i + 8 <= length && upperText.substring(i, i + 8).equals("ST.MODER")) {
                float endRed = 44 / 255.0f;
                float endGreen = 58 / 255.0f;
                float endBlue = 227 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inModer && i + 5 <= length && upperText.substring(i, i + 5).equals("MODER")) {
                float endRed = 44 / 255.0f;
                float endGreen = 58 / 255.0f;
                float endBlue = 227 / 255.0f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inGlModer && i + 8 <= length && upperText.substring(i, i + 8).equals("GL.MODER")) {
                float endRed = 0.275f;
                float endGreen = 0.235f;
                float endBlue = 0.569f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            else if (inYt && i + 2 <= length && upperText.substring(i, i + 2).equals("YT")) {
                red = 0.722f;
                green = 0.027f;
                blue = 0.086f;
                char c = text.charAt(i);
                posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                if (i + 1 < length) {
                    red = 1.0f;
                    green = 1.0f;
                    blue = 1.0f;
                    c = text.charAt(i + 1);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 1;
                continue;
            }
            else if (inMedia && i + 5 <= length && upperText.substring(i, i + 5).equals("MEDIA")) {
                float endRed = 0.404f;
                float endGreen = 0.141f;
                float endBlue = 0.749f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inAdmin && i + 5 <= length && upperText.substring(i, i + 5).equals("ADMIN")) {
                float endRed = 0.549f;
                float endGreen = 0.110f;
                float endBlue = 0.110f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 5 && i + j < length; j++) {
                    float t = j < 4 ? (float) j / 3.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 4;
                continue;
            }
            else if (inMlAdmin && i + 8 <= length && upperText.substring(i, i + 8).equals("ML.ADMIN")) {
                float endRed = 0.098f;
                float endGreen = 0.698f;
                float endBlue = 0.831f;
                float startRed = endRed * 0.85f;
                float startGreen = endGreen * 0.85f;
                float startBlue = endBlue * 0.85f;
                for (int j = 0; j < 8 && i + j < length; j++) {
                    float t = j < 7 ? (float) j / 6.0f : 1.0f;
                    red = startRed + t * (endRed - startRed);
                    green = startGreen + t * (endGreen - startGreen);
                    blue = startBlue + t * (endBlue - startBlue);
                    char c = text.charAt(i + j);
                    posX += font.renderGlyph(matrix, c, posX, posY, red, green, blue, alpha);
                }
                i += 7;
                continue;
            }
            red = rgb[0];
            green = rgb[1];
            blue = rgb[2];
            posX += font.renderGlyph(matrix, c0, posX, posY, red, green, blue, alpha);
        }
        matrices.pop();
        font.unbindTex();
        GlStateManager.disableBlend();
        return (posX - startPos) / 2.0f;
    }
    public void drawCenteredStringWithOutline(MatrixStack stack, String text, double x, double y, int color) {
        drawStringWithOutline(stack, text, x - getWidth(text) / 2F, y, color);
    }

    public float getWidth(String text) {
        float width = 0.0f;
        String replacedText = ReplaceUtil.replaceCustomFonts(text);

        for (int i = 0; i < replacedText.length(); i++) {
            char c0 = replacedText.charAt(i);
            if (c0 == 167 && i + 1 < replacedText.length() &&
                    StyledFontRenderer.STYLE_CODES.indexOf(replacedText.toLowerCase(Locale.ENGLISH).charAt(i + 1)) != -1) {
                i++;
            } else {
                width += getGlyphPage().getWidth(c0) + regular.getSpacing();
            }
        }

        return (width - regular.getSpacing()) / 2.0f;
    }

    private GlyphPage getGlyphPage() {
        return regular;
    }

    public float getFontHeight() {
        return regular.getFontHeight();
    }

    @Override
    public float getStretching() {
        return 0;
    }

    @Override
    public float getSpacing() {
        return 0;
    }

    public float getLifting() {
        return regular.getLifting();
    }

}
