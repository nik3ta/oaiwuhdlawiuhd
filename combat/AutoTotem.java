package nuclear.module.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventCrystalEntity;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.player.GuiMove;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Annotation(name = "AutoTotem",
        type = TypeList.Combat, desc = "Берёт тотемы при низком значение здоровья"
)
public class AutoTotem extends Module {
    private final SliderSetting health = new SliderSetting("Здоровье", 5.0f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting healthelytra = new SliderSetting("Здоровье на элитрах", 9f, 1.0f, 20.0f, 0.5f);
    private final SliderSetting healthbronya = new SliderSetting("Без полной брони", 8f, 1.0f, 20.0F, 0.5F);
    private final SliderSetting tntRadius = new SliderSetting("Радиус динамита", 10.0f, 5.0f, 35.0f, 5f);
    private final MultiBoxSetting mode = new MultiBoxSetting("Проверки", new BooleanSetting[]{
            new BooleanSetting("Золотые сердца", true),
            new BooleanSetting("Кристаллы", true),
            new BooleanSetting("Падение", true),
            new BooleanSetting("Кристалл в руке", true),
            new BooleanSetting("Динамит", false),
            new BooleanSetting("Трезубец", false)
    });

    private final TimerUtil stopWatch = new TimerUtil();
    private Item backItem = Items.AIR;
    private ItemStack backItemStack;
    private int nonEnchantedTotems;
    private boolean totemIsUsed;
    private int itemInMouse = -1;
    private int totemCount = 0;
    ItemStack currentStack = ItemStack.EMPTY;
    private int oldItem = -1;
    private boolean isSwapping = false;
    private int lastSwappedSlot = -1;
    private Item savedOffhandItem = Items.AIR;
    private ItemStack savedOffhandStack = ItemStack.EMPTY;
    private final BooleanSetting swapBack = new BooleanSetting("Возвращать предмет", true);
    private final BooleanSetting noBallSwitch = new BooleanSetting("Не сменять шар", false);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять зачарованный", true);
    private boolean waitingForSwap = false;
    private int targetSlot = -1;
    private int targetInventorySlot = -1;

    public AutoTotem() {
        addSettings(mode, health, healthelytra, healthbronya, tntRadius, swapBack, noBallSwitch, saveEnchanted);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventCrystalEntity) {
            handleCrystalEvent((EventCrystalEntity) event);
        } else if (event instanceof EventUpdate) {
            handleInventoryUpdate();
        } else if (event instanceof EventPacket) {
            handlePacketEvent((EventPacket) event);
        }
        return false;
    }

    private void handleCrystalEvent(EventCrystalEntity event) {
        Entity entity = event.getEntity();
        if (entity instanceof EnderCrystalEntity && swapBack.get() && entity.getDistance(mc.player) <= 6.0f) {
            swapToTotem();
        }
    }

    private void handlePacketEvent(EventPacket event) {
        if (event.isReceivePacket()) {
            IPacket packet = event.getPacket();

            if (packet instanceof SEntityStatusPacket statusPacket) {
                if (statusPacket.getOpCode() == 35 && statusPacket.getEntity(mc.world) == mc.player) {
                    totemIsUsed = true;
                }
            }

            if (packet instanceof SUpdateHealthPacket) {
                handleInventoryUpdate();
            }

            if (packet instanceof SSetSlotPacket slotPacket) {
                int windowId = slotPacket.getWindowId();
                int slot = slotPacket.getSlot();

                if (windowId == 0 && (slot == 40 || (slot >= 9 && slot <= 44))) {
                    handleInventoryUpdate();
                } else if (windowId == -2 && slot >= 0 && slot <= 40) {
                    handleInventoryUpdate();
                }
            }

            if (packet instanceof SEntityEquipmentPacket equipmentPacket) {
                if (equipmentPacket.getEntityID() == mc.player.getEntityId()) {
                    for (com.mojang.datafixers.util.Pair<EquipmentSlotType, net.minecraft.item.ItemStack> pair : equipmentPacket.func_241790_c_()) {
                        if (pair.getFirst() == EquipmentSlotType.OFFHAND) {
                            handleInventoryUpdate();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void handleInventoryUpdate() {
        if (waitingForSwap) return;
        
        totemCount = countTotems(true);
        nonEnchantedTotems = (int) mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .map(Slot::getStack)
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && !s.isEnchanted())
                .count();

        int slot = getSlotInInventory();
        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        boolean handNotNull = !(offhandStack.getItem() instanceof AirItem);

        boolean offhandIsTotem = offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
        boolean offhandCountsAsTotem = offhandIsTotem && !isSaveEnchanted(offhandStack);

        if (slot == -1 && !offhandIsTotem && savedOffhandItem != Items.AIR && swapBack.get()) {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                int backSlot = findBackSlot();
                if (backSlot != -1) {
                    waitingForSwap = true;
                    targetSlot = backSlot;
                    targetInventorySlot = 45;

                    GuiMove.stopMovementTemporarily(0.06f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(45);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        mc.execute(() -> {
                            if (mc.player != null && waitingForSwap && !MoveUtil.isMoving()) {
                                InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                waitingForSwap = false;
                                oldItem = -1;
                                savedOffhandItem = Items.AIR;
                                savedOffhandStack = ItemStack.EMPTY;
                            } else if (waitingForSwap && MoveUtil.isMoving()) {
                                waitingForSwap = false;
                            }
                        });
                    }).start();

                    return;
                }
            }
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                waitingForSwap = true;
                GuiMove.stopMovementTemporarily(0.06f);
                new Thread(() -> {
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    mc.execute(() -> {
                        if (!MoveUtil.isMoving()) {
                            returnItemToOffhand();
                            oldItem = -1;
                            savedOffhandItem = Items.AIR;
                            savedOffhandStack = ItemStack.EMPTY;
                        }
                        waitingForSwap = false;
                    });
                }).start();
            } else {
                waitingForSwap = true;
                returnItemToOffhand();
                oldItem = -1;
                savedOffhandItem = Items.AIR;
                savedOffhandStack = ItemStack.EMPTY;
                waitingForSwap = false;
            }
            return;
        }

        if (shouldToSwapTotem()) {
            if (offhandCountsAsTotem) {
                isSwapping = false;
                lastSwappedSlot = -1;
            } else if (slot != -1) {
                Item currentOffhandItem = mc.player.getHeldItemOffhand().getItem();
                if (handNotNull && savedOffhandItem == Items.AIR && currentOffhandItem != Items.AIR) {
                    savedOffhandItem = currentOffhandItem;
                    savedOffhandStack = mc.player.getHeldItemOffhand().copy();
                }
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    waitingForSwap = true;
                    targetSlot = slot;
                    targetInventorySlot = 45;

                    GuiMove.stopMovementTemporarily(0.06f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(45);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        mc.execute(() -> {
                            if (mc.player != null && waitingForSwap && !MoveUtil.isMoving()) {
                                InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                stopWatch.reset();

                                waitingForSwap = false;
                                isSwapping = false;
                                lastSwappedSlot = slot;
                                if (oldItem == -1) {
                                    oldItem = slot;
                                }
                            } else if (waitingForSwap && MoveUtil.isMoving()) {
                                waitingForSwap = false;
                            }
                        });
                    }).start();

                } else {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        waitingForSwap = true;
                        GuiMove.stopMovementTemporarily(0.06f);
                        final int finalSlot = slot;
                        new Thread(() -> {
                            try {
                                Thread.sleep(45);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mc.execute(() -> {
                                if (!MoveUtil.isMoving()) {
                                    InventoryUtils.moveItem(finalSlot, 45);
                                    lastSwappedSlot = finalSlot;
                                    if (oldItem == -1) {
                                        oldItem = finalSlot;
                                    }
                                }
                                waitingForSwap = false;
                            });
                        }).start();
                    } else {
                        waitingForSwap = true;
                        InventoryUtils.moveItem(slot, 45);
                        lastSwappedSlot = slot;
                        if (oldItem == -1) {
                            oldItem = slot;
                        }
                        waitingForSwap = false;
                    }
                }
            }
        } else if (oldItem != -1 && swapBack.get()) {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                int backSlot = findBackSlot();
                if (backSlot != -1) {
                    waitingForSwap = true;
                    targetSlot = backSlot;
                    targetInventorySlot = 45;

                    GuiMove.stopMovementTemporarily(0.06f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(45);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        mc.execute(() -> {
                            if (mc.player != null && waitingForSwap && !MoveUtil.isMoving()) {
                                InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                waitingForSwap = false;
                                oldItem = -1;
                                savedOffhandItem = Items.AIR;
                                savedOffhandStack = ItemStack.EMPTY;
                            } else if (waitingForSwap && MoveUtil.isMoving()) {
                                waitingForSwap = false;
                            }
                        });
                    }).start();

                } else {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        waitingForSwap = true;
                        GuiMove.stopMovementTemporarily(0.06f);
                        new Thread(() -> {
                            try {
                                Thread.sleep(45);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }

                            mc.execute(() -> {
                                if (!MoveUtil.isMoving()) {
                                    returnItemToOffhand();
                                    oldItem = -1;
                                    savedOffhandItem = Items.AIR;
                                    savedOffhandStack = ItemStack.EMPTY;
                                }
                                waitingForSwap = false;
                            });
                        }).start();
                    } else {
                        waitingForSwap = true;
                        returnItemToOffhand();
                        oldItem = -1;
                        savedOffhandItem = Items.AIR;
                        savedOffhandStack = ItemStack.EMPTY;
                        waitingForSwap = false;
                    }
                }
            } else {
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    waitingForSwap = true;
                    GuiMove.stopMovementTemporarily(0.06f);
                    new Thread(() -> {
                        try {
                            Thread.sleep(45);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        mc.execute(() -> {
                            if (!MoveUtil.isMoving()) {
                                returnItemToOffhand();
                                oldItem = -1;
                                savedOffhandItem = Items.AIR;
                                savedOffhandStack = ItemStack.EMPTY;
                            }
                            waitingForSwap = false;
                        });
                    }).start();
                } else {
                    waitingForSwap = true;
                    returnItemToOffhand();
                    oldItem = -1;
                    savedOffhandItem = Items.AIR;
                    savedOffhandStack = ItemStack.EMPTY;
                    waitingForSwap = false;
                }
            }
        }
    }

    private void swapToTotem() {
        if (waitingForSwap) return;
        
        int totemSlot = getSlotInInventory();
        if (totemSlot == -1 || isTotemInHands()) return;

        Item currentOffhand = mc.player.getHeldItemOffhand().getItem();
        if (currentOffhand == Items.TOTEM_OF_UNDYING) return;

        if (itemInMouse == -1) {
            itemInMouse = totemSlot;
            backItem = currentOffhand;
            backItemStack = mc.player.getHeldItemOffhand().copy();
        }
        if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
            waitingForSwap = true;
            targetSlot = totemSlot;
            targetInventorySlot = 45;

            GuiMove.stopMovementTemporarily(0.06f);
            new Thread(() -> {
                try {
                    Thread.sleep(45);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                mc.execute(() -> {
                    if (mc.player != null && waitingForSwap && !MoveUtil.isMoving()) {
                        InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                        stopWatch.reset();

                        waitingForSwap = false;
                        isSwapping = false;
                    } else if (waitingForSwap && MoveUtil.isMoving()) {
                        waitingForSwap = false;
                    }
                });
            }).start();

        } else {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                waitingForSwap = true;
                GuiMove.stopMovementTemporarily(0.06f);
                final int finalTotemSlot = totemSlot;
                new Thread(() -> {
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    mc.execute(() -> {
                        if (!MoveUtil.isMoving()) {
                            InventoryUtils.moveItem(finalTotemSlot, 45);
                            stopWatch.reset();
                        }
                        waitingForSwap = false;
                    });
                }).start();
            } else {
                waitingForSwap = true;
                InventoryUtils.moveItem(totemSlot, 45);
                stopWatch.reset();
                waitingForSwap = false;
            }
        }

        if (totemCount > 1 && totemIsUsed) {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                waitingForSwap = true;
                GuiMove.stopMovementTemporarily(0.06f);
                new Thread(() -> {
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    mc.execute(() -> {
                        if (!MoveUtil.isMoving()) {
                            backItemInMouse();
                        }
                        waitingForSwap = false;
                    });
                }).start();
            } else {
                waitingForSwap = true;
                backItemInMouse();
                waitingForSwap = false;
            }
            totemIsUsed = false;
        } else if (backItem != Items.AIR) {
            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                waitingForSwap = true;
                GuiMove.stopMovementTemporarily(0.06f);
                new Thread(() -> {
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    mc.execute(() -> {
                        if (!MoveUtil.isMoving()) {
                            scheduledSwapBack();
                        } else {
                            waitingForSwap = false;
                        }
                    });
                }).start();
            } else {
                waitingForSwap = true;
                scheduledSwapBack();
            }
        }
    }

    private boolean checkTntNearby() {
        if (!mode.get("Динамит")) return false;

        float radius = tntRadius.getValue().floatValue();

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity.getDistance(mc.player) > radius) continue;

            if (entity instanceof TNTEntity) {
                return true;
            }
            if (entity instanceof TNTMinecartEntity) {
                return true;
            }
        }

        return false;
    }

    private boolean checkTridentNearby() {
        if (!mode.get("Трезубец")) return false;

        float tolerance = 3.0f;

        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof TridentEntity)) continue;

            TridentEntity trident = (TridentEntity) entity;
            if (trident.isInGround()) continue;

            Vector3d tridentPos = trident.getPositionVec();
            Vector3d tridentMotion = trident.getMotion();

            if (tridentMotion.lengthSquared() < 0.001) continue;

            Vector3d playerPos = mc.player.getPositionVec().add(0, mc.player.getEyeHeight(), 0);

            Vector3d toPlayer = playerPos.subtract(tridentPos);
            Vector3d crossProduct = crossProduct(toPlayer, tridentMotion);
            double distanceToTrajectory = crossProduct.length() / tridentMotion.length();

            if (distanceToTrajectory <= tolerance) {
                double dotProduct = toPlayer.dotProduct(tridentMotion.normalize());
                if (dotProduct > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private Vector3d crossProduct(Vector3d a, Vector3d b) {
        return new Vector3d(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }

    private int findBackSlot() {
        if (savedOffhandItem == Items.AIR || savedOffhandStack.isEmpty() || !swapBack.get()) return -1;

        Slot slot = mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .filter(s -> !s.getStack().isEmpty() && ItemStack.areItemStacksEqual(s.getStack(), savedOffhandStack))
                .findFirst()
                .orElse(null);

        if (slot != null) {
            return slot.slotNumber;
        }

        slot = mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .filter(s -> s.getStack().getItem() == savedOffhandItem)
                .findFirst()
                .orElse(null);

        if (slot != null) {
            return slot.slotNumber;
        }

        return -1;
    }

    private void returnItemToOffhand() {
        int backSlot = findBackSlot();
        if (backSlot == -1) return;

        InventoryUtils.moveItem(backSlot, 45);
    }

    private void scheduledSwapBack() {
        if (backItem == Items.AIR || !swapBack.get()) {
            waitingForSwap = false;
            return;
        }

        mc.execute(() -> {
            if (mc.player == null || mc.world == null) {
                waitingForSwap = false;
                return;
            }

            int backSlot = findItemInHotbar(backItem);
            if (backSlot == -1) {
                backSlot = findItemInInventory(backItem);
            }

            if (backSlot != -1) {
                InventoryUtils.moveItem(backSlot, 45);
            }

            itemInMouse = -1;
            backItem = Items.AIR;
            waitingForSwap = false;
        });
    }

    private int findItemInHotbar(Item item) {
        Slot slot = mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .filter(s -> s.slotNumber >= 36 && s.slotNumber < 45)
                .filter(s -> s.getStack().getItem() == item)
                .findFirst()
                .orElse(null);

        if (slot != null) {
            return slot.slotNumber;
        }
        return -1;
    }

    private int findItemInInventory(Item item) {
        Slot slot = mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .filter(s -> s.slotNumber >= 9 && s.slotNumber < 36)
                .filter(s -> s.getStack().getItem() == item)
                .findFirst()
                .orElse(null);

        if (slot != null) {
            return slot.slotNumber;
        }
        return -1;
    }

    private int countTotems(boolean includeEnchanted) {
        return (int) mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .map(Slot::getStack)
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && (includeEnchanted || !s.isEnchanted()))
                .count();
    }

    private void backItemInMouse() {
        if (itemInMouse != -1) {
            InventoryUtils.moveItem(itemInMouse, 0);
        }
    }

    private boolean isTotemInHands() {
        Hand[] hands = Hand.values();

        for (Hand hand : hands) {
            ItemStack heldItem = mc.player.getHeldItem(hand);
            if (heldItem.getItem() == Items.TOTEM_OF_UNDYING && !isSaveEnchanted(heldItem)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSaveEnchanted(ItemStack itemStack) {
        return saveEnchanted.get() && itemStack.isEnchanted() && nonEnchantedTotems > 0;
    }

    private boolean shouldToSwapTotem() {
        this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        final float absorptionAmount = mc.player.isPotionActive(Effects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0f;
        float currentHealth = mc.player.getHealth();
        if (this.mode.get(0)) {
            currentHealth += absorptionAmount;
        }

        boolean hasFullArmor = true;
        for (int i = 0; i < mc.player.inventory.armorInventory.size(); i++) {
            ItemStack armor = mc.player.inventory.armorInventory.get(i);
            if (armor.isEmpty()) {
                if (i == 3 && hasJackHeadInInventory()) {
                    continue;
                }
                hasFullArmor = false;
                break;
            }
        }

        float healthThreshold = hasFullArmor ? this.health.getValue().floatValue() : this.healthbronya.getValue().floatValue();

        boolean inDanger = this.isInDangerousSituation();
        if (inDanger && !this.isOffhandItemBall()) {
            return true;
        }

        float threshold = (currentStack.getItem() == Items.ELYTRA ? healthelytra.getValue().floatValue() : healthThreshold);
        if (currentHealth <= threshold + 0.5f) {
            return true;
        }

        return checkFall();
    }

    private boolean isInDangerousSituation() {
        return checkCrystal()
                || checkPlayerWithCrystalNearObsidian()
                || checkTntNearby()
                || checkTridentNearby();
    }

    private boolean checkFall() {
        if (!this.mode.get(2)) {
            return false;
        }
        if (mc.player.isInWater() || mc.player.isElytraFlying()) {
            return false;
        }
        float fallDistance = mc.player.fallDistance;
        float fallDamage = calculateFallDamage(fallDistance);

        float currentHealth = mc.player.getHealth();

        return fallDamage >= currentHealth / 1.92f;
    }

    private float calculateFallDamage(float fallDistance) {
        if (fallDistance <= 3.0f) return 0;

        float fallDamage = (fallDistance - 3.0f) / 2;

        float armorReduction = 0;
        for (ItemStack armor : mc.player.inventory.armorInventory) {
            if (armor.getItem() instanceof ArmorItem) {
                armorReduction += ((ArmorItem) armor.getItem()).getDamageReductionAmount();
            }
        }

        ItemStack boots = mc.player.inventory.armorInventory.get(0);
        if (boots.getItem() instanceof ArmorItem) {
            int featherFallingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FEATHER_FALLING, boots);
            if (featherFallingLevel > 0) {
                float reductionFactor = 1.0f - (Math.min(featherFallingLevel, 4) * 0.171f);
                fallDamage *= reductionFactor;
            }
        }

        if (hasProtectionAura()) {
            fallDamage *= 0.2f;
        }

        float absorption = mc.player.isPotionActive(Effects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0f;
        fallDamage = Math.max(0, fallDamage - absorption);

        return Math.min(fallDamage, mc.player.getMaxHealth());
    }

    private boolean hasProtectionAura() {
        return mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .map(Slot::getStack)
                .anyMatch(stack -> stack.hasDisplayName()
                        && "Аура Защиты От Падения".equals(stack.getDisplayName().getString()));
    }

    private boolean checkCrystal() {
        if (!mode.get(1))
            return false;

        float checkRadius = 8.0f;
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof EnderCrystalEntity) {
                float distance = mc.player.getDistance(entity);
                if (distance <= checkRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPlayerWithCrystalNearObsidian() {
        if (!mode.get(3))
            return false;

        float checkRadius = 7.0f;
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof PlayerEntity && entity != mc.player) {
                float distance = mc.player.getDistance(entity);
                if (distance <= checkRadius) {
                    PlayerEntity otherPlayer = (PlayerEntity) entity;
                    ItemStack mainHand = otherPlayer.getHeldItemMainhand();
                    ItemStack offHand = otherPlayer.getHeldItemOffhand();
                    if (mainHand.getItem() == Items.END_CRYSTAL || offHand.getItem() == Items.END_CRYSTAL) {
                        BlockPos obsidianPos = getBlock(checkRadius, Blocks.OBSIDIAN);
                        if (obsidianPos != null && getDistanceOfEntityToBlock(otherPlayer, obsidianPos) <= checkRadius) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isOffhandItemBall() {
        return noBallSwitch.get() && mc.player.getHeldItemOffhand().getItem() == Items.PLAYER_HEAD;
    }

    private BlockPos getBlock(float distance, Block block) {
        return getSphere(getPlayerPosLocal(), distance, 6, false, true, 0).stream()
                .filter(position -> mc.world.getBlockState(position).getBlock() == block)
                .min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos)))
                .orElse(null);
    }

    private List<BlockPos> getSphere(BlockPos center, float radius, int height, boolean hollow, boolean fromBottom, int yOffset) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        for (int x = centerX - (int) radius; x <= centerX + radius; ++x) {
            for (int z = centerZ - (int) radius; z <= centerZ + radius; ++z) {
                int yStart = fromBottom ? (centerY - (int) radius) : centerY;
                for (int yEnd = fromBottom ? (centerY + (int) radius) : (centerY + height), y = yStart; y < yEnd; ++y) {
                    if (isPositionWithinSphere(centerX, centerY, centerZ, x, y, z, radius, hollow)) {
                        positions.add(new BlockPos(x, y + yOffset, z));
                    }
                }
            }
        }
        return positions;
    }

    private BlockPos getPlayerPosLocal() {
        if (mc.player == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(Math.floor(mc.player.getPosX()), Math.floor(mc.player.getPosY()), Math.floor(mc.player.getPosZ()));
    }

    private double getDistanceOfEntityToBlock(Entity entity, BlockPos blockPos) {
        return getDistance(entity.getPosX(), entity.getPosY(), entity.getPosZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    private double getDistance(double n, double n2, double n3, double n4, double n5, double n6) {
        double n7 = n - n4;
        double n8 = n2 - n5;
        double n9 = n3 - n6;
        return MathHelper.sqrt(n7 * n7 + n8 * n8 + n9 * n9);
    }

    private static boolean isPositionWithinSphere(int centerX, int centerY, int centerZ, int x, int y, int z, float radius, boolean hollow) {
        double distanceSq = Math.pow(centerX - x, 2.0) + Math.pow(centerZ - z, 2.0) + Math.pow(centerY - y, 2.0);
        return distanceSq < Math.pow(radius, 2.0) && (!hollow || distanceSq >= Math.pow(radius - 1.0f, 2.0));
    }

    private int getSlotInInventory() {
        Slot slot = mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .filter(s -> s.getStack().getItem() == Items.TOTEM_OF_UNDYING && !isSaveEnchanted(s.getStack()))
                .findFirst()
                .orElse(null);

        if (slot != null) {
            return slot.slotNumber;
        }
        return -1;
    }

    private boolean hasJackHeadInInventory() {
        return mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.inventory == mc.player.inventory)
                .map(Slot::getStack)
                .anyMatch(stack -> stack.getItem() == Items.PLAYER_HEAD
                        && stack.hasDisplayName()
                        && "Голова Джека".equals(stack.getDisplayName().getString()));
    }

    public void onDisable() {
        oldItem = -1;
        waitingForSwap = false;
        targetSlot = -1;
        targetInventorySlot = -1;
        super.onDisable();
    }
}
