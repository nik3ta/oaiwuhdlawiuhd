package nuclear.module.api;

import com.google.gson.JsonObject;
import lombok.Getter;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.impl.other.ClientSounds;
import nuclear.module.settings.Configurable;
import nuclear.module.settings.Setting;
import nuclear.module.settings.imp.*;
import nuclear.utils.IMinecraft;
import nuclear.utils.SoundUtils;
import nuclear.utils.anim.Animation;
import nuclear.utils.anim.animations.DecelerateAnimation;

public abstract class Module extends Configurable implements IMinecraft {
    private final Annotation info = this.getClass().getAnnotation(Annotation.class);
    public TypeList category;
    public String name, desc;
    public int bind;

    public Animation animation = new DecelerateAnimation(250, 1.0);

    @Getter
    public boolean state;

    public Module() {
        initializeProperties();
    }

    public Module(String name, TypeList category) {
        this.name = name;
        this.category = category;
        state = false;
        bind = 0;
        init();
    }

    public void init() {
    }

    private void initializeProperties() {
        name = info.name();
        category = info.type();
        state = false;
        bind = info.key();
        desc = info.desc();
    }

    public String getDescription() {
        Annotation annotation = getClass().getAnnotation(Annotation.class);
        if (annotation != null) {
            return annotation.desc();
        }
        return "Нету описание модуля";
    }

    public void setStateNotUsing(boolean enabled) {
        state = enabled;
    }

    public void setState(boolean enabled) {
        if (mc.player == null || mc.world == null) return;

        if (!enabled) {
            onDisable();
        } else {
            onEnable();
        }

        state = enabled;
    }

    public void toggle() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        this.state = !state;

        if (!state)
            onDisable();
        else
            onEnable();

        Manager.NOTIFICATION_MANAGER.add(name + " " + (state ? "enabled" : "disabled"), "Function Debug", 3, this.category);

        ClientSounds clientSounds = Manager.FUNCTION_MANAGER.clientSounds;

        if (clientSounds != null && clientSounds.state) {
            String mode = clientSounds.soundMode.get();
            float vol = clientSounds.volume.getValue().floatValue();
            String soundFile;

            if (mode.equalsIgnoreCase("Pop")) {
                soundFile = state ? "popenable.wav" : "popdisable.wav";
            } else {
                soundFile = state ? "Function_ON.wav" : "Function_OFF.wav";
            }

            SoundUtils.playSound(soundFile, vol, false);
        }
    }

    public boolean isState() {
        return this.state;
    }

    protected void onDisable() {
    }

    protected void onEnable() {
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();
        object.addProperty("bind", bind);
        object.addProperty("state", state);

        for (Setting setting : getSettingList()) {
            String name = setting.getName();
            switch (setting.getType()) {
                case BOOLEAN_OPTION -> object.addProperty(name, ((BooleanSetting) setting).get());
                case SLIDER_SETTING -> object.addProperty(name, ((SliderSetting) setting).getValue().floatValue());
                case MODE_SETTING -> object.addProperty(name, ((ModeSetting) setting).getIndex());
                case COLOR_SETTING -> object.addProperty(name, ((ColorSetting) setting).get());
                case MULTI_BOX_SETTING -> {
                    ((MultiBoxSetting) setting).options.forEach(option -> object.addProperty(option.getName(), option.get()));
                }
                case BIND_SETTING -> object.addProperty(name, ((BindSetting) setting).getKey());
                case TEXT_SETTING -> object.addProperty(name, ((TextSetting) setting).text);
            }
        }
        return object;
    }


    public void load(JsonObject object, boolean start) {
        bind = info != null ? info.key() : 0;
        if (!start) {
            setState(false);
        } else {
            setStateNotUsing(false);
        }

        if (object != null) {
            if (object.has("bind")) bind = object.get("bind").getAsInt();
            if (object.has("state")) {
                if (start) setStateNotUsing(object.get("state").getAsBoolean());
                else setState(object.get("state").getAsBoolean());
            }

            for (Setting setting : getSettingList()) {
                String name = setting.getName();

                switch (setting.getType()) {
                    case BOOLEAN_OPTION -> {
                        if (object.has(name)) {
                            ((BooleanSetting) setting).set(object.get(name).getAsBoolean());
                        } else {
                            ((BooleanSetting) setting).resetToDefault();
                        }
                    }
                    case SLIDER_SETTING -> {
                        if (object.has(name)) {
                            ((SliderSetting) setting).setValue((float) object.get(name).getAsDouble());
                        } else {
                            ((SliderSetting) setting).resetToDefault();
                        }
                    }
                    case MODE_SETTING -> {
                        if (object.has(name)) {
                            ((ModeSetting) setting).setIndex(object.get(name).getAsInt());
                        } else {
                            ((ModeSetting) setting).resetToDefault();
                        }
                    }
                    case BIND_SETTING -> {
                        if (object.has(name)) {
                            ((BindSetting) setting).setKey(object.get(name).getAsInt());
                        } else {
                            ((BindSetting) setting).resetToDefault();
                        }
                    }
                    case COLOR_SETTING -> {
                        if (object.has(name)) {
                            ((ColorSetting) setting).setValue(object.get(name).getAsInt());
                        } else {
                            ((ColorSetting) setting).resetToDefault();
                        }
                    }
                    case MULTI_BOX_SETTING -> {
                        ((MultiBoxSetting) setting).options.forEach(option -> {
                            if (object.has(option.getName())) {
                                option.set(object.get(option.getName()).getAsBoolean());
                            } else {
                                option.resetToDefault();
                            }
                        });
                    }
                    case TEXT_SETTING -> {
                        if (object.has(name)) {
                            ((TextSetting) setting).text = object.get(name).getAsString();
                        } else {
                            ((TextSetting) setting).resetToDefault();
                        }
                    }
                }
            }
        } else {
            for (Setting setting : getSettingList()) {
                switch (setting.getType()) {
                    case BOOLEAN_OPTION -> ((BooleanSetting) setting).resetToDefault();
                    case SLIDER_SETTING -> ((SliderSetting) setting).resetToDefault();
                    case MODE_SETTING -> ((ModeSetting) setting).resetToDefault();
                    case BIND_SETTING -> ((BindSetting) setting).resetToDefault();
                    case COLOR_SETTING -> ((ColorSetting) setting).resetToDefault();
                    case MULTI_BOX_SETTING -> {
                        ((MultiBoxSetting) setting).options.forEach(BooleanSetting::resetToDefault);
                    }
                    case TEXT_SETTING -> ((TextSetting) setting).resetToDefault();
                }
            }
        }
    }

    public abstract boolean onEvent(Event event);
}