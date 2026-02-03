package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;

@Getter
@AllArgsConstructor
public class PotionExplosionEvent extends Event {
    private final int rgbColor;
    private final BlockPos position;
}


