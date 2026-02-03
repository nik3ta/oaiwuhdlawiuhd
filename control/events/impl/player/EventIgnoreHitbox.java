package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;

@Data
@AllArgsConstructor
public class EventIgnoreHitbox extends Event {
    private BlockPos pos;
}

