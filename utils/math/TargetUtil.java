package nuclear.utils.math;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import nuclear.control.Manager;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.IMinecraft;

public class TargetUtil implements IMinecraft {

    public static boolean isPlayerTarget(Entity entity, MultiBoxSetting settings, boolean considerNaked) {
        if (!(entity instanceof PlayerEntity player) || entity == mc.player) return false;
        boolean isFriend = Manager.FRIEND_MANAGER.isFriend(entity.getName().getString());
        boolean isNaked = player.getTotalArmorValue() == 0;
        if (isFriend) {
            return settings.get("Друзей");
        }
        if (considerNaked && isNaked) {
            return settings.get("Голых");
        }
        return settings.get("Игроков");
    }

    public static boolean isPlayerTarget(Entity entity, MultiBoxSetting settings) {
        return isPlayerTarget(entity, settings, true);
    }

    public static boolean isVillagerTarget(LivingEntity entity, MultiBoxSetting settings) {
        return entity instanceof VillagerEntity && settings.get("Жителей");
    }

    public static boolean isAnimalTarget(LivingEntity entity, MultiBoxSetting settings) {
        return entity instanceof AnimalEntity && settings.get("Животных");
    }

    public static boolean isMobTarget(LivingEntity entity, MultiBoxSetting settings) {
        return entity instanceof MobEntity && settings.get("Мобов");
    }

    public static boolean isMonsterTarget(LivingEntity entity, MultiBoxSetting settings) {
        return entity instanceof MobEntity && !(entity instanceof AnimalEntity) && !(entity instanceof VillagerEntity) && settings.get("Монстров");
    }

    public static boolean isSelfTarget(Entity entity, MultiBoxSetting settings) {
        return entity == mc.player && settings.get("Себя") && !mc.gameSettings.getPointOfView().func_243192_a();
    }

    public static boolean isItemTarget(Entity entity, MultiBoxSetting settings) {
        return entity instanceof ItemEntity && settings.get("Предметы");
    }


    public static boolean isEntityTarget(Entity entity, MultiBoxSetting settings) {
        if (entity instanceof LivingEntity livingEntity) {
            return isSelfTarget(entity, settings) ||
                    isPlayerTarget(entity, settings, true) ||
                    isVillagerTarget(livingEntity, settings) ||
                    isAnimalTarget(livingEntity, settings) ||
                    isMonsterTarget(livingEntity, settings);
        }
        return isItemTarget(entity, settings);
    }
}
