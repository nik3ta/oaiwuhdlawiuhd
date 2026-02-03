package nuclear.module.impl.combat;

import net.minecraft.entity.LivingEntity;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventEntityHitBox;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "NoEntityTrace",
        type = TypeList.Combat, desc = "Убирает хитбокс сквозь блоков"
)
public class NoEntityTrace extends Module {

    public final BooleanSetting invisible = new BooleanSetting("Невидимки", true);

    public NoEntityTrace() {
        addSettings();
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

        LivingEntity entity = (LivingEntity) event.getEntity();

        HitBox hitBoxModule = Manager.FUNCTION_MANAGER.hitBoxFunction;
        
        if (entity.isInvisible()) {
            if (hitBoxModule != null && hitBoxModule.isState()) {
                if (!hitBoxModule.showHitBox.get()) {
                    return;
                }
            } else {
                if (!invisible.get()) {
                    return;
                }
            }
        }
        
        event.setSize(-0.75f);
    }
}
