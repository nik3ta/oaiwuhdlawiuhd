package nuclear.utils;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.jagrosh.discordipc.entities.User;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SUpdateBossInfoPacket;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventInput;
import nuclear.control.events.impl.player.EventJump;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventTrace;
import nuclear.utils.render.Vec2i;
import org.lwjgl.glfw.GLFW;
import nuclear.utils.math.KeyMappings;
import nuclear.utils.render.ColorUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class ClientUtils implements IMinecraft {

    public static User me;

    private static final String discordID = "1330055843282026536";
    private static final DiscordRichPresence discordRichPresence = new DiscordRichPresence();
    private static final DiscordRPC discordRPC = DiscordRPC.INSTANCE;
    public static ServerData serverData;
    private static boolean pvpMode;
    private static UUID uuid;

    public static String getKey(int integer) {
        if (integer < 0) {
            return switch (integer) {
                case -100 -> I18n.format("key.mouse.left");
                case -99 -> I18n.format("key.mouse.right");
                case -98 -> I18n.format("key.mouse.middle");
                default -> "MOUSE" + (integer + 101);
            };
        } else {
            String keyName = GLFW.glfwGetKeyName(integer, -1);
            if (keyName == null) {
                keyName = KeyMappings.reverseKeyMap.get(integer);
            }
            if (keyName != null) {
                keyName = replaceCyrillicWithLatin(keyName);
            }
            return keyName != null ? keyName : "UNKNOWN";
        }
    }

    private static String replaceCyrillicWithLatin(String input) {
        Map<Character, Character> cyrillicToLatinMap = new HashMap<>();
        String cyrillic = "йцукенгшщзхъфывапролджэячсмитьбюё";
        String latin = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";

        for (int i = 0; i < cyrillic.length(); i++) {
            cyrillicToLatinMap.put(cyrillic.charAt(i), latin.charAt(i));
            cyrillicToLatinMap.put(Character.toUpperCase(cyrillic.charAt(i)), Character.toUpperCase(latin.charAt(i)));
        }

        StringBuilder result = new StringBuilder();
        for (char ch : input.toCharArray()) {
            result.append(cyrillicToLatinMap.getOrDefault(ch, ch));
        }
        return result.toString();
    }

    public static Vector2f get(Vector3d target) {
        double posX = target.getX() - mc.player.getPosX();
        double posY = target.getY() - (mc.player.getPosY() + (double) mc.player.getEyeHeight());
        double posZ = target.getZ() - mc.player.getPosZ();
        double sqrt = MathHelper.sqrt(posX * posX + posZ * posZ);
        float yaw = (float) (Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(posY, sqrt) * 180.0 / Math.PI));
        float sens = (float) (Math.pow(mc.gameSettings.mouseSensitivity, 1.5) * 0.05f + 0.1f);
        float pow = sens * sens * sens * 1.2F;
        yaw -= yaw % pow;
        pitch -= pitch % (pow * sens);
        return new Vector2f(yaw, pitch);
    }

    public static void look(Event event, Vector2f rotation, Correction correction, LivingEntity livingEntity) {
        if (event instanceof EventTrace eventTrace) {
            eventTrace.setYaw(rotation.x);
            eventTrace.setPitch(rotation.y);
            eventTrace.setCancel(true);
        }

        if (event instanceof EventMotion eventMotion) {
            eventMotion.setYaw(rotation.x);
            eventMotion.setPitch(rotation.y);
            mc.player.rotationYawHead = rotation.x;
            mc.player.renderYawOffset = rotation.x;
            mc.player.rotationPitchHead = rotation.y;
        }

        if (correction.equals(Correction.CLIENT)) {
            mc.player.rotationYaw = rotation.x;
            mc.player.rotationPitch = rotation.y;
            return;
        }
        if (correction.equals(Correction.NONE)) return;
        if (event instanceof EventMotion eventMoveFix) {
            eventMoveFix.setYaw(rotation.x);
            eventMoveFix.setPitch(rotation.y);
        }

        if (event instanceof EventInput e && !correction.equals(Correction.STRICT)) {
            if (correction.equals(Correction.SILENT)) {
                e.setYaw(rotation.x);
            } else if (correction.equals(Correction.FULL) && livingEntity != null) {
                e.setYaw(rotation.x, get(livingEntity.getPositionVec()).x);
            }
        }

        if (event instanceof EventJump eventJump) {
            eventJump.setYaw(rotation.x);
        }
    }

    public static boolean collide(LivingEntity entity) {
        return collide(entity, 0);
    }

    public static boolean collide(LivingEntity entity, float grow) {
        AxisAlignedBB box = mc.player.getBoundingBox();
        AxisAlignedBB targetbox = entity.getBoundingBox().grow(grow, 0, grow);

        if (box.maxX > targetbox.minX
                && box.maxY > targetbox.minY && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ) return true;

        return false;
    }

    public static void updateBossInfo(SUpdateBossInfoPacket packet) {
        if (packet.getOperation() == SUpdateBossInfoPacket.Operation.ADD) {
            if (StringUtils.stripControlCodes(packet.getName().getString()).toLowerCase().contains("pvp")) {
                pvpMode = true;
                uuid = packet.getUniqueId();
            }
        } else if (packet.getOperation() == SUpdateBossInfoPacket.Operation.REMOVE) {
            if (packet.getUniqueId().equals(uuid))
                pvpMode = false;
        }
    }

    public static boolean isPvP() {
        return pvpMode;
    }

    public static boolean isConnectedToServer(String ip) {
        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            return mc.getCurrentServerData().serverIP.toLowerCase().contains(ip.toLowerCase());
        }
        return false;
    }

    public static Vec2i getMouse(int mouseX, int mouseY) {
        return new Vec2i((int) (mouseX * Minecraft.getInstance().getMainWindow().getGuiScaleFactor() / 2), (int) (mouseY * Minecraft.getInstance().getMainWindow().getGuiScaleFactor() / 2));
    }

    public static void sendMessage(String message) {
        if (mc.player == null) return;
        java.awt.Color baseColor = new java.awt.Color(getColorByName("primaryColor"));
        float[] hsb = java.awt.Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float darkerBrightness = Math.max(0f, hsb[2] - 0.4f);
        java.awt.Color darkerColor = java.awt.Color.getHSBColor(hsb[0], hsb[1], darkerBrightness);
        int firstColor = darkerColor.getRGB();
        int firstColor2 = getColorByName("primaryColor");

        mc.player.sendMessage(gradient("Nuclear", new java.awt.Color(firstColor).getRGB(), new java.awt.Color(firstColor2).getRGB()).append(new StringTextComponent(TextFormatting.DARK_GRAY + " » " + TextFormatting.RESET + message)), Util.DUMMY_UUID);
    }

    public static void sendMessage(ITextComponent message) {
        if (mc.player == null) return;
        java.awt.Color baseColor = new java.awt.Color(getColorByName("primaryColor"));
        float[] hsb = java.awt.Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float darkerBrightness = Math.max(0f, hsb[2] - 0.4f);
        java.awt.Color darkerColor = java.awt.Color.getHSBColor(hsb[0], hsb[1], darkerBrightness);
        int firstColor = darkerColor.getRGB();
        int firstColor2 = getColorByName("primaryColor");

        ITextComponent finalMessage = new StringTextComponent("")
                .append(gradient("Nuclear", new java.awt.Color(firstColor).getRGB(), new java.awt.Color(firstColor2).getRGB()))
                .append(new StringTextComponent(" » ").mergeStyle(TextFormatting.DARK_GRAY))
                .append(message);
        mc.player.sendMessage(finalMessage, Util.DUMMY_UUID);
    }

    public static StringTextComponent gradient(String message, int first, int end) {

        StringTextComponent text = new StringTextComponent("");
        for (int i = 0; i < message.length(); i++) {
            text.append(new StringTextComponent(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.setColor(new Color(ColorUtils.interpolateColor(first, end, (float) i / message.length()))).setBold(true)));
        }

        return text;

    }

    public static ITextComponent replace(ITextComponent original, String find, String replaceWith) {
        if (original == null || find == null || replaceWith == null) {
            return original;
        }

        String originalText = original.getString();
        String replacedText = originalText.replace(find, replaceWith);
        return new StringTextComponent(replacedText);
    }

    public enum Correction {
        NONE,
        SILENT,
        STRICT,
        FULL,
        CLIENT
    }
}
