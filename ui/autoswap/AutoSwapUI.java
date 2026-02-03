package nuclear.ui.autoswap;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.InputMappings;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;
import nuclear.control.Manager;
import nuclear.module.impl.combat.AutoSwap;
import nuclear.module.impl.player.GuiMove;
import nuclear.utils.IMinecraft;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class AutoSwapUI extends Screen implements IMinecraft {

    private static final double INNER_RADIUS = 65;
    private static final double OUTER_RADIUS = 120;
    private static final double HOVER_RADIUS_INCREASE = 5;
    private static final int SEGMENT_COUNT = 3;
    private static final double SEGMENT_ANGLE = 360.0 / SEGMENT_COUNT;
    private static final double GAP_ANGLE = 1.2;

    private boolean firstTick = true;
    private boolean keyWasHeld = false;
    private int hoveredOnRelease = -1;

    private final Color BASE = new Color(152, 151, 151, 90);
    private final Color HOVER_EMPTY = new Color(228, 228, 32, 160);
    private final Color HOVER_FILLED = new Color(255, 85, 85, 130);
    private final Color OUTLINE = new Color(179, 178, 178, 95);

    public AutoSwapUI() {
        super(StringTextComponent.EMPTY);
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            if (AutoSwap.wheelSegmentItems[i] == null) {
                AutoSwap.wheelSegmentItems[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        GLFW.glfwSetCursorPos(
                mw.getHandle(),
                mw.getWidth() / 2.0,
                mw.getHeight() / 2.0
        );
        firstTick = true;
        keyWasHeld = false;
        hoveredOnRelease = -1;
    }

    public void setSegmentItem(int segment, ItemStack stack) {
        if (segment < 0 || segment >= SEGMENT_COUNT) return;
        AutoSwap.wheelSegmentItems[segment] =
                stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private boolean isKeyHeld() {
        if (AutoSwap.swapKey == null) return false;
        int key = AutoSwap.swapKey.getKey();
        
        // Проверяем, является ли это кнопкой мыши (кнопки мыши хранятся как -100 + buttonNumber)
        if (key < 0) {
            int mouseButton = key + 100;
            // Проверяем состояние кнопки мыши через GLFW
            return GLFW.glfwGetMouseButton(mw.getHandle(), mouseButton) == GLFW.GLFW_PRESS;
        } else {
            // Для обычных клавиш используем стандартный метод
            return InputMappings.isKeyDown(mw.getHandle(), key);
        }
    }

    @Override
    public void tick() {
        boolean held = isKeyHeld();

        if (firstTick) {
            firstTick = false;
            keyWasHeld = held;
            return;
        }

        if (keyWasHeld && !held && hoveredOnRelease >= 0) {
            ItemStack stack = AutoSwap.wheelSegmentItems[hoveredOnRelease];

            if (!stack.isEmpty()) {
                ItemStack offhandItem = mc.player.getHeldItemOffhand();
                if (!offhandItem.isEmpty() && ItemStack.areItemStacksEqual(offhandItem, stack)) {
                    mc.displayGuiScreen(null);
                    return;
                }
                
                int slot = findItemSlot(stack);
                if (slot >= 0) {
                    int fromSlot = slot;
                    int toSlot = 45;
                    
                    if (MoveUtil.isMoving() && GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);

                        new Thread(() -> {
                            try {
                                Thread.sleep(42);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mc.execute(() -> {
                                if (mc.player != null) {
                                    InventoryUtils.moveItem(fromSlot, toSlot);
                                }
                            });
                        }).start();
                    } else {
                        InventoryUtils.moveItem(fromSlot, toSlot);
                    }
                    mc.displayGuiScreen(null);
                } else {
                    mc.displayGuiScreen(null);
                }
            }
            return;
        }

        keyWasHeld = held;
        if (!held) mc.displayGuiScreen(null);
    }

    private int findItemSlot(ItemStack target) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == target.getItem() && ItemStack.areItemStackTagsEqual(s, target)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float pt) {
        int cx = mw.getScaledWidth() / 2;
        int cy = mw.getScaledHeight() / 2;

        int hoverMouse = getHoveredSegment(cx, cy, mouseX, mouseY);
        hoveredOnRelease = hoverMouse;

        ItemStack[] items = AutoSwap.wheelSegmentItems;

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            double start = i * SEGMENT_ANGLE - 90 + GAP_ANGLE / 2;
            double end = (i + 1) * SEGMENT_ANGLE - 90 - GAP_ANGLE / 2;

            boolean hovered = hoverMouse == i;
            boolean empty = items[i].isEmpty();

            Color fill;
            if (hovered && empty) fill = HOVER_EMPTY;
            else if (hovered) fill = HOVER_FILLED;
            else fill = BASE;

            double currentOuterRadius = hovered ? OUTER_RADIUS + HOVER_RADIUS_INCREASE : OUTER_RADIUS;
            
            drawSegment(ms, cx, cy, INNER_RADIUS, currentOuterRadius, start, end, fill);
            drawSmoothOutline(ms, cx, cy, INNER_RADIUS, currentOuterRadius, start, end, OUTLINE);

            double mid = (start + end) / 2;
            double r = (INNER_RADIUS + currentOuterRadius) / 2;
            double ix = cx + Math.cos(Math.toRadians(mid)) * r;
            double iy = cy + Math.sin(Math.toRadians(mid)) * r;

            if (empty) {
                drawPlus(ms, ix, iy, 10, new Color(255, 255, 255, 120));
            } else {
                drawItem(ms, items[i], ix, iy);
            }
        }

        super.render(ms, mouseX, mouseY, pt);
    }

    private int getHoveredSegment(double cx, double cy, int mx, int my) {
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER_RADIUS) return -1;
        double angle = (Math.toDegrees(Math.atan2(dy, dx)) + 90 + 360) % 360;
        return (int) (angle / SEGMENT_ANGLE);
    }

    private void drawItem(MatrixStack ms, ItemStack stack, double x, double y) {
        RenderSystem.enableDepthTest();
        mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, (int) x - 8, (int) y - 8);
        RenderSystem.disableDepthTest();
    }

    private void drawPlus(MatrixStack ms, double x, double y, double size, Color c) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        Matrix4f m = ms.getLast().getMatrix();

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2f);

        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float bl = c.getBlue() / 255f;
        float a = c.getAlpha() / 255f;

        double h = size / 2;

        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        b.pos(m, (float)(x - h), (float)y, 0).color(r,g,bl,a).endVertex();
        b.pos(m, (float)(x + h), (float)y, 0).color(r,g,bl,a).endVertex();
        b.pos(m, (float)x, (float)(y - h), 0).color(r,g,bl,a).endVertex();
        b.pos(m, (float)x, (float)(y + h), 0).color(r,g,bl,a).endVertex();
        t.draw();

        RenderSystem.enableTexture();
    }

    private void drawSegment(MatrixStack ms, double cx, double cy,
                             double in, double out,
                             double sa, double ea, Color c) {

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        Matrix4f m = ms.getLast().getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();

        b.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float bl = c.getBlue() / 255f;
        float a = c.getAlpha() / 255f;

        int steps = (int) ((ea - sa) / 2) + 1;
        for (int i = 0; i <= steps; i++) {
            double ang = sa + (ea - sa) * i / steps;
            double rad = Math.toRadians(ang);
            b.pos(m, (float)(cx + Math.cos(rad) * out),
                    (float)(cy + Math.sin(rad) * out), 0).color(r,g,bl,a).endVertex();
            b.pos(m, (float)(cx + Math.cos(rad) * in),
                    (float)(cy + Math.sin(rad) * in), 0).color(r,g,bl,a).endVertex();
        }
        t.draw();
        RenderSystem.enableTexture();
    }

    private void drawSmoothOutline(MatrixStack ms,
                                   double cx, double cy,
                                   double innerR, double outerR,
                                   double sa, double ea,
                                   Color c) {

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        Matrix4f m = ms.getLast().getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2f);

        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float bl = c.getBlue() / 255f;
        float a = c.getAlpha() / 255f;

        int steps = (int) ((ea - sa) / 2) + 1;

        b.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double ang = sa + (ea - sa) * i / steps;
            double rad = Math.toRadians(ang);
            b.pos(m, (float)(cx + Math.cos(rad) * outerR),
                            (float)(cy + Math.sin(rad) * outerR), 0)
                    .color(r,g,bl,a).endVertex();
        }
        t.draw();

        b.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = steps; i >= 0; i--) {
            double ang = sa + (ea - sa) * i / steps;
            double rad = Math.toRadians(ang);
            b.pos(m, (float)(cx + Math.cos(rad) * innerR),
                            (float)(cy + Math.sin(rad) * innerR), 0)
                    .color(r,g,bl,a).endVertex();
        }
        t.draw();

        double sr = Math.toRadians(sa);
        double er = Math.toRadians(ea);

        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        b.pos(m, (float)(cx + Math.cos(sr) * innerR),
                (float)(cy + Math.sin(sr) * innerR), 0).color(r,g,bl,a).endVertex();
        b.pos(m, (float)(cx + Math.cos(sr) * outerR),
                (float)(cy + Math.sin(sr) * outerR), 0).color(r,g,bl,a).endVertex();

        b.pos(m, (float)(cx + Math.cos(er) * innerR),
                (float)(cy + Math.sin(er) * innerR), 0).color(r,g,bl,a).endVertex();
        b.pos(m, (float)(cx + Math.cos(er) * outerR),
                (float)(cy + Math.sin(er) * outerR), 0).color(r,g,bl,a).endVertex();
        t.draw();

        RenderSystem.enableTexture();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = mw.getScaledWidth() / 2;
        int cy = mw.getScaledHeight() / 2;

        int hovered = getHoveredSegment(cx, cy, (int) mouseX, (int) mouseY);
        if (hovered < 0) return super.mouseClicked(mouseX, mouseY, button);

        ItemStack stack = AutoSwap.wheelSegmentItems[hovered];

        if (stack.isEmpty()) {
            if (button == 0) {
                mc.displayGuiScreen(new AutoSwapInventoryScreen(mc.player, this, hovered));
                return true;
            }
        } else {
            if (button == 1) {
                setSegmentItem(hovered, ItemStack.EMPTY);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int cx = mw.getScaledWidth() / 2;
        int cy = mw.getScaledHeight() / 2;

        int hovered = getHoveredSegment(cx, cy, (int) mouseX, (int) mouseY);
        if (hovered < 0) return super.mouseScrolled(mouseX, mouseY, delta);

        ItemStack stack = AutoSwap.wheelSegmentItems[hovered];
        if (!stack.isEmpty()) {
            ItemStack offhandItem = mc.player.getHeldItemOffhand();
            if (!offhandItem.isEmpty() && ItemStack.areItemStacksEqual(offhandItem, stack)) {
                mc.displayGuiScreen(null);
                return true;
            }
            
            int slot = findItemSlot(stack);
            if (slot >= 0) {
                int fromSlot = slot;
                int toSlot = 45;
                
                if (MoveUtil.isMoving() && GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    GuiMove.stopMovementTemporarily(0.085f);

                    new Thread(() -> {
                        try {
                            Thread.sleep(42);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        mc.execute(() -> {
                            if (mc.player != null) {
                                InventoryUtils.moveItem(fromSlot, toSlot);
                            }
                        });
                    }).start();
                } else {
                    InventoryUtils.moveItem(fromSlot, toSlot);
                }
                mc.displayGuiScreen(null);
                return true;
            } else {
                mc.displayGuiScreen(null);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

