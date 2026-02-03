package nuclear.ui.clickgui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.ITextComponent;
import nuclear.control.Manager;
import nuclear.module.TypeList;
import nuclear.module.api.Module;
import nuclear.ui.clickgui.objects.ModuleObject;
import nuclear.utils.SoundUtils;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.Direction;
import nuclear.utils.anim.impl.DecelerateAnimation;
import nuclear.utils.anim.impl.EaseBackIn;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.ScaleMath;
import nuclear.utils.render.Vec2i;
import nuclear.utils.render.animation.AnimationMath;
import nuclear.utils.language.Translated;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

import static nuclear.utils.IMinecraft.mc;
import static nuclear.utils.IMinecraft.sr;
import static nuclear.utils.render.RenderUtils.Render2D.prepareScissor;

public class Window extends Screen {

    private Vector2f position = new Vector2f(0, 0);
    private float smoothHoverY = 0;
    public static Vector2f size = new Vector2f(500, 400);
    public Animation langAnimation = new DecelerateAnimation(250, 1.0);

    public static int dark = new Color(18, 19, 25).getRGB();
    public static int medium = new Color(18, 19, 25).brighter().getRGB();
    public static int light = new Color(129, 134, 153).getRGB();
    private TypeList currentCategory;

    public static ArrayList<ModuleObject> objects = new ArrayList<>();

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for(Panel p:panels){
            p.onScroll(mouseX,mouseY,delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public Window(ITextComponent titleIn) {
        super(titleIn);
        scrolling = 0;
        if (Manager.FUNCTION_MANAGER != null) {
            for (Module module : Manager.FUNCTION_MANAGER.getFunctions()) {
                objects.add(new ModuleObject(module));
            }
        }
        size = new Vector2f(450, 350);
        position = new Vector2f(mc.getMainWindow().scaledWidth() / 2f, mc.getMainWindow().scaledHeight() / 2f);
        float offset = 0;
        float width = 120;
        for(TypeList typeList : TypeList.values()){
            panels.add(new Panel(typeList,(mc.getMainWindow().scaledWidth() / 2f)+offset, mc.getMainWindow().scaledHeight() / 2f,width,268));
            offset+=width+3;
        }
    }

    ArrayList<Panel> panels = new ArrayList<>();

    @Override
    protected void init() {
        super.init();
        panels.clear();
        size = new Vector2f(450, 350);
        float offset = 0;
        float width = 120;
        float height = 268;
        position = new Vector2f(mc.getMainWindow().scaledWidth() / 2f - (TypeList.values().length * width) / 2f, (mc.getMainWindow().scaledHeight() / 2f)-height/2f);
        for (TypeList typeList : TypeList.values()) {
            Panel panel = new Panel(typeList, position.x + offset, position.y, width, height);
            panels.add(panel);
            offset += width - 29;
        }
        // Закрываем все пикеры при открытии GUI
        for (Panel.ColorEntry entry : Panel.getColorEntries()) {
            entry.isPickerOpen = false;
            entry.pickerAnimation = 0.0f;
        }
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
            SoundUtils.playSound("guiopen.wav", 62, false);
        }
        openAnimation = true;
        alphaAnimation.setDirection(Direction.FORWARDS);
    }

    public static float scrolling;
    public static float scrollingOut;
    public static boolean searching;
    public static boolean searchFocused = false;
    public static String searchText = "";
    public static boolean languageDropdownOpen = false;

    public Animation animation = new EaseBackIn(400, 1, 1.5f);
    public static Animation alphaAnimation = new DecelerateAnimation(200, 1.0);
    public static boolean openAnimation = false;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        // Позволяем ходить в Panel даже если модуль GuiMove выключен
        updateMovementKeys();

        float alphaProgress = (float) alphaAnimation.getOutput();
        if (alphaProgress <= 0.01) {
            resetMovementKeys();
            mc.displayGuiScreen(null);
            return;
        }

        MatrixStack ms = new MatrixStack();

        RenderSystem.pushMatrix();
        mc.gameRenderer.setupOverlayRendering(2);

        Vec2i fixed = ScaleMath.getMouse(mouseX, mouseY);
        int scaledMouseX = fixed.getX();
        int scaledMouseY = fixed.getY();

        if (openAnimation) {
            alphaAnimation.setDirection(Direction.FORWARDS);
        } else {
            alphaAnimation.setDirection(Direction.BACKWARDS);
        }

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (Panel p : panels) {
            p.render(matrixStack, scaledMouseX, scaledMouseY, alphaProgress);
        }

        renderSearchWithAlpha(matrixStack, alphaProgress);

        if (Panel.isThemesActive() && !panels.isEmpty()) {
            Panel.theme(matrixStack, scaledMouseX, scaledMouseY, panels.get(0), alphaProgress);
        }

        renderSearchTextWithAlpha(ms, alphaProgress);
        renderLanguageDropdown(ms, scaledMouseX, scaledMouseY, alphaProgress);

        scrollingOut = AnimationMath.fast(scrollingOut, scrolling, 15);

        if (alphaAnimation.getOutput() < 0.1f && !openAnimation) {
            mc.displayGuiScreen(null);
        }

        RenderSystem.popMatrix();
    }

    private void renderSearchWithAlpha(MatrixStack matrixStack, float alphaProgress) {
        Panel.search(matrixStack, alphaProgress);
    }

    private void renderLanguageDropdown(MatrixStack ms, int mouseX, int mouseY, float alphaProgress) {
        langAnimation.setDirection(languageDropdownOpen ? Direction.FORWARDS : Direction.BACKWARDS);
        float langProgress = (float) langAnimation.getOutput();

        if (langProgress <= 0.01 && !languageDropdownOpen) return;

        float langButtonX = sr.scaledWidth() / 2f - 60 + 87;
        float langButtonY = sr.scaledHeight() / 2f + 117f;
        float langButtonWidth = 27;
        float langButtonHeight = 12;
        float dropdownY = langButtonY + langButtonHeight + 2;

        String[] allLanguages = {"RUS", "ENG", "PL", "UKR"};
        String currentLang = Translated.getCurrentLanguage();
        java.util.List<String> availableLanguages = new java.util.ArrayList<>();
        for (String lang : allLanguages) {
            if (!lang.equals(currentLang)) availableLanguages.add(lang);
        }

        float itemHeight = 12;
        float targetHeight = availableLanguages.size() * itemHeight;
        float currentHeight = targetHeight * langProgress;

        int fonColor = ColorUtils.setAlpha(Panel.getColorByName("fonColor"), (int)(230 * alphaProgress * langProgress));
        RenderUtils.Render2D.drawBlurredRoundedRectangle(langButtonX, dropdownY, langButtonWidth, currentHeight, 2.5f, fonColor, 1);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        prepareScissor(langButtonX, dropdownY, langButtonWidth, currentHeight);

        boolean anyHovered = false;
        for (int i = 0; i < availableLanguages.size(); i++) {
            float optionY = dropdownY + i * itemHeight;
            if (RenderUtils.isInRegion(mouseX, mouseY, langButtonX, optionY, langButtonWidth, itemHeight)) {
                if (smoothHoverY == 0) smoothHoverY = optionY;
                smoothHoverY = AnimationMath.fast(smoothHoverY, optionY, 26);
                anyHovered = true;
                break;
            }
        }

        if (anyHovered && languageDropdownOpen) {
            int hoverColor = ColorUtils.setAlpha(new Color(120, 120, 120).getRGB(), (int)(100 * alphaProgress * langProgress));
            RenderUtils.Render2D.drawRoundedRect(langButtonX + 2, smoothHoverY + 1, langButtonWidth - 4, itemHeight - 2, 1.5f, hoverColor);
        }

        for (int i = 0; i < availableLanguages.size(); i++) {
            float optionY = dropdownY + i * itemHeight;
            boolean isCurrentHovered = RenderUtils.isInRegion(mouseX, mouseY, langButtonX, optionY, langButtonWidth, itemHeight);

            int textColor = Panel.getColorByName("textColor");
            int finalTextColor = ColorUtils.setAlpha(textColor, (int)((isCurrentHovered ? 255 : 160) * alphaProgress * langProgress));

            Fonts.newcode[11].drawCenteredString(ms, availableLanguages.get(i),
                    langButtonX + langButtonWidth / 2f, optionY + 5.5f, finalTextColor);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderSearchTextWithAlpha(MatrixStack ms, float alphaProgress) {
        float searchTextX = sr.scaledWidth() / 2f - 46;
        float searchTextY = sr.scaledHeight() / 2f + 122.5f;

        String displayText = searchText;
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            displayText += "_";
        }

        int textColor = Panel.getColorByName("textColor");
        int animatedTextColor = Panel.applyAlphaToColor(textColor, alphaProgress);

        if (displayText.isEmpty() && !searchFocused) {
            Fonts.icon[13].drawCenteredString(ms, "O", searchTextX, searchTextY,
                    ColorUtils.setAlpha(animatedTextColor, 180));
            Fonts.newcode[13].drawCenteredString(ms, "Search..", searchTextX + 18,
                    searchTextY - 0.5f, ColorUtils.setAlpha(animatedTextColor, 150));
        } else {
            Fonts.icon[13].drawCenteredString(ms, "O", searchTextX, searchTextY, animatedTextColor);
            Fonts.newcode[13].drawString(ms, displayText, searchTextX + 6,
                    searchTextY - 0.5f, animatedTextColor);

            if (!searchText.isEmpty()) {
                float clearX = searchTextX + 95;
                float clearY = searchTextY - 2;
                Fonts.newcode[12].drawCenteredString(ms, "×", clearX, clearY,
                        ColorUtils.rgba(219, 220, 223, (int)(200 * alphaProgress)));
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && (Character.isLetterOrDigit(codePoint) || codePoint == ' ')) {
            if (searchText.length() < 8) {
                searchText += codePoint;
                searching = !searchText.isEmpty();
            }
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (searchFocused) {
                searchFocused = false;
                return true;
            }

            for (Panel p : panels) {
                p.saveScrollPosition();
            }
            openAnimation = false;
            return true;
        }

        if (searchFocused) {
            if (keyCode == 259) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                searching = !searchText.isEmpty();
                return true;
            } else if (keyCode == 257 || keyCode == 258) {
                searchFocused = false;
                return true;
            }
        }

        // Не блокируем клавиши движения (W, A, S, D, Space, Shift), чтобы можно было ходить в ClickGUI
        if (mc.player != null) {
            final KeyBinding[] movementKeys = {
                    mc.gameSettings.keyBindForward,
                    mc.gameSettings.keyBindBack,
                    mc.gameSettings.keyBindLeft,
                    mc.gameSettings.keyBindRight,
                    mc.gameSettings.keyBindJump,
                    mc.gameSettings.keyBindSprint
            };
            
            for (KeyBinding keyBinding : movementKeys) {
                if (keyBinding.getDefault().getKeyCode() == keyCode) {
                    // Не блокируем клавиши движения, позволяем им обрабатываться
                    if (!searchFocused) {
                        for(Panel p:panels){
                            p.onKey(keyCode,scanCode,modifiers);
                        }
                    }
                    return false; // Возвращаем false, чтобы клавиша не блокировалась
                }
            }
        }

        if (!searchFocused) {
            for(Panel p:panels){
                p.onKey(keyCode,scanCode,modifiers);
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Panel p : panels) {
            p.onDrag(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for(Panel p:panels){
            p.onRelease(mouseX,mouseY,button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
        resetMovementKeys();
        for (Panel p : panels) {
            p.saveScrollPosition();
        }
        searching = false;
        searchFocused = false;
        searchText = "";
        languageDropdownOpen = false;
        openAnimation = false;
        for (ModuleObject m : objects) {
            m.exit();
        }
        for (Panel.ColorEntry entry : Panel.getColorEntries()) {
            entry.isPickerOpen = false;
        }
        if (Manager.CONFIG_MANAGER != null) {
            Manager.CONFIG_MANAGER.saveConfiguration("default");
        }
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.clickGui.sounds.get()) {
            SoundUtils.playSound("guiclose.wav", 62, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vec2i fixed = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();

        float searchX = sr.scaledWidth() / 2f - 54;
        float searchY = sr.scaledHeight() / 2f + 115;
        float searchWidth = 110;
        float searchHeight = 16;

        float langButtonX = sr.scaledWidth() / 2f - 60 + 87;
        float langButtonY = sr.scaledHeight() / 2f + 117f;
        float langButtonWidth = 27;
        float langButtonHeight = 12;

        // Handle language button click
        boolean clickedOnLangButton = RenderUtils.isInRegion(mouseX, mouseY, langButtonX, langButtonY, langButtonWidth, langButtonHeight);
        
        // Handle language dropdown clicks
        if (languageDropdownOpen) {
            float dropdownY = langButtonY + langButtonHeight + 2;
            float dropdownWidth = langButtonWidth;
            
            // All available languages
            String[] allLanguages = {"RUS", "ENG", "PL", "UKR"};
            String currentLang = Translated.getCurrentLanguage();
            
            // Filter out current language
            java.util.List<String> availableLanguages = new java.util.ArrayList<>();
            for (String lang : allLanguages) {
                if (!lang.equals(currentLang)) {
                    availableLanguages.add(lang);
                }
            }
            
            float dropdownHeight = availableLanguages.size() * 12;

            if (RenderUtils.isInRegion(mouseX, mouseY, langButtonX, dropdownY, dropdownWidth, dropdownHeight)) {
                // Find which language was clicked
                for (int i = 0; i < availableLanguages.size(); i++) {
                    float optionY = dropdownY + i * 12;
                    if (RenderUtils.isInRegion(mouseX, mouseY, langButtonX, optionY, dropdownWidth, 12)) {
                        // Clicked on a language - switch to it
                        Translated.setLanguage(availableLanguages.get(i));
                        languageDropdownOpen = false;
                        return true;
                    }
                }
            } else if (!clickedOnLangButton) {
                // Click outside dropdown and button - close it
                languageDropdownOpen = false;
            }
        }

        // Handle language button click to toggle dropdown
        if (clickedOnLangButton && button == 0) {
            languageDropdownOpen = !languageDropdownOpen;
            return true;
        }

        if (RenderUtils.isInRegion(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight)) {
            if (!searchText.isEmpty() && button == 0) {
                float clearX = searchX + 95;
                float clearY = searchY + 1;
                if (RenderUtils.isInRegion(mouseX, mouseY, clearX - 10, clearY - 2, 20, 20)) {
                    searchText = "";
                    searching = false;
                    searchFocused = true;
                    return true;
                }
            }

            searchFocused = true;
            searching = !searchText.isEmpty();
            return true;
        } else {
            searchFocused = false;
        }

        for (Panel p : panels) {
            p.onClick(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Обновляет состояние клавиш движения, копируя логику из GuiMove,
     * чтобы можно было ходить в Panel даже при выключенном модуле.
     */
    private void updateMovementKeys() {
        if (mc.player == null || mc.getMainWindow() == null) return;

        final KeyBinding[] pressedKeys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };

        for (KeyBinding keyBinding : pressedKeys) {
            boolean isKeyPressed = InputMappings.isKeyDown(mc.getMainWindow().getHandle(), keyBinding.getDefault().getKeyCode());
            keyBinding.setPressed(isKeyPressed);
        }
    }

    /**
     * Сбрасывает нажатия клавиш движения при закрытии ClickGUI.
     */
    private void resetMovementKeys() {
        if (mc.player == null) return;

        final KeyBinding[] pressedKeys = {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };

        for (KeyBinding keyBinding : pressedKeys) {
            keyBinding.setPressed(false);
        }
    }
}













