package nuclear.module.impl.other;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "ClientSounds", type = TypeList.Other)
public class ClientSounds extends Module {

    public final SliderSetting volume = new SliderSetting("Громкость", 65f, 5f, 100f, 5f);
    public final ModeSetting soundMode = new ModeSetting("Режим", "Nursultan", "Nursultan", "Pop");

    public ClientSounds() {
        super();
        addSettings(soundMode, volume);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}