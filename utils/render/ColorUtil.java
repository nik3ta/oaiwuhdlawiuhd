package nuclear.utils.render;

import net.minecraft.util.math.MathHelper;
import nuclear.utils.math.MathUtils;

import java.awt.*;

public class ColorUtil {

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

    public static float[] getColor(int color) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f};
    }

    public static int getColor(int r, int g, int b, int a) {
        return ((MathHelper.clamp(a, 0, 255) << 24) | (MathHelper.clamp(r, 0, 255) << 16) | (MathHelper.clamp(g, 0, 255) << 8) | MathHelper.clamp(b, 0, 255));
    }

    public static int getColor(int r, int g, int b) {
        return getColor(r, g, b, 255);
    }

    public static int applyOpacity(int color, float alpha) {
        return applyOpacity(color, (int) (MathHelper.clamp(alpha, 0f, 1f) * 255));
    }

    public static int applyOpacity(int color, int alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public static int interpolate(int start, int end, float value) {
        double percent = MathHelper.clamp(value, 0f, 1f);
        return getColor(MathUtils.interpolate(red(start), red(end), percent), MathUtils.interpolate(green(start), green(end), percent), MathUtils.interpolate(blue(start), blue(end), percent), MathUtils.interpolate(alpha(start), alpha(end), percent));
    }

    public static int darken(int color, float factor) {
        float[] rgb = getColor(color);
        float[] hsb = Color.RGBtoHSB((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), null);

        hsb[2] *= factor;
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2]));

        return applyOpacity(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), (rgb[3] * 255));
    }

    public static int boostColor(int color, int boost) {
        return getColor(Math.min(255, red(color) + boost), Math.min(255, green(color) + boost), Math.min(255, blue(color) + boost), alpha(color));
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
            return ColorUtil.getColor(r, g, b, 255);
        }
        if (s.length() == 8) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            int a = Integer.parseInt(s.substring(6, 8), 16);
            return ColorUtil.getColor(r, g, b, a);
        }
        throw new IllegalArgumentException("Unsupported hex format: " + hex);
    }

    public static int gradient(int speed, int index, int first, int second) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        return interpolate(first, second, (angle - 180) / 180f);
    }
}

