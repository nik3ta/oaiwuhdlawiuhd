package nuclear.module.settings.imp;

import nuclear.module.settings.Setting;

import java.awt.*;
import java.util.function.Supplier;

public class ColorSetting extends Setting {
    public int color = 0;
    private final int defaultValue;

    public ColorSetting(String name, int color) {
        super(name);
        this.color = color;
        this.defaultValue = color;
    }

    public ColorSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public int get() {
        return color;
    }

    public Color getColor() {
        return new Color(color);
    }

    public void setValue(int color) {
        this.color = color;
    }

    public int getDefaultValue() {
        return this.defaultValue;
    }

    public void resetToDefault() {
        this.color = this.defaultValue;
    }

    @Override
    public SettingType getType() {
        return SettingType.COLOR_SETTING;
    }
}
