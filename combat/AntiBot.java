package nuclear.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.control.events.impl.player.EventWorldChanged;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

import java.util.ArrayList;
import java.util.List;

@Annotation(
        name = "AntiBot",
        type = TypeList.Combat,
        desc = "Удаляет бота с сервера"
)
public class AntiBot extends Module {
    public static List<Entity> isBot = new ArrayList<>();

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            for (PlayerEntity entity : mc.world.getPlayers()) {
                if (mc.player != entity && entity.inventory != null && entity.inventory.armorInventory != null && entity.inventory.armorInventory.size() >= 4
                        && entity.inventory.armorInventory.get(0) != null && entity.inventory.armorInventory.get(0).getItem() != Items.AIR && entity.inventory.armorInventory.get(1) != null && entity.inventory.armorInventory.get(1).getItem() != Items.AIR && entity.inventory.armorInventory.get(2) != null && entity.inventory.armorInventory.get(2).getItem() != Items.AIR && entity.inventory.armorInventory.get(3) != null && entity.inventory.armorInventory.get(3).getItem() != Items.AIR && entity.inventory.armorInventory.get(0).isEnchantable()
                        && entity.inventory.armorInventory.get(1).isEnchantable() && entity.inventory.armorInventory.get(2).isEnchantable() && entity.inventory.armorInventory.get(3).isEnchantable()
                        && entity.getHeldItemOffhand() != null && entity.getHeldItemOffhand().getItem() == Items.AIR
                        && (entity.inventory.armorInventory.get(0).getItem() == Items.LEATHER_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.LEATHER_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.LEATHER_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.LEATHER_HELMET || entity.inventory.armorInventory.get(0).getItem() == Items.IRON_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.IRON_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.IRON_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.IRON_HELMET || entity.inventory.armorInventory.get(0).getItem() == Items.DIAMOND_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.DIAMOND_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.DIAMOND_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.DIAMOND_HELMET)
                        && (entity.getHeldItemMainhand() == null || entity.getHeldItemMainhand().getItem() == Items.AIR || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_SWORD || entity.getHeldItemMainhand().getItem() == Items.IRON_SWORD || entity.getHeldItemMainhand().getItem() == Items.FISHING_ROD || entity.getHeldItemMainhand().getItem() == Items.IRON_AXE || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_AXE || entity.getHeldItemMainhand().getItem() == Items.IRON_PICKAXE || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_PICKAXE || entity.getHeldItemMainhand().getItem() == Items.TORCH)
                        && entity.getHeldItemOffhand() != null && entity.getHeldItemOffhand().getItem() == Items.AIR && !entity.inventory.armorInventory.get(0).isDamaged() && !entity.inventory.armorInventory.get(1).isDamaged() && !entity.inventory.armorInventory.get(2).isDamaged() && !entity.inventory.armorInventory.get(3).isDamaged()
                        && entity.getFoodStats() != null && entity.getFoodStats().getFoodLevel() == 20) {

                    if (!isBot.contains(entity)) {
                        isBot.add(entity);
                    }

                    return false;
                }

                if (isBot.contains(entity)) {
                    isBot.remove(entity);
                }
            }
        }
        return false;
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && isBot.contains(entity);
    }

    @Override
    public void onDisable() {
        isBot.clear();
        super.onDisable();
    }
}



