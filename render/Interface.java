package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import nuclear.Nuclear;
import nuclear.control.Manager;
import nuclear.control.drag.Dragging;
import nuclear.module.impl.other.StreamerMode;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.IMinecraft;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.Direction;
import nuclear.utils.anim.impl.DecelerateAnimation;
import nuclear.utils.anim.impl.EaseBackIn;
import nuclear.utils.font.Fonts;
import nuclear.utils.font.ReplaceUtil;
import nuclear.utils.font.styled.StyledFont;
import nuclear.utils.math.MathUtil;
import nuclear.utils.misc.HudUtil;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.StencilUtils;
import nuclear.utils.render.animation.AnimationMath;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nuclear.module.impl.render.Interface.Status.*;
import static nuclear.ui.clickgui.Panel.getColorByName;
import static nuclear.utils.render.RenderUtils.Render2D.prepareScissor;

@Annotation(name = "Hud", type = TypeList.Render, desc = "Интерфейс чита")
public class Interface extends Module {
    private float perc;
    private int itemCount = 0;
    private final Map<String, LocalDateTime> staffJoinTimes = new HashMap<>();

    public MultiBoxSetting elements = new MultiBoxSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Кейбинд", true),
            new BooleanSetting("Тотемы", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Уведомления", true),
            new BooleanSetting("Стафф лист", true),
            new BooleanSetting("Список зелий", true),
            new BooleanSetting("Таймер индикатор", false),
            new BooleanSetting("Таргет Худ", true),
            new BooleanSetting("Расписание евентов", true),
            new BooleanSetting("Задержка предметов", false));
    public SliderSetting size = new SliderSetting("Размер худа", 1, 0.8f, 1.2f, 0.05f);

    public Dragging Music = Nuclear.createDrag(this, "Music", 220, 140);

    private final Animation musicAnimation = new EaseBackIn(300, 1, 1.5f);
    private final TimerUtil musicTimer = new TimerUtil();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean wasStaffChatOpen = false;

    public final BooleanSetting particle = new BooleanSetting("Партиклы с головы", true);
    public final BooleanSetting targetOnHover = new BooleanSetting("При наведении", false);
    public final BooleanSetting airDrop = new BooleanSetting("АйрДроп", true);
    public final BooleanSetting chest = new BooleanSetting("Лег сундук", true);
    public final BooleanSetting shipSiege = new BooleanSetting("Осада корабля", true);
    public final BooleanSetting bossBattle = new BooleanSetting("Битва с Боссом", true);
    public final BooleanSetting watermarkFPS = new BooleanSetting("ФПС", true);
    public final BooleanSetting watermarkPing = new BooleanSetting("Пинг", true);
    public final BooleanSetting watermarkTPS = new BooleanSetting("ТПС", true);
    public final BooleanSetting watermarkCoords = new BooleanSetting("Координаты", true);
    public final BooleanSetting watermarkBPS = new BooleanSetting("БПС", true);
    private final Animation targetInfoAnimation;
    private final Animation staffListAnimation = new EaseBackIn(300, 1, 1.5f);
    private final Map<String, Animation> staffAnimations = new HashMap<>();
    public Dragging keyBinds = Nuclear.createDrag(this, "KeyBinds", 120, 95);
    public Dragging staffList = Nuclear.createDrag(this, "StaffList", 350, 50);
    private float heightDynamic = 0;
    private int activeModules = 0;
    private int activeStaff = 0;
    private float hDynam = 0;
    private boolean wasChatOpen = false;
    private boolean potionAllow = false;
    private List<StaffPlayer> staffPlayers = new ArrayList<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|wne|мод|medi|хелп|помо|стаж|адм|владе|отри|таф|taf|yout|curat|курато|dev|раз|supp|сапп|yt|ютуб)(?<!D\\.HELPER).*", Pattern.CASE_INSENSITIVE);
    public Dragging totemCountDrag = Nuclear.createDrag(this, "TotemCount", 150, 150);
    public final Dragging HUDCooldown = Nuclear.createDrag(this, "Cooldown", 7, 105);
    public Dragging TimerHUD = Nuclear.createDrag(this, "TimerHUD", 160, 180);
    public Dragging events = Nuclear.createDrag(this, "onEventsRender", 350, 55);
    private String countdownText = "";
    private String countdownText3 = "";
    private String countdownText4 = "";
    private String countdownText5 = "";
    private float health = 0;
    private float health2 = 0;
    private float healthplus = 0;
    private float healthplus2 = 0;
    private float eventsAnimatedWidth = 0f;
    public final Dragging targetHUD = Nuclear.createDrag(this, "TargetHUD", 380, 240);
    private final TimerUtil targetTimer = new TimerUtil();
    private final TimerUtil eventsSettingsTimer = new TimerUtil();
    private final TimerUtil notificationSettingsTimer = new TimerUtil();
    private final List<HeadParticle> particles = new ArrayList<>();
    private LivingEntity target = null;
    private boolean allow;
    private boolean particlesSpawnedThisHit = false;
    private boolean targetSettingsOpen = false;
    private boolean eventsSettingsOpen = false;
    private boolean eventsSettingsAllow = false;
    public boolean notificationSettingsOpen = false;
    public boolean notificationSettingsAllow = false;
    private float particleAnimation = 0f;
    private float targetOnHoverAnimation = 0f;
    private float airDropAnimation = 0f;
    private float chestAnimation = 0f;
    private float shipSiegeAnimation = 0f;
    private float bossBattleAnimation = 0f;
    private float watermarkFPSAnimation = 0f;
    private float watermarkPingAnimation = 0f;
    private float watermarkTPSAnimation = 0f;
    private float watermarkCoordsAnimation = 0f;
    private float watermarkBPSAnimation = 0f;
    private float useTotemAnimation = 0f;
    private float useItemAnimation = 0f;
    private boolean particleFirstRender = true;
    private boolean targetOnHoverFirstRender = true;
    private boolean airDropFirstRender = true;
    private boolean chestFirstRender = true;
    private boolean shipSiegeFirstRender = true;
    private boolean bossBattleFirstRender = true;
    private boolean watermarkFPSFirstRender = true;
    private boolean watermarkPingFirstRender = true;
    private boolean watermarkTPSFirstRender = true;
    private boolean watermarkCoordsFirstRender = true;
    private boolean watermarkBPSFirstRender = true;
    private boolean useTotemFirstRender = true;
    private boolean useItemFirstRender = true;
    private boolean watermarkSettingsOpen = false;
    private final TimerUtil watermarkSettingsTimer = new TimerUtil();
    private boolean watermarkSettingsAllow = false;
    private final Animation watermarkSettingsAlpha = new DecelerateAnimation(250, 1.0);
    public final Animation notificationSettingsAlpha = new DecelerateAnimation(250, 1.0);
    public CopyOnWriteArrayList<net.minecraft.util.text.TextComponent> components = new CopyOnWriteArrayList<>();

    public Interface() {
        addSettings(elements, size);
        this.targetInfoAnimation = new DecelerateAnimation(300, 1.0);
    }

    private float getScale() {
        return size.getValue().floatValue();
    }

    private StyledFont getFont(int baseSize) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * getScale()), Fonts.newcode.length - 1));
        StyledFont font = Fonts.newcode[scaledSize];
        if (font == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.newcode.length && Fonts.newcode[i] != null) {
                    return Fonts.newcode[i];
                }
            }
            return Fonts.newcode[Math.min(14, Fonts.newcode.length - 1)];
        }
        return font;
    }

    private int getIconSize(int baseSize) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * getScale()), Fonts.icon.length - 1));
        if (Fonts.icon[scaledSize] == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.icon.length && Fonts.icon[i] != null) {
                    return i;
                }
            }
            return Math.min(15, Fonts.icon.length - 1);
        }
        return scaledSize;
    }

    private int getIcon2Size(int baseSize) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * getScale()), Fonts.icon2.length - 1));
        if (Fonts.icon2[scaledSize] == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.icon2.length && Fonts.icon2[i] != null) {
                    return i;
                }
            }
            return Math.min(15, Fonts.icon2.length - 1);
        }
        return scaledSize;
    }

    private int getBlodSize(int baseSize) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * getScale()), Fonts.blod.length - 1));
        if (Fonts.blod[scaledSize] == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.blod.length && Fonts.blod[i] != null) {
                    return i;
                }
            }
            return Math.min(12, Fonts.blod.length - 1);
        }
        return scaledSize;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate && elements.get(6)) {
            staffPlayers.clear();
            for (ScorePlayerTeam team : mc.world.getScoreboard().getTeams().stream().sorted(Comparator.comparing(Team::getName)).toList()) {
                String name = team.getMembershipCollection().toString();
                name = name.substring(1, name.length() - 1);
                if (namePattern.matcher(name).matches()) {
                    String cleanedPrefix = ReplaceUtil.replaceCustomFonts(team.getPrefix().getString());
                    if (prefixMatches.matcher(cleanedPrefix.toLowerCase(Locale.ROOT)).matches() || (Manager.STAFF_MANAGER != null && Manager.STAFF_MANAGER.isStaff(name))) {
                        staffPlayers.add(new StaffPlayer(name, team.getPrefix()));
                        staffJoinTimes.putIfAbsent(name, LocalDateTime.now());
                    }
                }
            }
        }
        if (event instanceof EventRender eventRender && eventRender.isRender2D()) {
            handleRender(eventRender);
        }
        return false;
    }

    private void handleRender(EventRender renderEvent) {
        final MatrixStack stack = renderEvent.matrixStack;
        if (!this.mc.gameSettings.showDebugInfo) {
            if (elements.get(0)) renderWatermark(stack);
            if (elements.get(1)) renderKeyBinds(stack);
            if (elements.get(2)) renderTotem(stack, renderEvent);
            if (elements.get(3)) renderArmor(renderEvent);
            if (elements.get(5)) onStaffListRender(stack, renderEvent);
            if (elements.get(6)) renderPotion(stack, renderEvent);
            if (elements.get(8)) renderTarget(stack);
            if (elements.get(7)) renderTimer(stack);
            if (elements.get(9)) renderEvents(stack);
            if (elements.get(10)) renderCooldown(stack, renderEvent);
        }

    }

    private void renderWatermark(MatrixStack stack) {
        float scale = getScale();
        StyledFont font = getFont(14);
        String name = "Nuclear";
        float widthname = font.getWidth(name) + 20;

        String user = Manager.USER_PROFILE.getName();
        float widthuser = font.getWidth(user) + 20;

        // Подготовка данных для элементов
        String fpsNumber = watermarkFPS.get() ? String.valueOf(mc.debugFPS) : "";
        String fpsUnit = watermarkFPS.get() ? "fps" : "";
        String fps = watermarkFPS.get() ? fpsNumber + fpsUnit : "";
        float widthfps = watermarkFPS.get() ? font.getWidth(fps) + 19 : 0;

        String pingNumber = watermarkPing.get() ? String.valueOf(HudUtil.calculatePing()) : "";
        String pingUnit = watermarkPing.get() ? "ms" : "";
        String ping = watermarkPing.get() ? pingNumber + pingUnit : "";
        float widthping = watermarkPing.get() ? font.getWidth(ping) + 19 : 0;

        String tpsNumber = watermarkTPS.get() ? String.valueOf(Nuclear.getServerTPS().getTPS()) : "";
        String tpsUnit = watermarkTPS.get() ? "tps" : "";
        String tps = watermarkTPS.get() ? tpsNumber + tpsUnit : "";
        float widthtps = watermarkTPS.get() ? font.getWidth(tps) + 19 : 0;

        boolean hideCoords = Manager.FUNCTION_MANAGER.streamerMode != null && Manager.FUNCTION_MANAGER.streamerMode.state && StreamerMode.hidenCord.get();
        int x = hideCoords ? 0 : (int) mc.player.getPosX();
        int y = hideCoords ? 0 : (int) mc.player.getPosY();
        int z = hideCoords ? 0 : (int) mc.player.getPosZ();
        String cord = watermarkCoords.get() ? "x" + x + " y" + y + " z" + z : "";
        float widthcord = watermarkCoords.get() ? font.getWidth(cord) + 20 : 0;

        String bpsNumber = watermarkBPS.get() ? HudUtil.calculateBPS() : "";
        String bpsUnit = watermarkBPS.get() ? "bps" : "";
        String bps = watermarkBPS.get() ? bpsNumber + bpsUnit : "";
        float widthbps = watermarkBPS.get() ? font.getWidth(bps) + (watermarkCoords.get() ? 18 : 20) : 0;

        int iconColor = getColorByName("iconColor");
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int textColor = getColorByName("textColor");
        java.awt.Color baseColor = new java.awt.Color(getColorByName("textColor"));
        float[] hsb = java.awt.Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float darkerBrightness = Math.max(0f, hsb[2] - 0.2f);
        java.awt.Color darkerColor = java.awt.Color.getHSBColor(hsb[0], hsb[1], darkerBrightness);
        int firstColor = darkerColor.getRGB();

        float baseHeight = 15f * scale;
        float baseRadius = 3.5f * scale;
        int iconSize = getIconSize(16);
        int iconSize2 = getIconSize(15);

        float textY = 7 + 6 * scale;
        float iconY = 7 + 6.5f * scale;
        float starY = 7 + 7.5f * scale;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(7, 7, widthname, baseHeight, baseRadius, fonColor, 1);
        Fonts.icon[iconSize].drawCenteredString(stack, "<", 15, iconY, iconColor);
        font.drawString(stack, name, 21, textY, textColor);

        float secondRowWidth = widthuser + widthfps + widthping + widthtps;
        RenderUtils.Render2D.drawBlurredRoundedRectangle(7 + font.getWidth(name) + 22, 7, secondRowWidth, baseHeight, baseRadius, fonColor, 1);


        Fonts.icon[iconSize2].drawCenteredString(stack, "U", 15 + font.getWidth(name) + 22, iconY, iconColor);
        font.drawString(stack, user, 21 + font.getWidth(name) + 22, textY, textColor);

        float currentX = 21 + font.getWidth(name) + 22 + font.getWidth(user) + 19;
        boolean hasAnyElement = watermarkFPS.get() || watermarkPing.get() || watermarkTPS.get();
        if (hasAnyElement) {
            font.drawString(stack, "*", currentX - 14, starY, new Color(60, 60, 60, 255).getRGB());
        }

        if (watermarkFPS.get()) {
            Fonts.icon[iconSize2].drawCenteredString(stack, "G", currentX - 3, iconY, iconColor);
            font.drawString(stack, fpsNumber, currentX + 2, textY, textColor);
            font.drawString(stack, fpsUnit, currentX + 2 + font.getWidth(fpsNumber), textY, firstColor);
            currentX += font.getWidth(fps) + 19;
            if (watermarkPing.get() || watermarkTPS.get()) {
                font.drawString(stack, "*", currentX - 14, starY, new Color(60, 60, 60, 255).getRGB());
            }
        }
        if (watermarkPing.get()) {
            Fonts.icon[iconSize2].drawCenteredString(stack, "h", currentX - 3, iconY, iconColor);
            font.drawString(stack, pingNumber, currentX + 2, textY, textColor);
            font.drawString(stack, pingUnit, currentX + 2 + font.getWidth(pingNumber), textY, firstColor);
            currentX += font.getWidth(ping) + 19;
            if (watermarkTPS.get()) {
                font.drawString(stack, "*", currentX - 14, starY, new Color(60, 60, 60, 255).getRGB());
            }
        }
        if (watermarkTPS.get()) {
            Fonts.icon[iconSize2].drawCenteredString(stack, "P", currentX - 3, iconY, iconColor);
            font.drawString(stack, tpsNumber, currentX + 2, textY, textColor);
            font.drawString(stack, tpsUnit, currentX + 2 + font.getWidth(tpsNumber), textY, firstColor);
        }

        // Третья строка: Координаты + BPS
        boolean hasThirdRow = watermarkCoords.get() || watermarkBPS.get();
        if (hasThirdRow) {
            float rowOffset = 17 * scale;
            float thirdRowWidth = widthcord + widthbps;
            RenderUtils.Render2D.drawBlurredRoundedRectangle(7, 7 + rowOffset, thirdRowWidth, baseHeight, baseRadius, fonColor, 1);

            float thirdRowX = 21;
            if (watermarkCoords.get()) {
                Fonts.icon[iconSize2].drawCenteredString(stack, "p", 15, iconY + rowOffset, iconColor);
                float cordX = thirdRowX;
                font.drawString(stack, "x", cordX, textY + rowOffset, firstColor);
                cordX += font.getWidth("x");
                font.drawString(stack, String.valueOf(x), cordX, textY + rowOffset, textColor);
                cordX += font.getWidth(String.valueOf(x));
                font.drawString(stack, " y", cordX, textY + rowOffset, firstColor);
                cordX += font.getWidth(" y");
                font.drawString(stack, String.valueOf(y), cordX, textY + rowOffset, textColor);
                cordX += font.getWidth(String.valueOf(y));
                font.drawString(stack, " z", cordX, textY + rowOffset, firstColor);
                cordX += font.getWidth(" z");
                font.drawString(stack, String.valueOf(z), cordX, textY + rowOffset, textColor);
                if (watermarkBPS.get()) {
                    font.drawString(stack, "*", 5 + widthcord, starY + rowOffset, new Color(60, 60, 60, 255).getRGB());
                }
                thirdRowX += widthcord;
            }
            if (watermarkBPS.get()) {
                Fonts.icon[iconSize2].drawCenteredString(stack, "e", 15 + widthcord, iconY + rowOffset, iconColor);
                font.drawString(stack, bpsNumber, thirdRowX, textY + rowOffset, textColor);
                font.drawString(stack, bpsUnit, thirdRowX + font.getWidth(bpsNumber), textY + rowOffset, firstColor);
            }
        }

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        if (isChatOpen && watermarkSettingsOpen) {
            watermarkSettingsTimer.reset();
            watermarkSettingsAllow = true;
        } else if (watermarkSettingsAllow && watermarkSettingsTimer.isReached(300)) {
            watermarkSettingsAllow = false;
        }

        boolean shouldShowSettings = watermarkSettingsAllow;
        watermarkSettingsAlpha.setDirection(shouldShowSettings ? Direction.FORWARDS : Direction.BACKWARDS);
        float settingsAlphaProgress = (float) watermarkSettingsAlpha.getOutput();

        if (settingsAlphaProgress > 0.0) {
            float watermarkWidth = calculateWatermarkWidth();
            renderWatermarkSettingsMenu(stack, 7, 7, watermarkWidth, settingsAlphaProgress);
        } else if (!isChatOpen) {
            watermarkSettingsOpen = false;
            watermarkSettingsAllow = false;
        }
    }

    private float calculateWatermarkWidth() {
        float scale = getScale();
        StyledFont font = getFont(14);
        String name = "Nuclear";
        float widthname = font.getWidth(name) + 20;
        String user = Manager.USER_PROFILE.getName();
        float widthuser = font.getWidth(user) + 20;

        float secondRowWidth = widthuser;
        if (watermarkFPS.get()) {
            String fps = String.valueOf(mc.debugFPS) + "fps";
            secondRowWidth += font.getWidth(fps) + 19;
        }
        if (watermarkPing.get()) {
            String ping = String.valueOf(HudUtil.calculatePing()) + "ms";
            secondRowWidth += font.getWidth(ping) + 19;
        }
        if (watermarkTPS.get()) {
            String tps = String.valueOf(Nuclear.getServerTPS().getTPS()) + "tps";
            secondRowWidth += font.getWidth(tps) + 19;
        }

        float firstRowWidth = Math.max(widthname, secondRowWidth);

        float thirdRowWidth = 0;
        if (watermarkCoords.get()) {
            boolean hideCoords = Manager.FUNCTION_MANAGER.streamerMode != null && Manager.FUNCTION_MANAGER.streamerMode.state && StreamerMode.hidenCord.get();
            int x = hideCoords ? 0 : (int) mc.player.getPosX();
            int y = hideCoords ? 0 : (int) mc.player.getPosY();
            int z = hideCoords ? 0 : (int) mc.player.getPosZ();
            String cord = "x" + x + " y" + y + " z" + z;
            thirdRowWidth += font.getWidth(cord) + 20;
        }
        if (watermarkBPS.get()) {
            String bps = HudUtil.calculateBPS() + "bps";
            thirdRowWidth += font.getWidth(bps) + (watermarkCoords.get() ? 0 : 20);
        }

        return Math.max(firstRowWidth, thirdRowWidth) + 48;
    }

    private void renderWatermarkSettingsMenu(MatrixStack stack, float hudX, float hudY, float hudWidth, float alphaProgress) {
        float menuX = hudX + hudWidth + 5f;
        float menuY = hudY;
        float menuWidth = 79f;
        float itemHeight = 8f;
        float padding = 4f;
        float headerHeight = 15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int fonAlpha = (int) (200 * alphaProgress);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), fonAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int yesColor = getColorByName("yesColor");
        int crossColor = getColorByName("crossColor");

        float menuHeight = headerHeight + (5 * itemHeight) + (4 * 2f) + padding;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(menuX, menuY, menuWidth, menuHeight, 4, fonColor, alphaProgress);

        Fonts.newcode[13].drawString(stack, "Watermark", menuX + padding - 0.5f, menuY + 5.5f, textColor);

        float itemY = menuY + headerHeight - 5f;
        float itemX = menuX + padding - 12.5f;

        double fpsMax = watermarkFPS.get() ? 8.5f : 0;
        if (watermarkFPSFirstRender) {
            this.watermarkFPSAnimation = (float) fpsMax;
            watermarkFPSFirstRender = false;
        } else {
            this.watermarkFPSAnimation = AnimationMath.fast(watermarkFPSAnimation, (float) fpsMax, 12);
        }
        renderBooleanSetting(stack, watermarkFPS, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, watermarkFPSAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double pingMax = watermarkPing.get() ? 8.5f : 0;
        if (watermarkPingFirstRender) {
            this.watermarkPingAnimation = (float) pingMax;
            watermarkPingFirstRender = false;
        } else {
            this.watermarkPingAnimation = AnimationMath.fast(watermarkPingAnimation, (float) pingMax, 12);
        }
        renderBooleanSetting(stack, watermarkPing, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, watermarkPingAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double tpsMax = watermarkTPS.get() ? 8.5f : 0;
        if (watermarkTPSFirstRender) {
            this.watermarkTPSAnimation = (float) tpsMax;
            watermarkTPSFirstRender = false;
        } else {
            this.watermarkTPSAnimation = AnimationMath.fast(watermarkTPSAnimation, (float) tpsMax, 12);
        }
        renderBooleanSetting(stack, watermarkTPS, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, watermarkTPSAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double coordsMax = watermarkCoords.get() ? 8.5f : 0;
        if (watermarkCoordsFirstRender) {
            this.watermarkCoordsAnimation = (float) coordsMax;
            watermarkCoordsFirstRender = false;
        } else {
            this.watermarkCoordsAnimation = AnimationMath.fast(watermarkCoordsAnimation, (float) coordsMax, 12);
        }
        renderBooleanSetting(stack, watermarkCoords, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, watermarkCoordsAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double bpsMax = watermarkBPS.get() ? 8.5f : 0;
        if (watermarkBPSFirstRender) {
            this.watermarkBPSAnimation = (float) bpsMax;
            watermarkBPSFirstRender = false;
        } else {
            this.watermarkBPSAnimation = AnimationMath.fast(watermarkBPSAnimation, (float) bpsMax, 12);
        }
        renderBooleanSetting(stack, watermarkBPS, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, watermarkBPSAnimation, elementAlpha);
    }

    public boolean handleWatermarkClick(double mouseX, double mouseY, int button) {
        if (!elements.get(0)) return false;

        float watermarkWidth = calculateWatermarkWidth();
        float watermarkHeight = 15f * getScale();
        boolean hasSecondRow = watermarkCoords.get() || watermarkBPS.get();
        if (hasSecondRow) {
            watermarkHeight += 17f * getScale();
        }

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        if (button == 1 && isChatOpen && MathUtil.isHovered((float) mouseX, (float) mouseY, 7, 7, watermarkWidth, watermarkHeight)) {
            watermarkSettingsOpen = !watermarkSettingsOpen;
            if (watermarkSettingsOpen) {
                targetSettingsOpen = false;
                eventsSettingsOpen = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
            } else {
                watermarkSettingsAllow = false;
            }
            return true;
        }

        if (button == 0 && isChatOpen && watermarkSettingsOpen) {
            float menuX = 7 + watermarkWidth + 14f;
            float menuY = 7;
            float padding = 4f;
            float headerHeight = 15f;
            float itemHeight = 8f;

            float itemX = menuX + padding - 12.5f;
            float itemY1 = menuY + headerHeight - 5f;
            float originalY1 = itemY1 + 4.5f - 8f;
            float originalH = itemHeight + 5 - 2;
            float itemW = 83.75f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY1, itemW, originalH)) {
                watermarkFPS.toggle();
                watermarkFPSFirstRender = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
                return true;
            }

            float itemY2 = itemY1 + itemHeight + 2f;
            float originalY2 = itemY2 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY2, itemW, originalH)) {
                watermarkPing.toggle();
                watermarkPingFirstRender = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
                return true;
            }

            float itemY3 = itemY2 + itemHeight + 2f;
            float originalY3 = itemY3 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY3, itemW, originalH)) {
                watermarkTPS.toggle();
                watermarkTPSFirstRender = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
                return true;
            }

            float itemY4 = itemY3 + itemHeight + 2f;
            float originalY4 = itemY4 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY4, itemW, originalH)) {
                watermarkCoords.toggle();
                watermarkCoordsFirstRender = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
                return true;
            }

            float itemY5 = itemY4 + itemHeight + 2f;
            float originalY5 = itemY5 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY5, itemW, originalH)) {
                watermarkBPS.toggle();
                watermarkBPSFirstRender = false;
                watermarkSettingsTimer.reset();
                watermarkSettingsAllow = true;
                return true;
            }
        }

        return false;
    }

    private final Animation kbAlpha = new DecelerateAnimation(250, 1.0);
    private final Animation widthAnim = new DecelerateAnimation(250, 1.0);
    private float animatedWidth = 40f;
    private float lastTargetWidth = 0f;
    private float startWidth = 40f;

    private void renderKeyBinds(MatrixStack stack) {
        float scale = getScale();
        float posX = keyBinds.getX();
        float posY = keyBinds.getY();

        float headerHeight = 15f * scale;
        float itemHeight = 9f * scale;

        java.util.List<Module> activeBinds = new java.util.ArrayList<>();
        float maxCombinedWidth = 0;

        StyledFont nameFont = getFont(16);
        StyledFont bindFont = getFont(12);

        if (Manager.FUNCTION_MANAGER == null) return;
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f == null) continue;
            f.animation.setDirection(f.state && f.bind != 0 ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!f.animation.isDone() || (f.state && f.bind != 0)) {
                activeBinds.add(f);

                String bindStr = nuclear.utils.ClientUtils.getKey(f.bind)
                        .replace("MOUSE", "M").replace("CONTROL", "C")
                        .replace("LEFT", "L").replace("RIGHT", "R").replace("_", "");
                String shortBind = bindStr.substring(0, Math.min(bindStr.length(), 4)).toUpperCase();

                float nameWidth = nameFont.getWidth(f.name);
                float bindWidth = bindFont.getWidth(shortBind);
                float combined = nameWidth + bindWidth + 30f * scale;
                if (combined > maxCombinedWidth) maxCombinedWidth = combined;
            }
        }

        boolean isChatOpen = this.mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean shouldShow = !activeBinds.isEmpty() || isChatOpen;

        kbAlpha.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);
        float alphaProgress = (float) kbAlpha.getOutput();

        if (alphaProgress <= 0.01) {
            animatedWidth = 70f * scale;
            return;
        }

        float targetWidth = shouldShow ? Math.max(70f * scale, maxCombinedWidth) : 70f * scale;

        if (targetWidth != lastTargetWidth) {
            startWidth = animatedWidth;
            lastTargetWidth = targetWidth;
            widthAnim.reset();
        }

        if (!widthAnim.isDone()) {
            animatedWidth = startWidth + (targetWidth - startWidth) * (float) widthAnim.getOutput();
        } else {
            animatedWidth = targetWidth;
        }

        float currentYOffset = 0;
        for (Module f : activeBinds) {
            currentYOffset += (float) (itemHeight * f.animation.getOutput());
        }
        float windowHeight = headerHeight + (activeBinds.isEmpty() ? (isChatOpen ? 3f * scale : 0f) : currentYOffset + 4f * scale);

        int elementAlpha = (int) (255 * alphaProgress);
        int infoColor = ColorUtils.setAlpha(getColorByName("infoColor"), elementAlpha);
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int iconnoColor = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int) (30 * alphaProgress));
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alphaProgress));

        RenderUtils.Render2D.drawBlurredRoundedRectangle(posX, posY, animatedWidth, windowHeight, 4 * scale, fonColor, alphaProgress);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        prepareScissor(posX, posY, animatedWidth, windowHeight);

        StyledFont headerFont = getFont(15);
        StyledFont itemFont = getFont(13);
        int icon2Size = getIcon2Size(15);

        if (animatedWidth > 50 * scale) {
            headerFont.drawString(stack, "Keybinds", posX + 4f * scale, posY + 6.5f * scale, infoColor);
            Fonts.icon2[icon2Size].drawString(stack, "f", posX + animatedWidth - 11 * scale, posY + 6.5f * scale, iconColor);
        }

        float renderY = posY + headerHeight + 1f * scale;
        for (Module f : activeBinds) {
            float anim = (float) f.animation.getOutput();

            if (anim > 0.01f) {
                stack.push();
                float itemCenterY = renderY + (itemHeight / 2f);

                stack.translate(posX + animatedWidth / 2f, itemCenterY, 0);
                stack.scale(1f, anim, 1f);
                stack.translate(-(posX + animatedWidth / 2f), -itemCenterY, 0);

                float slideOffset = (1f - anim) * 10f * scale;

                itemFont.drawString(stack, f.name, posX + 4f * scale - slideOffset, renderY + 3f * scale,
                        ColorUtils.setAlpha(textColor, (int) (220 * anim)));

                String bindStr = nuclear.utils.ClientUtils.getKey(f.bind)
                        .replace("MOUSE", "M").replace("CONTROL", "C")
                        .replace("LEFT", "L").replace("RIGHT", "R").replace("_", "");
                String shortBind = bindStr.substring(0, Math.min(bindStr.length(), 4)).toUpperCase();

                float bw = bindFont.getWidth(shortBind);
                float bx = posX + animatedWidth - bw - 5f * scale + slideOffset;

                if (animatedWidth > bw + 20 * scale) {
                    RenderUtils.Render2D.drawRoundedRect(bx - 2 * scale, renderY + 0.5f * scale, bw + 4 * scale, 8 * scale, 1.5f * scale,
                            ColorUtils.setAlpha(iconnoColor, (int) (elementAlpha * 0.4f * anim)));
                    bindFont.drawString(stack, shortBind, bx, renderY + 3.5f * scale,
                            ColorUtils.setAlpha(textColor, (int) (180 * anim)));
                }

                stack.pop();
                renderY += itemHeight * anim;
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        keyBinds.setWidth(animatedWidth);
        keyBinds.setHeight(windowHeight);
    }

    private void renderTotem(MatrixStack stack, EventRender renderEvent) {
        float scale = getScale();
        int totemCount = (int) mc.player.inventory.mainInventory.stream()
                .filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() == Items.TOTEM_OF_UNDYING)
                .count();
        totemCount += mc.player.inventory.offHandInventory.get(0).getItem() == Items.TOTEM_OF_UNDYING ? 1 : 0;
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        float x = totemCountDrag.getX();
        float y = totemCountDrag.getY();

        float width = 19 * scale;
        float height = 15 * scale;
        float radius = 2.5f * scale;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y, width, height, radius, fonColor, 1);

        int textColor = getColorByName("textColor");
        RenderSystem.depthMask(false);

        GlStateManager.pushMatrix();
        float itemScale = 0.6f * scale;
        GlStateManager.scaled(itemScale, itemScale, 1.0f);
        drawItemStack(new ItemStack(Items.TOTEM_OF_UNDYING), (int) ((x + 4.5f * scale) / itemScale), (int) ((y + 3f * scale) / itemScale), "", false);
        GlStateManager.popMatrix();
        RenderSystem.depthMask(true);
        StyledFont countFont = getFont(13);
        countFont.drawCenteredString(stack, String.valueOf(totemCount), x + 16 * scale, y + 9.5f * scale, textColor);
        totemCountDrag.setWidth(width);
        totemCountDrag.setHeight(height);
    }

    public static void drawItemStack(ItemStack stack, double x, double y, String altText, boolean withoutOverlay) {
        RenderSystem.translated(x, y, 0.0);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            String overlayText = (altText == null || altText.isEmpty()) ? null : altText;
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, overlayText);
        }
        RenderSystem.translated(-x, -y, 0.0);
    }

    private void renderArmor(final EventRender renderEvent) {
        float scale = getScale();
        float xPos = renderEvent.scaledResolution.scaledWidth() / 2f;
        float yPos = renderEvent.scaledResolution.scaledHeight();

        float off = 5 * scale;
        if (mc.player.isCreative()) {
            yPos += 14 * scale;
        } else {

            if (mc.player.getAir() < mc.player.getMaxAir() || mc.player.areEyesInFluid(FluidTags.WATER)) {
                yPos -= 10 * scale;
            }
        }
        for (ItemStack s : mc.player.inventory.armorInventory) {
            drawItemStack(s, xPos - off + 78 * (mc.gameSettings.guiScale / 2f) * scale, yPos - 55 * (mc.gameSettings.guiScale / 2f) * scale, null, false);
            off += 15 * scale;
        }
    }

    private String calculateTimeInList(LocalDateTime joinTime) {
        Duration duration = Duration.between(joinTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) :
                String.format("%d:%02d", minutes, seconds);
    }

    private void onStaffListRender(MatrixStack matrixStack, EventRender render) {
        float scale = getScale();
        float posX = staffList.getX();
        float posY = staffList.getY();

        float headerHeight = 15f * scale;
        float padding = 5f * scale;
        float itemHeight = 9f * scale;

        java.util.List<StaffPlayer> activeStaffList = new java.util.ArrayList<>();
        float maxCombinedWidth = 0;

        StyledFont prefixFont = getFont(12);
        StyledFont nameFont = getFont(13);

        for (StaffPlayer staff : staffPlayers.subList(0, Math.min(staffPlayers.size(), 10))) {
            String name = staff.getName();
            if (!staffAnimations.containsKey(name)) {
                staffAnimations.put(name, new EaseBackIn(300, 1, 1.5f));
            }

            Animation anim = staffAnimations.get(name);
            anim.setDirection(Direction.FORWARDS);

            if (!anim.isDone() || true) {
                activeStaffList.add(staff);

                String timeInList = calculateTimeInList(staffJoinTimes.getOrDefault(name, LocalDateTime.now()));
                float prefixWidth = prefixFont.getWidth(staff.getPrefix().getString());
                float nameWidth = nameFont.getWidth(name);
                float timeWidth = prefixFont.getWidth(timeInList);
                float combined = prefixWidth + nameWidth + timeWidth + padding;
                if (combined > maxCombinedWidth) maxCombinedWidth = combined;
            }
        }

        staffAnimations.entrySet().removeIf(entry -> {
            String name = entry.getKey();
            return activeStaffList.stream().noneMatch(s -> s.getName().equals(name)) &&
                    entry.getValue().getDirection() == Direction.BACKWARDS && entry.getValue().isDone();
        });

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        boolean shouldShow = !activeStaffList.isEmpty() || isChatOpen;

        staffListAnimation.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);
        float alphaProgress = (float) staffListAnimation.getOutput();

        if (alphaProgress <= 0.0) {
            return;
        }

        float currentTarget = Math.max(75f * scale, maxCombinedWidth + 5.5f * padding);

        int elementAlpha = (int) (255 * alphaProgress);
        int infoColor = ColorUtils.setAlpha(getColorByName("infoColor"), elementAlpha);
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int iconnoColor = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int) (30 * alphaProgress));
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alphaProgress));

        float currentYOffset = 0;
        for (StaffPlayer staff : activeStaffList) {
            Animation anim = staffAnimations.get(staff.getName());
            if (anim != null) {
                currentYOffset += (float) (itemHeight * anim.getOutput());
            }
        }
        float windowHeight = headerHeight + (activeStaffList.isEmpty() ? (isChatOpen ? 3f * scale : 0f) : currentYOffset + 4f * scale);

        StyledFont headerFont = getFont(15);
        int icon2Size = getIcon2Size(15);

        RenderUtils.Render2D.drawBlurredRoundedRectangle(posX, posY, currentTarget, windowHeight, 4 * scale, fonColor, alphaProgress);

        if (currentTarget > 45 * scale) {
            String staffText = "Staffs";
            headerFont.drawString(matrixStack, staffText, posX + 4.5f * scale, posY + 6.5f * scale, infoColor);
            Fonts.icon2[icon2Size].drawString(matrixStack, "h", posX + currentTarget - 12 * scale, posY + 6.5f * scale, iconColor);
        }

        float renderY = posY + headerHeight + 1f * scale;
        for (StaffPlayer staff : activeStaffList) {
            String name = staff.getName();
            Animation anim = staffAnimations.get(name);
            if (anim == null) continue;

            float animProgress = (float) anim.getOutput();

            if (animProgress > 0.01f) {
                matrixStack.push();
                float itemCenterY = renderY + (itemHeight / 2f);

                matrixStack.translate(posX + currentTarget / 2f, itemCenterY, 0);
                matrixStack.scale(1f, animProgress, 1f);
                matrixStack.translate(-(posX + currentTarget / 2f), -itemCenterY, 0);

                int finalAlpha = Math.max(0, Math.min(255, (int) (elementAlpha * animProgress)));
                float textSlide = (1f - animProgress) * -4f * scale;

                ITextComponent prefix = staff.getPrefix();
                float prefixWidth = prefixFont.getWidth(prefix.getString());
                prefixFont.drawText(matrixStack, prefix, posX + 4.5f * scale + textSlide, renderY + 3f * scale);
                nameFont.drawString(matrixStack, name, posX + 4.5f * scale + prefixWidth + textSlide, renderY + 3f * scale, ColorUtils.setAlpha(textColor, (int) (220 * animProgress)));

                String timeInList = calculateTimeInList(staffJoinTimes.getOrDefault(name, LocalDateTime.now()));
                int timeColor = staff.getStatus() == NEAR ? new Color(248, 215, 2, 255).getRGB() :
                        staff.getStatus() == SPEC ? new Color(59, 94, 184, 255).getRGB() :
                                staff.getStatus() == VANISHED ? new Color(205, 16, 16, 255).getRGB() :
                                        new Color(82, 188, 15, 255).getRGB();

                float tw = prefixFont.getWidth(timeInList);
                float tx = posX + currentTarget - tw - 6 * scale - textSlide;

                if (currentTarget > tw + 20 * scale) {
                    RenderUtils.Render2D.drawRoundedRect(tx - 2 * scale, renderY + 0.5f * scale, tw + 4 * scale, 8 * scale, 1.5f * scale, ColorUtils.setAlpha(iconnoColor, Math.max(0, Math.min(255, (int) (finalAlpha * 0.4f)))));
                    prefixFont.drawString(matrixStack, timeInList, tx, renderY + 3.5f * scale, ColorUtils.setAlpha(timeColor, Math.max(0, Math.min(255, (int) (180 * animProgress)))));
                }

                String prefixString = prefix.getString();
                if (prefixString.contains("§a●")) {
                    RenderUtils.Render2D.drawCircle2(posX + 6f * scale - 1f * scale + textSlide, renderY + 1.6f * (scale * 1.5f), 1.75F * scale, new Color(84, 252, 84, finalAlpha).getRGB());
                } else if (prefixString.contains("§c●")) {
                    RenderUtils.Render2D.drawCircle2(posX + 6f * scale - 1f * scale + textSlide, renderY + 1.6f * (scale * 1.5f), 1.75F * scale, new Color(252, 84, 84, finalAlpha).getRGB());
                } else if (prefixString.contains("§6●")) {
                    RenderUtils.Render2D.drawCircle2(posX + 6f * scale - 1f * scale + textSlide, renderY + 1.6f * (scale * 1.5f), 1.75F * scale, new Color(252, 168, 0, finalAlpha).getRGB());
                } else if (prefixString.contains("●")) {
                    RenderUtils.Render2D.drawCircle2(posX + 6f * scale - 1f * scale + textSlide, renderY + 1.6f * (scale * 1.5f), 1.75F * scale, new Color(252, 84, 84, finalAlpha).getRGB());
                } else if (prefixString.contains("•")) {
                    RenderUtils.Render2D.drawCircle2(posX + 6f * scale - 1f * scale + textSlide, renderY + 1.6f * (scale * 1.5f), 1.75F * scale, new Color(252, 84, 84, finalAlpha).getRGB());
                }

                matrixStack.pop();
            }
            renderY += itemHeight * animProgress;
        }

        staffJoinTimes.keySet().removeIf(staffName -> activeStaffList.stream().noneMatch(s -> s.getName().equals(staffName)));
        staffList.setWidth(currentTarget);
        staffList.setHeight(windowHeight);
    }

    private class StaffPlayer {

        @Getter
        String name;
        @Getter
        ITextComponent prefix;
        @Getter
        Status status;

        private StaffPlayer(String name, ITextComponent prefix) {
            this.name = name;
            this.prefix = prefix;

            updateStatus();
        }

        private void updateStatus() {
            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equals(name)) {
                    status = NEAR;
                    return;
                }
            }

            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    if (info.getGameType() == GameType.SPECTATOR) {
                        status = SPEC;
                        return;
                    }
                    status = NONE;
                    return;
                }
            }

            status = VANISHED;
        }
    }

    public enum Status {
        NONE(""),
        NEAR(" §e[NEAR]"),
        SPEC(" §c[SPEC]"),
        VANISHED(" §6[VANISHED]");

        @Getter
        final String string;

        Status(String string) {
            this.string = string;
        }
    }

    private final Map<Item, Animation> cooldownAnims = new HashMap<>();
    private final Animation chatAnimation = new EaseBackIn(300, 1, 1.5f);
    private float animatedCDWidth = 20f;

    private void renderCooldown(final MatrixStack matrixStack, final EventRender renderEvent) {
        if (mc.player == null) return;

        float scale = getScale();
        float x = this.HUDCooldown.getX();
        float y = this.HUDCooldown.getY();
        float squareSize = 20.0F * scale;
        float spacing = 4.0F * scale;

        CooldownTracker tracker = mc.player.getCooldownTracker();
        Map<Item, CooldownTracker.Cooldown> cooldowns = tracker.cooldowns;
        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        // Create a defensive copy to avoid ConcurrentModificationException
        Set<Item> cooldownItems = new HashSet<>(cooldowns.keySet());
        for (Item item : cooldownItems) {
            cooldownAnims.computeIfAbsent(item, k -> new EaseBackIn(250, 1, 1.3f)).setDirection(Direction.FORWARDS);
        }
        cooldownAnims.forEach((item, anim) -> {
            if (!cooldowns.containsKey(item)) anim.setDirection(Direction.BACKWARDS);
        });

        boolean shouldShow = !cooldownAnims.isEmpty() || isChatOpen;
        chatAnimation.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);

        float alphaProgress = (float) Math.max(0, Math.min(1, chatAnimation.getOutput()));
        if (alphaProgress <= 0.0) return;

        float targetWidth = 0;
        int visibleCount = 0;
        for (Animation a : cooldownAnims.values()) {
            float out = (float) Math.max(0, a.getOutput());
            if (out > 0.01f) {
                targetWidth += (squareSize + spacing) * out;
                visibleCount++;
            }
        }
        float finalTarget = (visibleCount > 0) ? targetWidth - spacing : squareSize;
        animatedCDWidth += (finalTarget - animatedCDWidth) * 0.15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alphaProgress));
        int primaryColor = ColorUtils.setAlpha(getColorByName("primaryColor"), (int) (120 * alphaProgress));
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);

        if (visibleCount == 0 && isChatOpen) {
            RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y, squareSize, squareSize, 3.5f * scale, fonColor, alphaProgress);
            String iconText = "m";
            int iconSize = getIconSize(20);
            float iw = Fonts.icon[iconSize].getWidth(iconText);
            Fonts.icon[iconSize].drawString(matrixStack, iconText, x + (squareSize - iw) / 2f + 0.5f * scale, y + (squareSize - 5f * scale) / 2f, iconColor);
            this.HUDCooldown.setWidth(squareSize);
            return;
        }

        float totalWidth = 0;
        int actualVisibleCount = 0;
        for (Animation a : cooldownAnims.values()) {
            float out = (float) Math.max(0, a.getOutput());
            if (out > 0.01f) {
                totalWidth += (squareSize + spacing) * out;
                actualVisibleCount++;
            }
        }
        if (totalWidth > 0) {
            totalWidth -= spacing;
        }

        float startX;
        if (actualVisibleCount <= 1) {
            startX = x;
        } else {
            startX = x - (totalWidth - squareSize) / 2f;
        }
        float currentX = startX;
        java.util.Iterator<Map.Entry<Item, Animation>> it = cooldownAnims.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Item, Animation> entry = it.next();
            Item item = entry.getKey();
            Animation anim = entry.getValue();
            float animOut = (float) Math.max(0, anim.getOutput());

            if (anim.isDone() && anim.getDirection() == Direction.BACKWARDS) {
                it.remove();
                continue;
            }

            if (animOut > 0.01f) {
                matrixStack.push();
                float centerX = currentX + squareSize / 2f;
                float centerY = y + squareSize / 2f;

                matrixStack.translate(centerX, centerY, 0);
                matrixStack.scale(animOut, animOut, 1);
                matrixStack.translate(-centerX, -centerY, 0);

                RenderUtils.Render2D.drawBlurredRoundedRectangle(currentX, y, squareSize, squareSize, 3.5f * scale, fonColor, alphaProgress * animOut);

                StencilUtils.initStencilToWrite();
                RenderUtils.Render2D.drawRoundedRect(currentX, y, squareSize, squareSize, 3.5f * scale, -1);
                StencilUtils.readStencilBuffer(1);

                RenderSystem.pushMatrix();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableDepthTest();
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, alphaProgress * animOut);

                drawItemStack(new ItemStack(item), currentX + 2f * scale, y + 2f * scale, "", true);

                RenderSystem.disableDepthTest();
                RenderSystem.popMatrix();

                float progressBar = tracker.getCooldown(item, renderEvent.getPartialTicks());
                if (progressBar > 0) {
                    RenderUtils.Render2D.drawRoundedRect(currentX, y, squareSize, squareSize * progressBar, 0, primaryColor);
                }

                StencilUtils.uninitStencilBuffer();

                if (cooldowns.containsKey(item)) {
                    float remTicks = cooldowns.get(item).getExpireTicks() - tracker.getTicks();
                    String durationText = String.format(Locale.ROOT, "%.1f", Math.max(0, remTicks / 20.0f));

                    int durationFontSize = getBlodSize(12);
                    float tw = Fonts.blod[durationFontSize].getWidth(durationText);
                    float th = 5f * scale;

                    int textAlpha = (int) Math.max(0, Math.min(255, 255 * animOut * alphaProgress));

                    float textX = currentX + (squareSize - tw) / 2f;
                    float textY = y + (squareSize - th) / 2f;

                    Fonts.blod[durationFontSize].drawString(matrixStack, durationText, textX, textY, ColorUtils.setAlpha(-1, textAlpha));
                }

                matrixStack.pop();
                currentX += (squareSize + spacing) * animOut;
            }
        }
        this.HUDCooldown.setWidth(animatedCDWidth);
        this.HUDCooldown.setHeight(20);
    }

    public final Dragging HUDPotion = Nuclear.createDrag(this, "Potion", 7, 42);
    private final Map<Integer, Animation> potionAnimations = new HashMap<>();
    private final Map<Integer, EffectInstance> lastKnownEffects = new HashMap<>();
    private final Animation potionAlphaAnim = new EaseBackIn(300, 1, 1.5f);
    private float animatedPotionWidth = 75f;

    private void renderPotion(final MatrixStack matrixStack, final EventRender renderEvent) {
        if (mc.player == null) return;

        float scale = getScale();
        float posX = HUDPotion.getX();
        float posY = HUDPotion.getY();
        float headerHeight = 15f * scale;
        float itemHeight = 9f * scale;

        java.util.Collection<EffectInstance> currentEffects = mc.player.getActivePotionEffects();

        for (EffectInstance effect : currentEffects) {
            int id = System.identityHashCode(effect);
            lastKnownEffects.put(id, effect);
            potionAnimations.computeIfAbsent(id, k -> new EaseBackIn(300, 1, 1.5f)).setDirection(Direction.FORWARDS);
        }

        potionAnimations.forEach((id, anim) -> {
            if (currentEffects.stream().noneMatch(e -> System.identityHashCode(e) == id)) {
                anim.setDirection(Direction.BACKWARDS);
            }
        });

        java.util.List<Integer> idsToRender = new java.util.ArrayList<>();
        float maxCombinedWidth = 0;

        StyledFont effectFont = getFont(13);
        StyledFont durationFont = getFont(12);

        for (Map.Entry<Integer, Animation> entry : potionAnimations.entrySet()) {
            int id = entry.getKey();
            Animation anim = entry.getValue();

            if (!anim.isDone() || anim.getDirection() == Direction.FORWARDS) {
                EffectInstance instance = lastKnownEffects.get(id);
                if (instance != null) {
                    idsToRender.add(id);
                    String text = I18n.format(instance.getEffectName());
                    String durationText = EffectUtils.getPotionDurationString(instance, 1.0F);
                    float combined = effectFont.getWidth(text) + durationFont.getWidth(durationText) + 35f * scale;
                    if (combined > maxCombinedWidth) maxCombinedWidth = combined;
                }
            }
        }

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        boolean shouldShow = !idsToRender.isEmpty() || isChatOpen;
        potionAlphaAnim.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);

        float alphaProgress = (float) Math.max(0, Math.min(1, potionAlphaAnim.getOutput()));

        if (alphaProgress <= 0.0) {
            animatedPotionWidth = 75f * scale;
            return;
        }

        float targetWidth = shouldShow ? Math.max(75f * scale, maxCombinedWidth) : 75f * scale;
        animatedPotionWidth += (targetWidth - animatedPotionWidth) * 0.15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int infoColor = ColorUtils.setAlpha(getColorByName("infoColor"), elementAlpha);
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int iconnoColor = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int) (30 * alphaProgress));
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alphaProgress));

        float currentYOffset = 0;
        for (int id : idsToRender) {
            currentYOffset += itemHeight * (float) Math.max(0, potionAnimations.get(id).getOutput());
        }
        float windowHeight = headerHeight + (idsToRender.isEmpty() ? (isChatOpen ? 3f * scale : 0f) : currentYOffset + 4f * scale);

        StyledFont headerFont = getFont(15);
        int iconSize = getIconSize(15);

        RenderUtils.Render2D.drawBlurredRoundedRectangle(posX, posY, animatedPotionWidth, windowHeight, 4 * scale, fonColor, alphaProgress);

        if (animatedPotionWidth > 50 * scale) {
            headerFont.drawString(matrixStack, "Potions", posX + 4.5f * scale, posY + 6.5f * scale, infoColor);
            Fonts.icon[iconSize].drawString(matrixStack, "z", posX + animatedPotionWidth - 12 * scale, posY + 6.5f * scale, iconColor);
        }


        float renderY = posY + headerHeight + 1f * scale;
        for (int id : idsToRender) {
            EffectInstance instance = lastKnownEffects.get(id);
            Animation anim = potionAnimations.get(id);
            float animProgress = (float) Math.max(0, anim.getOutput());

            if (animProgress > 0.001f) {
                float flashingAlpha = 1.0f;
                if (instance.getDuration() > 0 && instance.getDuration() <= 100) {
                    flashingAlpha = 0.4f + (float) (Math.sin(System.currentTimeMillis() / 150.0) + 1.0) / 3.33f;
                }

                matrixStack.push();
                float itemCenterY = renderY + (itemHeight / 2f);

                matrixStack.translate(posX + animatedPotionWidth / 2f, itemCenterY, 0);
                matrixStack.scale(1f, animProgress, 1f);
                matrixStack.translate(-(posX + animatedPotionWidth / 2f), -itemCenterY, 0);

                float slideOffset = (1f - animProgress) * 10f * scale;

                int combinedAlpha = (int) (elementAlpha * animProgress * flashingAlpha);
                int finalAlpha = Math.max(0, Math.min(255, combinedAlpha));

                Effect effect = instance.getPotion();
                String text = I18n.format(instance.getEffectName());
                String durationText = EffectUtils.getPotionDurationString(instance, 1.0F);
                String levelText = instance.getAmplifier() != 0 ? " " + (instance.getAmplifier() + 1) : "";

                int effectColor = !effect.isBeneficial() ?
                        new Color(182, 35, 35, finalAlpha).getRGB() :
                        ColorUtils.setAlpha(textColor, (int) (220 * animProgress * flashingAlpha));

                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.color4f(1f, 1f, 1f, (float) finalAlpha / 255f);

                TextureAtlasSprite sprite = mc.getPotionSpriteUploader().getSprite(effect);
                mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
                blitSprite(matrixStack, posX + 4.5f * scale - slideOffset, renderY + 1f * scale, 10, (int) (7 * scale), (int) (7 * scale), sprite);

                RenderSystem.color4f(1f, 1f, 1f, 1f);
                RenderSystem.depthMask(true);

                effectFont.drawString(matrixStack, text, posX + 13f * scale - slideOffset, renderY + 3f * scale, effectColor);

                if (instance.getAmplifier() != 0) {
                    float tw = effectFont.getWidth(text);
                    durationFont.drawString(matrixStack, levelText, posX + 13f * scale + tw - slideOffset, renderY + 3f * scale, ColorUtils.setAlpha(textColor, (int) (200 * animProgress * flashingAlpha)));
                }

                float timeW = durationFont.getWidth(durationText);
                float tx = posX + animatedPotionWidth - timeW - 6f * scale + slideOffset;
                if (animatedPotionWidth > timeW + 25 * scale) {
                    int rectAlpha = (int) (finalAlpha * 0.4f);
                    RenderUtils.Render2D.drawRoundedRect(tx - 2 * scale, renderY + 0.5f * scale, timeW + 4 * scale, 8 * scale, 1.5f * scale, ColorUtils.setAlpha(iconnoColor, rectAlpha));
                    durationFont.drawString(matrixStack, durationText, tx, renderY + 3.5f * scale, ColorUtils.setAlpha(textColor, (int) (180 * animProgress * flashingAlpha)));
                }

                matrixStack.pop();
                renderY += itemHeight * animProgress;
            }
        }

        potionAnimations.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone() && entry.getValue().getDirection() == Direction.BACKWARDS) {
                lastKnownEffects.remove(entry.getKey());
                return true;
            }
            return false;
        });

        HUDPotion.setWidth(animatedPotionWidth);
        HUDPotion.setHeight(windowHeight);
    }


    private void renderTimer(MatrixStack stack) {
        float scale = getScale();
        int firstColor = getColorByName("primaryColor");
        int fonColorBase = ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int textColor = getColorByName("textColor");

        if (Manager.FUNCTION_MANAGER == null || Manager.FUNCTION_MANAGER.timer == null || Manager.FUNCTION_MANAGER.timer.timerAmount == null) return;
        float timerAmountValue = Manager.FUNCTION_MANAGER.timer.timerAmount.getValue().floatValue();
        if (timerAmountValue == 0) return; // Защита от деления на ноль
        float quotient = Manager.FUNCTION_MANAGER.timer.maxViolation / timerAmountValue;
        float minimumValue = Math.min(Manager.FUNCTION_MANAGER.timer.getViolation(), quotient);
        this.perc = AnimationMath.lerp(this.perc, (quotient - minimumValue) / quotient, 10.0F);

        int percentage = (int) MathHelper.clamp(this.perc * 100, 0, 100);

        float x = this.TimerHUD.getX();
        float y = this.TimerHUD.getY();
        float width = 55f * scale;
        float height = 22f * scale;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y, width, height, 3 * scale, fonColorBase, 1.0f);

        Color baseColor = new Color(firstColor);
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float darkerBrightness = Math.max(0f, hsb[2] - 0.4f);
        int darkerColor = Color.getHSBColor(hsb[0], hsb[1], darkerBrightness).getRGB();

        float barX = x + 3f * scale;
        float barY = y + 14f * scale;
        float barWidth = width - 6f * scale;
        float barHeight = 4f * scale;

        int bgBarColor = ColorUtils.setAlpha(firstColor, 82);
        RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth, barHeight, 0.5F * scale, bgBarColor);

        RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth * this.perc, barHeight, 2F * scale, firstColor, darkerColor);

        String percText = percentage + "%";
        StyledFont textFont = getFont(10);
        StyledFont mainFont = getFont(13);
        float textX = x + (width / 2f) - (textFont.getWidth(percText) / 2f);
        mainFont.drawString(stack, percText, textX, y + 6f * scale, textColor);

        this.TimerHUD.setWidth(width);
        this.TimerHUD.setHeight(height);
    }

    private void updateCountdown3() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
        LocalDateTime targetTime = now.withHour((now.getHour() / 6) * 6).withMinute(0).withSecond(0);

        if (!now.isBefore(targetTime)) {
            targetTime = targetTime.plusHours(6);
        }

        long hours = ChronoUnit.HOURS.between(now, targetTime);
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();
        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m ");
        }
        countdownBuilder.append(seconds).append("s");

        countdownText3 = countdownBuilder.toString().trim();
    }

    private void updateCountdown4() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        // Находим следующую субботу в 17:30
        LocalDateTime targetTime = now.with(DayOfWeek.SATURDAY).withHour(17).withMinute(30).withSecond(0);

        if (now.isAfter(targetTime) || now.equals(targetTime)) {
            targetTime = targetTime.plusWeeks(1);
        }

        long totalDays = ChronoUnit.DAYS.between(now, targetTime);
        long hours = ChronoUnit.HOURS.between(now, targetTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();

        if (totalDays > 0) {
            countdownBuilder.append(totalDays).append("d ");
        }
        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m");
        }
        // Если есть дни, секунды не показываем. Если дней нет, показываем секунды только если они > 0
        if (totalDays == 0 && seconds > 0) {
            if (countdownBuilder.length() > 0) {
                countdownBuilder.append(" ");
            }
            countdownBuilder.append(seconds).append("s");
        }

        countdownText4 = countdownBuilder.toString().trim();
    }

    private void updateCountdown5() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        // Дни недели для битвы с боссом: понедельник, среда, пятница в 17:30
        List<DayOfWeek> eventDays = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        LocalDateTime targetTime = null;

        // Проверяем сегодняшний день
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalDateTime todayEvent = now.withHour(17).withMinute(30).withSecond(0);

        if (eventDays.contains(currentDay) && now.isBefore(todayEvent)) {
            targetTime = todayEvent;
        } else {
            // Ищем следующий подходящий день
            for (int i = 1; i <= 7; i++) {
                LocalDateTime candidate = now.plusDays(i);
                DayOfWeek candidateDay = candidate.getDayOfWeek();
                if (eventDays.contains(candidateDay)) {
                    targetTime = candidate.withHour(17).withMinute(30).withSecond(0);
                    break;
                }
            }
        }

        if (targetTime == null) {
            countdownText5 = "";
            return;
        }

        long totalDays = ChronoUnit.DAYS.between(now, targetTime);
        long hours = ChronoUnit.HOURS.between(now, targetTime) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, targetTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, targetTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();

        if (totalDays > 0) {
            countdownBuilder.append(totalDays).append("d ");
        }
        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m");
        }
        // Если есть дни, секунды не показываем. Если дней нет, показываем секунды только если они > 0
        if (totalDays == 0 && seconds > 0) {
            if (countdownBuilder.length() > 0) {
                countdownBuilder.append(" ");
            }
            countdownBuilder.append(seconds).append("s");
        }

        countdownText5 = countdownBuilder.toString().trim();
    }

    private void updateCountdown() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));

        List<LocalDateTime> schedule = Arrays.asList(
                now.withHour(9).withMinute(0).withSecond(0),
                now.withHour(11).withMinute(0).withSecond(0),
                now.withHour(13).withMinute(0).withSecond(0),
                now.withHour(15).withMinute(0).withSecond(0),
                now.withHour(17).withMinute(0).withSecond(0),
                now.withHour(19).withMinute(0).withSecond(0),
                now.withHour(21).withMinute(0).withSecond(0),
                now.withHour(23).withMinute(0).withSecond(0)
        );


        LocalDateTime nextEventTime = schedule.stream()
                .filter(t -> t.isAfter(now))
                .findFirst()
                .orElse(now.withHour(7).plusDays(1));

        long hours = ChronoUnit.HOURS.between(now, nextEventTime);
        long minutes = ChronoUnit.MINUTES.between(now, nextEventTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, nextEventTime) % 60;

        StringBuilder countdownBuilder = new StringBuilder();

        if (hours > 0) {
            countdownBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            countdownBuilder.append(minutes).append("m ");
        }
        countdownBuilder.append(seconds).append("s");

        countdownText = countdownBuilder.toString().trim();
    }

    private final Animation eventsAlpha = new DecelerateAnimation(250, 1.0);
    private final Animation eventsWidthAnim = new DecelerateAnimation(250, 1.0);
    private float eventsStartWidth = 85f;
    private float eventsLastTargetWidth = 0f;

    private void renderEvents(MatrixStack stack) {
        float scale = getScale();
        float posX = events.getX();
        float posY = events.getY();

        float headerHeight = 15f * scale;
        float padding = 5f * scale;
        float itemHeight = 9f * scale;

        updateCountdown();
        updateCountdown3();
        updateCountdown4();
        updateCountdown5();

        List<String> eventNamesList = new ArrayList<>();
        List<String> timesList = new ArrayList<>();

        if (airDrop.get()) {
            eventNamesList.add("AirDrop");
            timesList.add(countdownText);
        }
        if (chest.get()) {
            eventNamesList.add("Chest");
            timesList.add(countdownText3);
        }
        if (shipSiege.get()) {
            eventNamesList.add("Siege");
            timesList.add(countdownText4);
        }
        if (bossBattle.get()) {
            eventNamesList.add("Boss Battle");
            timesList.add(countdownText5);
        }

        String[] eventNames = eventNamesList.toArray(new String[0]);
        String[] times = timesList.toArray(new String[0]);

        eventsAlpha.setDirection(Direction.FORWARDS);
        float alphaProgress = (float) eventsAlpha.getOutput();

        if (alphaProgress <= 0.0) {
            eventsAnimatedWidth = 70f * scale;
            return;
        }

        StyledFont nameFont = getFont(13);
        StyledFont timeFont = getFont(12);

        float maxCombinedWidth = 0;
        for (int i = 0; i < eventNames.length; i++) {
            float nameWidth = nameFont.getWidth(eventNames[i]);
            float timeWidth = timeFont.getWidth(times[i]);
            float combined = nameWidth + timeWidth + padding;
            if (combined > maxCombinedWidth) maxCombinedWidth = combined;
        }

        float currentTarget = Math.max(60f * scale, maxCombinedWidth + 6.5f * padding);

        if (currentTarget != eventsLastTargetWidth) {
            eventsStartWidth = eventsAnimatedWidth;
            eventsLastTargetWidth = currentTarget;
            eventsWidthAnim.reset();
        }

        if (!eventsWidthAnim.isDone()) {
            eventsAnimatedWidth = eventsStartWidth + (currentTarget - eventsStartWidth) * (float) eventsWidthAnim.getOutput();
        } else {
            eventsAnimatedWidth = currentTarget;
        }

        float renderX = posX + (currentTarget - eventsAnimatedWidth) / 2f;

        int elementAlpha = (int) (255 * alphaProgress);
        int infoColor = ColorUtils.setAlpha(getColorByName("infoColor"), elementAlpha);
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int iconnoColor = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int) (30 * alphaProgress));
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alphaProgress));

        float windowHeight = headerHeight + (eventNames.length * itemHeight) + 4f * scale;

        StyledFont headerFont = getFont(15);
        int icon2Size = getIcon2Size(15);

        RenderUtils.Render2D.drawBlurredRoundedRectangle(renderX, posY, eventsAnimatedWidth, windowHeight, 4 * scale, fonColor, alphaProgress);

        if (eventsAnimatedWidth > 45 * scale) {
            headerFont.drawString(stack, "Events", renderX + 4.5f * scale, posY + 6.5f * scale, infoColor);
            Fonts.icon2[icon2Size].drawString(stack, "g", renderX + eventsAnimatedWidth - 12 * scale, posY + 6.5f * scale, iconColor);
        }

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        if (isChatOpen && eventsSettingsOpen) {
            eventsSettingsTimer.reset();
            eventsSettingsAllow = true;
        } else if (eventsSettingsAllow && eventsSettingsTimer.isReached(300)) {
            eventsSettingsAllow = false;
        }

        boolean shouldShowSettings = eventsSettingsAllow;
        eventsSettingsAlpha.setDirection(shouldShowSettings ? Direction.FORWARDS : Direction.BACKWARDS);
        float settingsAlphaProgress = (float) eventsSettingsAlpha.getOutput();

        if (settingsAlphaProgress > 0.0) {
            renderEventsSettingsMenu(stack, renderX, posY, eventsAnimatedWidth, settingsAlphaProgress);
        } else if (!isChatOpen) {
            eventsSettingsOpen = false;
            eventsSettingsAllow = false;
        }

        float renderY = posY + headerHeight + 1f * scale;
        for (int i = 0; i < eventNames.length; i++) {

            nameFont.drawString(stack, eventNames[i], renderX + 4.5f * scale, renderY + 3f * scale, ColorUtils.setAlpha(textColor, (int) (220 * alphaProgress)));

            String timeStr = times[i];
            float tw = timeFont.getWidth(timeStr);
            float tx = renderX + eventsAnimatedWidth - tw - 6 * scale;

            if (eventsAnimatedWidth > tw + 20 * scale) {
                RenderUtils.Render2D.drawRoundedRect(tx - 2 * scale, renderY + 0.5f * scale, tw + 4 * scale, 8 * scale, 1.5f * scale, ColorUtils.setAlpha(iconnoColor, (int) (elementAlpha * 0.4f)));
                timeFont.drawString(stack, timeStr, tx, renderY + 3.5f * scale, ColorUtils.setAlpha(textColor, (int) (180 * alphaProgress)));
            }

            renderY += itemHeight;
        }

        events.setWidth(currentTarget);
        events.setHeight(windowHeight);
    }

    private void renderEventsSettingsMenu(MatrixStack stack, float hudX, float hudY, float hudWidth, float alphaProgress) {
        float menuX = hudX + hudWidth + 5f;
        float menuY = hudY;
        float menuWidth = 79f;
        float itemHeight = 8f;
        float padding = 4f;
        float headerHeight = 15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int fonAlpha = (int) (200 * alphaProgress);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), fonAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int yesColor = getColorByName("yesColor");
        int crossColor = getColorByName("crossColor");

        float menuHeight = headerHeight + (4 * itemHeight) + (3 * 2f) + padding;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(menuX, menuY, menuWidth, menuHeight, 4, fonColor, alphaProgress);

        Fonts.newcode[13].drawString(stack, "Events", menuX + padding - 0.5f, menuY + 5.5f, textColor);

        float itemY = menuY + headerHeight - 5f;
        float itemX = menuX + padding - 12.5f;

        double airDropMax = airDrop.get() ? 8.5f : 0;
        if (airDropFirstRender) {
            this.airDropAnimation = (float) airDropMax;
            airDropFirstRender = false;
        } else {
            this.airDropAnimation = AnimationMath.fast(airDropAnimation, (float) airDropMax, 12);
        }

        renderBooleanSetting(stack, airDrop, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, airDropAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double chestMax = chest.get() ? 8.5f : 0;
        if (chestFirstRender) {
            this.chestAnimation = (float) chestMax;
            chestFirstRender = false;
        } else {
            this.chestAnimation = AnimationMath.fast(chestAnimation, (float) chestMax, 12);
        }

        renderBooleanSetting(stack, chest, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, chestAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double shipSiegeMax = shipSiege.get() ? 8.5f : 0;
        if (shipSiegeFirstRender) {
            this.shipSiegeAnimation = (float) shipSiegeMax;
            shipSiegeFirstRender = false;
        } else {
            this.shipSiegeAnimation = AnimationMath.fast(shipSiegeAnimation, (float) shipSiegeMax, 12);
        }

        renderBooleanSetting(stack, shipSiege, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, shipSiegeAnimation, elementAlpha);
        itemY += itemHeight + 2f;

        double bossBattleMax = bossBattle.get() ? 8.5f : 0;
        if (bossBattleFirstRender) {
            this.bossBattleAnimation = (float) bossBattleMax;
            bossBattleFirstRender = false;
        } else {
            this.bossBattleAnimation = AnimationMath.fast(bossBattleAnimation, (float) bossBattleMax, 12);
        }

        renderBooleanSetting(stack, bossBattle, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, bossBattleAnimation, elementAlpha);
    }

    private void renderBooleanSetting(MatrixStack stack, BooleanSetting setting, float itemX, float itemY, float menuWidth, float itemHeight, int textColor, int yesColor, int crossColor, float anim, int elementAlpha) {
        int baseTextColor = getColorByName("textColor");
        int colorfont = ColorUtils.interpolateColor(ColorUtils.setAlpha(baseTextColor, 150), baseTextColor, anim / 8.5f);
        colorfont = ColorUtils.setAlpha(colorfont, elementAlpha);
        Fonts.newcode[12].drawScissorString(stack, setting.getName(), itemX + 12.5f, itemY + 7f, colorfont, 54);

        int backgroundAlpha = (int) (20 * (elementAlpha / 255f));
        int backgroundColor = ColorUtils.interpolateColor(
                ColorUtils.setAlpha(crossColor, backgroundAlpha),
                ColorUtils.setAlpha(yesColor, backgroundAlpha),
                anim / 8.5f
        );

        float toggleX = itemX + 75.25f;
        float toggleY = itemY + 3.5f;
        float toggleSize = 8.5f;

        RenderUtils.Render2D.drawRoundedRect(toggleX, toggleY, toggleSize, toggleSize, 2f, backgroundColor);
        RenderUtils.Render2D.drawRoundOutline(toggleX, toggleY, toggleSize, toggleSize, 2f, -0.5f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                backgroundColor, backgroundColor, backgroundColor, backgroundColor
        ));

        int iconColor = ColorUtils.interpolateColor(crossColor, yesColor, anim / 8.5f);
        if (setting.get()) {
            iconColor = ColorUtils.applyOpacity(iconColor, 0.75f);
        } else {
            iconColor = ColorUtils.applyOpacity(iconColor, 0.65f);
        }
        iconColor = ColorUtils.setAlpha(iconColor, elementAlpha);

        Fonts.icon[11].drawString(stack, setting.get() ? "2" : "1", toggleX + 1.45f, toggleY + 4f, iconColor);
    }

    public void renderNotificationSettingsMenu(MatrixStack stack, float hudX, float hudY, float hudWidth, float alphaProgress) {
        if (Manager.NOTIFICATION_MANAGER == null) return;

        // Синхронизация с UseTracker при открытии меню
        if (Manager.FUNCTION_MANAGER != null && Manager.NOTIFICATION_MANAGER != null) {
            nuclear.module.api.Module useTracker = Manager.FUNCTION_MANAGER.get("UseTracker");
            if (useTracker instanceof nuclear.module.impl.other.UseTracker && Manager.NOTIFICATION_MANAGER.useItems != null) {
                Manager.NOTIFICATION_MANAGER.useItems.set(((nuclear.module.impl.other.UseTracker) useTracker).usetracker.get());
            }
        }

        float menuX = hudX + hudWidth + 5f;
        float menuY = hudY;
        float menuWidth = 79f;
        float itemHeight = 8f;
        float padding = 4f;
        float headerHeight = 15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int fonAlpha = (int) (200 * alphaProgress);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), fonAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int yesColor = getColorByName("yesColor");
        int crossColor = getColorByName("crossColor");

        float menuHeight = headerHeight + (3 * itemHeight) + (2 * 2f) + padding;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(menuX, menuY, menuWidth, menuHeight, 4, fonColor, alphaProgress);

        Fonts.newcode[13].drawString(stack, "Notifications", menuX + padding - 0.5f, menuY + 5.5f, textColor);

        float itemY = menuY + headerHeight - 5f;
        float itemX = menuX + padding - 12.5f;

        nuclear.module.settings.imp.BooleanSetting modulesSetting = Manager.NOTIFICATION_MANAGER.modules;
        nuclear.module.settings.imp.BooleanSetting useItemsSetting = Manager.NOTIFICATION_MANAGER.useItems;
        nuclear.module.settings.imp.BooleanSetting useTotemsSetting = Manager.NOTIFICATION_MANAGER.useTotems;

        // Получаем или создаем анимацию для модулей
        String animKeyModules = "notif_modules";
        if (!moduleNotificationAnimations.containsKey(animKeyModules)) {
            moduleNotificationAnimations.put(animKeyModules, 0f);
        }
        double animMaxModules = modulesSetting.get() ? 8.5f : 0;
        float currentAnimModules = moduleNotificationAnimations.get(animKeyModules);
        float newAnimModules = AnimationMath.fast(currentAnimModules, (float) animMaxModules, 12);
        moduleNotificationAnimations.put(animKeyModules, newAnimModules);
        renderBooleanSetting(stack, modulesSetting, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, newAnimModules, elementAlpha);
        itemY += itemHeight + 2f;

        // Получаем или создаем анимацию для предметов
        String animKeyItems = "notif_items";
        if (!moduleNotificationAnimations.containsKey(animKeyItems)) {
            moduleNotificationAnimations.put(animKeyItems, 0f);
        }
        double animMaxItems = useItemsSetting.get() ? 8.5f : 0;
        float currentAnimItems = moduleNotificationAnimations.get(animKeyItems);
        float newAnimItems = AnimationMath.fast(currentAnimItems, (float) animMaxItems, 12);
        moduleNotificationAnimations.put(animKeyItems, newAnimItems);
        renderBooleanSetting(stack, useItemsSetting, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, newAnimItems, elementAlpha);
        itemY += itemHeight + 2f;

        // Получаем или создаем анимацию для тотемов
        String animKeyTotems = "notif_totems";
        if (!moduleNotificationAnimations.containsKey(animKeyTotems)) {
            moduleNotificationAnimations.put(animKeyTotems, 0f);
        }
        double animMaxTotems = useTotemsSetting.get() ? 8.5f : 0;
        float currentAnimTotems = moduleNotificationAnimations.get(animKeyTotems);
        float newAnimTotems = AnimationMath.fast(currentAnimTotems, (float) animMaxTotems, 12);
        moduleNotificationAnimations.put(animKeyTotems, newAnimTotems);
        renderBooleanSetting(stack, useTotemsSetting, itemX, itemY, menuWidth, itemHeight, textColor, yesColor, crossColor, newAnimTotems, elementAlpha);
    }

    private final Map<String, Float> moduleNotificationAnimations = new HashMap<>();

    private final Animation thudAlpha = new DecelerateAnimation(250, 1.0);
    private final Animation targetSettingsAlpha = new DecelerateAnimation(250, 1.0);
    private final Animation eventsSettingsAlpha = new DecelerateAnimation(250, 1.0);

    private void renderTarget(final MatrixStack stack) {
        if (!elements.get(8)) return;

        float scale = getScale();
        target = getTarget(target);

        if (target == null) {
            return;
        }

        boolean shouldShow = allow;
        thudAlpha.setDirection(shouldShow ? Direction.FORWARDS : Direction.BACKWARDS);

        float alphaProgress = (float) thudAlpha.getOutput();
        if (target == null) {
            particles.clear();
            return;
        }

        if (target.hurtTime > 0 && !particlesSpawnedThisHit) {
            spawnHeadParticles();
            particlesSpawnedThisHit = true;
        } else if (target.hurtTime == 0) {
            particlesSpawnedThisHit = false;
        }
        if (alphaProgress <= 0.0) {
            target = null;
            return;
        }

        int firstColor = getColorByName("primaryColor");
        int textColor = getColorByName("textColor");
        int iconColor = getColorByName("iconColor");
        int goldColor = getColorByName("goldColor");

        int elementAlpha = (int) (255 * alphaProgress);
        int fonAlpha = (int) (200 * alphaProgress);
        int bgBarAlpha = (int) (82 * alphaProgress);
        int healthBarAlpha = (int) (100 * alphaProgress);
        int mainHealthAlpha = elementAlpha;

        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), fonAlpha);
        int animatedTextColor = ColorUtils.setAlpha(textColor, elementAlpha);
        int animatedIconColor = ColorUtils.setAlpha(iconColor, elementAlpha);

        float x = targetHUD.getX();
        float y = targetHUD.getY();
        float width = 90f * scale;
        float height = 30f * scale;
        updatePlayerHealth();

        RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y, width, height, 4 * scale, fonColor, alphaProgress);

        this.health = MathHelper.clamp(AnimationMath.fast(health, target.getHealth() / target.getMaxHealth(), 12), 0, 1);
        this.health2 = MathHelper.clamp(AnimationMath.fast(health2, target.getHealth() / target.getMaxHealth(), 4.5f), 0, 1);
        this.healthplus2 = MathHelper.clamp(AnimationMath.fast(this.healthplus2, target.getAbsorptionAmount() / target.getMaxHealth(), 4.5f), 0, 1);
        this.healthplus = MathHelper.clamp(AnimationMath.fast(this.healthplus, target.getAbsorptionAmount() / target.getMaxHealth(), 12), 0, 1);

        Color baseColor = new Color(firstColor);
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        float darkerBrightness = Math.max(0f, hsb[2] - 0.4f);
        Color darkerColor = Color.getHSBColor(hsb[0], hsb[1], darkerBrightness);

        int firstColor2 = getColorByName("primaryColor");
        Color gC = new Color(goldColor);
        int darkGoldColor = new Color((int) (gC.getRed() * 0.6f), (int) (gC.getGreen() * 0.6f), (int) (gC.getBlue() * 0.6f)).getRGB();

        int bgBarColor = ColorUtils.setAlpha(firstColor2, bgBarAlpha);
        float barX = x + 30F * scale;
        float barY = y + 21F * scale;
        float barWidth = 57F * scale;
        float barHeight = 6f * scale;
        RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth, barHeight, 2F * scale, bgBarColor);

        if (health >= healthplus) {
            int darkBgColor1 = ColorUtils.setAlpha(firstColor2, healthBarAlpha);
            int darkBgColor2 = ColorUtils.setAlpha(darkerColor.getRGB(), healthBarAlpha);
            RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth * health2, barHeight * 0.983f, 2F * scale,
                    darkBgColor1,
                    darkBgColor2);

            int mainColor1 = ColorUtils.setAlpha(firstColor2, mainHealthAlpha);
            int mainColor2 = ColorUtils.setAlpha(darkerColor.getRGB(), mainHealthAlpha);
            RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth * health, barHeight, 2F * scale,
                    mainColor1,
                    mainColor2);
        }

        if (!ClientUtils.isConnectedToServer("funtime")) {
            if (healthplus > 0) {
                int goldBg1 = ColorUtils.setAlpha(goldColor, healthBarAlpha);
                int goldBg2 = ColorUtils.setAlpha(darkGoldColor, healthBarAlpha);
                RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth * healthplus2, barHeight * 0.983f, 2F * scale,
                        goldBg1,
                        goldBg2);

                int goldMain1 = ColorUtils.setAlpha(goldColor, mainHealthAlpha);
                int goldMain2 = ColorUtils.setAlpha(darkGoldColor, mainHealthAlpha);
                RenderUtils.Render2D.drawRoundedRect(barX, barY, barWidth * healthplus, barHeight, 2F * scale,
                        goldMain1,
                        goldMain2);
            }
        }

        float faceSize = 24f * scale;
        float faceX = x + 3f * scale;
        float faceY = y + 3f * scale;

        if (target instanceof AbstractClientPlayerEntity) {
            RenderUtils.Render2D.drawRoundFace(faceX, faceY, (int) faceSize, (int) faceSize, 3f * scale, alphaProgress, (AbstractClientPlayerEntity) target);

            if (target.hurtTime > 0) {
                float hurtAlpha = (float) target.hurtTime / 10.0f * alphaProgress;
                Color hurtColor = new Color(255, 0, 0, (int) (hurtAlpha * 100));
                RenderUtils.Render2D.drawRoundedRect(faceX - 0.3f * scale, faceY - 0.3f * scale, faceSize + 0.6f * scale, faceSize + 0.6f * scale, 2.5f * scale, hurtColor.getRGB());
            }
        } else {
            StencilUtils.initStencilToWrite();
            RenderUtils.Render2D.drawRoundedRect(faceX, faceY, faceSize, faceSize, 3 * scale, ColorUtils.rgba(21, 21, 21, (int) (190 * alphaProgress)));
            StencilUtils.readStencilBuffer(1);
            RenderUtils.Render2D.drawRoundedRect(faceX, faceY, faceSize, faceSize, 3f * scale, ColorUtils.setAlpha(new Color(23, 23, 23).getRGB(), (int) (50 * alphaProgress)));

            String iconChar = ">";
            int fontSize = getIconSize(50);
            float iconWidth = Fonts.icon[fontSize].getWidth(iconChar);
            Fonts.icon[fontSize].drawString(stack, iconChar, faceX + (faceSize - iconWidth) / 2f, faceY + 2.5f * scale, animatedIconColor);

            StencilUtils.uninitStencilBuffer();
        }

        String targetName = target.getName().getString();
        boolean isFriend = target instanceof AbstractClientPlayerEntity && Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(target.getName().getString());
        boolean shouldProtect = isFriend && Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.nameProtect != null && Manager.FUNCTION_MANAGER.nameProtect.state && Manager.FUNCTION_MANAGER.nameProtect.friends != null && Manager.FUNCTION_MANAGER.nameProtect.friends.get();

        if (shouldProtect) {
            targetName = "Protect";
        }

        StyledFont nameFont = getFont(13);
        nameFont.drawScissorString(stack, targetName, x + 30f * scale, y + 6f * scale,
                animatedTextColor, (int) (35 * scale));

        String healthValue = String.valueOf((int) MathUtil.round(this.health * target.getMaxHealth(), 1.0f));
        if (ClientUtils.isConnectedToServer("funtime") && target.isInvisible()) healthValue = "0";

        float valW = nameFont.getWidth(healthValue);
        int healthIconSize = getIconSize(11);
        float iconW = Fonts.icon[healthIconSize].getWidth("e");
        float fullW = valW + iconW + 2f * scale;
        float startX = x + 79f * scale - (fullW / 2f);

        Color textColor2 = new Color(getColorByName("textColor"));
        float[] hsbtext = Color.RGBtoHSB(textColor2.getRed(), textColor2.getGreen(), textColor2.getBlue(), null);
        float darkerBrightnesstext = Math.max(0f, hsbtext[2] - 0.2f);
        int darkerText = Color.getHSBColor(hsbtext[0], hsbtext[1], darkerBrightnesstext).getRGB();
        int animatedDarkerText = ColorUtils.setAlpha(darkerText, elementAlpha);

        nameFont.drawString(stack, healthValue, startX + 1f * scale, y + 6f * scale, animatedDarkerText);

        Fonts.icon[healthIconSize].drawString(stack, "e", startX + valW + 2f * scale, y + 6.5f * scale, animatedIconColor);

        drawItemStack(x + 26f * scale, y + 7.5f * scale, 8f * scale);
        if (particle.get()) {
            particles.removeIf(p -> System.currentTimeMillis() - p.time > 2500L);
            for (HeadParticle p : particles) {
                p.update();
                float size = 1.0f - (float) (System.currentTimeMillis() - p.time) / 2500.0f;
                float particleSize = size * 3.5f * scale;
                RenderUtils.Render2D.drawRoundedCorner((float) p.pos.x, (float) p.pos.y, particleSize, particleSize, (particleSize / 2) + 1 * scale, ColorUtils.setAlpha(firstColor2, (int) (255.0f * p.alpha * size)));
            }
        }
        targetHUD.setWidth(width);
        targetHUD.setHeight(height);

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowSettings = isChatOpen && targetSettingsOpen;
        targetSettingsAlpha.setDirection(shouldShowSettings ? Direction.FORWARDS : Direction.BACKWARDS);
        float settingsAlphaProgress = (float) targetSettingsAlpha.getOutput();

        if (settingsAlphaProgress > 0.0 && shouldShowSettings) {
            renderTargetSettingsMenu(stack, x, y, width, settingsAlphaProgress);
        } else if (!isChatOpen) {
            targetSettingsOpen = false;
        }
    }

    private void renderTargetSettingsMenu(MatrixStack stack, float hudX, float hudY, float hudWidth, float alphaProgress) {
        float menuX = hudX + hudWidth + 5f;
        float menuY = hudY;
        float menuWidth = 79f;
        float itemHeight = 8f;
        float padding = 4f;
        float headerHeight = 15f;

        int elementAlpha = (int) (255 * alphaProgress);
        int fonAlpha = (int) (200 * alphaProgress);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), fonAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);
        int yesColor = getColorByName("yesColor");
        int crossColor = getColorByName("crossColor");

        float menuHeight = headerHeight + (2 * itemHeight) + (1 * 2f) + padding;

        RenderUtils.Render2D.drawBlurredRoundedRectangle(menuX, menuY, menuWidth, menuHeight, 4, fonColor, alphaProgress);

        Fonts.newcode[13].drawString(stack, "TargetHud", menuX + padding - 0.5f, menuY + 5.5f, textColor);

        float itemY = menuY + headerHeight - 5f;

        double particleMax = particle.get() ? 8.5f : 0;
        if (particleFirstRender) {
            this.particleAnimation = (float) particleMax;
            particleFirstRender = false;
        } else {
            this.particleAnimation = AnimationMath.fast(particleAnimation, (float) particleMax, 12);
        }

        float itemX = menuX + padding - 12.5f;

        int colorfont = ColorUtils.interpolateColor(ColorUtils.setAlpha(getColorByName("textColor"), 150), getColorByName("textColor"), particleAnimation / 8.5f);
        colorfont = ColorUtils.setAlpha(colorfont, elementAlpha);
        Fonts.newcode[12].drawScissorString(stack, particle.getName(), itemX + 12.5f, itemY + 7f, colorfont, 54);

        int backgroundAlpha = (int) (20 * alphaProgress);
        int backgroundColor = ColorUtils.interpolateColor(
                ColorUtils.setAlpha(crossColor, backgroundAlpha),
                ColorUtils.setAlpha(yesColor, backgroundAlpha),
                particleAnimation / 8.5f
        );

        float toggleX = itemX + 75.25f;
        float toggleY = itemY + 3.5f;
        float toggleSize = 8.5f;

        RenderUtils.Render2D.drawRoundedRect(toggleX, toggleY, toggleSize, toggleSize, 2f, backgroundColor);
        RenderUtils.Render2D.drawRoundOutline(toggleX, toggleY, toggleSize, toggleSize, 2f, -0.5f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                backgroundColor, backgroundColor, backgroundColor, backgroundColor
        ));

        int iconColor = ColorUtils.interpolateColor(crossColor, yesColor, particleAnimation / 8.5f);
        if (particle.get()) {
            iconColor = ColorUtils.applyOpacity(iconColor, 0.75f);
        } else {
            iconColor = ColorUtils.applyOpacity(iconColor, 0.65f);
        }
        iconColor = ColorUtils.setAlpha(iconColor, elementAlpha);

        Fonts.icon[11].drawString(stack, particle.get() ? "2" : "1", toggleX + 1.45f, toggleY + 4f, iconColor);

        itemY += itemHeight + 2f;

        double targetOnHoverMax = targetOnHover.get() ? 8.5f : 0;
        if (targetOnHoverFirstRender) {
            this.targetOnHoverAnimation = (float) targetOnHoverMax;
            targetOnHoverFirstRender = false;
        } else {
            this.targetOnHoverAnimation = AnimationMath.fast(targetOnHoverAnimation, (float) targetOnHoverMax, 12);
        }

        int colorfont2 = ColorUtils.interpolateColor(ColorUtils.setAlpha(getColorByName("textColor"), 150), getColorByName("textColor"), targetOnHoverAnimation / 8.5f);
        colorfont2 = ColorUtils.setAlpha(colorfont2, elementAlpha);
        Fonts.newcode[12].drawScissorString(stack, targetOnHover.getName(), itemX + 12.5f, itemY + 7f, colorfont2, 54);

        int backgroundColor2 = ColorUtils.interpolateColor(
                ColorUtils.setAlpha(crossColor, backgroundAlpha),
                ColorUtils.setAlpha(yesColor, backgroundAlpha),
                targetOnHoverAnimation / 8.5f
        );

        float toggleX2 = itemX + 75.25f;
        float toggleY2 = itemY + 3.5f;

        RenderUtils.Render2D.drawRoundedRect(toggleX2, toggleY2, toggleSize, toggleSize, 2f, backgroundColor2);
        RenderUtils.Render2D.drawRoundOutline(toggleX2, toggleY2, toggleSize, toggleSize, 2f, -0.5f, ColorUtils.rgba(25, 26, 33, 0), new Vector4i(
                backgroundColor2, backgroundColor2, backgroundColor2, backgroundColor2
        ));

        int iconColor2 = ColorUtils.interpolateColor(crossColor, yesColor, targetOnHoverAnimation / 8.5f);
        if (targetOnHover.get()) {
            iconColor2 = ColorUtils.applyOpacity(iconColor2, 0.75f);
        } else {
            iconColor2 = ColorUtils.applyOpacity(iconColor2, 0.65f);
        }

        Fonts.icon[11].drawString(stack, targetOnHover.get() ? "2" : "1", toggleX2 + 1.45f, toggleY2 + 4f, iconColor2);
    }

    public boolean handleTargetHudClick(double mouseX, double mouseY, int button) {
        if (!elements.get(8)) return false;

        float x = targetHUD.getX();
        float y = targetHUD.getY();
        float width = targetHUD.getWidth();
        float height = targetHUD.getHeight();

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        if (button == 1 && isChatOpen && MathUtil.isHovered((float) mouseX, (float) mouseY, x, y, width, height)) {
            targetSettingsOpen = !targetSettingsOpen;
            if (targetSettingsOpen) {
                eventsSettingsOpen = false;
            }
            return true;
        }

        if (button == 0 && isChatOpen && targetSettingsOpen) {
            float menuX = x + width + 5f;
            float menuY = y;
            float padding = 4f;
            float headerHeight = 15f;
            float itemHeight = 8f;

            float itemX = menuX + padding - 12.5f;

            float itemY1 = menuY + headerHeight - 5f;

            float originalY1 = itemY1 + 4.5f - 8f;
            float originalH = itemHeight + 5 - 2;

            float itemW = 83.75f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY1, itemW, originalH)) {
                particle.toggle();
                particleFirstRender = false;
                return true;
            }

            float itemY2 = itemY1 + itemHeight + 2f;
            float originalY2 = itemY2 + 4.5f - 8f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY2, itemW, originalH)) {
                targetOnHover.toggle();
                targetOnHoverFirstRender = false;
                return true;
            }
        }

        return false;
    }

    public boolean handleEventsClick(double mouseX, double mouseY, int button) {
        if (!elements.get(9)) return false;

        float x = events.getX();
        float y = events.getY();
        float width = events.getWidth();
        float height = events.getHeight();

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        if (button == 1 && isChatOpen && MathUtil.isHovered((float) mouseX, (float) mouseY, x, y, width, height)) {
            eventsSettingsOpen = !eventsSettingsOpen;
            if (eventsSettingsOpen) {
                targetSettingsOpen = false;
                eventsSettingsTimer.reset();
                eventsSettingsAllow = true;
            } else {
                eventsSettingsAllow = false;
            }
            return true;
        }

        if (button == 0 && isChatOpen && eventsSettingsOpen) {
            float menuX = x + width + 5f;
            float menuY = y;
            float padding = 4f;
            float headerHeight = 15f;
            float itemHeight = 8f;

            float itemX = menuX + padding - 12.5f;
            float itemY1 = menuY + headerHeight - 5f;
            float originalY1 = itemY1 + 4.5f - 8f;
            float originalH = itemHeight + 5 - 2;
            float itemW = 83.75f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY1, itemW, originalH)) {
                airDrop.toggle();
                airDropFirstRender = false;
                eventsSettingsTimer.reset();
                eventsSettingsAllow = true;
                return true;
            }

            float itemY2 = itemY1 + itemHeight + 2f;
            float originalY2 = itemY2 + 4.5f - 8f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY2, itemW, originalH)) {
                chest.toggle();
                chestFirstRender = false;
                eventsSettingsTimer.reset();
                eventsSettingsAllow = true;
                return true;
            }

            float itemY3 = itemY2 + itemHeight + 2f;
            float originalY3 = itemY3 + 4.5f - 8f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY3, itemW, originalH)) {
                shipSiege.toggle();
                shipSiegeFirstRender = false;
                eventsSettingsTimer.reset();
                eventsSettingsAllow = true;
                return true;
            }

            float itemY4 = itemY3 + itemHeight + 2f;
            float originalY4 = itemY4 + 4.5f - 8f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX, originalY4, itemW, originalH)) {
                bossBattle.toggle();
                bossBattleFirstRender = false;
                eventsSettingsTimer.reset();
                eventsSettingsAllow = true;
                return true;
            }
        }

        return false;
    }

    public boolean handleNotificationClick(double mouseX, double mouseY, int button) {
        if (!elements.get(4)) return false;

        boolean isChatOpen = this.mc.currentScreen instanceof ChatScreen;

        // Check if clicking on notification placeholder area
        if (Manager.NOTIFICATION_MANAGER != null) {
            float scale = getScale();
            int fontSize = Math.max(10, Math.min((int) (13 * scale), Fonts.newcode.length - 1));
            String placeholderText = "Это уведомление, клик на меня для настройки";
            float textWidth = Fonts.newcode[fontSize].getWidth(placeholderText);
            float fullWidth = textWidth + 20 * scale;
            float height = 12 * scale;

            float x = (mc.getMainWindow().scaledWidth() / 2f) - (fullWidth / 2f);
            float baseY = mc.getMainWindow().scaledHeight() / 2f + 37 * scale;

            if ((button == 0 || button == 1) && isChatOpen && MathUtil.isHovered((float) mouseX, (float) mouseY, x, baseY, fullWidth, height)) {
                notificationSettingsOpen = !notificationSettingsOpen;
                if (notificationSettingsOpen) {
                    eventsSettingsOpen = false;
                    targetSettingsOpen = false;
                    notificationSettingsTimer.reset();
                    notificationSettingsAllow = true;
                } else {
                    notificationSettingsAllow = false;
                }
                return true;
            }
        }

        if (button == 0 && isChatOpen && notificationSettingsOpen && Manager.NOTIFICATION_MANAGER != null) {
            float scale = getScale();
            int fontSize = Math.max(10, Math.min((int) (13 * scale), Fonts.newcode.length - 1));
            String placeholderText = "Это уведомление, клик на меня для настройки";
            float textWidth = Fonts.newcode[fontSize].getWidth(placeholderText);
            float fullWidth = textWidth + 20 * scale;

            float placeholderX = (mc.getMainWindow().scaledWidth() / 2f) - (fullWidth / 2f);
            float placeholderY = mc.getMainWindow().scaledHeight() / 2f + 37 * scale;
            float menuX = placeholderX + fullWidth + 5f;
            float menuY = placeholderY;
            float menuWidth = 79f;
            float padding = 4f;
            float headerHeight = 15f;
            float itemHeight = 8f;

            float itemX = menuX + padding - 12.5f;
            float itemY1 = menuY + headerHeight - 5f;
            float originalY1 = itemY1 + 4.5f - 8f;
            float originalH = itemHeight + 5 - 2;
            float itemW = 83.75f;

            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX + 8f, originalY1, itemW, originalH)) {
                Manager.NOTIFICATION_MANAGER.modules.toggle();
                notificationSettingsTimer.reset();
                notificationSettingsAllow = true;
                return true;
            }

            float itemY2 = itemY1 + itemHeight + 2f;
            float originalY2 = itemY2 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX + 8f, originalY2, itemW, originalH)) {
                if (Manager.NOTIFICATION_MANAGER != null && Manager.NOTIFICATION_MANAGER.useItems != null) {
                    Manager.NOTIFICATION_MANAGER.useItems.toggle();
                    // Синхронизация с UseTracker
                    if (Manager.FUNCTION_MANAGER != null) {
                        nuclear.module.api.Module useTracker = Manager.FUNCTION_MANAGER.get("UseTracker");
                        if (useTracker instanceof nuclear.module.impl.other.UseTracker) {
                            ((nuclear.module.impl.other.UseTracker) useTracker).usetracker.set(Manager.NOTIFICATION_MANAGER.useItems.get());
                        }
                    }
                }
                notificationSettingsTimer.reset();
                notificationSettingsAllow = true;
                return true;
            }

            float itemY3 = itemY2 + itemHeight + 2f;
            float originalY3 = itemY3 + 4.5f - 8f;
            if (MathUtil.isHovered((float) mouseX, (float) mouseY - 5, itemX + 8f, originalY3, itemW, originalH)) {
                Manager.NOTIFICATION_MANAGER.useTotems.toggle();
                // Синхронизация с UseTracker
                if (Manager.FUNCTION_MANAGER != null) {
                    nuclear.module.api.Module useTracker = Manager.FUNCTION_MANAGER.get("UseTracker");
                    if (useTracker instanceof nuclear.module.impl.other.UseTracker) {
                        ((nuclear.module.impl.other.UseTracker) useTracker).totemtracker.set(Manager.NOTIFICATION_MANAGER.useTotems.get());
                    }
                }
                notificationSettingsTimer.reset();
                notificationSettingsAllow = true;
                return true;
            }
        }

        return false;
    }

    private void spawnHeadParticles() {
        float scale = getScale();
        float headX = targetHUD.getX() + 10f * scale;
        float headY = targetHUD.getY() + 10.5f * scale;
        for (int i = 0; i < 9; ++i) {
            particles.add(new HeadParticle(new Vector3d(headX, headY, 0.0)));
        }
    }

    public static class HeadParticle {
        private Vector3d pos;
        private final Vector3d end;
        private final long time;
        private float alpha;

        public HeadParticle(Vector3d pos) {
            this.pos = pos;
            this.end = pos.add(
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F),
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F),
                    -ThreadLocalRandom.current().nextFloat(-80.0F, 80.0F)
            );
            this.time = System.currentTimeMillis();
            this.alpha = 0.0f;
        }

        public void update() {
            this.alpha = MathHelper.lerp(this.alpha, 1.0F, 0.1F);
            this.pos = AnimationMath.fast(this.pos, this.end, 0.5F);
        }
    }

    private void blitSprite(MatrixStack matrixStack, float x, float y, int blitOffset, int width, int height, TextureAtlasSprite sprite) {
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(matrix, x, y + height, (float) blitOffset).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        bufferbuilder.pos(matrix, x + width, y + height, (float) blitOffset).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        bufferbuilder.pos(matrix, x + width, y, (float) blitOffset).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        bufferbuilder.pos(matrix, x, y, (float) blitOffset).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);
    }

    private void updatePlayerHealth() {
        String myPlayerName = mc.player.getName().getString();

        if (target.getName().getString().equals(myPlayerName)) {
            return;
        }

        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.scoreboardHealth != null && Manager.FUNCTION_MANAGER.scoreboardHealth.state) {
            for (Map.Entry<ScoreObjective, Score> entry : IMinecraft.mc.world.getScoreboard().getObjectivesForEntity(target.getName().getString()).entrySet()) {
                Score score = entry.getValue();
                int newHealth = score.getScorePoints();

                if (newHealth >= 1) {
                    target.setHealth(newHealth);
                } else {
                    target.setHealth(1);
                }
            }
        }
    }

    private void drawItemStack(float x, float y, float offset) {
        float scale = getScale();
        List<ItemStack> stackList = new ArrayList<>(Arrays.asList(target.getHeldItemMainhand(), target.getHeldItemOffhand()));
        List<ItemStack> armorItems = (List<ItemStack>) target.getArmorInventoryList();
        stackList.add(armorItems.get(3));
        stackList.add(armorItems.get(2));
        stackList.add(armorItems.get(1));
        stackList.add(armorItems.get(0));

        List<ItemStack> nonEmptyStacks = stackList.stream()
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toList());

        itemCount = nonEmptyStacks.size();

        final AtomicReference<Float> posX = new AtomicReference<>(x);

        float scaleValue = 0.5f * scale * (float) thudAlpha.getOutput();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        nonEmptyStacks.forEach(stack -> {
            float currentX = posX.get();
            float itemSize = 16.0f * scale;

            GlStateManager.pushMatrix();
            GlStateManager.translatef(currentX + itemSize / 2.0f, y + itemSize / 2.0f, 0);
            GlStateManager.scalef(scaleValue, scaleValue, 1.0F);
            GlStateManager.translatef(-itemSize / 2.0f, -itemSize / 2.0f, 0);

            IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, null);

            GlStateManager.popMatrix();

            posX.set(currentX + offset);
        });

        GlStateManager.popMatrix();
    }

    private LivingEntity getTarget(LivingEntity previousTarget) {
        LivingEntity target = previousTarget;

        if (Manager.FUNCTION_MANAGER.auraFunction.getTarget() instanceof LivingEntity) {
            target = (LivingEntity) Manager.FUNCTION_MANAGER.auraFunction.getTarget();
            targetTimer.reset();
            allow = true;
        } else if (mc.currentScreen instanceof ChatScreen) {
            target = mc.player;
            targetTimer.reset();
            allow = true;
        } else if (targetOnHover.get() && mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
            if (((EntityRayTraceResult) mc.objectMouseOver).getEntity() instanceof LivingEntity) {
                target = (LivingEntity) ((EntityRayTraceResult) mc.objectMouseOver).getEntity();
                targetTimer.reset();
                allow = true;
            } else {
                allow = false;
            }
        } else if (allow && targetTimer.isReached(200)) {
            allow = false;
        }

        return target;
    }
}