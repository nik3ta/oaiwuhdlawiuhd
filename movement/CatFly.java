package nuclear.module.impl.movement;

import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.world.InventoryUtils;

@Annotation(
        name = "CatFly",
        type = TypeList.Movement, desc = "Полёт использующий элитры при этом летя на нагруднике"
)
public class CatFly extends Module {
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil timerUtil1 = new TimerUtil();
    private final TimerUtil timerUtil2 = new TimerUtil();
    private final SliderSetting timerStartFireWork = new SliderSetting("Задержка фейер", 4F, 1F, 15F, 1F);
    int oldItem = -1;

    public CatFly() {
        this.addSettings(timerStartFireWork);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
                return false;
            }
            int timeSwap = 550;
            for (int i = 0; i < 9; ++i) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA && !mc.player.isOnGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isElytraFlying()) {
                    if (this.timerUtil1.hasTimeElapsed((long) timeSwap)) {
                        this.timerUtil2.reset();
                        mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                        mc.player.startFallFlying();
                        mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                        mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                        this.oldItem = i;
                        this.timerUtil1.reset();
                    }

                    if (this.timerUtil.hasTimeElapsed(timerStartFireWork.getValue().intValue() * 40) && mc.player.isElytraFlying()) {
                        if (mc.player.isHandActive() && !mc.player.isBlocking()) return false;
                        this.useFirework();
                        this.timerUtil.reset();
                    }
                }
            }
        }
        return false;
    }

    private void useFirework() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            ClientUtils.sendMessage(TextFormatting.RED + "У тебя нету фейерверков!");
        } else {
            InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
        }
    }

    public void onDisable() {
        super.onDisable();
        if (this.oldItem != -1 && mc.player != null && mc.player.inventory != null && mc.player.inventory.armorInventory != null && mc.player.inventory.armorInventory.size() > 2) {
            ItemStack chestSlot = mc.player.inventory.armorInventory.get(2);
            if (chestSlot != null && chestSlot.getItem() == Items.ELYTRA && mc.player.inventory.getStackInSlot(this.oldItem) != null && mc.player.inventory.getStackInSlot(this.oldItem).getItem() instanceof ArmorItem) {
                mc.playerController.windowClick(0, 6, this.oldItem, ClickType.SWAP, mc.player);
            }

            this.oldItem = -1;
        }

        if (mc.gameSettings != null) {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
    }
}

