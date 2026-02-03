package nuclear.utils.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.item.ItemStack;
import nuclear.utils.IMinecraft;

public class HudUtil implements IMinecraft {

    public static String calculateBPS() {
        double distance = Math.sqrt(Math.pow(mc.player.getPosX() - mc.player.prevPosX, 2) +
                Math.pow(mc.player.getPosY() - mc.player.prevPosY, 2) +
                Math.pow(mc.player.getPosZ() - mc.player.prevPosZ, 2));
        float bps = (float) (distance * mc.timer.timerSpeed * 20.0D);
        return String.valueOf((float) (Math.round(bps * 10) / 10.0f));
    }

    public static void drawItemStack(ItemStack stack, double x, double y,  float size, String altText, boolean withoutOverlay) {
        RenderSystem.pushMatrix();
        RenderSystem.translated(x, y, 0.0);
        RenderSystem.scalef(size, size, 1.0f);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
        }
        RenderSystem.popMatrix();
    }

    public static int calculatePing() {
        return mc.player.connection.getPlayerInfo(mc.player.getUniqueID()) != null ?
                mc.player.connection.getPlayerInfo(mc.player.getUniqueID()).getResponseTime() : 0;
    }

    public static String serverIP() {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null && !mc.isSingleplayer() ? mc.getCurrentServerData().serverIP : "";
    }
}
