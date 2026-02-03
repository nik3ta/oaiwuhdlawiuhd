package nuclear.ui.alt;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import nuclear.utils.IMinecraft;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.impl.DecelerateAnimation;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.*;
import nuclear.utils.render.animation.AnimationMath;
import nuclear.utils.render.shader.core.Shader;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static nuclear.ui.clickgui.Panel.getColorByName;
import static nuclear.utils.IMinecraft.mc;

public class AltManager extends Screen {
    private static Shader raysGlowShader;
    private static long startTime = System.currentTimeMillis();
    private final Animation targetInfoAnimation;

    public AltManager() {
        super(new StringTextComponent(""));
        this.targetInfoAnimation = new DecelerateAnimation(300, 1.0);
    }

    public ArrayList<Account> accounts = new ArrayList<>();

    private ArrayList<Account> getFilteredAccounts() {
        ArrayList<Account> source = accounts;
        if (!searchQuery.isEmpty()) {
            source = new ArrayList<>();
            String lowerQuery = searchQuery.toLowerCase();
            for (Account account : accounts) {
                if (account.accountName.toLowerCase().contains(lowerQuery)) {
                    source.add(account);
                }
            }
        }
        
        ArrayList<Account> sorted = new ArrayList<>();
        for (Account account : source) {
            if (account.pinned) {
                sorted.add(account);
            }
        }
        for (Account account : source) {
            if (!account.pinned) {
                sorted.add(account);
            }
        }
        return sorted;
    }


    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrollbar && button == 0) {
            int windowWidth = mc.getMainWindow().scaledWidth();
            int windowHeight = mc.getMainWindow().scaledHeight();
            float inputPanelWidth = 110f;
            float listPanelWidth = 220f;
            float panelSpacing = 4f;
            float totalWidth = inputPanelWidth + panelSpacing + listPanelWidth;
            float inputPanelX = windowWidth / 2f - totalWidth / 2f;
            float listPanelX = inputPanelX + inputPanelWidth + panelSpacing;
            float listPanelY = windowHeight / 2f - 80f;
            float listPanelHeight = 160f;
            float accountStartY = listPanelY + 28f;
            float stencilStartY = listPanelY + 25f;
            float stencilHeight = listPanelHeight - 30f;
            float visibleHeight = stencilHeight - (accountStartY - stencilStartY);
            float scrollbarX = listPanelX + listPanelWidth - 3 - 3;
            float scrollbarY = listPanelY + 28f;
            float scrollbarHeight = listPanelHeight - 28f;
            
            float accountCardHeight = 24f;
            float accountCardSpacing = 2f;
            float itemHeight = accountCardHeight + accountCardSpacing;
            ArrayList<Account> filteredAccountsForDrag = getFilteredAccounts();
            float totalRows = (float) Math.ceil(filteredAccountsForDrag.size() / 2.0);
            float totalHeight = totalRows * itemHeight;
            float thumbHeight = totalHeight > visibleHeight ? Math.max(20, scrollbarHeight * (visibleHeight / totalHeight)) : scrollbarHeight;
            float maxScroll = totalHeight > visibleHeight ? -(totalHeight - visibleHeight) : 0;
            
            float mouseDeltaY = (float) mouseY - dragStartY;
            float scrollRange = scrollbarHeight - thumbHeight;
            float scrollPerPixel = maxScroll != 0 ? maxScroll / scrollRange : 0;
            scroll = dragStartScroll + (mouseDeltaY * scrollPerPixel);
            
            scroll = MathHelper.clamp(scroll, maxScroll, 0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searching) {
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                searchQuery += Minecraft.getInstance().keyboardListener.getClipboardString();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty())
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searching = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                return true;
            }
            return true;
        }

        if (typing) {
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                altName += Minecraft.getInstance().keyboardListener.getClipboardString();
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!altName.isEmpty())
                    altName = altName.substring(0, altName.length() - 1);
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (!altName.isEmpty())
                    accounts.add(new Account(altName));
                typing = false;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                altName = "";
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searching) {
            searchQuery += Character.toString(codePoint);
            return true;
        }
        if (typing) {
            altName += Character.toString(codePoint);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vec2i fixed = ScaleMath.getMouse((int) mouseX, (int) mouseY);
        mouseX = fixed.getX();
        mouseY = fixed.getY();

        int windowWidth = mc.getMainWindow().scaledWidth();
        int windowHeight = mc.getMainWindow().scaledHeight();
        
        float inputPanelWidth = 110f;
        float listPanelWidth = 220f;
        float panelSpacing = 4f;
        float totalWidth = inputPanelWidth + panelSpacing + listPanelWidth;
        float inputPanelX = windowWidth / 2f - totalWidth / 2f;
        float inputPanelY = windowHeight / 2f - 45f;
        float inputFieldWidth = 100f;
        float inputFieldHeight = 16f;
        float inputFieldX = inputPanelX + (inputPanelWidth - inputFieldWidth) / 2f;
        float usernameY = inputPanelY + 25f;
        float passwordY = usernameY + inputFieldHeight + 2f;
        float iconButtonSize = 14f;
        float iconButtonSpacing = 6f;
        float buttonsY = passwordY + inputFieldHeight + 4f;
        float buttonsStartX = inputPanelX + (inputPanelWidth - (iconButtonSize * 3 + iconButtonSpacing * 2)) / 2f;
        float backBtnX = inputPanelX + inputPanelWidth - iconButtonSize - 4f;
        float backBtnY = inputPanelY + 5f;
        
        float listPanelX = inputPanelX + inputPanelWidth + panelSpacing;
        float listPanelY = windowHeight / 2f - 80f;
        float listPanelHeight = 160f;
        float accountCardHeight = 24f;
        float accountCardSpacing = 2f;
        float columnSpacing = 2f;
        float accountCardWidth = (listPanelWidth - 16f - columnSpacing) / 2f;
        float accountStartX = listPanelX + 8f;
        float accountStartY = listPanelY + 28f;
        float searchFieldX = listPanelX + 8f;
        float searchFieldY = listPanelY + 6.5f;
        float searchFieldWidth = listPanelWidth - 16f;
        float searchFieldHeight = 16f;
        
        if (RenderUtils.isInRegion(mouseX, mouseY, searchFieldX, searchFieldY, searchFieldWidth, searchFieldHeight)) {
            searching = !searching;
            typing = false;
            return true;
        }
        
        if (RenderUtils.isInRegion(mouseX, mouseY, inputFieldX, usernameY, inputFieldWidth, inputFieldHeight)) {
            typing = !typing;
            searching = false;
            return true;
        }
        
        if (RenderUtils.isInRegion(mouseX, mouseY, backBtnX, backBtnY, iconButtonSize, iconButtonSize)) {
            IMinecraft.mc.displayGuiScreen(new MainMenuScreen());
            return true;
        }
        
        float itemHeight = accountCardHeight + accountCardSpacing;
        float scrollbarX = listPanelX + listPanelWidth - 6f;
        float scrollbarY = listPanelY + 28f;
        float scrollbarWidth = 2f;
        float scrollbarHeight = listPanelHeight - 28f;
        float stencilStartY = listPanelY + 25f;
        float stencilHeight = listPanelHeight - 30f;
        float visibleHeight = stencilHeight - (accountStartY - stencilStartY);
        float totalRows = (float) Math.ceil(accounts.size() / 2.0);
        float totalHeight = totalRows * itemHeight;
        float visibleRows = visibleHeight / itemHeight;
        float thumbHeight = Math.max(20, scrollbarHeight * (visibleRows / Math.max(totalRows, 1)));
        float maxScroll = totalHeight > visibleHeight ? -(totalHeight - visibleHeight) : 0;
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;
        
        if (RenderUtils.isInRegion(mouseX, mouseY, scrollbarX, thumbY, scrollbarWidth, thumbHeight) && button == 0) {
            isDraggingScrollbar = true;
            dragStartY = (float) mouseY;
            dragStartScroll = scroll;
            return true;
        }
        
        ArrayList<Account> filteredAccountsForClick = getFilteredAccounts();
        for (int i = 0; i < filteredAccountsForClick.size(); i++) {
            Account account = filteredAccountsForClick.get(i);
            int column = i % 2;
            int row = i / 2;
            float acX = accountStartX + column * (accountCardWidth + columnSpacing);
            float acY = accountStartY + (row * (accountCardHeight + accountCardSpacing)) + scrollAn;
            float pinIconX = acX + 91f;
            float pinIconY = acY + 9f;
            float pinIconSize = 12f;
            
            if (RenderUtils.isInRegion(mouseX, mouseY, pinIconX - pinIconSize / 2f, pinIconY - pinIconSize / 2f, pinIconSize, pinIconSize) && button == 0) {
                account.pinned = !account.pinned;
                AltConfig.updateFile();
                return true;
            }
            
            if (RenderUtils.isInRegion(mouseX, mouseY, acX, acY, accountCardWidth, accountCardHeight)) {
                if (button == 0) {
                    IMinecraft.mc.session = new Session(account.accountName, "", "", "mojang");
                } else if (button == 1) {
                    int realIndex = accounts.indexOf(account);
                    if (realIndex != -1) {
                        accounts.remove(realIndex);
                        AltConfig.updateFile();
                    }
                }
                return true;
            }
        }

        if (button == 0) {
            float microsoftBtnX = buttonsStartX + 3;
            if (RenderUtils.isInRegion(mouseX, mouseY, microsoftBtnX, buttonsY, iconButtonSize, iconButtonSize)) {
                MicrosoftAuth.login();
                return true;
            }
            if (RenderUtils.isInRegion(mouseX, mouseY, buttonsStartX + iconButtonSize + iconButtonSpacing, buttonsY, iconButtonSize, iconButtonSize)) {
                AltConfig.updateFile();
                accounts.add(new Account(RandomStringUtils.randomAlphabetic(9)));
                return true;
            }
            float addBtnX = buttonsStartX + (iconButtonSize + iconButtonSpacing) * 2 - 3;
            if (RenderUtils.isInRegion(mouseX, mouseY, addBtnX, buttonsY, iconButtonSize, iconButtonSize)) {
                AltConfig.updateFile();
                accounts.add(new Account(RandomStringUtils.randomAlphabetic(9)));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
    }

    @Override
    public void tick() {
        super.tick();
        scrollAn = AnimationMath.lerp(scrollAn, scroll, 0.8f);
    }

    public float scroll;
    public float scrollAn;

    public boolean hoveredFirst;
    public boolean hoveredSecond;

    public float hoveredFirstAn;
    public float hoveredSecondAn;

    private String altName = "";
    private boolean typing;
    private String searchQuery = "";
    private boolean searching;
    private boolean isDraggingScrollbar = false;
    private float dragStartY;
    private float dragStartScroll;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int windowWidth = mc.getMainWindow().scaledWidth();
        int windowHeight = mc.getMainWindow().scaledHeight();
        float inputPanelWidth = 110f;
        float listPanelWidth = 220f;
        float panelSpacing = 4f;
        float totalWidth = inputPanelWidth + panelSpacing + listPanelWidth;
        float inputPanelX = windowWidth / 2f - totalWidth / 2f;
        float listPanelX = inputPanelX + inputPanelWidth + panelSpacing;
        float listPanelY = windowHeight / 2f - 80f;
        float listPanelHeight = 160f;
        float accountStartY = listPanelY + 28f;
        float stencilStartY = listPanelY + 25f;
        float stencilHeight = listPanelHeight - 30f;
        float visibleHeight = stencilHeight - (accountStartY - stencilStartY);
        
        float accountCardHeight = 24f;
        float accountCardSpacing = 2f;
        float itemHeight = accountCardHeight + accountCardSpacing;
        float totalRows = (float) Math.ceil(accounts.size() / 2.0);
        float totalHeight = totalRows * itemHeight;
        float maxScroll = totalHeight > visibleHeight ? -(totalHeight - visibleHeight) : 0;
        scroll += delta * 12f;
        scroll = MathHelper.clamp(scroll, maxScroll, 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        return mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + buttonWidth && mouseY < buttonY + buttonHeight;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        scrollAn = AnimationMath.lerp(scrollAn, scroll, 8f);
        hoveredFirst = RenderUtils.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f + 21), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 80 + 17 + 90, (int) 58.5f, 19);
        hoveredSecond = RenderUtils.isInRegion(mouseX, mouseY, (int) ((int) (IMinecraft.mc.getMainWindow().scaledWidth() / 2f) - 79.5f + 21 + 60), IMinecraft.mc.getMainWindow().scaledHeight() / 2 - 80 + 17 + 90, (int) 58.5f, 19);
        hoveredFirstAn = AnimationMath.lerp(hoveredFirstAn, hoveredFirst ? 1 : 0, 10);
        hoveredSecondAn = AnimationMath.lerp(hoveredSecondAn, hoveredSecond ? 1 : 0, 10);

        int fonColor = ColorUtils.setAlpha(ColorUtils.ensureMinAlpha(getColorByName("fonColor"), 40), 200);

        IMinecraft.mc.gameRenderer.setupOverlayRendering(2);

        int windowWidth = mc.getMainWindow().scaledWidth();
        int windowHeight = mc.getMainWindow().scaledHeight();
        float screenW = (float) mc.getMainWindow().getFramebufferWidth();
        float screenH = (float) mc.getMainWindow().getFramebufferHeight();

        RenderUtils.Render2D.drawImage(new ResourceLocation("nuclear/images/mainmenu/background.png"), 0, 0, windowWidth, windowHeight, -1);

        if (nuclear.utils.render.shader.ShaderUtil.sun_rays != null) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            com.mojang.blaze3d.platform.GlStateManager.enableBlend();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            nuclear.utils.render.shader.ShaderUtil.sun_rays.attach();
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_resolution", screenW, screenH);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_time", (System.currentTimeMillis() - startTime) / 1000f);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_rays", 0.1f);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_colors[0]", 1.0f, 1.0f, 1.0f, 1.0f);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_colors[1]", 1.0f, 1.0f, 1.0f, 1.0f);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_intensity", 0.12f);
            nuclear.utils.render.shader.ShaderUtil.sun_rays.setUniformf("u_reach", 0.4f);

            RenderUtils.Render2D.drawRect(0, 0, windowWidth, windowHeight, -1);

            nuclear.utils.render.shader.ShaderUtil.sun_rays.detach();
            com.mojang.blaze3d.platform.GlStateManager.disableBlend();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        int iconColor = ColorUtils.ensureMinAlpha(getColorByName("iconColor"), 40);
        int textColor2 = ColorUtils.ensureMinAlpha(getColorByName("textColor"), 40);
        int textColor = ColorUtils.ensureMinAlpha(getColorByName("textColor"), 40);

        float inputPanelWidth = 120f;
        float listPanelWidth = 220f;
        float panelSpacing = 4f;
        float totalWidth = inputPanelWidth + panelSpacing + listPanelWidth;
        float inputPanelX = windowWidth / 2f - totalWidth / 2f;
        float inputPanelY = windowHeight / 2f - 45;
        float inputPanelHeight = 90f;

        GaussianBlur.startBlur();
        RenderUtils.Render2D.drawRoundedRect(inputPanelX, inputPanelY, inputPanelWidth, inputPanelHeight, 4f, -1);
        GaussianBlur.endBlur(8, 1);
        RenderUtils.Render2D.drawRoundedRect(inputPanelX, inputPanelY, inputPanelWidth, inputPanelHeight, 4f, fonColor);

        Fonts.newcode[18].drawCenteredString(matrixStack, "Alt Manager", inputPanelX + inputPanelWidth / 2f, inputPanelY + 10f, textColor2);

        float listPanelX = inputPanelX + inputPanelWidth + panelSpacing;
        float listPanelY = windowHeight / 2f - 80f;
        float listPanelHeight = 160f;

        GaussianBlur.startBlur();
        RenderUtils.Render2D.drawRoundedRect(listPanelX, listPanelY, listPanelWidth, listPanelHeight, 4f, -1);
        GaussianBlur.endBlur(8, 1);
        RenderUtils.Render2D.drawRoundedRect(listPanelX, listPanelY, listPanelWidth, listPanelHeight, 4f, fonColor);

        float searchFieldX = listPanelX + 8f;
        float searchFieldY = listPanelY + 6.5f;
        float searchFieldWidth = listPanelWidth - 16f;
        float searchFieldHeight = 16f;
        boolean searchHovered = isMouseOverButton(mouseX, mouseY, (int) searchFieldX, (int) searchFieldY, (int) searchFieldWidth, (int) searchFieldHeight);

        RenderUtils.Render2D.drawRoundedRect(searchFieldX, searchFieldY, searchFieldWidth, searchFieldHeight, 4f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(235, 40)), ColorUtils.setAlpha(fonColor, Math.max(250, 40)), searchHovered ? 1f : 0f));
        Fonts.icon[15].drawString(matrixStack, "O", searchFieldX + 5f, searchFieldY + 6.5f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(180, 40)), iconColor, searchHovered ? 1f : 0f));
        String searchText = searchQuery.isEmpty() && !searching ? "Search.." : searchQuery;
        Fonts.newcode[15].drawString(matrixStack, searchText + (searching ? (System.currentTimeMillis() % 1000 > 500 ? "_" : "") : ""), searchFieldX + 16f, searchFieldY + 6.5f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(180, 40)), textColor, (searchHovered || searching) ? 1f : 0f));

        float inputFieldWidth = 100f;
        float inputFieldHeight = 16f;
        float inputFieldX = inputPanelX + (inputPanelWidth - inputFieldWidth) / 2f;
        float usernameY = inputPanelY + 25f;
        float passwordY = usernameY + inputFieldHeight + 2f;

        boolean usernameHovered = isMouseOverButton(mouseX, mouseY, (int) inputFieldX, (int) usernameY, (int) inputFieldWidth, (int) inputFieldHeight);
        RenderUtils.Render2D.drawRoundedRect(inputFieldX, usernameY, inputFieldWidth, inputFieldHeight, 3f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), usernameHovered ? 1f : 0f));
        Fonts.icon[14].drawString(matrixStack, "U", inputFieldX + 4f, usernameY + 7f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(150, 40)), iconColor, usernameHovered ? 1f : 0f));
        Fonts.newcode[14].drawString(matrixStack, (altName.isEmpty() && !typing ? "Username" : altName) + (typing ? (System.currentTimeMillis() % 1000 > 500 ? "_" : "") : ""), inputFieldX + 14f, usernameY + 6.5f, ColorUtils.interpolateColor(ColorUtils.setAlpha(textColor, Math.max(200, 40)), textColor, usernameHovered ? 1f : 0f));

        boolean passwordHovered = isMouseOverButton(mouseX, mouseY, (int) inputFieldX, (int) passwordY, (int) inputFieldWidth, (int) inputFieldHeight);
        RenderUtils.Render2D.drawRoundedRect(inputFieldX, passwordY, inputFieldWidth, inputFieldHeight, 3f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), passwordHovered ? 1f : 0f));
        Fonts.icon[14].drawString(matrixStack, "v", inputFieldX + 4f, passwordY + 7f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(150, 40)), iconColor, passwordHovered ? 1f : 0f));
        Fonts.newcode[14].drawString(matrixStack, "Password", inputFieldX + 14f, passwordY + 6.5f, ColorUtils.interpolateColor(ColorUtils.setAlpha(textColor, Math.max(200, 40)), textColor, passwordHovered ? 1f : 0f));

        float iconButtonSize = 14f;
        float iconButtonSpacing = 6f;
        float buttonsY = passwordY + inputFieldHeight + 4f;
        float buttonsStartX = inputPanelX + (inputPanelWidth - (iconButtonSize * 3 + iconButtonSpacing * 2)) / 2f;

        float microsoftBtnX = buttonsStartX + 3;
        boolean microsoftBtnHovered = isMouseOverButton(mouseX, mouseY, (int) microsoftBtnX, (int) buttonsY, (int) iconButtonSize, (int) iconButtonSize);
        RenderUtils.Render2D.drawRoundedRect(microsoftBtnX, buttonsY, iconButtonSize, iconButtonSize, 2f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), microsoftBtnHovered ? 1f : 0f));
        RenderUtils.Render2D.drawRoundedRect(microsoftBtnX + 3.5f, buttonsY + 4, 3, 3, 0f, ColorUtils.interpolateColor(new Color(255, 0, 0, Math.max(155, 40)).getRGB(), new Color(255, 0, 0, Math.max(190, 40)).getRGB(), microsoftBtnHovered ? 1f : 0f));
        RenderUtils.Render2D.drawRoundedRect(microsoftBtnX + 3.5f, buttonsY + 8, 3, 3, 0f, ColorUtils.interpolateColor(new Color(3, 178, 218, Math.max(155, 40)).getRGB(), new Color(3, 178, 218, Math.max(190, 40)).getRGB(), microsoftBtnHovered ? 1f : 0f));
        RenderUtils.Render2D.drawRoundedRect(microsoftBtnX + 7.5f, buttonsY + 4, 3, 3, 0f, ColorUtils.interpolateColor(new Color(6, 202, 16, Math.max(155, 40)).getRGB(), new Color(6, 202, 16, Math.max(190, 40)).getRGB(), microsoftBtnHovered ? 1f : 0f));
        RenderUtils.Render2D.drawRoundedRect(microsoftBtnX + 7.5f, buttonsY + 8, 3, 3, 0f, ColorUtils.interpolateColor(new Color(255, 234, 0, Math.max(155, 40)).getRGB(), new Color(255, 234, 0, Math.max(190, 40)).getRGB(), microsoftBtnHovered ? 1f : 0f));

        float randomBtnX = buttonsStartX + iconButtonSize + iconButtonSpacing;
        boolean randomBtnHovered = isMouseOverButton(mouseX, mouseY, (int) randomBtnX, (int) buttonsY, (int) iconButtonSize, (int) iconButtonSize);
        RenderUtils.Render2D.drawRoundedRect(randomBtnX, buttonsY, iconButtonSize, iconButtonSize, 2f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), randomBtnHovered ? 1f : 0f));
        Fonts.icon2[16].drawCenteredString(matrixStack, "v", randomBtnX + iconButtonSize / 2f, buttonsY + iconButtonSize / 2f - 1f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(150, 40)), iconColor, randomBtnHovered ? 1f : 0f));

        float addBtnX = buttonsStartX + (iconButtonSize + iconButtonSpacing) * 2 - 3;
        boolean addBtnHovered = isMouseOverButton(mouseX, mouseY, (int) addBtnX, (int) buttonsY, (int) iconButtonSize, (int) iconButtonSize);
        RenderUtils.Render2D.drawRoundedRect(addBtnX, buttonsY, iconButtonSize, iconButtonSize, 2f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), addBtnHovered ? 1f : 0f));
        Fonts.icon2[16].drawCenteredString(matrixStack, "w", addBtnX + iconButtonSize / 2f, buttonsY + iconButtonSize / 2f - 1f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(150, 40)), iconColor, addBtnHovered ? 1f : 0f));

        float backBtnX = inputPanelX + inputPanelWidth - iconButtonSize - 4f;
        float backBtnY = inputPanelY + 5f;
        boolean backBtnHovered = isMouseOverButton(mouseX, mouseY, (int) backBtnX, (int) backBtnY, (int) iconButtonSize, (int) iconButtonSize);
        RenderUtils.Render2D.drawRoundedRect(backBtnX + 1, backBtnY + 1, iconButtonSize - 2, iconButtonSize - 2, 2f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(200, 40)), ColorUtils.setAlpha(fonColor, Math.max(235, 40)), backBtnHovered ? 1f : 0f));
        Fonts.icon[17].drawCenteredString(matrixStack, "1", backBtnX + iconButtonSize / 2f, backBtnY + iconButtonSize / 2f - 1.5f, ColorUtils.interpolateColor(ColorUtils.setAlpha(iconColor, Math.max(150, 40)), iconColor, backBtnHovered ? 1f : 0f));

        float accountCardHeight = 24f;
        float accountCardSpacing = 2f;
        float columnSpacing = 2f;
        float accountCardWidth = (listPanelWidth - 16 - columnSpacing) / 2f;
        float accountStartX = listPanelX + 8f;
        float accountStartY = listPanelY + 28f;

        StencilUtils.initStencilToWrite();
        RenderUtils.Render2D.drawRoundedRect(listPanelX, listPanelY + 25, listPanelWidth, listPanelHeight - 30, 4f, fonColor);
        StencilUtils.readStencilBuffer(1);

        ArrayList<Account> filteredAccounts = getFilteredAccounts();
        for (int i = 0; i < filteredAccounts.size(); i++) {
            Account account = filteredAccounts.get(i);
            int column = i % 2;
            int row = i / 2;
            float acX = accountStartX + column * (accountCardWidth + columnSpacing);
            float acY = accountStartY + (row * (accountCardHeight + accountCardSpacing)) + scrollAn;

            boolean isActive = account.accountName.equalsIgnoreCase(IMinecraft.mc.session.getUsername());
            boolean accountHovered = RenderUtils.isInRegion(mouseX, mouseY, (int) acX, (int) acY, (int) accountCardWidth, (int) accountCardHeight);

            int fonAlphaBase = isActive ? Math.min(255, (int)(200 * 1.6f)) : 200;
            RenderUtils.Render2D.drawRoundedRect(acX, acY, accountCardWidth, accountCardHeight, 4f, ColorUtils.interpolateColor(ColorUtils.setAlpha(fonColor, Math.max(fonAlphaBase, 40)), ColorUtils.setAlpha(fonColor, Math.max(isActive ? 255 : 235, 40)), accountHovered ? 1f : 0f));

            RenderUtils.Render2D.drawRoundFace(acX + 3f, acY + 3f, 18, 18, 3f, (float) this.targetInfoAnimation.getOutput(), account.skin);

            int nameAlphaBase = isActive ? Math.min(255, (int)(210 * 1.6f)) : 210;
            Fonts.newcode[13].drawString(matrixStack, account.accountName, acX + 24f, acY + 7f, ColorUtils.interpolateColor(ColorUtils.setAlpha(textColor, Math.max(nameAlphaBase, 40)), ColorUtils.setAlpha(textColor, 255), accountHovered ? 1f : 0f));

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            String formattedDate = dateFormat.format(new Date(account.dateAdded));
            int dateAlphaBase = isActive ? Math.min(255, (int)(180 * 1.6f)) : 180;
            Fonts.newcode[12].drawString(matrixStack, formattedDate, acX + 24f, acY + 14f, ColorUtils.interpolateColor(ColorUtils.setAlpha(textColor, Math.max(dateAlphaBase, 40)), ColorUtils.setAlpha(textColor, isActive ? 255 : 235), accountHovered ? 1f : 0f));

            float pinIconX = acX + 91f;
            float pinIconY = acY + 9f;
            boolean pinIconHovered = RenderUtils.isInRegion(mouseX, mouseY, (int) (pinIconX - 6f), (int) (pinIconY - 6f), 12, 12);
            int pinIconColor = account.pinned ? ColorUtils.ensureMinAlpha(getColorByName("goldColor"), 40) : iconColor;
            int pinAlphaBase = isActive ? Math.min(255, (int)(150 * 1.6f)) : 150;
            Fonts.icon2[22].drawCenteredString(matrixStack, "p", pinIconX, pinIconY, ColorUtils.interpolateColor(ColorUtils.setAlpha(pinIconColor, Math.max(pinAlphaBase, 40)), ColorUtils.setAlpha(pinIconColor, 255), pinIconHovered ? 1f : 0f));
        }

        StencilUtils.uninitStencilBuffer();

        if (filteredAccounts.isEmpty()) {
            String message = searchQuery.isEmpty() ? "No accounts" : "No results";
            Fonts.newcode[14].drawCenteredString(matrixStack, message, listPanelX + listPanelWidth / 2f, listPanelY + listPanelHeight / 2f, textColor2);
        }

        float totalRows = (float) Math.ceil(filteredAccounts.size() / 2.0);
        float itemHeight = accountCardHeight + accountCardSpacing;
        float stencilHeight = listPanelHeight - 30f;
        float visibleHeight = stencilHeight - (accountStartY - (listPanelY + 25f));
        float totalHeight = totalRows * itemHeight;
        float maxScroll = totalHeight > visibleHeight ? -(totalHeight - visibleHeight) : 0;

        scroll = MathHelper.clamp(scroll, maxScroll, 0);
        scrollAn = MathHelper.clamp(scrollAn, maxScroll, 0);

        float scrollbarX = listPanelX + listPanelWidth - 6;
        float scrollbarY = listPanelY + 28f;
        float scrollbarWidth = 1f;
        float scrollbarHeight = listPanelHeight - 28f;
        float thumbHeight = totalHeight > visibleHeight ? Math.max(20, scrollbarHeight * (visibleHeight / totalHeight)) : scrollbarHeight;
        float scrollRatio = maxScroll != 0 ? scrollAn / maxScroll : 0;
        float thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollRatio;

        if (totalHeight > visibleHeight) {
            int scrollColor = ColorUtils.ensureMinAlpha(getColorByName("scrollColor"), 40);
            if (scrollColor == 0) scrollColor = iconColor;

            RenderUtils.Render2D.drawRoundedRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 4f, ColorUtils.setAlpha(scrollColor, 45));
            RenderUtils.Render2D.drawRoundedRect(scrollbarX, thumbY, scrollbarWidth, thumbHeight, 4f, ColorUtils.setAlpha(scrollColor, isDraggingScrollbar ? 60 : 50));
        }

        IMinecraft.mc.gameRenderer.setupOverlayRendering();
    }


}