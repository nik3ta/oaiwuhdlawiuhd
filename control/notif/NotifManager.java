package nuclear.control.notif;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import nuclear.module.TypeList;
import nuclear.utils.font.ReplaceUtil;
import nuclear.utils.IMinecraft;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.Direction;
import nuclear.utils.anim.impl.DecelerateAnimation;
import nuclear.utils.anim.impl.EaseBackIn;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;
import nuclear.utils.language.Translated;
import nuclear.module.settings.imp.BooleanSetting;

import java.util.concurrent.CopyOnWriteArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;

public class NotifManager {

    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private final Animation placeholderAnimation = new DecelerateAnimation(300, 1.0);
    private long placeholderIconLastChange = System.currentTimeMillis();
    private int placeholderIconIndex = 0;
    private final String[] placeholderIcons = {"T", "S", "F", "g"};

    public final BooleanSetting modules = new BooleanSetting("Вкл/выкл модулей", true);

    public final BooleanSetting useItems = new BooleanSetting("Исп предметов", true);

    public final BooleanSetting useTotems = new BooleanSetting("Исп тотемы", true);

    private float getScale() {
        if (nuclear.control.Manager.FUNCTION_MANAGER != null) {
            for (nuclear.module.api.Module module : nuclear.control.Manager.FUNCTION_MANAGER.getFunctions()) {
                if (module.name.equals("Hud") && module instanceof nuclear.module.impl.render.Interface) {
                    return ((nuclear.module.impl.render.Interface) module).size.getValue().floatValue();
                }
            }
        }
        return 1.0f;
    }

    private ITextComponent getItemDisplayName(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        ITextComponent displayName = itemStack.getDisplayName();
        String displayNameString = displayName.getString();
        if (displayNameString.contains("Лучший ТГ @StarikZako")) {
            return itemStack.getItem().getName();
        } else {
            return displayName;
        }
    }

    public void add(String text, String content, int time, TypeList category) {
        boolean isModuleNotification = text.contains(" enabled") || text.contains(" disabled");
        if (isModuleNotification && !modules.get()) {
            return;
        }
        notifications.add(new Notification(text, content, time, category, null, null));
    }

    public void add(String text, String content, int time, TypeList category, ITextComponent component) {
        notifications.add(new Notification(text, content, time, category, component, null));
    }

    public void add(String text, String content, int time, TypeList category, ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) {
                if (!useTotems.get()) {
                    return;
                }
            } else {
                if (!useItems.get()) {
                    return;
                }
            }
        }
        notifications.add(new Notification(text, content, time, category, getItemDisplayName(itemStack), itemStack));
    }

    public void add(String text, String content, int time, TypeList category, ITextComponent playerNameComponent, ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING) {
                if (!useTotems.get()) {
                    return;
                }
            } else {
                if (!useItems.get()) {
                    return;
                }
            }
        }
        notifications.add(new Notification(text, content, time, category, playerNameComponent, itemStack));
    }

    public void draw(MatrixStack stack) {
        float scale = getScale();
        float yOffset = 0;

        int activeNotifications = 0;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            int visualIndex = notifications.size() - 1 - i;

            if (visualIndex > 4) {
                notification.animation.setDirection(Direction.BACKWARDS);
            } else {
                long timeElapsed = System.currentTimeMillis() - notification.getTime();
                if (timeElapsed > (notification.time2 * 1000L) / 1.5 - 250) {
                    notification.animation.setDirection(Direction.BACKWARDS);
                } else {
                    notification.animation.setDirection(Direction.FORWARDS);
                }
            }

            notification.alpha = (float) notification.animation.getOutput();

            if (notification.animation.finished(Direction.BACKWARDS) && notification.alpha <= 0.01f) {
                notifications.remove(notification);
                continue;
            }

            int fontSize = getFontSize(13, scale);
            String textForWidth = notification.getText();
            float width;
            ItemStack itemStack = notification.getItemStack();

            if (itemStack != null && !itemStack.isEmpty()) {
                if (itemStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING && textForWidth.contains(" потерял тотем бессмертия, зачарован:")) {
                    String playerName = notification.component != null ? notification.component.getString() : textForWidth.split(" потерял")[0];
                    String totemText = " потерял тотем бессмертия, зачарован:";
                    float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                    float totemTextWidth = Fonts.newcode[fontSize].getWidth(totemText);
                    width = playerNameWidth + totemTextWidth + 4 * scale + 20 * scale;
                } else if (textForWidth.contains(" использовал ")) {
                    String[] parts = textForWidth.split(" использовал ", 2);
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                        float usedTextWidth = Fonts.newcode[fontSize].getWidth(" использовал ");
                        float itemNameWidth = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString());
                        width = playerNameWidth + usedTextWidth + itemNameWidth + 20 * scale;
                    } else {
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        width = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString()) + 20 * scale;
                    }
                } else {
                    ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                    width = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString()) + 20 * scale;
                }
            } else {
                width = Fonts.newcode[fontSize].getWidth(textForWidth) + 20 * scale;
            }

            float x = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - (width / 2f);
            float baseY = IMinecraft.mc.getMainWindow().scaledHeight() / 2f + 37 * scale;

            float targetY = baseY + yOffset;

            if (notification.getY() >= IMinecraft.mc.getMainWindow().scaledHeight() - 20 * scale) {
                notification.setY(targetY);
            } else {
                notification.setY(AnimationMath.fast(notification.getY(), targetY, 15));
            }

            notification.setX(x);

            if (notification.alpha > 0.01f) {
                notification.draw(stack, visualIndex, scale);
                yOffset += 14.5f * scale * notification.alpha;
                activeNotifications++;
            }
        }

        boolean isChatOpen = IMinecraft.mc.currentScreen instanceof ChatScreen;
        boolean shouldShowPlaceholder = activeNotifications == 0 && isChatOpen;

        placeholderAnimation.setDirection(shouldShowPlaceholder ? Direction.FORWARDS : Direction.BACKWARDS);
        float placeholderAlpha = (float) placeholderAnimation.getOutput();

        if (placeholderAlpha > 0.01f) {
            drawPlaceholder(stack, scale, placeholderAlpha);

            nuclear.module.impl.render.Interface hud = getHudInterface();
            if (hud != null && hud.notificationSettingsOpen) {
                if (!isChatOpen) {
                    hud.notificationSettingsOpen = false;
                    hud.notificationSettingsAllow = false;
                } else {
                    String placeholderText = getPlaceholderText();
                    int fontSize = getFontSize(13, scale);
                    float textWidth = Fonts.newcode[fontSize].getWidth(placeholderText);
                    float fullWidth = textWidth + 20 * scale;
                    float x = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - (fullWidth / 2f);
                    float baseY = IMinecraft.mc.getMainWindow().scaledHeight() / 2f + 37 * scale;

                    boolean shouldShowSettings = hud.notificationSettingsAllow;
                    hud.notificationSettingsAlpha.setDirection(shouldShowSettings ? Direction.FORWARDS : Direction.BACKWARDS);
                    float settingsAlphaProgress = (float) hud.notificationSettingsAlpha.getOutput();

                    if (settingsAlphaProgress > 0.0) {
                        hud.renderNotificationSettingsMenu(stack, x, baseY, fullWidth, settingsAlphaProgress);
                    }
                }
            }
        } else {
            nuclear.module.impl.render.Interface hud = getHudInterface();
            if (hud != null && hud.notificationSettingsOpen && !isChatOpen) {
                hud.notificationSettingsOpen = false;
                hud.notificationSettingsAllow = false;
            }
        }
    }

    private nuclear.module.impl.render.Interface getHudInterface() {
        if (nuclear.control.Manager.FUNCTION_MANAGER != null) {
            for (nuclear.module.api.Module module : nuclear.control.Manager.FUNCTION_MANAGER.getFunctions()) {
                if (module.name.equals("Hud") && module instanceof nuclear.module.impl.render.Interface) {
                    return (nuclear.module.impl.render.Interface) module;
                }
            }
        }
        return null;
    }

    private void drawPlaceholder(MatrixStack stack, float scale, float alpha) {
        if (System.currentTimeMillis() - placeholderIconLastChange >= 600) {
            placeholderIconIndex = (placeholderIconIndex + 1) % placeholderIcons.length;
            placeholderIconLastChange = System.currentTimeMillis();
        }

        int fontSize = getFontSize(13, scale);
        int iconSize = getIconSize(15, scale);

        String placeholderText = getPlaceholderText();
        float textWidth = Fonts.newcode[fontSize].getWidth(placeholderText);
        float fullWidth = textWidth + 20 * scale;
        float height = 12 * scale;
        float radius = 3f * scale;

        float x = (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - (fullWidth / 2f);
        float baseY = IMinecraft.mc.getMainWindow().scaledHeight() / 2f + 37 * scale;

        int elementAlpha = (int) (255 * alpha);
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alpha));
        int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), elementAlpha);
        int textColor = ColorUtils.setAlpha(getColorByName("textColor"), elementAlpha);

        stack.push();
        RenderUtils.Render2D.drawBlurredRoundedRectangle(x, baseY, fullWidth, height, radius, fonColor, alpha);

        float iconX = x + 6f * scale;
        float textY = baseY + (height - Fonts.newcode[fontSize].getFontHeight()) + 0.5f;
        float iconY = baseY + (height / 3f) + 0.5f;

        String currentIcon = placeholderIcons[placeholderIconIndex];
        Fonts.icon[iconSize].drawCenteredString(stack, currentIcon, iconX + 2 * scale, iconY, iconColor);

        float textX = x + 14f * scale;
        Fonts.newcode[fontSize].drawString(stack, placeholderText, textX + 1, textY, textColor);

        stack.pop();
    }

    private int getFontSize(int baseSize, float scale) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * scale), Fonts.newcode.length - 1));
        if (Fonts.newcode[scaledSize] == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.newcode.length && Fonts.newcode[i] != null) return i;
            }
            return Math.min(14, Fonts.newcode.length - 1);
        }
        return scaledSize;
    }

    private int getIconSize(int baseSize, float scale) {
        int scaledSize = Math.max(10, Math.min((int) (baseSize * scale), Fonts.icon.length - 1));
        if (Fonts.icon[scaledSize] == null) {
            for (int i = scaledSize; i >= 10; i--) {
                if (i < Fonts.icon.length && Fonts.icon[i] != null) return i;
            }
            return Math.min(15, Fonts.icon.length - 1);
        }
        return scaledSize;
    }

    private String getPlaceholderText() {
        String lang = Translated.getCurrentLanguage();
        switch (lang) {
            case "RUS":
                return "Это уведомление, клик на меня для настройки";
            case "ENG":
                return "This is a notification, click on me to configure";
            case "PL":
                return "To jest powiadomienie, kliknij mnie, aby skonfigurować";
            case "UKR":
                return "Це сповіщення, натисніть на мене для налаштування";
            default:
                return "This is a notification, click on me to configure";
        }
    }

    private class Notification {
        private final TypeList category;
        @Getter @Setter private float x, y = (float) IMinecraft.mc.getMainWindow().scaledHeight();
        @Getter private String text;
        @Getter private String content;
        private final ITextComponent component;
        @Getter private final ItemStack itemStack;
        private boolean isState;
        private boolean state;
        @Getter private long time = System.currentTimeMillis();

        public Animation animation = new DecelerateAnimation(200, 1, Direction.FORWARDS);
        public Animation yAnimation = new DecelerateAnimation(200, 1, Direction.FORWARDS);
        public Animation scaleAnimation = new EaseBackIn(150, 1, 1.5f, Direction.FORWARDS);
        float alpha;
        float scale = 1.0f;
        int time2 = 2;

        public Notification(String text, String content, int time, TypeList category, ITextComponent component, ItemStack itemStack) {
            this.text = text;
            this.content = content;
            this.time2 = time;
            this.category = category;
            this.component = component;
            this.itemStack = itemStack;
        }

        private int getMinusColor(int elementAlpha, float darkness) {
            if (component == null) {
                return new java.awt.Color(252, 84, 84, (int) (elementAlpha * darkness)).getRGB(); // Красный по умолчанию
            }

            String componentText = component.getString();
            String replacedComponentText = ReplaceUtil.replaceCustomFonts(componentText);

            if (!replacedComponentText.startsWith("- ")) {
                return new java.awt.Color(252, 84, 84, (int) (elementAlpha * darkness)).getRGB(); // Красный по умолчанию
            }

            Style style = component.getStyle();
            if (style.getColor() != null) {
                int color = style.getColor().getColor();
                return ColorUtils.reAlphaInt(color, (int) (elementAlpha * darkness));
            }

            for (ITextComponent sibling : component.getSiblings()) {
                String siblingText = sibling.getString();
                String replacedSiblingText = ReplaceUtil.replaceCustomFonts(siblingText);
                if (replacedSiblingText.startsWith("- ") || replacedSiblingText.startsWith("-")) {
                    Style siblingStyle = sibling.getStyle();
                    if (siblingStyle.getColor() != null) {
                        int color = siblingStyle.getColor().getColor();
                        return ColorUtils.reAlphaInt(color, (int) (elementAlpha * darkness));
                    }
                }
            }

            String replacedText = ReplaceUtil.replaceCustomFonts(text);
            if (replacedText.startsWith("- ")) {
                if (component.getSiblings().isEmpty() && style.getColor() == null) {
                    if (text.contains("§c") || text.contains("§4")) {
                        return ColorUtils.reAlphaInt(0xFFFF5555, (int) (elementAlpha * darkness));
                    } else if (text.contains("§a") || text.contains("§2")) {
                        return ColorUtils.reAlphaInt(0xFF55FF55, (int) (elementAlpha * darkness));
                    } else if (text.contains("§6") || text.contains("§e")) {
                        return ColorUtils.reAlphaInt(0xFFFFAA00, (int) (elementAlpha * darkness));
                    }
                }
            }

            return new java.awt.Color(252, 84, 84, (int) (elementAlpha * darkness)).getRGB();
        }

        public float draw(MatrixStack stack, int index, float scale) {
            String statusSuffix = text.contains("enabled") ? " enabled" : (text.contains("disabled") ? " disabled" : "");
            String moduleName = text.replace(" enabled", "").replace(" disabled", "");

            int fontSize = getFontSize(13, scale);
            int iconSize = getIconSize(15, scale);

            float moduleNameWidth = Fonts.newcode[fontSize].getWidth(moduleName);
            String textForWidth = text;
            float fullWidth;

            if (itemStack != null && !itemStack.isEmpty()) {
                if (itemStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING && text.contains(" потерял тотем бессмертия, зачарован:")) {
                    String playerName = component != null ? component.getString() : text.split(" потерял")[0];
                    String totemText = " потерял тотем бессмертия, зачарован:";
                    float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                    float totemTextWidth = Fonts.newcode[fontSize].getWidth(totemText);
                    fullWidth = playerNameWidth + totemTextWidth + 4 * scale + 20 * scale;
                } else if (text.contains(" использовал ")) {
                    String[] parts = text.split(" использовал ", 2);
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                        float usedTextWidth = Fonts.newcode[fontSize].getWidth(" использовал ");
                        float itemNameWidth = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString());
                        fullWidth = playerNameWidth + usedTextWidth + itemNameWidth + 20 * scale;
                    } else {
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        fullWidth = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString()) + 20 * scale;
                    }
                } else {
                    ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                    fullWidth = Fonts.newcode[fontSize].getWidth(itemDisplayName.getString()) + 20 * scale;
                }
            } else {
                fullWidth = Fonts.newcode[fontSize].getWidth(textForWidth) + 20 * scale;
            }

            float height = 12 * scale;
            float radius = 3f * scale;

            float darkness = MathHelper.clamp(1.0f - (index * 0.1f), 0.7f, 1.0f);
            int elementAlpha = (int) (255 * alpha);
            int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int) (200 * alpha * darkness));
            int iconColor = ColorUtils.setAlpha(getColorByName("iconColor"), (int) (elementAlpha * darkness));
            int mainTextColor = ColorUtils.setAlpha(getColorByName("textColor"), (int) (elementAlpha * darkness));

            int statusColor = statusSuffix.contains("enabled") ? ColorUtils.reAlphaInt(0xFF55FF55, (int) (elementAlpha * darkness)) : ColorUtils.reAlphaInt(0xFFFF5555, (int) (elementAlpha * darkness));

            stack.push();
            RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y, fullWidth, height, radius, fonColor, alpha);

            float iconX = x + 6f * scale;
            float textY = y + (height - Fonts.newcode[fontSize].getFontHeight()) + 0.5f;
            float iconY = y + (height / 3f) + 0.5f;

            if (itemStack != null && !itemStack.isEmpty()) {
                float itemAlpha = alpha * this.scale;
                if (itemAlpha > 0.1f) {
                    RenderSystem.depthMask(false);
                    drawItemStack(itemStack, iconX - 3f * scale, y + 1f * scale, null, true, this.scale, scale);
                    RenderSystem.depthMask(true);
                }
            } else {
                String iconChar = statusSuffix.contains("enabled") ? "T" : (statusSuffix.contains("disabled") ? "S" : "a");
                Fonts.icon[iconSize].drawCenteredString(stack, iconChar, iconX + 2 * scale, iconY, iconColor);
            }

            float textX = x + 14f * scale;
            if (itemStack != null && !itemStack.isEmpty()) {
                if (itemStack.getItem() == net.minecraft.item.Items.TOTEM_OF_UNDYING && text.contains(" потерял тотем бессмертия, зачарован:")) {
                    ITextComponent playerNameComponent = component != null ? component : new StringTextComponent(text.split(" потерял")[0]);
                    String playerName = component != null ? component.getString() : text.split(" потерял")[0];
                    float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                    Fonts.newcode[fontSize].drawText(stack, playerNameComponent, textX + 1, textY, mainTextColor);

                    float totemTextX = textX + 1 + playerNameWidth;
                    String totemText = " потерял тотем бессмертия, зачарован:";
                    float totemTextWidth = Fonts.newcode[fontSize].getWidth(totemText);
                    int grayColor = ColorUtils.reAlphaInt(0xFFAAAAAA, (int) (elementAlpha * darkness));
                    Fonts.newcode[fontSize].drawString(stack, totemText, totemTextX, textY, grayColor);

                    float symbolX = totemTextX + totemTextWidth + 1.5f;
                    float circleY = textY - 0.3f * scale;
                    int symbolColor = itemStack.isEnchanted()
                            ? new java.awt.Color(84, 252, 84, (int) (elementAlpha * darkness)).getRGB()
                            : new java.awt.Color(252, 84, 84, (int) (elementAlpha * darkness)).getRGB();
                    RenderUtils.Render2D.drawCircle2(symbolX, circleY, 1.75F * scale, symbolColor);
                    String replacedText = ReplaceUtil.replaceCustomFonts(String.valueOf(playerNameComponent));
                    if (replacedText.contains("-")) {
                        int minusColor = getMinusColor(elementAlpha, darkness);
                        RenderUtils.Render2D.drawCircle2(textX + 1.3f, circleY, 1.75F * scale, minusColor);
                    }
                } else if (text.contains(" использовал ")) {
                    String[] parts = text.split(" использовал ", 2);
                    if (parts.length == 2) {
                        String playerName = parts[0];

                        ITextComponent playerNameComponent = component != null ? component : new StringTextComponent(playerName);
                        float playerNameWidth = Fonts.newcode[fontSize].getWidth(playerName);
                        Fonts.newcode[fontSize].drawText(stack, playerNameComponent, textX + 1, textY, mainTextColor);

                        float usedTextX = textX + 1 + playerNameWidth;
                        ITextComponent usedTextComponent = new StringTextComponent(" использовал ")
                                .setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                        float usedTextWidth = Fonts.newcode[fontSize].getWidth(" использовал ");
                        Fonts.newcode[fontSize].drawText(stack, usedTextComponent, usedTextX, textY, mainTextColor);

                        float itemTextX = usedTextX + usedTextWidth;
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        Fonts.newcode[fontSize].drawText(stack, itemDisplayName, itemTextX, textY, mainTextColor);
                        String replacedText = ReplaceUtil.replaceCustomFonts(text);
                        if (replacedText.contains("-")) {
                            int minusColor = getMinusColor(elementAlpha, darkness);
                            RenderUtils.Render2D.drawCircle2(textX + 1.3f, textY - 0.25f, 1.75F * scale, minusColor);
                        }
                    } else {
                        ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                        Fonts.newcode[fontSize].drawText(stack, itemDisplayName, textX + 1, textY, mainTextColor);
                    }
                } else {
                    ITextComponent itemDisplayName = getItemDisplayName(itemStack);
                    Fonts.newcode[fontSize].drawText(stack, itemDisplayName, textX + 1, textY, mainTextColor);
                }
            } else if (component != null && statusSuffix.isEmpty()) {
                Fonts.newcode[fontSize].drawText(stack, component, textX + 1, textY, mainTextColor);
            } else if (!statusSuffix.isEmpty()) {
                Fonts.newcode[fontSize].drawString(stack, moduleName, textX + 1, textY, mainTextColor);
                Fonts.newcode[fontSize].drawString(stack, statusSuffix, textX + moduleNameWidth + 1, textY, statusColor);
            } else {
                Fonts.newcode[fontSize].drawString(stack, text, textX + 1, textY, mainTextColor);
            }

            stack.pop();
            return 15 * scale;
        }

        public static void drawItemStack(ItemStack stack, double x, double y, String altText, boolean withoutOverlay, float animationScale, float hudScale) {
            RenderSystem.pushMatrix();
            float baseScale = 0.6f * hudScale;
            float iconSize = 16 * baseScale;
            float centerX = (float)x + iconSize / 2.0f;
            float centerY = (float)y + iconSize / 2.0f;
            RenderSystem.translatef(centerX, centerY, 0.0f);
            RenderSystem.scalef(animationScale, animationScale, 1.0f);
            RenderSystem.translatef(-centerX, -centerY, 0.0f);
            RenderSystem.scalef(baseScale, baseScale, 1.0f);
            RenderSystem.translated(x * (1.0 / baseScale), y * (1.0 / baseScale), 0.0);
            IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
            if (!withoutOverlay) IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
            RenderSystem.popMatrix();
        }
    }
}