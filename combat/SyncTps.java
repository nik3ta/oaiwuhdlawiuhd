package nuclear.module.impl.combat;

import net.minecraft.util.math.MathHelper;
import nuclear.Nuclear;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "SyncTps", type = TypeList.Combat)
public class SyncTps extends Module {

    public SyncTps() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }

    /**
     * Получает текущий TPS сервера
     */
    public float getCurrentTPS() {
        if (Nuclear.getServerTPS() == null) {
            return 20.0f;
        }
        float tps = Nuclear.getServerTPS().getTPS();
        return MathHelper.clamp(tps, 0.1f, 20.0f);
    }

    /**
     * Вычисляет скорректированный кулдаун на основе TPS сервера
     * Когда TPS ниже 20, увеличиваем кулдаун, чтобы удары успевали регистрироваться
     * 
     * @param baseCooldown базовый кулдаун в миллисекундах
     * @return скорректированный кулдаун в миллисекундах
     */
    public long getAdjustedCooldown(long baseCooldown) {
        if (!state) {
            return baseCooldown;
        }

        float tps = getCurrentTPS();
        if (tps >= 20.0f) {
            return baseCooldown;
        }

        float multiplier = 20.0f / tps;
        // Добавляем дополнительный буфер для низких TPS
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long) (baseCooldown * multiplier * additionalFactor);
        
        return Math.min(adjusted, 3000);
    }

    /**
     * Проверяет, можно ли атаковать с учетом TPS
     * Возвращает true, если прошло достаточно времени с учетом лагов сервера
     * 
     * @param lastAttackTime время последней атаки
     * @param baseCooldown базовый кулдаун в миллисекундах
     * @param currentTime текущее время
     * @return true если можно атаковать
     */
    public boolean canAttack(long lastAttackTime, long baseCooldown, long currentTime) {
        if (!state) {
            return currentTime >= lastAttackTime + baseCooldown;
        }

        long adjustedCooldown = getAdjustedCooldown(baseCooldown);
        return currentTime >= lastAttackTime + adjustedCooldown;
    }
}
