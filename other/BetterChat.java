package nuclear.module.impl.other;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "BetterChat", type = TypeList.Other)
public class BetterChat extends Module {
    public final BooleanSetting chatHistory = new BooleanSetting("История чата", true);
    public final BooleanSetting antiSpam = new BooleanSetting("АнтиСпам в чате", true);

    public BetterChat() {
        addSettings(chatHistory, antiSpam);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}

