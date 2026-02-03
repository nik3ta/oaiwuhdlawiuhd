package nuclear.module.impl.render;

import net.minecraft.client.Minecraft;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.TextSetting;

@Annotation(name = "NameProtect", type = TypeList.Player, desc = "Изменяет ваш реальный ник")
public class NameProtect extends Module {

    public BooleanSetting friends = new BooleanSetting("Друзья", false);
    public static TextSetting name = new TextSetting("Никнейм", "Protect");

    public NameProtect() {
        addSettings(name, friends);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }

    public String patch(String text) {
        String out = text;
        if (this.state) {
            out = text.replaceAll(Minecraft.getInstance().session.getUsername(), name.get());
        }
        return out;
    }

    public static void setNameProtect(String newName) {
        name.text = newName;
    }

    public static String getNameProtect() {
        return name.get();
    }
}
