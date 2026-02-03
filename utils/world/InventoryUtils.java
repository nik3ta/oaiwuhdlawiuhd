package nuclear.utils.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.module.impl.combat.AttackAura;
import nuclear.module.impl.player.GuiMove;
import nuclear.utils.ClientUtils;
import nuclear.utils.IMinecraft;

import static net.minecraft.inventory.InventoryHelper.getItemIndex;

public class InventoryUtils implements IMinecraft {

    public static boolean stackHasAnyEffect(ItemStack stack, boolean includeRegular, boolean includeSplash, boolean includeLingering, Effect... effects) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        boolean typeOk = (includeRegular && item == Items.POTION) || (includeSplash && item == Items.SPLASH_POTION) || (includeLingering && item == Items.LINGERING_POTION);
        if (!typeOk) return false;

        for (EffectInstance instance : PotionUtils.getEffectsFromStack(stack)) {
            for (Effect effect : effects) {
                if (instance.getPotion() == effect) return true;
            }
        }
        return false;
    }

    public static void syncInventory() {
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
        }
    }

    public static int getHotBarSlot(Item input) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == input) {
                return i;
            }
        }
        return -1;
    }

    public static int getTrident() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() instanceof TridentItem) {
                return i;
            }
        }
        return -1;
    }

    public static int getItem(Item item, boolean hotbar) {
        for (int i = 0; i < (hotbar ? 9 : 45); ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int getItemSlot(Item input) {
        for (ItemStack stack : mc.player.getArmorInventoryList()) {
            if (stack.getItem() == input) {
                return -2;
            }
        }
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s.getItem() == input) {
                slot = i;
                break;
            }
        }
        if (slot < 9 && slot != -1) {
            slot = slot + 36;
        }
        return slot;
    }

    public static boolean moveItem(final int from, final int to, final boolean air) {
        if (from == to) {
            return air;
        }
        pickupItem(from, 0);
        pickupItem(to, 0);
        if (air) {
            pickupItem(from, 0);
        }
        return air;
    }

    public static void pickupItem(int slot, int button) {
        mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
    }
    public static void swapHand(int slotId, Hand hand, boolean packet) {
        if (slotId == -1) return;
        int button = hand.equals(Hand.MAIN_HAND) ? mc.player.inventory.currentItem : 40;

        clickSlotId(slotId, button, ClickType.SWAP, packet);
    }

    public static void clickSlotId(int slotId, int buttonId, ClickType clickType, boolean packet) {
        clickSlotId(mc.player.openContainer.windowId, slotId, buttonId, clickType, packet);
    }

    public static void clickSlotId(int windowId, int slotId, int buttonId, ClickType clickType, boolean packet) {
        if (packet) {
            mc.player.connection.sendPacket(new CClickWindowPacket(windowId, slotId, buttonId, clickType, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
        } else {
            mc.playerController.windowClick(windowId, slotId, buttonId, clickType, mc.player);
        }

    }

    public static void dropItem(int slot) {
        mc.playerController.windowClick(0, slot, 0, ClickType.THROW, mc.player);
    }

    public static int getAxe(boolean hotBar) {
        int startSlot = hotBar ? 0 : 9;
        int endSlot = hotBar ? 9 : 36;

        for (int i = startSlot; i < endSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AxeItem) {
                return i;
            }
        }

        return -1;
    }

    public static boolean doesHotbarHaveItem(Item item) {
        for(int i = 0; i < 9; ++i) {
            mc.player.inventory.getStackInSlot(i);
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }

        return false;
    }

    public static int findFreeSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static void antipolet(Item item) {
        if (getItemIndex(item) != -1) {
            for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    int originalSlot = i < 9 ? 36 + i : i;
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }

                    InventoryUtils.moveItem(originalSlot, 45);

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                                mc.gameSettings.keyBindSneak.setPressed(true);
                            }
                        }
                    }, 150);

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                                mc.gameSettings.keyBindSneak.setPressed(false);
                            }
                        }
                    }, 200);

                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            InventoryUtils.moveItem(45, originalSlot);
                        }
                    }, 300);

                    break;
                }
            }
        }
    }

    public static void moveItem(int from, int to) {
        if (from != to && from != -1) {
            from = from < 9 ? from + 36 : from;
            if (to == 45) {
                clickSlotId(from, 40, ClickType.SWAP, false);
            } else {
                clickSlotId(from, 0, ClickType.SWAP, false);
                clickSlotId(to, 0, ClickType.SWAP, false);
                clickSlotId(from, 0, ClickType.SWAP, false);
            }
        }
    }

    public static void holySwapClick(Item item, boolean rotation) {
        if (getItemIndex(item) != -1) {
            int i;
            if (doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; i++) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        int originalSlot = mc.player.inventory.currentItem;

                        mc.player.inventory.currentItem = i;
                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
                            }
                        }, 40);

                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                mc.player.inventory.currentItem = originalSlot;
                            }
                        }, 60);
                        break;
                    }
                }
            }


            if (!doesHotbarHaveItem(item)) {
                for (i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        GuiMove.stopMovementTemporarily(0.085f);
                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8, ClickType.SWAP, mc.player);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8));

                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
                            }
                        }, 40);
                        int finalI = i;
                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                                mc.playerController.windowClick(0, finalI, mc.player.inventory.currentItem % 8, ClickType.SWAP, mc.player);
                            }
                        }, 60);
                        break;
                    }
                }
            }
        }
    }
    public static void inventorySwapClick(Item item, boolean rotation) {
        if (getItemIndex(item) != -1) {
            int i;
            if (doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; i++) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                        }

                        if (rotation && Manager.FUNCTION_MANAGER.auraFunction.target != null) {
                            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                        }

                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        }
                        break;
                    }
                }
            }


            if (!doesHotbarHaveItem(item)) {
                for (i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {

                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));

                        if (rotation && AttackAura.target != null) {
                            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                        }

                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                        break;
                    }
                }
            }
        }
    }
    public static void inventorySwapClickFF(Item item, boolean rotation) {
        int slot = getItemIndex(item);
        if (slot == -1) return;

        if (doesHotbarHaveItem(item)) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    if (Manager.FUNCTION_MANAGER.elytraHelper.state && Manager.FUNCTION_MANAGER.elytraHelper.swap.get() && mc.player.isHandActive() && !mc.player.isBlocking() && i != mc.player.inventory.currentItem) {
                        moveItem(i, 45);
                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
                        moveItem(i, 45);
                        return;
                    }

                    if (i != mc.player.inventory.currentItem) {
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                    }

                    if (rotation && Manager.FUNCTION_MANAGER.auraFunction.target != null) {
                        mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                    }

                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));

                    if (i != mc.player.inventory.currentItem) {
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                    }
                    return;
                }
            }
        } else {
            for (int i = 0; i < 36; i++) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    if (Manager.FUNCTION_MANAGER.elytraHelper.state && Manager.FUNCTION_MANAGER.elytraHelper.swap.get() && mc.player.isHandActive() && !mc.player.isBlocking()) {
                        moveItem(i, 45);
                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
                        moveItem(i, 45);
                        return;
                    }

                    int swapSlot = (mc.player.inventory.currentItem % 8) + 1;
                    mc.playerController.windowClick(0, i, swapSlot, ClickType.SWAP, mc.player);
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(swapSlot));

                    if (rotation && Manager.FUNCTION_MANAGER.auraFunction.target != null) {
                        mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(mc.player.rotationYaw, mc.player.rotationPitch, false));
                    }

                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));

                    mc.playerController.windowClick(0, i, swapSlot, ClickType.SWAP, mc.player);
                    return;
                }
            }
        }
    }

    public static void inventorySwapClick(Item item, boolean rotation, UseType useType, PlayerEntity playerEntity, BlockPos position) {
        if (getItemIndex(item) != -1) {
            int i;
            if (doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; i++) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                        }

                        use(useType, Hand.MAIN_HAND, position);

                        if (i != mc.player.inventory.currentItem) {
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        }
                        break;
                    }
                }
            }


            if (!doesHotbarHaveItem(item)) {
                for (i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {

                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));

                        use(useType, Hand.MAIN_HAND, position);

                        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                        mc.playerController.windowClick(0, i, mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, mc.player);
                        break;
                    }
                }
            }
        }
    }

    public static void use(UseType useType, Hand hand, BlockPos position) {
        if (useType == InventoryUtils.UseType.USE_ITEM) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        }

        if (useType == InventoryUtils.UseType.ATTACK) {
            mc.playerController.attackEntity(mc.player, AttackAura.target);
        }

        if (useType == InventoryUtils.UseType.PLACE_BLOCK) {
            BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(new Vector3d((double) ((float) position.getX() + 0.5F), (double) ((float) position.getY() + 0.5F), (double) ((float) position.getZ() + 0.5F)), Direction.UP, position, false);
            if (mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, rayTraceResult) == ActionResultType.SUCCESS) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
        }

    }

    public static class Hands {
        public static boolean isEnabled;
        private boolean isChangingItem;
        private int originalSlot = -1;

        public void handleItemChange(boolean resetItem) {
            if (this.isChangingItem && this.originalSlot != -1) {
                isEnabled = true;
                mc.player.inventory.currentItem = this.originalSlot;
                if (resetItem) {
                    this.isChangingItem = false;
                    this.originalSlot = -1;
                    isEnabled = false;
                }
            }
        }

        public void setOriginalSlot(int slot) {
            this.originalSlot = slot;
        }
    }

    public static enum UseType {
        ATTACK,
        USE_ITEM,
        PLACE_BLOCK;
    }
}
