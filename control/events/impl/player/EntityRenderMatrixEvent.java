package nuclear.control.events.impl.player;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.Entity;
import nuclear.control.events.Event;

@Data
@AllArgsConstructor
public class EntityRenderMatrixEvent extends Event {
    private final MatrixStack matrix;
    private final Entity entity;
}

