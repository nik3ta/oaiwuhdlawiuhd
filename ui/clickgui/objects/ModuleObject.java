package nuclear.ui.clickgui.objects;

import com.mojang.blaze3d.matrix.MatrixStack;
import nuclear.module.api.Module;
import nuclear.module.settings.Setting;
import nuclear.module.settings.imp.*;
import nuclear.ui.clickgui.objects.sets.*;
import nuclear.utils.ClientUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.render.BloomHelper;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.render.animation.AnimationMath;
import org.joml.Vector4i;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;
import static nuclear.ui.clickgui.Window.light;

public class ModuleObject extends Object {

    public Module module;
    public ArrayList<Object> object = new ArrayList<>();
    public float animation,animation_height;

    boolean binding;

    public ModuleObject(Module module) {
        this.module = module;
        this.toggleAnimation = module.state ? 1.0f : 0.0f;
        this.expand_anim = module.expanded ? 1.0f : 0.0f;
        for (Setting setting : module.settingList) {
            if (setting instanceof BooleanSetting option) {
                object.add(new BooleanObject(this, option));
            }
            if (setting instanceof ColorSetting option) {
                object.add(new ColorObject(this, option));
            }
            if (setting instanceof SliderSetting option) {
                object.add(new SliderObject(this, option));
            }
            if (setting instanceof ModeSetting option) {
                object.add(new ModeObject(this, option));
            }
            if (setting instanceof MultiBoxSetting option) {
                object.add(new MultiObject(this, option));
            }
            if (setting instanceof BindSetting option) {
                object.add(new BindObject(this, option));
            }
            if (setting instanceof InfoSetting option) {
                object.add(new InfoObject(this, option));
            }
            if (setting instanceof ButtonSetting option) {
                object.add(new ButtonObject(this, option));
            }
        }
    }

    float lastHeight;
    public boolean isBinding = false;


    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for ( Object object1 : object) {
            object1.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (isHovered(mouseX, mouseY, x + 7.5f, y, width - 15, 15)) {
            if (mouseButton == 0)
                module.toggle();
            if (mouseButton == 2)
                isBinding = !isBinding;
        }
    }

    @Override
    public void drawComponent(MatrixStack matrixStack, int mouseX, int mouseY) {

    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for ( Object object1 : object) {
            object1.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
        if (isBinding) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE ||
                    keyCode == GLFW.GLFW_KEY_ESCAPE) {
                module.bind = 0;
            } else {
                module.bind = keyCode;
            }
            isBinding = false;
        }

        for (Object obj : object) {
            if (obj instanceof BindObject m) {
                if (m.isBinding) {
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE ||
                            keyCode == GLFW.GLFW_KEY_ESCAPE) {
                        m.set.setKey(0);
                        m.isBinding = false;
                        continue;
                    }
                    m.set.setKey(keyCode);
                    m.isBinding = false;
                }
            }
            obj.keyTyped(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for( Object obj:  object){
            obj.charTyped(codePoint, modifiers);
        }
    }

    float hover_anim;

    public float expand_anim;
    public float toggleAnimation = 0f;

    @Override
    public void draw(MatrixStack stack, int mouseX, int mouseY) {
        super.draw(stack, mouseX, mouseY);
        hover_anim = AnimationMath.fast(hover_anim, RenderUtils.isInRegion(mouseX,mouseY,x, y, width, height) ? 1 : 0, 10);
        RenderUtils.Render2D.drawRoundOutline(x, y, width, height, 1, 0f, ColorUtils.rgba(25, 26, 33, 100), new Vector4i(
                ColorUtils.gradient(10,0, ColorUtils.rgba(155, 155, 155, 255*hover_anim), ColorUtils.rgba(26, 26, 26, 0*hover_anim)),
                ColorUtils.gradient(10,90, ColorUtils.rgba(155, 155, 155, 255*hover_anim), ColorUtils.rgba(25, 26, 33, 0*hover_anim)),
                ColorUtils.gradient(10,180, ColorUtils.rgba(155, 155, 155, 255*hover_anim), ColorUtils.rgba(25, 26, 33, 0*hover_anim)),
                ColorUtils.gradient(10,270, ColorUtils.rgba(155, 155, 155, 255*hover_anim), ColorUtils.rgba(25, 26, 33, 0*hover_anim))
        ));
        animation = AnimationMath.fast(animation, module.state ? 1 : 0, 5);
        animation_height = AnimationMath.fast(animation_height, height, 5);
        toggleAnimation = AnimationMath.ultraSmooth(toggleAnimation, module.state ? 1.0f : 0.0f, 15f);

        if(module.state){
            if(module.settingList.isEmpty()){
            }
            else{
            }
        }

        String text = module.name;
        if(binding)text += "...";
        if(module.bind != 0)text += KeyEvent.getKeyText(module.bind);

        String finalText = text;
        BloomHelper.registerRenderCall(() -> {
            if(module.state){
                Fonts.msSemiBold[15].drawString(stack, ClientUtils.gradient(finalText, getColorByName("primaryColor"), getColorByName("primaryColor")), x + 10, y + 10, RenderUtils.reAlphaInt(-1, (int) (255 * animation)));
            }else{
                Fonts.msSemiBold[15].drawString(stack, finalText, x + 10, y + 10, RenderUtils.reAlphaInt(-1, (int) (255 * animation)));
            }
        });
        if(module.state){
            Fonts.msSemiBold[15].drawString(stack, ClientUtils.gradient(text, getColorByName("primaryColor"), getColorByName("primaryColor")), x + 10, y + 10, ColorUtils.interpolateColor(light,-1, animation));
        }else{
            Fonts.msSemiBold[15].drawString(stack, text, x + 10, y + 10, ColorUtils.interpolateColor(light,-1, animation));
        }
        if (!module.settingList.isEmpty()) {
            RenderUtils.Render2D.drawRect(x + 10, y + 22, width - 20, 0.5f, ColorUtils.rgba(32, 35, 57,255));
        }

        drawObjects(stack, mouseX, mouseY);
    }

    public void drawObjects(MatrixStack stack, int mouseX, int mouseY) {
        float offset = -4;
        for ( Object object : object) {
            if (object.setting.visible()) {
                object.x = x;
                object.y = y + 15 + offset;
                object.width = 160;
                object.height = 8;
                object.draw(stack, mouseX, mouseY);
                offset += object.height;
            }
        }
    }

}
