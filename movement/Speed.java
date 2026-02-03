package nuclear.module.impl.movement;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.move.MoveUtil;

import static net.minecraft.client.Minecraft.player;
import static nuclear.utils.move.MoveUtil.forward;

@Annotation(name = "Speed", type = TypeList.Movement, desc = "Ускоряет ваше движение")
public class Speed extends Module {
    public ModeSetting mode = new ModeSetting("Режим", "Collision", new String[]{"Collision", "Matrix", "MetaHvH", "HolyWorld"});
    public SliderSetting speed3 = new SliderSetting("Скорocть", 0.36f, 0.10f, 0.7f, 0.01f).setVisible(() -> mode.is("Matrix"));
    public SliderSetting speed4 = new SliderSetting("Скорость", 1.1f, 0.5f, 2, 0.05f).setVisible(() -> mode.is("Collision"));

    public BooleanSetting autojump = new BooleanSetting("Авто прыжок", true).setVisible(() -> mode.is("Matrix") || mode.is("MetaHvH"));

    public Speed() {
        this.addSettings(mode, autojump, speed3, speed4);
    }

    public boolean onEvent(Event event) {
        if (!mc.player.abilities.isFlying) {
            if (event instanceof EventUpdate && (mode.is("Collision") || mode.is("Matrix") || mode.is("MetaHvH") || mode.is("HolyWorld"))) {
                if (mode.is("Matrix")) {
                    if (!mc.player.isElytraFlying() && !mc.player.isInWater() && !player.areEyesInFluid(FluidTags.WATER) && !mc.player.isOnGround()) {
                        MoveUtil.setSpeed(speed3.getValue().floatValue());
                    }
                    if (autojump.get()) {
                        if (MoveUtil.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.player.jump();
                        }
                    }
                }
                if (mode.is("MetaHvH")) {
                    float currentSpeed;
                    EffectInstance spd = mc.player.getActivePotionEffect(Effects.SPEED);
                    if (spd != null) {
                        if (spd.getAmplifier() == 0) {
                            currentSpeed = (0.358f) * 1.2630f;
                        } else if (spd.getAmplifier() == 1) {
                            currentSpeed = (0.358f) * 1.4530f;
                        } else if (spd.getAmplifier() >= 2) {
                            currentSpeed = (0.358f) * 1.6520f;
                        } else {
                            currentSpeed = 0.358f;
                        }
                    } else {
                        currentSpeed = 0.358f;
                    }
                    if (!mc.player.isElytraFlying() && !mc.player.isInWater() && !player.areEyesInFluid(FluidTags.WATER) && !mc.player.isOnGround()) {
                        MoveUtil.setSpeed(currentSpeed);
                    }
                    if (autojump.get()) {
                        if (MoveUtil.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.player.jump();
                        }
                    }
                }
                if (mode.is("Collision")) {
                    AxisAlignedBB aabb = mc.player.getBoundingBox().grow(0.1);
                    boolean canBoost = mc.world.getEntitiesWithinAABB(
                            LivingEntity.class,
                            aabb,
                            entity -> !(entity instanceof ArmorStandEntity)
                    ).size() > 1;
                    if (canBoost && !mc.player.isOnGround()) {
                        mc.player.jumpMovementFactor = speed4.getValue().floatValue() / 10;
                    }
                }
                if (mode.is("HolyWorld")) {
                    if (MoveUtil.isMoving()) {
                        AxisAlignedBB near = mc.player.getBoundingBox().grow(0.34, 0.15, 0.34);
                        int playersNearby = mc.world.getEntitiesWithinAABB(PlayerEntity.class, near, entity -> entity != mc.player && entity.isAlive()).size();

                        if (playersNearby <= 0) return false;

                        double boost = Math.max(0.0, 0.045);
                        double[] dir = forward(boost);
                        mc.player.addVelocity(dir[0], 0.0, dir[1]);
                    }
                }
            }

        }
        return false;
    }

    public void onDisable() {
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }
}