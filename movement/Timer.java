package nuclear.module.impl.movement;

import net.minecraft.util.math.MathHelper;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;

@Annotation(name = "Timer", type = TypeList.Movement)
public class Timer extends Module {


    public SliderSetting timerAmount = new SliderSetting("Скорость", 2, 1, 10, 0.01f);

    public BooleanSetting smart = new BooleanSetting("Умный",  true);
    public SliderSetting upValue = new SliderSetting("Добавлять в движении", 0.02f, 0.01f, 0.5f, 0.01f);

    public SliderSetting ticks = new SliderSetting("Скорость убывания", 1.0f, 0.15f, 3.0f, 0.1f);

    public float maxViolation = 100.0F;
    private float violation = 0.0F;
    private double prevPosX, prevPosY, prevPosZ;
    private float yaw;
    private float pitch;
    public float animWidth;

    private boolean isBoost;

    private TimerUtil timerUtil = new TimerUtil();

    public Timer() {
        addSettings(timerAmount, smart, upValue, ticks);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate) {
            handleEventUpdate();
        }
        return false;
    }


    private void handleEventUpdate() {
        if (timerUtil.hasTimeElapsed(25000)) {
            reset();
            timerUtil.reset();
        }
        if (!mc.player.isOnGround() && !isBoost) {
            this.violation += 0.1f;
            this.violation = MathHelper.clamp(this.violation, 0.0F, this.maxViolation / (this.timerAmount.getValue().floatValue()));
        }

        mc.timer.timerSpeed = this.timerAmount.getValue().floatValue();

        // Пропускаем функцию при умном режиме или если значение таймера <= 1.0F
        if (!this.smart.get() || mc.timer.timerSpeed <= 1.0F) {
            return;
        }
        // Увеличение значения ticks и нарушение speed
        if (this.violation < (this.maxViolation) / (this.timerAmount.getValue().floatValue())) {
            this.violation += this.ticks.getValue().floatValue();
            this.violation = MathHelper.clamp(this.violation, 0.0F, this.maxViolation / (this.timerAmount.getValue().floatValue()));
        } else {
            // Сбрасывает speed, если достигнуто максимальное нарушение
            this.resetSpeed();
        }
    }

    /**
     * Обновляет таймер и устанавливает новые значения положения и поворота игрока.
     *
     * @param yaw   Значение поворота по горизонтали
     * @param pitch Значение поворота по вертикали
     * @param posX  Координата X положения игрока
     * @param posY  Координата Y положения игрока
     * @param posZ  Координата Z положения игрока
     */
    public void updateTimer(float yaw, float pitch, double posX, double posY, double posZ) {
        // Проверяет, находится ли игрок в том же местоположении
        if (this.notMoving()) {
            // Уменьшает speed на основе ticks и константы 0.4

            this.violation = (float) ((double) this.violation - ((double) this.ticks.getValue().floatValue() + 0.4));

            this.violation -= this.upValue.getValue().floatValue();
        }

        // Ограничивает speed в пределах диапазона
        this.violation = (float) MathHelper.clamp(this.violation, 0.0, Math.floor(this.maxViolation));

        // Устанавливает новые значения положения и поворота
        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Проверяет, находится ли игрок в покое.
     */
    private boolean notMoving() {
        // Проверяет, изменились ли текущие координаты и ротация с прошлого
        return this.prevPosX == mc.player.getPosX()
                && this.prevPosY == mc.player.getPosY()
                && this.prevPosZ == mc.player.getPosZ()
                && this.yaw == mc.player.rotationYaw
                && this.pitch == mc.player.rotationPitch;
    }


    /**
     * Возвращает текущее значение violation.
     */
    public float getViolation() {
        return this.violation;
    }

    /**
     * Сбрасывает скорость в нормальное.
     */
    public void resetSpeed() {
        // Сбрасывает скорость в нормальное
        this.setState(false);
        mc.timer.timerSpeed = 1.0F;
    }

    public void reset() {
    }

    @Override
    public void onDisable() {
        reset();
        // Сбрасывает скорость таймера при отключении
        mc.timer.timerSpeed = 1;
        timerUtil.reset();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        reset();
        // Устанавливает скорость таймера в 1.0F при включении
        mc.timer.timerSpeed = 1.0F;
        super.onEnable();
    }
}