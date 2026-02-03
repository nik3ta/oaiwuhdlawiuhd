package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;

import java.util.Random;

@Annotation(name = "AntiAfk", type = TypeList.Player)
public class AntiAfk extends Module {

    private final BooleanSetting withMove = new BooleanSetting("Движение", true);
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil actionTimer = new TimerUtil();
    private final Random random = new Random();
    
    private int movementDuration = 0;
    private int movementTick = 0;
    private int nextMovementDelay = 0;
    private boolean isMovingPeriod = false;

    public AntiAfk() {
        addSettings(withMove);
        nextMovementDelay = random.nextInt(401) + 500;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (withMove.get()) {
                releaseAllKeys();
                
                if (!isMovingPeriod) {
                    if (actionTimer.hasTimeElapsed(nextMovementDelay)) {
                        isMovingPeriod = true;
                        movementDuration = random.nextInt(4) + 2;
                        movementTick = 0;
                        actionTimer.reset();
                    }
                } else {
                    if (movementTick < movementDuration) {
                        executeRandomMovement();
                        movementTick++;
                    } else {
                        isMovingPeriod = false;
                        nextMovementDelay = random.nextInt(101) + 100;
                        actionTimer.reset();
                    }
                }
            } else {
                if (!MoveUtil.isMoving()) {
                    if (timerUtil.hasTimeElapsed(15000)) {
                        mc.player.sendChatMessage("/" + generateRandomString(5));

                        mc.rightClickMouse();

                        timerUtil.reset();
                    }
                } else {
                    timerUtil.reset();
                }
            }
        }
        return false;
    }
    
    private void executeRandomMovement() {
        if (random.nextBoolean()) {
            mc.gameSettings.keyBindForward.setPressed(true);
        }
        if (random.nextBoolean()) {
            mc.gameSettings.keyBindBack.setPressed(true);
        }
        if (random.nextBoolean()) {
            mc.gameSettings.keyBindLeft.setPressed(true);
        }
        if (random.nextBoolean()) {
            mc.gameSettings.keyBindRight.setPressed(true);
        }
        if (random.nextBoolean() && mc.player.isOnGround()) {
            mc.gameSettings.keyBindJump.setPressed(true);
        }
    }
    
    private void releaseAllKeys() {
        mc.gameSettings.keyBindForward.setPressed(false);
        mc.gameSettings.keyBindBack.setPressed(false);
        mc.gameSettings.keyBindLeft.setPressed(false);
        mc.gameSettings.keyBindRight.setPressed(false);
        mc.gameSettings.keyBindJump.setPressed(false);
    }

    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }
}
