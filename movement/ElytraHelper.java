package nuclear.module.impl.movement;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.player.GuiMove;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;

@Annotation(name = "ElytraHelper", type = TypeList.Movement, desc = "Помогает свапать элитры с инвентаря")
public class ElytraHelper extends Module {
    private final BindSetting swapChestKey = new BindSetting("Элитры", 0);
    private final BindSetting fireWorkKey = new BindSetting("Фейерверк", 0);
    private final BooleanSetting autoFly = new BooleanSetting("Авто взлёт", true);
    private final BooleanSetting autofireWork = new BooleanSetting("Авто фейерверк", false);
    public final BooleanSetting swap = new BooleanSetting("Фейр в левую руку", true);
    private final BooleanSetting autofireWorkstart = new BooleanSetting("Только при взлёте", false).setVisible(() -> autofireWork.get());
    private final BooleanSetting cakeWorld = new BooleanSetting("Обход CakeWorld", false);
    ItemStack currentStack = ItemStack.EMPTY;
    private final TimerUtil stopWatch = new TimerUtil();
    boolean fireworkUsed;
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean recentlySwapped = false;
    private final TimerUtil swapCooldownTimer = new TimerUtil();
    private boolean hasFiredOnStart = false;

    public ElytraHelper() {
        addSettings(swapChestKey, fireWorkKey, autoFly, autofireWork, autofireWorkstart, swap, cakeWorld);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (this.autoFly.get() && !mc.player.abilities.isFlying && mc.player.isOnGround() && ((ItemStack)mc.player.inventory.armorInventory.get(2)).getItem() == Items.ELYTRA && !mc.gameSettings.keyBindJump.isKeyDown() && !mc.player.isInWater() && !mc.player.isInLava()) {
                mc.player.jump();
            }

            if (this.autoFly.get() && !mc.player.abilities.isFlying && !mc.player.isInWater() && !mc.player.isOnGround() && !mc.player.isElytraFlying() && ((ItemStack)mc.player.inventory.armorInventory.get(2)).getItem() == Items.ELYTRA) {
                mc.player.startFallFlying();
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                if (autofireWork.get() && autofireWorkstart.get() && !hasFiredOnStart) {
                    if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                        InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
                        hasFiredOnStart = true;
                    } else {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
                    }
                }
            }

            if (mc.player.isOnGround() || mc.player.isInWater() || mc.player.isInLava()) {
                hasFiredOnStart = false;
            }

            if (mc.player.isElytraFlying() && autofireWork.get() && !autofireWorkstart.get() && timerUtil.hasTimeElapsed(570L)) {
                if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                    InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
                } else {
                    ClientUtils.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
                }
                timerUtil.reset();
            }

            this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);

            if (recentlySwapped && swapCooldownTimer.hasTimeElapsed(2000L)) {
                recentlySwapped = false;
            }

            if (fireworkUsed) {
                if (mc.player.isElytraFlying()) {
                    useFirework();
                    fireworkUsed = false;
                }
            }
        }

        if (event instanceof EventKey e) {
            if (e.key == swapChestKey.getKey() && stopWatch.hasTimeElapsed(150L)) {
                boolean wasSprinting = mc.player.isSprinting();
                boolean wasSprintKeyPressed = mc.gameSettings.keyBindSprint.isKeyDown();
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    GuiMove.stopMovementTemporarily(0.06f);
                }

                if (cakeWorld.get() && wasSprinting) {
                    mc.gameSettings.keyBindSprint.setPressed(false);
                    mc.player.setSprinting(false);
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
                }

                if (getItemSlot(Items.ELYTRA) == -1) {
                    changeChestPlate(currentStack);
                    InventoryUtils.syncInventory();
                    stopWatch.reset();
                } else {
                    changeChestPlate(currentStack);
                    InventoryUtils.syncInventory();
                    stopWatch.reset();
                }

                if (cakeWorld.get() && wasSprinting) {
                    if (wasSprintKeyPressed) {
                        mc.gameSettings.keyBindSprint.setPressed(true);
                    }
                    mc.player.setSprinting(true);
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_SPRINTING));
                }

                recentlySwapped = true;
                swapCooldownTimer.reset();
            }

            if (e.key == fireWorkKey.getKey()) {
                fireworkUsed = true;
            }
        }
        return false;
    }

    private void changeChestPlate(ItemStack stack) {
        if (stack.getItem() != Items.ELYTRA) {
            int elytraSlot = getItemSlot(Items.ELYTRA);
            int freeSlot = findFreeInventorySlot();
            if (elytraSlot >= 0) {
                InventoryUtils.moveItem(elytraSlot, 6);
            } else if (freeSlot >= 0) {
            }
            return;
        }
        int armorSlot = getChestPlateSlot();
        int freeSlot = findFreeInventorySlot();
        if (armorSlot >= 0) {
            InventoryUtils.moveItem(armorSlot, 6);
        } else if (freeSlot >= 0) {
            InventoryUtils.moveItem(6, freeSlot);
        }
    }

    private int findFreeInventorySlot() {
        for (int i = 10; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getChestPlateSlot() {
        Item[] items = {
                Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE
        };

        for (Item item : items) {
            for (int i = 0; i < 36; ++i) {
                Item stack = mc.player.inventory.getStackInSlot(i).getItem();
                if (stack == item) {
                    if (i < 9) {
                        i += 36;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItemSlot(Item item) {
        int finalSlot = -1;

        for (int i = 0; i < 36; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                finalSlot = i;
                break;
            }
        }

        if (finalSlot < 9 && finalSlot != -1) {
            finalSlot += 36;
        }

        return finalSlot;
    }

    private void useFirework() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            if (mc.player.isElytraFlying()) {
                ClientUtils.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
            }
        } else {
            if (Manager.FUNCTION_MANAGER.middleClickPearlFunction.legit.get()) {
                InventoryUtils.holySwapClick(Items.ENDER_PEARL, false);
            } else {
                InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
            }
        }
    }

    @Override
    public void onDisable() {
        stopWatch.reset();
        timerUtil.reset();
        hasFiredOnStart = false;
        super.onDisable();
    }
}