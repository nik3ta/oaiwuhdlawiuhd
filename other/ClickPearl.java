package nuclear.module.impl.other;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.world.InventoryUtils;

@Annotation(name = "ClickPearl", type = TypeList.Other, desc = "Кидает пёрл при нажатие на клавишу")
public class ClickPearl extends Module {
    public BindSetting clickKey = new BindSetting("Кнопка", -98);
    public BooleanSetting legit = new BooleanSetting("Легитный", false);
    InventoryUtils.Hands handUtil = new InventoryUtils.Hands();
    long delay;

    public ClickPearl() {
        addSettings(clickKey, legit);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventKey e) {
            if (e.key == clickKey.getKey()) {
                handleMouseTickEvent();
            }
        }
        return false;
    }

    private void handleMouseTickEvent() {
        if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL) && InventoryUtils.getItemSlot(Items.ENDER_PEARL) != -1) {
            if (legit.get()) {
                InventoryUtils.holySwapClick(Items.ENDER_PEARL, false);
            } else {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);

                    int hbSlot = findItem(Items.ENDER_PEARL, true);
                    int invSlot = findItem(Items.ENDER_PEARL, false);

                    if (Items.ENDER_PEARL != null) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                        }
                    }
                    useItem(Hand.MAIN_HAND);

                } else {
                    InventoryUtils.inventorySwapClick(Items.ENDER_PEARL, false);
                    useItem(Hand.MAIN_HAND);

                }
            }

        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }


    private void useItem(Hand hand) {
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        mc.player.swingArm(hand);
    }
}

