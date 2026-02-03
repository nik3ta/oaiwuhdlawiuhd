package nuclear.module.impl.render;

import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "Brightness", type = TypeList.Render)
public class Brightness extends Module {
    public final ModeSetting mode = new ModeSetting("Режим", "Фиксирующий", "Фиксирующий", "Гамма", "Эффект");
    public final SliderSetting strengthSlider = new SliderSetting("Сила яркости", 1.0f, 0.1f, 2.0f, 0.01f);

    public Brightness() {
        addSettings(mode, strengthSlider);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            updateBrightness();
        }
        return false;
    }

    private void updateBrightness() {
        if (mc.player != null && !mode.is("Эффект") && mc.player.isPotionActive(Effects.NIGHT_VISION)) {
            mc.player.removePotionEffect(Effects.NIGHT_VISION);
        }

        if (mode.is("Эффект")) {
            if (mc.player != null) {
                mc.player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 1337, 1));
            }
        } else if (mode.is("Гамма")) {
            mc.gameSettings.gamma = 1000f;
        } else if (mode.is("Фиксирующий")) {
            mc.gameSettings.gamma = strengthSlider.getValue().floatValue();
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        updateBrightness();
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.removePotionEffect(Effects.NIGHT_VISION);
        }
        if (mc.gameSettings != null) {
            mc.gameSettings.gamma = 1.0f;
        }
        super.onDisable();
    }
}