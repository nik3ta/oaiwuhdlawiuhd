package nuclear.module.settings.imp;

import lombok.Getter;
import lombok.Setter;
import nuclear.module.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class BindSetting extends Setting {

    private int key;
    private final int defaultKey;

    public BindSetting(String name, int defaultKey) {
        super(name);
        key = defaultKey;
        this.defaultKey = defaultKey;
    }
    public BindSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public int getDefaultKey() {
        return this.defaultKey;
    }

    public void resetToDefault() {
        this.key = this.defaultKey;
    }

    @Override
    public SettingType getType() {
        return SettingType.BIND_SETTING;
    }
}
