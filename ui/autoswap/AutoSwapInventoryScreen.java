package nuclear.ui.autoswap;

import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class AutoSwapInventoryScreen extends InventoryScreen {
    private final AutoSwapUI parentScreen;
    private final int targetSegment;
    private boolean itemSelected = false;
    
    public AutoSwapInventoryScreen(PlayerEntity player, AutoSwapUI parent, int segment) {
        super(player);
        this.parentScreen = parent;
        this.targetSegment = segment;
    }
    
    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, net.minecraft.inventory.container.ClickType type) {
        if (mouseButton == 0 && slotIn != null && !slotIn.getStack().isEmpty()) {
            ItemStack clickedStack = slotIn.getStack();
            parentScreen.setSegmentItem(targetSegment, clickedStack.copy());
            itemSelected = true;
            return;
        }
        
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (itemSelected) {
            itemSelected = false;
            this.minecraft.displayGuiScreen(parentScreen);
        }
    }
}

