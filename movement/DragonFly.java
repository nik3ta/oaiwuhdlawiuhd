package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMove;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.move.MoveUtil;

@Annotation(name = "DragonFly", type = TypeList.Movement, desc = "Ускоряет ваш полёт")
public class DragonFly extends Module {

    public final ModeSetting mode = new ModeSetting("Тип", "ReallyWorld", "ReallyWorld", "Custom");
    public final SliderSetting speedXZ = new SliderSetting("Скорость XZ", 1.15F, 0.5F, 3.0F, 0.01F).setVisible(() -> mode.is("Custom"));
    public final SliderSetting speedY = new SliderSetting("Скорость Y", 1.2F, 0.1F, 3.0F, 0.01F).setVisible(() -> mode.is("Custom"));

    public DragonFly() {
        addSettings(mode, speedXZ, speedY);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventMove move) {
            handleDragonFly(move);
        }
        return false;
    }

    private void handleDragonFly(EventMove move) {
        if (mc.player.abilities.isFlying) {

            switch (mode.get()) {
                case "ReallyWorld":
                    handleReallyWorldMode(move);
                    break;
                case "Custom":
                    handleCustomMode(move);
                    break;
            }
        }
    }
    private void handleReallyWorldMode(EventMove move) {
        if (ClientUtils.isConnectedToServer("cakeworld")) {
            float speedYValue = 1.2f;
            float speedXZValueUp = 1.109399f;
            float speedXZValueNormal = 1.111f;

            if (!mc.player.isSneaking() && mc.gameSettings.keyBindJump.isKeyDown()) {
                move.motion().y = speedYValue;
            }
            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                move.motion().y = -speedYValue;
            }

            if (mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindSneak.isKeyDown()) {
                MoveUtil.MoveEvent.setMoveMotion(move, speedXZValueUp);
            } else {
                MoveUtil.MoveEvent.setMoveMotion(move, speedXZValueNormal);
            }
        } else {

            float speedYMoving = 0.49f;
            float speedYNotMoving = 1.191f;

            float speedXZValueUp = 1.095399f;
            float speedXZValueNormal = 1.1725f;

            if (!mc.player.isSneaking() && mc.gameSettings.keyBindJump.isKeyDown()) {
                if (MoveUtil.isMoving()) {
                    move.motion().y = speedYMoving;
                } else {
                    move.motion().y = speedYNotMoving;
                }
            }
            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                if (MoveUtil.isMoving()) {
                    move.motion().y = -speedYMoving;
                } else {
                    move.motion().y = -speedYNotMoving;
                }
            }

            if (mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindSneak.isKeyDown()) {
                MoveUtil.MoveEvent.setMoveMotion(move, speedXZValueUp);
            } else {
                MoveUtil.MoveEvent.setMoveMotion(move, speedXZValueNormal);
            }
        }
    }
    private void handleCustomMode(EventMove move) {
        float customSpeedY = speedY.getValue().floatValue();
        float customSpeedXZ = speedXZ.getValue().floatValue();
        if (!mc.player.isSneaking() && mc.gameSettings.keyBindJump.isKeyDown()) {
            move.motion().y = customSpeedY;
        }
        if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            move.motion().y = -customSpeedY;
        }

        if (mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindSneak.isKeyDown()) {
            MoveUtil.MoveEvent.setMoveMotion(move, customSpeedY);
        } else {
            MoveUtil.MoveEvent.setMoveMotion(move, customSpeedXZ);
        }
    }
}