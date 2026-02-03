package nuclear.ui.clickgui;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.MathHelper;
import nuclear.control.Manager;
import nuclear.module.TypeList;
import nuclear.module.api.Module;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.ui.clickgui.objects.Object;
import nuclear.utils.SoundUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.math.GLUtils;
import nuclear.utils.render.*;
import nuclear.utils.render.animation.AnimationMath;
import nuclear.utils.language.Translated;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static nuclear.utils.IMinecraft.mc;
import static nuclear.utils.IMinecraft.sr;

public class Panel {
    public static int selectedColor;
    private static float savedHue;
    private static float savedSaturation;
    private static float savedBrightness;
    private static float savedAlpha;
    private static boolean isThemesActive = true;
    private static final float COLOR_PICKER_ANIMATION_SPEED = 20.0f;

    public static class ColorEntry {
        public int color;
        String displayName;
        public String useName;
        int position;
        public float hue;
        public float saturation;
        public float brightness;
        public float alpha;
        public boolean isPickerOpen;
        float pickerAnimation;
        boolean colorSelectorDragging;
        boolean hueSelectorDragging;
        boolean alphaSelectorDragging;
        float copyButtonHoverAnim;
        float copyButtonClickAnim;
        float pasteButtonHoverAnim;
        float pasteButtonClickAnim;

        ColorEntry(int color, String displayName, String useName, int position) {
            this.color = color;
            this.displayName = displayName;
            this.useName = useName;
            this.position = position;
            float[] hsb = RGBtoHSB(color);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
            this.alpha = ((color >> 24) & 0xFF) / 255.0f;
            this.isPickerOpen = false;
            this.pickerAnimation = 0.0f;
            this.colorSelectorDragging = false;
            this.hueSelectorDragging = false;
            this.alphaSelectorDragging = false;
            this.copyButtonHoverAnim = 0.0f;
            this.copyButtonClickAnim = 0.0f;
            this.pasteButtonHoverAnim = 0.0f;
            this.pasteButtonClickAnim = 0.0f;
        }
    }

    private static final ArrayList<ColorEntry> colorEntries = new ArrayList<>();

    static {
        selectedColor = Color.WHITE.getRGB();
        float[] hsb = RGBtoHSB(selectedColor);
        savedHue = hsb[0];
        savedSaturation = hsb[1];
        savedBrightness = hsb[2];
        savedAlpha = 1.0f;

        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(125, 29, 29, 255), 1.0f).getRGB(),
                "Основной",
                "primaryColor",
                0
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(6, 0, 0, 204), 1.0f).getRGB(),
                "Фон",
                "fonColor",
                1
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(255, 255, 255, 255), 1.0f).getRGB(),
                "Текста",
                "textColor",
                2
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(168, 25, 25, 255), 1.0f).getRGB(),
                "Иконки",
                "iconColor",
                3
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(255, 255, 255, 100), 1.0f).getRGB(),
                "Прочее",
                "iconnoColor",
                4
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(255, 255, 255, 255), 1.0f).getRGB(),
                "Заголовки",
                "infoColor",
                5
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(99, 99, 99, 255), 1.0f).getRGB(),
                "Скролл бар",
                "scrollColor",
                6
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(0, 220, 130, 255), 1.0f).getRGB(),
                "Галочка",
                "yesColor",
                7
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(220, 70, 70, 255), 1.0f).getRGB(),
                "Крестик",
                "crossColor",
                8
        ));
        colorEntries.add(new ColorEntry(
                ColorUtils.applyOpacity(new Color(160, 125, 82, 255), 1.0f).getRGB(),
                "Золотая полоска",
                "goldColor",
                9
        ));
    }

    public static boolean isThemesActive() {
        return isThemesActive;
    }

    public static void setThemesActive(boolean active) {
        isThemesActive = active;
    }

    public static int getColorByName(String useName) {
        for (ColorEntry entry : colorEntries) {
            if (entry.useName.equals(useName)) {
                return entry.color;
            }
        }
        return Color.WHITE.getRGB();
    }

    public static ArrayList<ColorEntry> getColorEntries() {
        return colorEntries;
    }

    public static void setSelectedColor(int color, float hue, float saturation, float brightness, float alpha) {
        selectedColor = color;
        savedHue = hue;
        savedSaturation = saturation;
        savedBrightness = brightness;
        savedAlpha = alpha;
    }

    public static JsonObject saveColors() {
        JsonObject colorsObject = new JsonObject();
        for (ColorEntry entry : colorEntries) {
            JsonObject entryObject = new JsonObject();
            entryObject.addProperty("color", entry.color);
            entryObject.addProperty("hue", entry.hue);
            entryObject.addProperty("saturation", entry.saturation);
            entryObject.addProperty("brightness", entry.brightness);
            entryObject.addProperty("alpha", entry.alpha);
            colorsObject.add(entry.useName, entryObject);
        }
        colorsObject.addProperty("selectedColor", selectedColor);
        colorsObject.addProperty("savedHue", savedHue);
        colorsObject.addProperty("savedSaturation", savedSaturation);
        colorsObject.addProperty("savedBrightness", savedBrightness);
        colorsObject.addProperty("savedAlpha", savedAlpha);
        return colorsObject;
    }

    public static void loadColors(JsonObject colorsObject) {
        if (colorsObject == null) return;

        for (ColorEntry entry : colorEntries) {
            if (colorsObject.has(entry.useName)) {
                JsonObject entryObject = colorsObject.getAsJsonObject(entry.useName);
                if (entryObject.has("color")) {
                    entry.color = entryObject.get("color").getAsInt();
                }
                if (entryObject.has("hue")) {
                    entry.hue = entryObject.get("hue").getAsFloat();
                }
                if (entryObject.has("saturation")) {
                    entry.saturation = entryObject.get("saturation").getAsFloat();
                }
                if (entryObject.has("brightness")) {
                    entry.brightness = entryObject.get("brightness").getAsFloat();
                }
                if (entryObject.has("alpha")) {
                    entry.alpha = entryObject.get("alpha").getAsFloat();
                }
            }
        }

        if (colorsObject.has("selectedColor")) {
            selectedColor = colorsObject.get("selectedColor").getAsInt();
        }
        if (colorsObject.has("savedHue")) {
            savedHue = colorsObject.get("savedHue").getAsFloat();
        }
        if (colorsObject.has("savedSaturation")) {
            savedSaturation = colorsObject.get("savedSaturation").getAsFloat();
        }
        if (colorsObject.has("savedBrightness")) {
            savedBrightness = colorsObject.get("savedBrightness").getAsFloat();
        }
        if (colorsObject.has("savedAlpha")) {
            savedAlpha = colorsObject.get("savedAlpha").getAsFloat();
        }

        // Обновляем secondColor после загрузки
        secondColor = getColorByName("primaryColor");
    }

    private static float[] RGBtoHSB(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return Color.RGBtoHSB(red, green, blue, null);
    }

    private static String colorToHex(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;

        if (alpha == 255) {
            return String.format("#%02X%02X%02X", red, green, blue);
        } else {
            return String.format("#%02X%02X%02X%02X", red, green, blue, alpha);
        }
    }

    private static int hexToColor(String hex) {
        try {
            return ColorUtils.hex(hex);
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean isMouseOver(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static final Map<TypeList, Float> savedScrollPositions = new HashMap<>();
    TypeList typeList;
    float x;
    float y;
    float width;
    float height;
    float scroll;
    float scrollAn;
    float animationProgress;
    ArrayList<ModuleObject> moduleObjects = new ArrayList<>();
    ArrayList<ModuleObject> filteredModuleObjects = new ArrayList<>();
    private boolean isOpen;
    private boolean isDraggingScrollbar = false;
    private float dragStartY;
    private float dragStartScroll;
    private boolean firstRender = true;

    public Panel(TypeList typeList, float x, float y, float width, float height) {
        this.typeList = typeList;
        this.x = x + 73;
        this.y = y + 19f;
        this.width = width - 25;
        this.height = height - 87f;
        this.animationProgress = 0.0f;
        this.isOpen = false;

        if (savedScrollPositions.containsKey(typeList)) {
            this.scroll = savedScrollPositions.get(typeList);
        } else {
            this.scroll = 0.0f;
        }
        this.scrollAn = this.scroll;

        if (Manager.FUNCTION_MANAGER != null) {
            for (Module m2 : Manager.FUNCTION_MANAGER.getFunctions().stream().filter(m -> m.category == typeList).toList()) {
                this.moduleObjects.add(new ModuleObject(m2));
            }
        }
        this.filteredModuleObjects = new ArrayList<>(this.moduleObjects);
    }

    private void updateFilteredModules() {
        if (Window.searching && !Window.searchText.isEmpty()) {
            String searchQuery = Window.searchText.toLowerCase();
            filteredModuleObjects = moduleObjects.stream()
                    .filter(m -> m.module.name.toLowerCase().contains(searchQuery))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } else {
            filteredModuleObjects = new ArrayList<>(moduleObjects);
        }
    }

    public void saveScrollPosition() {
        savedScrollPositions.put(this.typeList, this.scroll);
    }

    int firstColor = getColorByName("primaryColor");
    static int secondColor = getColorByName("primaryColor");

    private float calculateTotalHeight() {
        updateFilteredModules();

        float offset = -4f;
        float off = 11f;

        for (ModuleObject m : this.filteredModuleObjects) {
            m.expand_anim = AnimationMath.ultraSmooth(m.expand_anim, m.module.expanded ? 1.0f : 0.0f, 12.0f);

            for (Object object1 : m.object) {
                if (object1.setting != null && !object1.setting.visible()) continue;
                off += (object1.height + 9.5f) * m.expand_anim;
            }

            off += offset + 20.0f;
        }

        return off - 37;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float alphaProgress) {
        MatrixStack ms = new MatrixStack();

        updateFilteredModules();

        if (firstRender) {
            this.scrollAn = this.scroll;
            for (ModuleObject m : this.moduleObjects) {
                m.toggleAnimation = m.module.state ? 1.0f : 0.0f;
                m.expand_anim = m.module.expanded ? 1.0f : 0.0f;
            }
            firstRender = false;
        } else {
            this.scrollAn = AnimationMath.lerp(this.scrollAn, this.scroll, 20f);
        }

        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int)(200 * alphaProgress));
        int infoColor = applyAlphaToColor(getColorByName("infoColor"), alphaProgress);
        int iconColor1 = applyAlphaToColor(getColorByName("iconColor"), alphaProgress);

        RenderUtils.Render2D.drawBlurredRoundedRectangle(this.x + 5, y, this.width - 9, this.height - 1 + 4 + 31, 4, fonColor, alphaProgress);

        Fonts.newcode[15].drawCenteredString(ms, this.typeList.name(), (double) (this.x + this.width / 2.0f) + 5, (double) (y + 8f), infoColor);
        Fonts.icon[14].drawCenteredString(ms, typeList.icon, (double) (this.x + this.width / 2.0f) - 1 - (Fonts.newcode[15].getWidth(typeList.name()) / 2), (double) (y + 9f), iconColor1);

        float offset = -4f;
        float off = 11f;

        StencilUtils.initStencilToWrite();

        int animatedSecondColor = ColorUtils.setAlpha(secondColor, (int)(50 * alphaProgress));
        int animatedFirstColor = ColorUtils.setAlpha(firstColor, (int)(50 * alphaProgress));

        RenderUtils.Render2D.drawGradientRound(this.x + 5, y, this.width - 9, this.height + 28, 4.6f,
                animatedSecondColor,
                animatedFirstColor,
                animatedSecondColor,
                animatedFirstColor);
        StencilUtils.readStencilBuffer(1);

        float originalWidth = this.width - 1.0f;
        float originalHeight = 15.0f;

        for (ModuleObject m : this.filteredModuleObjects) {
            SmartScissor.push();
            SmartScissor.setFromComponentCoordinates((int) this.x, (int) y + 18, (int) this.width, (int) this.height + 12);

            m.width = originalWidth;
            m.height = originalHeight;
            m.x = this.x + 1.0f;
            m.y = y + off + offset + this.scrollAn + 12.5f;

            float moduleHeight = m.height;

            m.expand_anim = AnimationMath.ultraSmooth(m.expand_anim, m.module.expanded ? 1.0f : 0.0f, 12.0f);
            m.toggleAnimation = AnimationMath.ultraSmooth(m.toggleAnimation, m.module.state ? 1.0f : 0.0f, 15f);

            GL11.glPushMatrix();

            if (m.module.expanded) {
                for (Object object1 : m.object) {
                    if (object1.setting != null && !object1.setting.visible()) continue;
                    moduleHeight += object1.height + 9.5f;
                }
            }

            int disabledColor = new Color(37, 37, 37, (int)(46 * alphaProgress)).getRGB();
            int enabledColor = new Color(37, 37, 37, (int)(80 * alphaProgress)).getRGB();
            int gradientColor = ColorUtils.interpolateColor(
                    ColorUtils.interpolateColor(disabledColor, enabledColor, m.toggleAnimation),
                    ColorUtils.interpolateColor(enabledColor, enabledColor, 0.3f),
                    m.toggleAnimation * 0.4f
            );

            RenderUtils.Render2D.drawRoundedRect(m.x + 7.5f, m.y, m.width - 20 + 5, moduleHeight, 2, gradientColor);

            float iconXStart = this.x + 12.0f;
            float iconXEnd = this.x + 15.0f + 1f;
            float iconX = iconXStart + (iconXEnd - iconXStart) * m.toggleAnimation;
            float iconY = (float) (y + off + offset + this.scrollAn + 20f) + 1f - 1;

            float textXStart = this.x + 12.0f;
            float textXEnd = this.x + 20.5f;
            float textX = textXStart + (textXEnd - textXStart) * m.toggleAnimation;
            float textY = (float) (y + off + offset + this.scrollAn + 19f);

            String name = m.isBinding ? "Select a Key.." : m.module.name;

            int animatedIconColor = ColorUtils.interpolateColor(
                    ColorUtils.setAlpha(iconColor1, 0),
                    iconColor1,
                    m.toggleAnimation
            );
            Fonts.icon[12].drawCenteredString(ms, typeList.icon, iconX, iconY, animatedIconColor);

            int textColor1 = getColorByName("textColor");
            int animatedTextColor = applyAlphaToColor(textColor1, alphaProgress);

            int textColor = ColorUtils.interpolateColor(
                    ColorUtils.setAlpha(animatedTextColor, (int)(150 * alphaProgress)),
                    ColorUtils.setAlpha(animatedTextColor, (int)(255 * alphaProgress)),
                    m.toggleAnimation
            );
            Fonts.newcode[13].drawString(ms, name, textX, textY, textColor);

            if (!m.module.settingList.isEmpty()) {
                int dotsColor = ColorUtils.interpolateColor(
                        new Color(255, 255, 255, (int)(140 * alphaProgress)).getRGB(),
                        new Color(255, 255, 255, (int)(220 * alphaProgress)).getRGB(),
                        m.toggleAnimation
                );
                Fonts.newcode[16].drawCenteredString(ms, "...", (float) (m.x + m.width - 10) - 5.5f, (float) (m.y + 3.75f), dotsColor);
            }

            float yd = 6.0f;
            for (Object object1 : m.object) {
                if (object1.setting != null && !object1.setting.visible()) continue;

                object1.x = this.x;
                object1.y = y + yd + off + offset + this.scrollAn + 25.0f;
                object1.width = this.width;
                object1.height = 10.0f;

                if (m.expand_anim > 0.6) {
                    renderObjectWithAlpha(object1, ms, mouseX, mouseY, alphaProgress);
                }

                off += (object1.height + 9.5f) * m.expand_anim;
            }

            GL11.glPopMatrix();
            off += offset + 20.0f;

            SmartScissor.pop();
        }

        StencilUtils.uninitStencilBuffer();

        float max2 = calculateTotalHeight();
        float maxScroll = max2 < this.height - 6.0f ? 0.0f : -(max2 - (this.height - 16.0f));
        this.scroll = MathHelper.clamp(this.scroll, maxScroll, 0.0f);
        this.scrollAn = MathHelper.clamp(this.scrollAn, maxScroll, 0.0f);

        float scrollbarX = this.x + this.width - 3 - 3;
        float scrollbarY = y + 16;
        float scrollbarWidth = 1;
        float scrollbarHeight = this.height + 16;
        float visibleHeight = this.height - 6;
        float totalHeight = max2;
        float thumbHeight = totalHeight > visibleHeight ? Math.max(20, scrollbarHeight * (visibleHeight / totalHeight)) : scrollbarHeight;
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;

        if (totalHeight > visibleHeight) {
            int scrollColor = getColorByName("scrollColor");
            int animatedScrollColor = applyAlphaToColor(scrollColor, alphaProgress);

            StencilUtils.initStencilToWrite();
            RenderUtils.Render2D.drawRoundedRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4f,
                    ColorUtils.setAlpha(animatedScrollColor, (int)(45 * alphaProgress)));
            StencilUtils.readStencilBuffer(1);

            RenderUtils.Render2D.drawRoundedRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4f,
                    ColorUtils.setAlpha(animatedScrollColor, (int)(45 * alphaProgress)));
            RenderUtils.Render2D.drawRoundedRect(scrollbarX, thumbY, scrollbarWidth, thumbHeight, 4f,
                    ColorUtils.setAlpha(animatedScrollColor, isDraggingScrollbar ? (int)(60 * alphaProgress) : (int)(50 * alphaProgress)));

            StencilUtils.uninitStencilBuffer();
        }
    }

    private void renderObjectWithAlpha(Object object, MatrixStack ms, int mouseX, int mouseY, float alphaProgress) {
        object.draw(ms, mouseX, mouseY);
    }

    public static void search(MatrixStack matrixStack, float alphaProgress) {
        int fonColor = ColorUtils.setAlpha(getColorByName("fonColor"), (int)(200 * alphaProgress));
        int fonColor2 = ColorUtils.setAlpha(getColorByName("iconnoColor"), (int)(30 * alphaProgress));
        int textColor = getColorByName("textColor");
        int animatedTextColor = applyAlphaToColor(textColor, alphaProgress);

        RenderUtils.Render2D.drawBlurredRoundedRectangle(sr.scaledWidth() / 2 - 54, sr.scaledHeight() / 2f + 115, 110, 16, 3.5f, fonColor, alphaProgress);
        RenderUtils.Render2D.drawRoundedRect(sr.scaledWidth() / 2 - 60 + 87, sr.scaledHeight() / 2f + 117f, 27, 12, 2.5f, fonColor2);
        
        // Show current language instead of "ALT + T"
        String currentLang = nuclear.utils.language.Translated.getCurrentLanguage();
        Fonts.newcode[11].drawCenteredString(matrixStack, currentLang, sr.scaledWidth() / 2 - 60 + 101, sr.scaledHeight() / 2f + 122.5f,
                ColorUtils.setAlpha(animatedTextColor, (int)(150 * alphaProgress)));
    }

    public static int applyAlphaToColor(int color, float alpha) {
        Color colorObj = new Color(color);
        return new Color(
                colorObj.getRed(),
                colorObj.getGreen(),
                colorObj.getBlue(),
                (int)(colorObj.getAlpha() * alpha)
        ).getRGB();
    }

    private static String translateColorEntryName(String displayName) {
        String lang = Translated.getCurrentLanguage();
        
        // Translate only Russian names
        switch (displayName) {
            case "Основной":
                switch (lang) {
                    case "RUS": return "Основной";
                    case "ENG": return "Primary";
                    case "PL": return "Główny";
                    case "UKR": return "Основний";
                    default: return "Primary";
                }
            case "Фон":
                switch (lang) {
                    case "RUS": return "Фон";
                    case "ENG": return "Background";
                    case "PL": return "Tło";
                    case "UKR": return "Фон";
                    default: return "Background";
                }
            case "Текста":
                switch (lang) {
                    case "RUS": return "Текста";
                    case "ENG": return "Text";
                    case "PL": return "Tekst";
                    case "UKR": return "Текст";
                    default: return "Text";
                }
            case "Иконки":
                switch (lang) {
                    case "RUS": return "Иконки";
                    case "ENG": return "Icons";
                    case "PL": return "Ikony";
                    case "UKR": return "Іконки";
                    default: return "Icons";
                }
            case "Прочее":
                switch (lang) {
                    case "RUS": return "Прочее";
                    case "ENG": return "Other";
                    case "PL": return "Inne";
                    case "UKR": return "Інше";
                    default: return "Other";
                }
            case "Заголовки":
                switch (lang) {
                    case "RUS": return "Заголовки";
                    case "ENG": return "Headers";
                    case "PL": return "Nagłówki";
                    case "UKR": return "Заголовки";
                    default: return "Headers";
                }
            case "Скролл бар":
                switch (lang) {
                    case "RUS": return "Скролл бар";
                    case "ENG": return "Scroll Bar";
                    case "PL": return "Pasek Przewijania";
                    case "UKR": return "Смуга Прокрутки";
                    default: return "Scroll Bar";
                }
            case "Галочка":
                switch (lang) {
                    case "RUS": return "Галочка";
                    case "ENG": return "Checkmark";
                    case "PL": return "Zaznaczenie";
                    case "UKR": return "Галочка";
                    default: return "Checkmark";
                }
            case "Крестик":
                switch (lang) {
                    case "RUS": return "Крестик";
                    case "ENG": return "Cross";
                    case "PL": return "Krzyżyk";
                    case "UKR": return "Хрестик";
                    default: return "Cross";
                }
            case "Золотая полоска":
                switch (lang) {
                    case "RUS": return "Золотая полоска";
                    case "ENG": return "Gold Stripe";
                    case "PL": return "Złoty Pasek";
                    case "UKR": return "Золота Смуга";
                    default: return "Gold Stripe";
                }
            default:
                return displayName; // Return original if not found
        }
    }

    public void onClick(double mouseX, double mouseY, int button) {
        Vec2i mo = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        mouseX = mo.getX();
        mouseY = mo.getY();

        if (button == 0 && isMouseOver((int) mouseX, (int) mouseY,
                14, sr.scaledHeight() / 2f - 115, 105, 17)) {
            setThemesActive(true);
            return;
        }

        if (isThemesActive) {
            boolean clickInAnyPicker = false;
            float colorOffsetY = sr.scaledHeight() / 2f - 95;

            for (ColorEntry entry : colorEntries) {
                float x = 14;
                float entryX = x + 4;
                float entryY = colorOffsetY;
                float entryWidth = 105 - 8;
                float entryHeight = 15;

                float pickerX = x + 105 + 7;
                float pickerY = colorOffsetY - 15.5f;
                float pickerWidth = 54;
                float pickerHeight = 54;
                float hueSliderX = x + 105 + 7;
                float hueSliderY = colorOffsetY + 41.5f;
                float hueSliderWidth = 54;
                float hueSliderHeight = 4.5f;
                float alphaSliderY = colorOffsetY + 48.5f;

                boolean inPicker = RenderUtils.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight);
                boolean inHue = RenderUtils.isInRegion(mouseX, mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight);
                boolean inAlpha = RenderUtils.isInRegion(mouseX, mouseY, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight);
                boolean inEntry = isMouseOver((int) mouseX, (int) mouseY, entryX, entryY, entryWidth, entryHeight);

                if (entry.isPickerOpen) {
                    float pickerZoneX = x + 105 + 3.5f;
                    float pickerZoneY = colorOffsetY - 19.5f;
                    float pickerZoneWidth = 61f;
                    float pickerZoneHeight = 88f;
                    boolean inPickerZone = RenderUtils.isInRegion(mouseX, mouseY, pickerZoneX, pickerZoneY, pickerZoneWidth, pickerZoneHeight);

                    if (inPicker || inHue || inAlpha || inEntry || inPickerZone) {
                        clickInAnyPicker = true;
                    }
                }

                if ((button == 0 || button == 1) && inEntry) {
                    boolean wasOpen = entry.isPickerOpen;
                    for (ColorEntry other : colorEntries) {
                        other.isPickerOpen = false;
                    }
                    entry.isPickerOpen = !wasOpen;
                    entry.pickerAnimation = entry.isPickerOpen ? 0.0f : 1.0f;
                    if (entry.isPickerOpen) {
                        float[] hsb = RGBtoHSB(entry.color);
                        entry.hue = hsb[0];
                        entry.saturation = hsb[1];
                        entry.brightness = hsb[2];
                        entry.alpha = ((entry.color >> 24) & 0xFF) / 255.0f;
                    }
                    if (Manager.CONFIG_MANAGER != null) {
                        Manager.CONFIG_MANAGER.saveConfiguration("default");
                    }
                    return;
                }

                if (entry.isPickerOpen) {
                    float copyButtonX = x + 105 + 6f;
                    float copyButtonY = colorOffsetY - 19.5f + 75f;
                    float copyButtonWidth = 27;
                    float copyButtonHeight = 10;
                    float pasteButtonX = x + 105 + 34f;
                    float pasteButtonY = colorOffsetY - 19.5f + 75f;
                    float pasteButtonWidth = 27;
                    float pasteButtonHeight = 10;

                    boolean inCopyButton = RenderUtils.isInRegion(mouseX, mouseY, copyButtonX, copyButtonY, copyButtonWidth, copyButtonHeight);
                    boolean inPasteButton = RenderUtils.isInRegion(mouseX, mouseY, pasteButtonX, pasteButtonY, pasteButtonWidth, pasteButtonHeight);

                    if (button == 0 && inCopyButton) {
                        entry.copyButtonClickAnim = 1.0f;
                        String hexColor = colorToHex(entry.color);
                        mc.keyboardListener.setClipboardString(hexColor);
                        if (Manager.CONFIG_MANAGER != null) {
                            Manager.CONFIG_MANAGER.saveConfiguration("default");
                        }
                        return;
                    }

                    if (button == 0 && inPasteButton) {
                        entry.pasteButtonClickAnim = 1.0f;
                        try {
                            String clipboardText = mc.keyboardListener.getClipboardString();
                            if (clipboardText != null && !clipboardText.trim().isEmpty()) {
                                int pastedColor = hexToColor(clipboardText.trim());
                                if (pastedColor != -1) {
                                    entry.color = pastedColor;
                                    float[] hsb = RGBtoHSB(entry.color);
                                    entry.hue = hsb[0];
                                    entry.saturation = hsb[1];
                                    entry.brightness = hsb[2];
                                    entry.alpha = ((entry.color >> 24) & 0xFF) / 255.0f;

                                    if (entry.useName.equals("primaryColor")) {
                                        selectedColor = entry.color;
                                        savedHue = entry.hue;
                                        savedSaturation = entry.saturation;
                                        savedBrightness = entry.brightness;
                                        savedAlpha = entry.alpha;
                                    }
                                    if (Manager.CONFIG_MANAGER != null) {
                                        Manager.CONFIG_MANAGER.saveConfiguration("default");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Игнорируем ошибки парсинга
                        }
                        return;
                    }

                    if (button == 0 && !entry.colorSelectorDragging && inPicker) {
                        entry.colorSelectorDragging = true;
                        return;
                    }

                    if (button == 0 && !entry.hueSelectorDragging && inHue) {
                        entry.hueSelectorDragging = true;
                        return;
                    }

                    if (button == 0 && !entry.alphaSelectorDragging && inAlpha) {
                        entry.alphaSelectorDragging = true;
                        return;
                    }
                }

                colorOffsetY += 16.5f;
            }

            if ((button == 0 || button == 1) && !clickInAnyPicker) {
                for (ColorEntry entry : colorEntries) {
                    entry.isPickerOpen = false;
                }
                if (Manager.CONFIG_MANAGER != null) {
                    Manager.CONFIG_MANAGER.saveConfiguration("default");
                }
            }
        }

        updateFilteredModules();

        float max2 = calculateTotalHeight();
        float maxScroll = max2 < this.height - 6.0f ? 0.0f : -(max2 - (this.height - 16.0f));
        float scrollbarX = this.x + this.width - 3 - 3;
        float scrollbarY = y + 16;
        float scrollbarWidth = 1;
        float scrollbarHeight = this.height + 16;
        float visibleHeight = this.height - 6;
        float totalHeight = max2;
        float thumbHeight = totalHeight > visibleHeight ? Math.max(20, scrollbarHeight * (visibleHeight / totalHeight)) : scrollbarHeight;
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;

        if (totalHeight > visibleHeight && RenderUtils.isInRegion(mouseX, mouseY, scrollbarX, thumbY, scrollbarWidth, thumbHeight) && button == 0) {
            isDraggingScrollbar = true;
            dragStartY = (float) mouseY;
            dragStartScroll = scroll;
        }

        int zoneX = (int) this.x;
        int zoneY = (int) this.y + 18;
        int zoneWidth = (int) this.width;
        int zoneHeight = (int) this.height + 12;

        if (RenderUtils.isInRegion(mouseX, mouseY, zoneX, zoneY, zoneWidth, zoneHeight)) {
            float offset = -4f;
            float off = 11f;

            for (ModuleObject m : this.filteredModuleObjects) {
                m.mouseClicked((int) mouseX, (int) mouseY, button);

                if (RenderUtils.isInRegion(mouseX, mouseY, m.x + 7.5f, m.y, m.width - 20 + 5, 15) && button == 1) {
                    m.module.expanded = !m.module.expanded;

                    if (m.module.expanded && !isOpen) {
                        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                            SoundUtils.playSound("moduleopen.wav", 60, false);
                        }
                        isOpen = true;
                    } else if (!m.module.expanded && isOpen) {
                        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
                            SoundUtils.playSound("moduleclose.wav", 60, false);
                        }
                        isOpen = false;
                    }
                }

                if (m.module.expanded) {
                    float yd = 5.0f;
                    for (Object object1 : m.object) {
                        if (object1.setting == null) {
                            continue;
                        }

                        if (object1.setting.visible()) {
                            object1.y = this.y + yd + off + offset + this.scrollAn + 25.0f;
                            off += object1.height + 5.0f;
                        }
                    }
                }

                off += offset + 20.0f;
            }
        }
    }

    public void onScroll(double mouseX, double mouseY, double delta) {
        Vec2i m = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        if (RenderUtils.isInRegion(mouseX = (double) m.getX(), mouseY = (double) m.getY(), this.x, this.y, this.width, this.height + 32)) {
            float max2 = calculateTotalHeight();
            float maxScroll = max2 < this.height - 6.0f ? 0.0f : -(max2 - (this.height - 16.0f));

            scroll += (float) delta * 15f;
            scroll = MathHelper.clamp(scroll, maxScroll, 0.0f);
        }
    }

    public void onDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && button == 0) {
            float max2 = calculateTotalHeight();
            float maxScroll = max2 < this.height - 6.0f ? 0.0f : -(max2 - (this.height - 16.0f));
            float scrollbarHeight = this.height + 16;
            float visibleHeight = this.height - 6;
            float totalHeight = max2;
            float thumbHeight = totalHeight > visibleHeight ? Math.max(20, scrollbarHeight * (visibleHeight / totalHeight)) : scrollbarHeight;

            float mouseDeltaY = (float) mouseY - dragStartY;
            float scrollRange = scrollbarHeight - thumbHeight;
            float scrollPerPixel = maxScroll != 0 ? maxScroll / scrollRange : 0;
            scroll = dragStartScroll + (mouseDeltaY * scrollPerPixel);

            scroll = MathHelper.clamp(scroll, maxScroll, 0.0f);
        }
    }

    public void onRelease(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
        }
        Vec2i mo = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        mouseX = mo.getX();
        mouseY = mo.getY();
        float offset = -4f;
        float off = 11f;
        for (ModuleObject m : this.moduleObjects) {
            for (Object o : m.object) {
                o.mouseReleased((int) mouseX, (int) mouseY, button);
            }
        }

        if (button == 0) {
            for (ColorEntry entry : colorEntries) {
                entry.colorSelectorDragging = false;
                entry.hueSelectorDragging = false;
                entry.alphaSelectorDragging = false;
            }
            if (Manager.CONFIG_MANAGER != null) {
                Manager.CONFIG_MANAGER.saveConfiguration("default");
            }
        }
    }

    public void onKey(int keyCode, int scanCode, int modifiers) {
        for (ModuleObject m : this.moduleObjects) {
            m.keyTyped(keyCode, scanCode, modifiers);
        }
    }

    public static void theme(MatrixStack matrixStack, int mouseX, int mouseY, Panel panel, float alphaProgress) {
        MatrixStack ms = new MatrixStack();
        int x = 14;
        int y = sr.scaledHeight() / 2 - 98;
        int width = 105;
        int height = 197;

        int iconColor = getColorByName("iconColor");
        int fonColor =  ColorUtils.setAlpha(getColorByName("fonColor"), 200);
        int infoColor = getColorByName("infoColor");

        int animatedIconColor = applyAlphaToColor(iconColor, alphaProgress);
        int animatedInfoColor = applyAlphaToColor(infoColor, alphaProgress);
        int animatedFonColor = ColorUtils.setAlpha(fonColor, (int)(200 * alphaProgress));

        RenderUtils.Render2D.drawBlurredRoundedRectangle(x, y - 17, width, height + 17,
                4, animatedFonColor, alphaProgress);

        Fonts.newcode[15].drawCenteredString(ms, "Themes", x + 35f + 21, y - 9.5f, animatedInfoColor);
        Fonts.icon[14].drawCenteredString(ms, "u", x + 16.5f + 21, y - 9f, animatedIconColor);

        if (isThemesActive) {
            float colorOffsetY = y + 3;
            for (ColorEntry entry : colorEntries) {
                entry.pickerAnimation = AnimationMath.fast(entry.pickerAnimation,
                        entry.isPickerOpen ? 1.0f : 0.0f,
                        COLOR_PICKER_ANIMATION_SPEED);

                // Синхронизируем анимацию пикеров с альфой GUI при закрытии
                float finalPickerAnimation = entry.pickerAnimation * alphaProgress;

                int originalLighterColor = ColorUtils.boostColor(entry.color, 60);
                int animatedLighterColor = applyAlphaToColor(originalLighterColor, alphaProgress);

                RenderUtils.Render2D.drawRoundedCorner(x + 4, colorOffsetY, width - 8, 15,
                        3, ColorUtils.setAlpha(10, animatedLighterColor, 10, (int)(20 * alphaProgress)));

                RenderUtils.Render2D.drawCircle2(x + width - 12.5f, colorOffsetY + 5.5f,
                        2f, ColorUtils.setAlpha(animatedLighterColor, (int)(122 * alphaProgress)));

                int textAlpha = (int)(200 + 55 * alphaProgress);
                int animatedTextColor = ColorUtils.setAlpha(originalLighterColor, textAlpha);
                String translatedDisplayName = translateColorEntryName(entry.displayName);
                Fonts.newcode[13].drawString(ms, translatedDisplayName, x + 7, colorOffsetY + 6.5f, animatedTextColor);

                if (finalPickerAnimation > 0.01f) {
                    float pickerX = x + width + 7;
                    float pickerY = colorOffsetY - 15.5f;
                    float pickerWidth = 54;
                    float pickerHeight = 54;

                    float animationScale = finalPickerAnimation;
                    float animationAlpha = finalPickerAnimation * 214;

                    // Центр масштабирования - центр всей зоны колорпикера для синхронной анимации
                    float pickerZoneX = x + width + 3.5f;
                    float pickerZoneY = colorOffsetY - 19.5f;
                    float pickerZoneWidth = 61f;
                    float pickerZoneHeight = 88f;
                    float scaleCenterX = pickerZoneX + pickerZoneWidth / 2f;
                    float scaleCenterY = pickerZoneY + pickerZoneHeight / 2f;

                    GLUtils.scaleStart(scaleCenterX, scaleCenterY, animationScale);

                    if (entry.colorSelectorDragging &&
                            RenderUtils.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                        float xDiff = mouseX - pickerX;
                        entry.saturation = Math.max(0, Math.min(1, xDiff / pickerWidth));
                        float yDiff = mouseY - pickerY;
                        entry.brightness = Math.max(0, Math.min(1, 1 - yDiff / pickerHeight));
                        entry.color = ColorUtils.applyOpacity(
                                new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)),
                                entry.alpha
                        ).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        if (Manager.CONFIG_MANAGER != null) {
                            Manager.CONFIG_MANAGER.saveConfiguration("default");
                        }
                    }
                    int textColor = getColorByName("textColor");

                    RenderUtils.Render2D.drawBlurredRoundedRectangle(x + width + 3.5f, colorOffsetY - 19.5f,
                            61f, 88f, 4, fonColor, alphaProgress);

                    float copyButtonX = x + width + 6f;
                    float copyButtonY = colorOffsetY - 19.5f + 75f;
                    float copyButtonWidth = 27;
                    float copyButtonHeight = 10;
                    float pasteButtonX = x + width + 34f;
                    float pasteButtonY = colorOffsetY - 19.5f + 75f;
                    float pasteButtonWidth = 27;
                    float pasteButtonHeight = 10;

                    boolean isCopyHovered = RenderUtils.isInRegion(mouseX, mouseY, copyButtonX, copyButtonY, copyButtonWidth, copyButtonHeight);
                    boolean isPasteHovered = RenderUtils.isInRegion(mouseX, mouseY, pasteButtonX, pasteButtonY, pasteButtonWidth, pasteButtonHeight);

                    entry.copyButtonHoverAnim = AnimationMath.fast(entry.copyButtonHoverAnim, isCopyHovered ? 1.0f : 0.0f, 12.0f);
                    entry.pasteButtonHoverAnim = AnimationMath.fast(entry.pasteButtonHoverAnim, isPasteHovered ? 1.0f : 0.0f, 12.0f);
                    entry.copyButtonClickAnim = AnimationMath.fast(entry.copyButtonClickAnim, 0.0f, 15.0f);
                    entry.pasteButtonClickAnim = AnimationMath.fast(entry.pasteButtonClickAnim, 0.0f, 15.0f);

                    int fonColorButton = ColorUtils.setAlpha(getColorByName("fonColor"), 150);
                    int copyButtonColor = ColorUtils.interpolateColor(
                            fonColorButton,
                            ColorUtils.setAlpha(getColorByName("primaryColor"), (int)(100 * animationAlpha / 214)),
                            entry.copyButtonHoverAnim
                    );
                    int pasteButtonColor = ColorUtils.interpolateColor(
                            fonColorButton,
                            ColorUtils.setAlpha(getColorByName("primaryColor"), (int)(100 * animationAlpha / 214)),
                            entry.pasteButtonHoverAnim
                    );

                    float copyButtonClickEffect = 1.0f - entry.copyButtonClickAnim * 0.25f;
                    float pasteButtonClickEffect = 1.0f - entry.pasteButtonClickAnim * 0.25f;

                    Color copyColorObj = new Color(copyButtonColor, true);
                    Color pasteColorObj = new Color(pasteButtonColor, true);
                    int finalCopyButtonAlpha = (int)(copyColorObj.getAlpha() * copyButtonClickEffect);
                    int finalPasteButtonAlpha = (int)(pasteColorObj.getAlpha() * pasteButtonClickEffect);

                    int finalCopyButtonColor = new Color(copyColorObj.getRed(), copyColorObj.getGreen(), copyColorObj.getBlue(), finalCopyButtonAlpha).getRGB();
                    int finalPasteButtonColor = new Color(pasteColorObj.getRed(), pasteColorObj.getGreen(), pasteColorObj.getBlue(), finalPasteButtonAlpha).getRGB();

                    RenderUtils.Render2D.drawRoundedRect(copyButtonX, copyButtonY, copyButtonWidth, copyButtonHeight, 2f, finalCopyButtonColor);
                    RenderUtils.Render2D.drawRoundedRect(pasteButtonX, pasteButtonY, pasteButtonWidth, pasteButtonHeight, 2f, finalPasteButtonColor);

                    int copyTextAlpha = (int)((180 + 75 * entry.copyButtonHoverAnim) * copyButtonClickEffect);
                    int pasteTextAlpha = (int)((180 + 75 * entry.pasteButtonHoverAnim) * pasteButtonClickEffect);

                    Fonts.newcode[12].drawCenteredString(matrixStack, "Копир", x + width + 19f, colorOffsetY - 19.5f + 79f,
                            ColorUtils.setAlpha(textColor, copyTextAlpha));

                    Fonts.newcode[12].drawCenteredString(matrixStack, "Встав", x + width + 19f + 29f, colorOffsetY - 19.5f + 79f,
                            ColorUtils.setAlpha(textColor, pasteTextAlpha));

                    RenderUtils.Render2D.drawGradientRoundedRect(matrixStack, pickerX, pickerY,
                            pickerWidth, pickerHeight, 3,
                            -1, Color.BLACK.getRGB(),
                            Color.HSBtoRGB(entry.hue, 1, 1),
                            Color.BLACK.getRGB());

                    float selectorX = pickerX + entry.saturation * pickerWidth;
                    float selectorY = pickerY + (1 - entry.brightness) * pickerHeight;

                    selectorX = Math.max(pickerX, Math.min(pickerX + pickerWidth - 4, selectorX));
                    selectorY = Math.max(pickerY, Math.min(pickerY + pickerHeight - 4, selectorY));

                    RenderUtils.Render2D.drawCircle2(selectorX, selectorY, 2f,
                            ColorUtils.rgba(255, 255, 255,
                                    (int) (animationAlpha * 255 / 214)));

                    float hueSliderX = x + width + 7;
                    float hueSliderY = colorOffsetY + 41.5f;
                    float hueSliderWidth = 54;
                    float hueSliderHeight = 4.5f;

                    if (entry.hueSelectorDragging &&
                            RenderUtils.isInRegion(mouseX, mouseY, hueSliderX, hueSliderY, hueSliderWidth, hueSliderHeight)) {
                        float xDiff = mouseX - hueSliderX;
                        entry.hue = Math.max(0, Math.min(1, xDiff / hueSliderWidth));
                        entry.color = ColorUtils.applyOpacity(
                                new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)),
                                entry.alpha
                        ).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        if (Manager.CONFIG_MANAGER != null) {
                            Manager.CONFIG_MANAGER.saveConfiguration("default");
                        }
                    }

                    float times = 5;
                    float size = hueSliderWidth / times;
                    float sliderX = hueSliderX;

                    for (int i = 0; i < times; i++) {
                        int color1 = Color.HSBtoRGB(0.2F * i, 1, 1);
                        int color2 = Color.HSBtoRGB(0.2F * (i + 1), 1, 1);

                        if (i == 0) {
                            RenderUtils.Render2D.drawCustomGradientRoundedRect(matrixStack,
                                    sliderX, hueSliderY, size + 0.4f, hueSliderHeight,
                                    4, 4, 0, 0, color1, color1, color2, color2);
                        } else if (i == times - 1) {
                            RenderUtils.Render2D.drawCustomGradientRoundedRect(matrixStack,
                                    sliderX, hueSliderY, size, hueSliderHeight,
                                    0, 0, 4, 4, color1, color1, color2, color2);
                        } else {
                            RenderUtils.Render2D.drawGradientRectCustom(matrixStack,
                                    sliderX + 0.001f, hueSliderY, size + 0.4f, hueSliderHeight,
                                    color1, color1, color2, color2);
                        }

                        sliderX += size;
                    }

                    float hueSelectorX = entry.hue * hueSliderWidth;
                    hueSelectorX = Math.max(0, Math.min(hueSliderWidth - 4.5f, hueSelectorX));

                    RenderUtils.Render2D.drawRoundOutline(hueSliderX + hueSelectorX, hueSliderY,
                            4.5f, 4.5f, 2f, 0.1f, ColorUtils.rgba(25, 26, 33, 0),
                            new Vector4i(
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214))
                            ));

                    float alphaSliderY = colorOffsetY + 48.5f;
                    if (entry.alphaSelectorDragging &&
                            RenderUtils.isInRegion(mouseX, mouseY, hueSliderX, alphaSliderY, hueSliderWidth, hueSliderHeight)) {
                        float xDiff = mouseX - hueSliderX;
                        entry.alpha = Math.max(0, Math.min(1, xDiff / hueSliderWidth));
                        entry.color = ColorUtils.applyOpacity(
                                new Color(Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness)),
                                entry.alpha
                        ).getRGB();
                        if (entry.useName.equals("primaryColor")) {
                            selectedColor = entry.color;
                            savedHue = entry.hue;
                            savedSaturation = entry.saturation;
                            savedBrightness = entry.brightness;
                            savedAlpha = entry.alpha;
                        }
                        if (Manager.CONFIG_MANAGER != null) {
                            Manager.CONFIG_MANAGER.saveConfiguration("default");
                        }
                    }

                    int color = Color.HSBtoRGB(entry.hue, entry.saturation, entry.brightness);
                    RenderUtils.Render2D.drawGradientRoundedRect(matrixStack, hueSliderX, alphaSliderY,
                            hueSliderWidth, hueSliderHeight, 2,
                            -1, -1,
                            ColorUtils.rgba(color >> 16 & 255, color >> 8 & 255, color & 255,
                                    (int) (animationAlpha * 255 / 214)),
                            ColorUtils.rgba(color >> 16 & 255, color >> 8 & 255, color & 255,
                                    (int) (animationAlpha * 255 / 214)));

                    float alphaSelectorX = entry.alpha * hueSliderWidth;
                    alphaSelectorX = Math.max(0, Math.min(hueSliderWidth - 4.5f, alphaSelectorX));

                    RenderUtils.Render2D.drawRoundOutline(hueSliderX + alphaSelectorX, alphaSliderY,
                            4.5f, 4.5f, 2f, 0.1f, ColorUtils.rgba(25, 26, 33, 0),
                            new Vector4i(
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214)),
                                    ColorUtils.rgba(255, 255, 255, (int) (animationAlpha * 255 / 214))
                            ));

                    GLUtils.scaleEnd();
                }

                colorOffsetY += 16.5f;
            }
        }
    }
}