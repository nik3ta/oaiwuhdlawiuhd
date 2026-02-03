package nuclear.control.events.impl.player;

import net.minecraft.entity.Entity;
import nuclear.control.events.Event;

public class EventDestroyTotem extends Event {
    public Entity entity;

    public EventDestroyTotem(Entity entity) {
        this.entity = entity;
    }
}

