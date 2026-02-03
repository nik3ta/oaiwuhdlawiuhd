package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;

@Data
@AllArgsConstructor
public class EventBlockDamage extends Event {
    private BlockState blockState;
    private BlockPos pos;
    private State state;

    public enum State {
        START,
        STOP
    }
}
