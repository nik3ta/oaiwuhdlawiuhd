package nuclear.module.impl.movement;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.combat.AttackAura;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.utils.math.MathUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;

@Annotation(
        name = "ElytraJump",
        type = TypeList.Movement,
        desc = "Поднимает вас в верх с авто-взлётом и прыжком"
)
public class ElytraJump extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "Быстрый", "Быстрый", "ReallyWorld");

    private final BooleanSetting auto = new BooleanSetting("Умный свап", false);
    private boolean hasFiredOnStart = false;

    public ElytraJump() {
        this.addSettings(mode, auto);
    }

    private int getItemSlot(net.minecraft.item.Item item) {
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

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventUpdate)) {
            handleNonUpdateEvents(event);
            return false;
        }

        if (!mc.player.abilities.isFlying
                && mc.player.isOnGround()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA
                && !mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.player.jump();
        }

        if (!mc.player.abilities.isFlying
                && !mc.player.isOnGround()
                && !mc.player.isInWater()
                && !mc.player.isElytraFlying()
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {

            mc.player.startFallFlying();
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
        }

        if (mc.player.isOnGround() || mc.player.isInWater() || mc.player.isInLava()) {
            hasFiredOnStart = false;
        }

        if (mc.player.hurtTime > 0 && auto.get()
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
            swapToChestplate();
            return false;
        }

        if (mc.player.movementInput != null
                && getItemSlot(Items.ELYTRA) == 38
                && mc.player.movementInput.jump
                && !mc.player.isElytraFlying()) {
            mc.player.movementInput.jump = false;
        }

        if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
            MoveUtil.setMotion(0);
            mc.gameSettings.keyBindJump.setPressed(true);

            if (mc.player.isElytraFlying()) {
                if (mode.is("ReallyWorld")) {
                    mc.player.getMotion().y += 0.02658f;
                } else {
                    mc.player.getMotion().y += MathUtil.randomizeFloat(0.06f, 0.061f);
                }
            }
        } else if (auto.get()) {
            swapToElytra();
        }

        return false;
    }

    private void handleNonUpdateEvents(Event event) {

        if (event instanceof EventMotion motionEvent && mc.player.isElytraFlying()) {
            AttackAura aura = Manager.FUNCTION_MANAGER.auraFunction;
            float targetYaw = aura.rotate != null ? aura.rotate.x : mc.player.rotationYaw;
            float targetPitch = 0.0f;

            motionEvent.setYaw(targetYaw);
            motionEvent.setPitch(targetPitch);

            mc.player.rotationYaw = targetYaw;
            mc.player.rotationPitch = targetPitch;
            mc.player.rotationYawHead = targetYaw;
            mc.player.renderYawOffset = targetYaw;
        }
    }

    @Override
    public void onEnable() {
        hasFiredOnStart = false;
        if (auto.get()) {
            swapToElytra();
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (auto.get() && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
            swapToChestplate();
        }
        super.onDisable();
    }

    private void swapToElytra() {
        int elytraSlot = getItemSlot(Items.ELYTRA);
        if (elytraSlot != -1) {
            InventoryUtils.moveItem(elytraSlot, 6);
        }
    }

    private void swapToChestplate() {
        int chestplateSlot = getChestPlateSlot();
        if (chestplateSlot != -1) {
            InventoryUtils.moveItem(chestplateSlot, 6);
        }
    }
}
