package nuclear.module.impl.player;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.world.PotionUtil;

import java.util.function.Supplier;

import static net.minecraft.client.Minecraft.player;

@Annotation(name = "AutoPotion", type = TypeList.Player, desc = "Кидает взрывные зелье под себя")
public class AutoPotion extends Module {
    private static MultiBoxSetting potions = new MultiBoxSetting("Бафать",
            new BooleanSetting("Силу", true),
            new BooleanSetting("Скорость", true),
            new BooleanSetting("Огнестойкость", true),
            new BooleanSetting("Исцеление", false));
    private SliderSetting healthThreshold = new SliderSetting("Здоровье", 8, 1, 20, 0.5f).setVisible(() -> potions.get("Исцеление"));
    private BooleanSetting onlyPvP = new BooleanSetting("Только в PVP", false);
    public boolean isActive;
    private int selectedSlot;
    private float previousPitch;
    private TimerUtil time = new TimerUtil();
    private PotionUtil potionUtil = new PotionUtil();

    public AutoPotion() {
        addSettings(potions, healthThreshold, onlyPvP);
    }

    public boolean isActivePotion;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (player.isElytraFlying() || (player.isHandActive() && !player.isBlocking())) {
                return false;
            }
            if (this.isActive()) {
                for (PotionType potionType : PotionType.values()) {
                    isActivePotion = potionType.isEnabled();
                }
            } else {
                isActivePotion = false;
            }

            if (this.isActive() && previousPitch == player.getLastReportedPitch()) {
                int oldItem = player.inventory.currentItem;
                this.selectedSlot = -1;

                for (PotionType potionType : PotionType.values()) {
                    if (potionType.isEnabled()) {
                        int slot = this.findPotionSlot(potionType);
                        if (this.selectedSlot == -1) {
                            this.selectedSlot = slot;
                        }

                        this.isActive = true;
                    }
                }

                if (this.selectedSlot > 8) {
                    mc.playerController.pickItem(this.selectedSlot);
                }

                player.connection.sendPacket(new CHeldItemChangePacket(oldItem));
            }

            if (time.hasTimeElapsed(500L)) {
                try {
                    this.reset();
                    this.selectedSlot = -2;
                } catch (Exception ignored) {
                }
            }

            this.potionUtil.changeItemSlot(this.selectedSlot == -2);
        }
        if (event instanceof EventMotion e) {
            if (!this.isActive()) {
                return false;
            }

            float[] angles = new float[]{player.rotationYaw, 90.0F};
            this.previousPitch = 90.0F;
            e.setYaw(angles[0]);
            e.setPitch(this.previousPitch);
            player.rotationPitchHead = this.previousPitch;
            player.rotationYawHead = angles[0];
            player.renderYawOffset = angles[0];
        }
        return false;
    }


    private void reset() {
        for (PotionType potionType : PotionType.values()) {
            if (potionType.isPotionSettingEnabled().get()) {
                potionType.setEnabled(this.isPotionActive(potionType));
            }
        }
    }

    private int findPotionSlot(PotionType type) {
        int hbSlot = this.getPotionIndexHb(type.getPotionId());
        if (hbSlot != -1) {
            this.potionUtil.setPreviousSlot(player.inventory.currentItem);
            player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            PotionUtil.useItem(Hand.MAIN_HAND);
            type.setEnabled(false);
            time.reset();
            return hbSlot;
        } else {
            int invSlot = this.getPotionIndexInv(type.getPotionId());
            if (invSlot != -1) {
                this.potionUtil.setPreviousSlot(player.inventory.currentItem);
                mc.playerController.pickItem(invSlot);
                PotionUtil.useItem(Hand.MAIN_HAND);
                player.connection.sendPacket(new CHeldItemChangePacket(player.inventory.currentItem));
                type.setEnabled(false);
                time.reset();
                return invSlot;
            } else {
                return -1;
            }
        }
    }

    public boolean isActive() {
        for (PotionType potionType : PotionType.values()) {
            if (potionType.isPotionSettingEnabled().get() && potionType.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private boolean isPotionActive(PotionType type) {
        // Для исцеления проверяем здоровье вместо эффекта
        if (type == PotionType.HEALING) {
            // Проверяем настройку "Только в PVP"
            if (onlyPvP.get() && !hasNearbyEnemies()) {
                this.isActive = false;
                return false;
            }
            
            // getHealth() возвращает здоровье в half-hearts (1 heart = 2 HP)
            // healthThreshold в hearts, поэтому делим на 2
            float currentHealthHearts = player.getHealth() / 2.0f;
            float threshold = healthThreshold.getValue().floatValue();
            
            // Проверяем порог здоровья и наличие зелья
            boolean shouldHeal = currentHealthHearts <= threshold;
            boolean hasPotion = this.getPotionIndexInv(type.getPotionId()) != -1 || this.getPotionIndexHb(type.getPotionId()) != -1;
            
            if (!shouldHeal) {
                this.isActive = false;
                return false;
            }
            
            return hasPotion;
        }
        
        // Для остальных зелий проверяем наличие эффекта
        if (player.isPotionActive(type.getPotion())) {
            this.isActive = false;
            return false;
        } else {
            return this.getPotionIndexInv(type.getPotionId()) != -1 || this.getPotionIndexHb(type.getPotionId()) != -1;
        }
    }

    private boolean hasNearbyEnemies() {
        if (mc.world == null || player == null) return false;
        
        for (PlayerEntity nearbyPlayer : mc.world.getPlayers()) {
            if (nearbyPlayer instanceof ClientPlayerEntity) continue;
            if (nearbyPlayer == player) continue;
            if (!nearbyPlayer.isAlive()) continue;
            if (nearbyPlayer.getDistance(player) > 50.0) continue; // Проверяем в радиусе 50 блоков
            
            // Проверяем, не является ли игрок другом
            String playerName = nearbyPlayer.getName().getString();
            if (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(playerName)) continue;
            
            // Найден вражеский игрок поблизости
            return true;
        }
        
        return false;
    }

    private int getPotionIndexHb(int id) {
        for (int i = 0; i < 9; ++i) {
            for (EffectInstance potion : net.minecraft.potion.PotionUtils.getEffectsFromStack(player.inventory.getStackInSlot(i))) {
                if (potion.getPotion() == Effect.get(id) && player.inventory.getStackInSlot(i).getItem() == Items.SPLASH_POTION) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int getPotionIndexInv(int id) {
        for (int i = 9; i < 36; ++i) {
            for (EffectInstance potion : net.minecraft.potion.PotionUtils.getEffectsFromStack(player.inventory.getStackInSlot(i))) {
                if (potion.getPotion() == Effect.get(id) && player.inventory.getStackInSlot(i).getItem() == Items.SPLASH_POTION) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    protected void onDisable() {
        isActive = false;
        super.onDisable();
    }

    enum PotionType {
        STRENGHT(Effects.STRENGTH, 5, () -> potions.get(0)),
        SPEED(Effects.SPEED, 1, () -> potions.get(1)),
        FIRE_RESIST(Effects.STRENGTH, 12, () -> potions.get(2)),
        HEALING(Effects.INSTANT_HEALTH, 6, () -> potions.get(3));

        private final Effect potion;
        private final int potionId;
        private final Supplier<Boolean> potionSetting;
        private boolean enabled;

        PotionType(Effect potion, int potionId, Supplier<Boolean> potionSetting) {
            this.potion = potion;
            this.potionId = potionId;
            this.potionSetting = potionSetting;
        }

        public Effect getPotion() {
            return this.potion;
        }

        public int getPotionId() {
            return this.potionId;
        }

        public Supplier<Boolean> isPotionSettingEnabled() {
            return this.potionSetting;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean var1) {
            this.enabled = var1;
        }

    }
}