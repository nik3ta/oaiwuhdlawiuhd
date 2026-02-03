package nuclear.module.impl.combat;

import net.minecraft.entity.LivingEntity;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventEntityHitBox;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "HitBox",
        type = TypeList.Combat, desc = "Увеличивает хитбокс энтити"
)
public class HitBox extends Module {

    public final SliderSetting size = new SliderSetting("Размер", 0.2f, 0F, 1, 0.05F);
    public final BooleanSetting showHitBox = new BooleanSetting("Невидимые", false);

    public HitBox() {
        addSettings(size, showHitBox);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventEntityHitBox e) {
            handleEvent(e);
        }
        return false;
    }

    private void handleEvent(EventEntityHitBox event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        event.setSize(size.getValue().floatValue());
    }

    public boolean shouldShowHitBox() {
        return isState() && !showHitBox.get();
    }
}
