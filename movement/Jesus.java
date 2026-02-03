package nuclear.module.impl.movement;

import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.combat.AttackAura;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.move.MoveUtil;

@Annotation(name = "Jesus", type = TypeList.Movement, desc = "Ходьба по воде и лаве")
public class Jesus extends Module {
    private final SliderSetting speed = new SliderSetting("Скорость", 0.3F, 0.1F, 0.8F, 0.01F);
    private final SliderSetting boostSpeed = new SliderSetting("Скорость буста", 0.55F, 0.3F, 1.5F, 0.01F);
    private final SliderSetting boostTime = new SliderSetting("Время буста", 3F, 1F, 7F, 1F);
    private final BindSetting boostKey = new BindSetting("Ускорение", -1);
    public final BooleanSetting nocollision = new BooleanSetting("Не приземлятся", true);
    private boolean boostActive = false;
    private int boostTicks = 0;
    private boolean wasSneaking = false;

    public Jesus() {
        addSettings(speed, boostKey, boostSpeed, boostTime, nocollision);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey) {
            EventKey e = (EventKey) event;
            if (e.key == boostKey.getKey()) {
                boostActive = true;
                boostTicks = boostTime.getValue().intValue() * 20;
            }
        }


        if (event instanceof EventUpdate) {
            if (mc.player == null || mc.world == null) return false;
            if (mc.player.isInWater() || mc.player.isInLava()) {
                if (AttackAura.target != null) {
                    if (!mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.gameSettings.keyBindJump.setPressed(true);
                    }
                }
                if (!nocollision.get() && mc.player.collidedHorizontally) return false;
                if (!MoveUtil.isMoving()) {
                    mc.player.setMotion(0.0, mc.player.getMotion().y, 0.0);
                }

                float currentSpeed;

                if (boostActive) {
                    if (boostTicks > 0) {
                        currentSpeed = boostSpeed.getValue().floatValue();
                        boostTicks--;
                    } else {
                        boostActive = false;
                        currentSpeed = speed.getValue().floatValue();
                    }
                } else {
                    EffectInstance spd = mc.player.getActivePotionEffect(Effects.SPEED);
                    if (spd != null) {
                        if (spd.getAmplifier() == 0) {
                            currentSpeed = (speed.getValue().floatValue()) * 1.2630f;
                        } else if (spd.getAmplifier() == 1) {
                            currentSpeed = (speed.getValue().floatValue()) * 1.3530f;
                        } else if (spd.getAmplifier() >= 2) {
                            currentSpeed = (speed.getValue().floatValue()) * 1.5520f;
                        } else {
                            currentSpeed = speed.getValue().floatValue();
                        }
                    } else {
                        currentSpeed = speed.getValue().floatValue();
                    }
                }

                EffectInstance slow = mc.player.getActivePotionEffect(Effects.SLOWNESS);

                if (slow != null) {
                    currentSpeed *= 0.85F;
                }

                MoveUtil.setSpeed(currentSpeed);

                double ySpeed = mc.gameSettings.keyBindJump.isKeyDown() ? 0.019 : 0.0031;

                mc.player.setMotion(mc.player.getMotion().x, ySpeed, mc.player.getMotion().z);
            } else {
                boostActive = false;
                boostTicks = 0;
                if (wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                    wasSneaking = false;
                }
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        boostActive = false;
        boostTicks = 0;
        if (wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            wasSneaking = false;
        }
        super.onDisable();
    }
}
