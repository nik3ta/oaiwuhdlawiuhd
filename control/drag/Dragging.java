package nuclear.control.drag;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import nuclear.module.api.Module;
import nuclear.utils.ClientUtils;
import nuclear.utils.math.MathUtil;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.Vec2i;

import static nuclear.utils.math.MathUtil.lerp;

public class Dragging {
    @Expose
    @SerializedName("x")
    private float xPos;
    @Expose
    @SerializedName("y")
    private float yPos;

    private float targetXPos;
    private float targetYPos;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    private boolean dragging;

    private float width, height;

    @Expose
    @SerializedName("name")
    private final String name;

    private final Module module;

    private static final float CENTER_LINE_WIDTH = 0.5f;
    private static final float SNAP_THRESHOLD = 0.01f;

    private static final float[] X_SNAP_POINTS = {0.1667f, 0.25f, 0.3333f, 0.5f, 0.6667f, 0.75f, 0.8333f};
    private static final float[] Y_SNAP_POINTS = {0.1667f, 0.25f, 0.3333f, 0.5f, 0.6667f, 0.75f, 0.8333f};
    private boolean[] snapXActive;
    private boolean[] snapYActive;

    private static final float LERP_SPEED = 0.25f;
    private static final float ALPHA_ANIMATION_SPEED = 0.05f;

    private float hoverTextAlpha;
    private float lineFadeAlpha;
    private boolean wasHovered = false;
    private boolean linesActive = false;

    public Dragging(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.xPos = initialXVal;
        this.yPos = initialYVal;
        this.targetXPos = initialXVal;
        this.targetYPos = initialYVal;
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;
        this.hoverTextAlpha = 0.0f;
        this.lineFadeAlpha = 0.0f;
        this.snapXActive = new boolean[X_SNAP_POINTS.length];
        this.snapYActive = new boolean[Y_SNAP_POINTS.length];
    }

    public Module getModule() {
        return module;
    }

    public String getName() {
        return name;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getX() {
        return xPos;
    }

    public void setX(float x) {
        this.xPos = x;
    }

    public float getY() {
        return yPos;
    }

    public void setY(float y) {
        this.yPos = y;
    }

    public void onDraw(int mouseX, int mouseY, MainWindow res, MatrixStack stack) {
        Vec2i fixed = ClientUtils.getMouse(mouseX, mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();

        float scaledWidth = res.scaledWidth();
        float scaledHeight = res.scaledHeight();

        boolean isHovered = MathUtil.isHovered(mouseX, mouseY, xPos, yPos, width, height);

        if (isHovered && !dragging) {
            if (!wasHovered) {
                wasHovered = true;
            }
            hoverTextAlpha = lerp(hoverTextAlpha, 1.0f, ALPHA_ANIMATION_SPEED);
        } else {
            if (wasHovered) {
                wasHovered = false;
            }
            hoverTextAlpha = lerp(hoverTextAlpha, 0.0f, ALPHA_ANIMATION_SPEED);
        }

        if (dragging) {
            for (int i = 0; i < snapXActive.length; i++) snapXActive[i] = false;
            for (int i = 0; i < snapYActive.length; i++) snapYActive[i] = false;

            targetXPos = mouseX - startX;
            targetYPos = mouseY - startY;

            boolean snapped = false;

            for (int i = 0; i < X_SNAP_POINTS.length; i++) {
                float snapX = scaledWidth * X_SNAP_POINTS[i];
                if (Math.abs(targetXPos + width / 2.0f - snapX) < SNAP_THRESHOLD * scaledWidth) {
                    targetXPos = snapX - width / 2.0f;
                    snapXActive[i] = true;
                    snapped = true;
                }
            }

            for (int i = 0; i < Y_SNAP_POINTS.length; i++) {
                float snapY = scaledHeight * Y_SNAP_POINTS[i];
                if (Math.abs(targetYPos + height / 2.0f - snapY) < SNAP_THRESHOLD * scaledHeight) {
                    targetYPos = snapY - height / 2.0f;
                    snapYActive[i] = true;
                    snapped = true;
                }
            }

            // Boundary checks
            if (targetXPos + width > scaledWidth) {
                targetXPos = scaledWidth - width;
            }
            if (targetYPos + height > scaledHeight) {
                targetYPos = scaledHeight - height;
            }
            if (targetXPos < 0) {
                targetXPos = 0;
            }
            if (targetYPos < 0) {
                targetYPos = 0;
            }

            xPos = lerp(xPos, targetXPos, LERP_SPEED);
            yPos = lerp(yPos, targetYPos, LERP_SPEED);

            updateLineAnimation(snapped);
        } else {
            updateLineAnimation(false);
        }

        drawCenterLines(res);
    }

    private void updateLineAnimation(boolean active) {
        if (active && !linesActive) {
            linesActive = true;
            lineFadeAlpha = lerp(lineFadeAlpha, 1.0f, ALPHA_ANIMATION_SPEED);
        } else if (!active && linesActive) {
            lineFadeAlpha = lerp(lineFadeAlpha, 0.0f, ALPHA_ANIMATION_SPEED);
            if (lineFadeAlpha < 0.01f) {
                linesActive = false;
            }
        } else if (active && linesActive) {
            lineFadeAlpha = lerp(lineFadeAlpha, 1.0f, ALPHA_ANIMATION_SPEED);
        }
    }

    private void drawCenterLines(MainWindow res) {
        if (lineFadeAlpha > 0.0f) {
            float scaledWidth = res.scaledWidth();
            float scaledHeight = res.scaledHeight();
            int color = (int) (lineFadeAlpha * 255) << 24 | 0xFFFFFF;

            // Draw snap lines
            for (int i = 0; i < X_SNAP_POINTS.length; i++) {
                if (snapXActive[i]) {
                    float snapX = scaledWidth * X_SNAP_POINTS[i];
                    RenderUtils.Render2D.drawRoundedRect(snapX - CENTER_LINE_WIDTH, 0, CENTER_LINE_WIDTH, scaledHeight, 1f, color);
                }
            }

            for (int i = 0; i < Y_SNAP_POINTS.length; i++) {
                if (snapYActive[i]) {
                    float snapY = scaledHeight * Y_SNAP_POINTS[i];
                    RenderUtils.Render2D.drawRoundedRect(0, snapY - CENTER_LINE_WIDTH, scaledWidth, CENTER_LINE_WIDTH, 1f, color);
                }
            }
        }
    }

    public final boolean onClick(double mouseX, double mouseY, int button) {
        Vec2i fixed = ClientUtils.getMouse((int) mouseX, (int) mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();
        if (button == 0 && MathUtil.isHovered((float) mouseX, (float) mouseY, xPos, yPos, width, height)) {
            dragging = true;
            startX = (float) (mouseX - xPos);
            startY = (float) (mouseY - yPos);
            return true;
        }
        return false;
    }

    public final void onRelease(int button) {
        if (button == 0) {
            dragging = false;
            updateLineAnimation(false);
        }
    }
}