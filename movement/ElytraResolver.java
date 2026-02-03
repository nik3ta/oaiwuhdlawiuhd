package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(
        name = "ElytraResolver",
        type = TypeList.Movement,
        desc = "Меняет ваше движение так что бы по вам не могли ударить на элитрах"
)
public class ElytraResolver extends Module {

    public final MultiBoxSetting vector = new MultiBoxSetting("Векторы лива",
            new BooleanSetting("Вверх", true),
            new BooleanSetting("Вниз", false),
            new BooleanSetting("Восток", true),
            new BooleanSetting("Запад", true),
            new BooleanSetting("Юг", true),
            new BooleanSetting("Север", true));
    public final SliderSetting elytradistance = new SliderSetting("Дистанция", 4.5F, 3.0F, 8F, 0.5F);

    public final BooleanSetting skipvector = new BooleanSetting("Исключать столкновение", true);
    public final BooleanSetting autoF = new BooleanSetting("Авто фейерверк", true);
    public final BooleanSetting freezeDummy = new BooleanSetting("Замораживать игрока", true);

    public ElytraResolver() {
        this.addSettings(vector, elytradistance, skipvector, autoF, freezeDummy);
    }

    public boolean onEvent(Event event) {
        return false;
    }
}
