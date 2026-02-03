package nuclear.module.settings.imp;

import nuclear.module.settings.Setting;

import java.util.function.Supplier;

public class TextSetting extends Setting {
    public String text;
    private final String defaultValue;

    public TextSetting(String name, String text) {
        super(name);
       this.text = text;
       this.defaultValue = text;
    }

    public String get() {
        return text;
    }

    public TextSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public void resetToDefault() {
        this.text = this.defaultValue;
    }

    @Override
    public SettingType getType() {
        return SettingType.TEXT_SETTING;
    }
}
