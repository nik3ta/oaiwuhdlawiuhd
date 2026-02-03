package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventHandsRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ColorSetting;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.KawaseBlur;
import nuclear.utils.render.shader.ShaderUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

@Annotation(name = "Hands", type = TypeList.Render, desc = "Создаёт стрелочки к игрокам в мире и зелёные стрелочки к игрокам в пати")
public class Hands extends Module {

    private ColorSetting colorSetting = new ColorSetting("Цвет", ColorUtils.hex("#8A98FFFF"));

    public Hands() {
        addSettings(colorSetting);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventHandsRender.Pre pre) {
            onHandsRender(pre);
        } else if (event instanceof EventHandsRender.Post post) {
            onHandsRender(post);
        }
        return false;
    }

    private void onHandsRender(EventHandsRender.Pre e) {
        ShaderUtil.hands.attach();
        GL13.glActiveTexture(GL20.GL_TEXTURE5);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, KawaseBlur.blur.BLURRED.framebufferTexture);
        GL13.glActiveTexture(GL20.GL_TEXTURE0);

        ShaderUtil.hands.setUniform("originalTexture", 0);
        ShaderUtil.hands.setUniform("blurredTexture", 5);


        float[] rgba = ColorUtils.getColor2(colorSetting.get());
        ShaderUtil.hands.setUniformf("multiplier", rgba[0], rgba[1], rgba[2], rgba[3]);
        ShaderUtil.hands.setUniformf("viewOffset", mw.getFramebufferWidth() / 8F, mw.getFramebufferHeight());
        ShaderUtil.hands.setUniformf("resolution", mw.getFramebufferWidth() * 2, mw.getFramebufferHeight() * 2);
    }

    private void onHandsRender(EventHandsRender.Post e) {
        ShaderUtil.hands.detach();
    }
}

