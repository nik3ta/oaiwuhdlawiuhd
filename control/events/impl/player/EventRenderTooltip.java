package nuclear.control.events.impl.player;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import nuclear.control.events.Event;

public class EventRenderTooltip extends Event {
    public final MatrixStack matrixStack;
    public final ItemStack stack;
    public final int mouseX;
    public final int mouseY;

    public EventRenderTooltip(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.stack = stack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}