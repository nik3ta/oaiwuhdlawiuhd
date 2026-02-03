package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.Entity;
import nuclear.control.events.Event;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventCrystalEntity extends Event {

    private Entity entity;
}