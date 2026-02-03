package nuclear.module.settings.imp;

import nuclear.module.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiBoxSetting extends Setting {
    public List<BooleanSetting> options;
    public int index;
    public float animation;

    public MultiBoxSetting(String name, BooleanSetting... options) {
        super(name);
        this.options = List.of(options);
    }

    public boolean get(String name) {
        return this.options.stream()
                .filter((option) -> option.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(BooleanSetting::get)
                .orElse(false);
    }

    public void set(String name, boolean value) {
        this.options.stream()
                .filter((option) -> option.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(option -> option.set(value));
    }

    public List<BooleanSetting> getToggled() {
        return this.options.stream().filter(BooleanSetting::get).collect(Collectors.toList());
    }

    public String get() {
        List<String> includedOptions = new ArrayList<>();
        for (BooleanSetting option : options) {
            if (option.get()) {
                includedOptions.add(option.getName());
            }
        }
        return String.join(", ", includedOptions);
    }

    public void set(int index, boolean value) {
        if (index >= 0 && index < this.options.size()) {
            this.options.get(index).set(value);
        }
    }

    public boolean get(int index) {
        if (index >= 0 && index < this.options.size()) {
            return this.options.get(index).get();
        }
        return false;
    }

    public MultiBoxSetting setVisible(Supplier<Boolean> bool) {
        this.visible = bool;
        return this;
    }

    @Override
    public SettingType getType() {
        return SettingType.MULTI_BOX_SETTING;
    }

    public List<BooleanSetting> getValues() {
        return this.options;
    }
}
