package nuclear.module.impl.other;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.MathHelper;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;

@Annotation(name = "AimingBalls", type = TypeList.Player, desc = "Наводит ваш прицел на ценные предметы")
public class AimingBalls extends Module {
    public MultiBoxSetting element = new MultiBoxSetting("Наводится на",
            new BooleanSetting("Шар", true),
            new BooleanSetting("Элитра", true),
            new BooleanSetting("Осколок", true),
            new BooleanSetting("Острота VI", false),
            new BooleanSetting("Анти полёт", false),
            new BooleanSetting("Аура", true),
            new BooleanSetting("Трезубец", false));
    public final BooleanSetting fall = new BooleanSetting("Искл ауру падение", true).setVisible(() -> element.get("Аура"));

    public AimingBalls() {
        addSettings(element, fall);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            for (Entity entity : mc.world.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack itemStack = itemEntity.getItem();
                    String displayName = itemStack.getDisplayName().getString();

                    if (element.get(0) && itemStack.getItem() instanceof SkullItem) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(1) && itemStack.getItem() instanceof ElytraItem) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(2) && itemStack.getItem() == Items.GHAST_TEAR && displayName.contains("Осколок")) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(3) && (itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof AxeItem) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, itemStack) >= 6) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(4) && itemStack.getItem() == Items.FIREWORK_STAR && displayName.contains("Анти Полёт")) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(5)) {
                        if ((itemStack.getItem() == Items.SUNFLOWER && displayName.contains("Аура Охотника")) ||
                                (itemStack.getItem() == Items.CLAY_BALL && displayName.contains("Аура Твёрдости Брони")) ||
                                (itemStack.getItem() == Items.POPPED_CHORUS_FRUIT && displayName.contains("Аура Телепортации")) ||
                                (itemStack.getItem() == Items.GOLD_NUGGET && displayName.contains("Аура Богача")) ||
                                (itemStack.getItem() == Items.WHITE_DYE && displayName.contains("Аура Защиты От Падения") && !fall.getValue()) ||
                                (itemStack.getItem() == Items.GHAST_TEAR && displayName.contains("Аура Защиты От Кристаллов"))) {
                            mc.player.rotationYaw = this.rotations(entity)[0];
                            mc.player.rotationPitch = this.rotations(entity)[1];
                        }
                    }
                    if (element.get(6) && itemStack.getItem() instanceof TridentItem) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                }
            }
        }

        return false;
    }

    public float[] rotations(Entity entity) {
        double x = entity.getPosX() - mc.player.getPosX();
        double y = entity.getPosY() - mc.player.getPosY() - 1F;
        double z = entity.getPosZ() - mc.player.getPosZ();
        double u = MathHelper.sqrt(x * x + z * z);
        float u2 = (float) (MathHelper.atan2(z, x) * 57.29577951308232 - 90.0);
        float u3 = (float) (-MathHelper.atan2(y, u) * 57.29577951308232);
        return new float[]{u2, u3};
    }
}