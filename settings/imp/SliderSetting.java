package nuclear.module.settings.imp;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import nuclear.module.settings.Setting;

import java.util.function.Supplier;

public class SliderSetting extends Setting {
    private float value;
    @Getter
    private final float min;
    @Getter
    private final float max;
    @Getter
    private final float increment;
    @Getter
    private final float defaultValue;

    public SliderSetting(String name, float value, float min, float max, float increment) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.defaultValue = value;
    }

    public SliderSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public Number getValue() {
        return MathHelper.clamp(value, getMin(), getMax());
    }

    public void setValue(float value) {
        this.value = MathHelper.clamp(value, getMin(), getMax());
    }

    public void resetToDefault() {
        setValue(defaultValue);
    }

    @Override
    public SettingType getType() {
        return SettingType.SLIDER_SETTING;
    }
}