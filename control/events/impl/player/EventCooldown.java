package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;
import nuclear.control.events.Event;

@Setter
@Getter
@AllArgsConstructor
public class EventCooldown extends Event {
    private final Item item;
    private int ticks;
}

