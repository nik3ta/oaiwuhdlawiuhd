package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;

@AllArgsConstructor
public class EventCollision extends Event {
    @Getter
    private final BlockPos blockPos;
}

