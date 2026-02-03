package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ButtonSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "ViewModel", type = TypeList.Render, desc = "Меняет положение руки игрока")
public class ViewMode extends Module {
    public final SliderSetting right_x = new SliderSetting("Право по X", 0.0F, -2, 2, 0.05F);
    public final SliderSetting right_y = new SliderSetting("Право по Y", 0.0F, -2, 2, 0.05F);
    public final SliderSetting right_z = new SliderSetting("Право по Z", 0.0F, -2, 2, 0.05F);
    public final SliderSetting left_x = new SliderSetting("Лево по X", 0.0F, -2, 2, 0.05F);
    public final SliderSetting left_y = new SliderSetting("Лево по Y", 0.0F, -2, 2, 0.05F);
    public final SliderSetting left_z = new SliderSetting("Лево по Z", 0.0F, -2, 2, 0.05F);
    public static BooleanSetting onlyaura = new BooleanSetting("Только с Aura", false);

    public ButtonSetting reset = new ButtonSetting("Сбросить", () -> {
        right_x.setValue(0.0F);
        right_y.setValue(0.0F);
        right_z.setValue(0.0F);
        left_x.setValue(0.0F);
        left_y.setValue(0.0F);
        left_z.setValue(0.0F);
    });

    public ViewMode() {
        addSettings(right_x, right_y, right_z, left_x, left_y, left_z, onlyaura, reset);
    }
    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
