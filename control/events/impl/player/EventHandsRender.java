package nuclear.control.events.impl.player;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.renderer.ActiveRenderInfo;
import nuclear.control.events.Event;

@Data
@AllArgsConstructor
public class EventHandsRender extends Event {
    private ActiveRenderInfo activeRenderInfo;
    private MatrixStack stack;
    private float part;

    public static class Pre extends EventHandsRender {

        public Pre(ActiveRenderInfo activeRenderInfo, MatrixStack stack, float part) {
            super(activeRenderInfo, stack, part);
        }
    }

    public static class Post extends EventHandsRender {
        public Post(ActiveRenderInfo activeRenderInfo, MatrixStack stack, float part) {
            super(activeRenderInfo, stack, part);
        }
    }
}

