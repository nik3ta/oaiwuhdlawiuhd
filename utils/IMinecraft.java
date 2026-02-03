package nuclear.utils;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;

import java.util.ArrayList;
import java.util.List;

public interface IMinecraft {
    Tessellator TESSELLATOR = Tessellator.getInstance();
    BufferBuilder BUFFER = TESSELLATOR.getBuffer();
    Minecraft mc = Minecraft.getInstance();
    Framebuffer FRAMEBUFFER = mc.getFramebuffer();
    MainWindow sr = mc.getMainWindow();
    FontRenderer fr = mc.fontRenderer;
    MainWindow mw = mc.getMainWindow();
    List<Runnable> glow = new ArrayList<>();
    static void update() {
        glow.clear();
    }
}
