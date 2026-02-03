package nuclear.utils.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.vector.Vector2f;
import nuclear.utils.IMinecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class CustomFramebuffer extends Framebuffer implements IMinecraft {
    private boolean linear;

    public CustomFramebuffer(boolean useDepth) {
        super(1, 1, useDepth, Minecraft.IS_RUNNING_ON_MAC);
    }

    public CustomFramebuffer(int width, int height, boolean useDepth) {
        super(width, height, useDepth, Minecraft.IS_RUNNING_ON_MAC);
    }

    private static void resizeFramebuffer(CustomFramebuffer framebuffer) {
        if (needsNewFramebuffer(framebuffer)) {
            framebuffer.createBuffers(Math.max(mc.getMainWindow().getFramebufferWidth(), 1), Math.max(mc.getMainWindow().getFramebufferHeight(), 1), Minecraft.IS_RUNNING_ON_MAC);
        }
    }

    public CustomFramebuffer setLinear() {
        this.linear = true;
        return this;
    }

    @Override
    public void setFramebufferFilter(int framebufferFilterIn) {
        super.setFramebufferFilter(this.linear ? 9729 : framebufferFilterIn);
    }

    public void setup(boolean clear) {
        resizeFramebuffer(this);
        if (clear) this.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
        this.bindFramebuffer(false);
    }

    public void setup() {
        setup(true);
    }

    public static void drawQuads(double x, double y, double width, double height) {
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        BUFFER.pos(x, y, 0).tex(0, 1).endVertex();
        BUFFER.pos(x, y + height, 0).tex(0, 0).endVertex();
        BUFFER.pos(x + width, y + height, 0).tex(1, 0).endVertex();
        BUFFER.pos(x + width, y, 0).tex(1, 1).endVertex();
        TESSELLATOR.draw();
    }

    public static void drawQuads() {
        Vector2f window = getMouse(mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
        double width = window.getX();
        double height = window.getY();
        drawQuads(0, 0, width, height);
    }

    public void draw() {
        this.bindFramebufferTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        drawQuads();
    }

    public static boolean needsNewFramebuffer(CustomFramebuffer framebuffer) {
        return framebuffer == null || framebuffer.framebufferWidth != mw.getFramebufferWidth() || framebuffer.framebufferHeight != mw.getFramebufferHeight();
    }

    public static CustomFramebuffer createFramebuffer(CustomFramebuffer framebuffer) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new CustomFramebuffer(mw.getFramebufferWidth(), mw.getFramebufferHeight(), true);
        }
        return framebuffer;
    }


    public static Vector2f getMouse(int mouseX, int mouseY) {
        return GuiUtils.getMouse(mouseX, mouseY);
    }

    public static class GuiUtils {
        public static Vector2f getMouse(float mouseX, float mouseY) {
            double scale = mw.getGuiScaleFactor() / 2;
            return new Vector2f((float) (mouseX * scale), (float) (mouseY * scale));
        }
    }
}