package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;

@Getter
@AllArgsConstructor
public class EventPlace extends Event {
    private final Block block;
    private final BlockPos pos;
}
