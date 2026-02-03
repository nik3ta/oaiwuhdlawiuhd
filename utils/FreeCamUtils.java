package nuclear.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.util.FoodStats;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.SoundEvent;

import java.util.UUID;

public class FreeCamUtils extends ClientPlayerEntity implements IMinecraft {

    private static final ClientPlayNetHandler NETWORK_HANDLER = new ClientPlayNetHandler(IMinecraft.mc, IMinecraft.mc.currentScreen, IMinecraft.mc.getConnection().getNetworkManager(), new GameProfile(UUID.randomUUID(), "user001")) {
        @Override
        public void sendPacket(IPacket<?> packetIn) {
            super.sendPacket(packetIn);
        }
    };

    public FreeCamUtils(int i) {
        super(IMinecraft.mc, IMinecraft.mc.world, NETWORK_HANDLER, IMinecraft.mc.player.getStats(), IMinecraft.mc.player.getRecipeBook(), false, false);

        setEntityId(i);
        movementInput = new MovementInputFromOptions(IMinecraft.mc.gameSettings);
    }

    public void spawn() {
        if (world != null) {
            world.addEntity(this);
        }
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (IMinecraft.mc.player != null) {
            this.inventory.copyInventory(IMinecraft.mc.player.inventory);
            this.setHealth(IMinecraft.mc.player.getHealth());
            this.setAbsorptionAmount(IMinecraft.mc.player.getAbsorptionAmount());
            this.copyMaxHealthFrom(IMinecraft.mc.player);
            this.copyFoodStatsFrom(IMinecraft.mc.player);
        }
    }

    @Override
    public void rotateTowards(double yaw, double pitch) {
        super.rotateTowards(yaw, pitch);
    }

    public void copyArmorFrom(ClientPlayerEntity other) {
        for (EquipmentSlotType slot : EquipmentSlotType.values()) {
            if (slot.getSlotType() == EquipmentSlotType.Group.ARMOR) {
                ItemStack otherArmor = other.getItemStackFromSlot(slot);
                this.setItemStackToSlot(slot, otherArmor.copy());
            }
        }

        ModifiableAttributeInstance thisArmor = this.getAttribute(Attributes.ARMOR);
        ModifiableAttributeInstance otherArmor = other.getAttribute(Attributes.ARMOR);
        if (thisArmor != null && otherArmor != null) {
            thisArmor.setBaseValue(otherArmor.getBaseValue());
            thisArmor.removeAllModifiers();
            for (AttributeModifier modifier : otherArmor.getModifierListCopy()) {
                thisArmor.applyNonPersistentModifier(new AttributeModifier(
                        modifier.getID(),
                        modifier.getName(),
                        modifier.getAmount(),
                        modifier.getOperation()
                ));
            }
        }

        ModifiableAttributeInstance thisToughness = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        ModifiableAttributeInstance otherToughness = other.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (thisToughness != null && otherToughness != null) {
            thisToughness.setBaseValue(otherToughness.getBaseValue());
            thisToughness.removeAllModifiers();
            for (AttributeModifier modifier : otherToughness.getModifierListCopy()) {
                thisToughness.applyNonPersistentModifier(new AttributeModifier(
                        modifier.getID(),
                        modifier.getName(),
                        modifier.getAmount(),
                        modifier.getOperation()
                ));
            }
        }
    }

    @Override
    public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
        this.inventory.setInventorySlotContents(slotIn.getIndex() + 5, stack); // Armor slots are offset in inventory
        if (slotIn.getSlotType() == EquipmentSlotType.Group.ARMOR) {
            this.onEquipmentChange(slotIn, ItemStack.EMPTY, stack, false);
        } else {
            super.setItemStackToSlot(slotIn, stack);
        }
    }

    private void onEquipmentChange(EquipmentSlotType slot, ItemStack oldStack, ItemStack newStack, boolean playSound) {
        if (!ItemStack.areItemStacksEqual(oldStack, newStack)) {
            if (playSound) {
                SoundEvent sound = newStack.getDrinkSound();
                if (sound != null && !newStack.isEmpty()) {
                    this.world.playSound(null, this.getPosX(), this.getPosY(), this.getPosZ(), sound, this.getSoundCategory(), 1.0F, 1.0F);
                }
            }
        }
    }

    public void copyMaxHealthFrom(ClientPlayerEntity other) {
        ModifiableAttributeInstance thisHealth = this.getAttribute(Attributes.MAX_HEALTH);
        ModifiableAttributeInstance otherHealth = other.getAttribute(Attributes.MAX_HEALTH);
        if (thisHealth != null && otherHealth != null) {
            thisHealth.setBaseValue(otherHealth.getBaseValue());
            thisHealth.removeAllModifiers();
            for (AttributeModifier modifier : otherHealth.getModifierListCopy()) {
                thisHealth.applyNonPersistentModifier(new AttributeModifier(
                        modifier.getID(),
                        modifier.getName(),
                        modifier.getAmount(),
                        modifier.getOperation()
                ));
            }
        }
    }

    public void copyFoodStatsFrom(ClientPlayerEntity other) {
        FoodStats otherFoodStats = other.getFoodStats();
        FoodStats thisFoodStats = this.getFoodStats();
        if (otherFoodStats != null && thisFoodStats != null) {
            thisFoodStats.setFoodLevel(otherFoodStats.getFoodLevel());
            thisFoodStats.setFoodSaturationLevel(otherFoodStats.getSaturationLevel());
        }
    }
}