package nuclear.module.impl.movement;

import net.minecraft.network.IPacket;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.move.MoveUtil;

import java.util.concurrent.CopyOnWriteArrayList;

@Annotation(name = "Flight", type = TypeList.Movement)
public class Flight extends Module {
    private final ModeSetting flMode = new ModeSetting("Flight Mode",
            "Motion",
            "Motion", "Glide");

    private final SliderSetting motion = new SliderSetting("Скорость по XZ", 1F, 0F, 8F, 0.1F).setVisible(() -> !flMode.is("OnlyHit"));
    private final SliderSetting motionY = new SliderSetting("Скорость по Y", 1F, 0F, 8F, 0.1F).setVisible(() -> !flMode.is("OnlyHit"));

    public CopyOnWriteArrayList<IPacket<?>> packets = new CopyOnWriteArrayList<>();

    public Flight() {
        addSettings(flMode, motion, motionY);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate) {
            handleFlyMode();
        }
        return false;
    }


    private void handleFlyMode() {
        switch (flMode.get()) {
            case "Motion" -> handleMotionFly();
            case "Glide" -> handleGlideFly();
        }
    }

    private void handleMotionFly() {
        final float motionY = this.motionY.getValue().floatValue();
        final float speed = this.motion.getValue().floatValue();

        mc.player.motion.y = 0;

        if (mc.gameSettings.keyBindJump.isPressed()) {
            mc.player.motion.y = motionY;
        } else if (mc.player.isSneaking()) {
            mc.player.motion.y = -motionY;
        }

        MoveUtil.setMotion(speed);
    }


    private void handleGlideFly() {
        mc.player.setVelocity(0, 0.023, 0);
        MoveUtil.setMotion(motion.getValue().floatValue());
    }

    @Override
    protected void onDisable() {
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }
}
