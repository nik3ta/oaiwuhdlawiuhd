package nuclear.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import nuclear.utils.IMinecraft;
import nuclear.utils.math.MathUtils;
import org.joml.Vector4i;

import java.awt.*;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_COLOR;
import static nuclear.ui.clickgui.Panel.getColorByName;

public class ColorUtils implements IMinecraft {
    public static final int green = ColorUtils.rgba(36, 218, 118, 255);
    public static final int yellow = ColorUtils.rgba(255, 196, 67, 255);
    public static final int orange = ColorUtils.rgba(255, 134, 0, 255);
    public static final int red = ColorUtils.rgba(239, 72, 54, 255);

    public static void setAlphaColor(final int color, final float alpha) {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        RenderSystem.color4f(red, green, blue, alpha);
    }
    public static void setColor(int color) {
        setAlphaColor(color, (float) (color >> 24 & 255) / 255.0F);
    }

    public static int boostColor(int color, int boost) {
        return getColor(Math.min(255, red(color) + boost), Math.min(255, green(color) + boost), Math.min(255, blue(color) + boost), alpha(color));
    }

    public static float[] getColor2(int color) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f};
    }

    public static int darken(int color, float factor) {
        float[] rgb = getColor(color);
        float[] hsb = Color.RGBtoHSB((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), null);

        hsb[2] *= factor;
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2]));

        return applyOpacity(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), (rgb[3] * 255));
    }

    public static int hex(String hex) {
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);

        if (s.length() == 3 || s.length() == 4) {
            char r = s.charAt(0);
            char g = s.charAt(1);
            char b = s.charAt(2);
            char a = s.length() == 4 ? s.charAt(3) : 'F';
            s = ("" + r + r + g + g + b + b + a + a);
        }

        if (s.length() == 6) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return ColorUtils.getColor(r, g, b, 255);
        }
        if (s.length() == 8) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            int a = Integer.parseInt(s.substring(6, 8), 16);
            return ColorUtils.getColor(r, g, b, a);
        }
        throw new IllegalArgumentException("Unsupported hex format: " + hex);
    }

    public static int astolfo(int speed, int offset, float saturation, float brightness, float alpha) {
        float hue = (float) calculateHueDegrees(speed, offset);
        hue = (float) ((double) hue % 360.0);
        float hueNormalized;
        return reAlphaInt(
                Color.HSBtoRGB((double) ((hueNormalized = hue % 360.0F) / 360.0F) < 0.5 ? -(hueNormalized / 360.0F) : hueNormalized / 360.0F, saturation, brightness),
                Math.max(0, Math.min(255, (int) (alpha * 255.0F)))
        );
    }

    private static int calculateHueDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / divisor + offset) % 360L;
        return (int) calculatedValue;
    }

    public static int getColorStyle(float index) {
        return getColorByName("primaryColor");
    }

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static float[] rgba(int color) {
        return new float[]{(color >> 16 & 0xFF) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f,
                (color >> 24 & 0xFF) / 255.0f};
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }
    public static int applyOpacity(int color, float alpha) {
        return reAlphaInt(color, (int) (MathHelper.clamp(alpha, 0f, 1f) * 255));
    }
    public static int rgba(double r, double g, double b, double a) {
        return rgba((int) r, (int) g, (int) b, (int) a);
    }

    public static int getRed(final int hex) {
        return hex >> 16 & 255;
    }

    public static int getGreen(final int hex) {
        return hex >> 8 & 255;
    }

    public static int getBlue(final int hex) {
        return hex & 255;
    }

    public static int getAlpha(final int hex) {
        return hex >> 24 & 255;
    }

    public static int getColor(int r, int g, int b, int a) {
        return ((MathHelper.clamp(a, 0, 255) << 24) | (MathHelper.clamp(r, 0, 255) << 16) | (MathHelper.clamp(g, 0, 255) << 8) | MathHelper.clamp(b, 0, 255));
    }

    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int alpha(int c) {
        return (c >> 24) & 0xFF;
    }

    public static float[] getColor(int color, int alpha) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f};
    }

    public static float[] getColor(int color) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f};
    }


    //    gradient with more than two colors
    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360f * colors.length - colorIndex);
    }


    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolate(int start, int end, float value) {
        double percent = MathHelper.clamp(value, 0f, 1f);
        return getColor(MathUtils.interpolate(red(start), red(end), percent), MathUtils.interpolate(green(start), green(end), percent), MathUtils.interpolate(blue(start), blue(end), percent), MathUtils.interpolate(alpha(start), alpha(end), percent));
    }
    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue);
    }
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r, g, b;

        if (saturation == 0) {
            int value = (int) (brightness * 255.0f + 0.5f);
            return 0xff000000 | (value << 16) | (value << 8) | value;
        }

        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - (saturation * (1.0f - f)));

        switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            default:
                throw new IllegalArgumentException("Invalid hue value");
        }

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int setAlpha(int color, int alpha) {
        Color c = new Color(color, true);
        int existingAlpha = c.getAlpha();
        int finalAlpha = (existingAlpha != 255) ? Math.max(3, (existingAlpha * alpha / 255)) : Math.max(3, alpha);
        finalAlpha = MathHelper.clamp(finalAlpha, 0, 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), finalAlpha).getRGB();
    }

    public static int setAlpha(int color, int alpha, int dopAlpha) {
        Color c = new Color(color, true);
        int finalAlpha = 200;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), finalAlpha).getRGB();
    }

    public static int setAlpha(int min, int color, int alpha, int dopAlpha) {
        Color c = new Color(color, true);
        int existingAlpha = c.getAlpha();
        int finalAlpha = (existingAlpha != 255) ? Math.max(min, (existingAlpha * alpha / 255)) : Math.max(min, alpha);
        finalAlpha = MathHelper.clamp(finalAlpha, 0, 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), finalAlpha).getRGB();
    }
    public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                      int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }

    public static int setAlpha(int color, float alpha) {
        int alphaInt = (int) (Math.min(1.0f, Math.max(0.0f, alpha)) * 255);
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }

    public static void quads(float x, float y, float width, float height, int glQuads, int color) {
        BUFFER.begin(glQuads, POSITION_TEX_COLOR);
        {
            BUFFER.pos(x, y, 0).tex(0, 0).color(color).endVertex();
            BUFFER.pos(x, y + height, 0).tex(0, 1).color(color).endVertex();
            BUFFER.pos(x + width, y + height, 0).tex(1, 1).color(color).endVertex();
            BUFFER.pos(x + width, y, 0).tex(1, 0).color(color).endVertex();
        }
        TESSELLATOR.draw();
    }

    public static int reAlphaInt(final int color,
                                 final int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height, Vector4i color) {
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        mc.getTextureManager().bindTexture(resourceLocation);
        BUFFER.begin(7, POSITION_TEX_COLOR);
        {
            BUFFER.pos(x, y, 0).tex(0, 1 - 0.01f).lightmap(0, 240).color(color.x).endVertex();
            BUFFER.pos(x, y + height, 0).tex(1, 1 - 0.01f).lightmap(0, 240).color(color.y).endVertex();
            BUFFER.pos(x + width, y + height, 0).tex(1, 0).lightmap(0, 240).color(color.z).endVertex();
            BUFFER.pos(x + width, y, 0).tex(0, 0).lightmap(0, 240).color(color.w).endVertex();

        }
        TESSELLATOR.draw();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    public static int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }

    public static int ensureMinAlpha(int color, int minAlpha) {
        int currentAlpha = alpha(color);
        if (currentAlpha < minAlpha) {
            return setAlpha(color, minAlpha);
        }
        return color;
    }
}