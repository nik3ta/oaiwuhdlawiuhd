package nuclear.module.settings.imp;

import nuclear.module.settings.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class BooleanSetting extends Setting {
    private boolean value;
    private final boolean defaultValue;
    private String desc;
    public float anim;

    public BooleanSetting(String name, boolean value) {
        super(name);
        this.value = value;
        this.defaultValue = value;
    }

    public BooleanSetting(String name, String desc, boolean value) {
        super(name);
        this.value = value;
        this.defaultValue = value;
        this.desc = desc;
    }

    public BooleanSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    public void set(boolean value) {
        this.value = value;
    }


    @Override
    public SettingType getType() {
        return SettingType.BOOLEAN_OPTION;
    }

    public List<BooleanSetting> getValues() {
        return Arrays.asList(this);
    }

    public boolean getValue() {
        return this.value;
    }

    public boolean getDefaultValue() {
        return this.defaultValue;
    }

    public void resetToDefault() {
        this.value = this.defaultValue;
    }
}
