package nuclear.module.settings.imp;

import lombok.Getter;
import lombok.Setter;
import nuclear.module.settings.Setting;

import java.util.function.Supplier;

@Getter
@Setter
public class InfoSetting extends Setting {

    private Runnable run;
    private String toggleText;

    public InfoSetting(String name, Runnable run) {
        super(name);
        this.run = run;
    }

    public InfoSetting(String name, String toggleText, Runnable run) {
        super(name);
        this.run = run;
        this.toggleText = toggleText;
    }

    public InfoSetting setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    @Override
    public SettingType getType() {
        return SettingType.BUTTON_SETTING;
    }
}