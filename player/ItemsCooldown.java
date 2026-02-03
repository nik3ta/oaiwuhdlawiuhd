package nuclear.module.impl.player;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventCalculateCooldown;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;

import java.util.*;
import java.util.function.Supplier;

@Annotation(name = "ItemsCooldown", type = TypeList.Player, desc = "Ставит задержку на использование предметов")
public class ItemsCooldown extends Module {

    public static final MultiBoxSetting items = new MultiBoxSetting("Предметы",
            new BooleanSetting("Геплы", true),
            new BooleanSetting("Перки", true),
            new BooleanSetting("Хорусы", false),
            new BooleanSetting("Чарки", false));

    public static final SliderSetting gappleTime = new SliderSetting("Кулдаун гепла", 4.5F, 0.5F, 10.0F, 0.05F)
            .setVisible(() -> items.get(0));
    private static final SliderSetting pearlTime = new SliderSetting("Кулдаун перок", 14F, 1.0F, 15.0F, 0.05F)
            .setVisible(() -> items.get(1));
    private static final SliderSetting horusTime = new SliderSetting("Кулдаун хорусов", 3F, 1.0F, 10.0F, 0.05F)
            .setVisible(() -> items.get(2));
    private static final SliderSetting enchantmentGappleTime = new SliderSetting("Кулдаун чарок", 4.5F, 1.0F, 10.0F, 0.05F)
            .setVisible(() -> items.get(3));
    private BooleanSetting onlyPvP = new BooleanSetting("Только в PvP", true);

    public HashMap<Item, Long> lastUseItemTime = new HashMap<>();
    private boolean wasPvP = false;

    public ItemsCooldown() {
        addSettings(items, gappleTime, enchantmentGappleTime, pearlTime, horusTime, onlyPvP);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventCalculateCooldown calculateCooldown) {
            applyItemCooldown(calculateCooldown);
        }
        boolean isPvP = ClientUtils.isPvP();
        if (wasPvP && !isPvP && onlyPvP.get()) {
            lastUseItemTime.clear();
        }
        wasPvP = isPvP;
        return false;
    }

    private void applyItemCooldown(EventCalculateCooldown calcCooldown) {
        List<Item> itemsToRemove = new ArrayList<>();

        for (Map.Entry<Item, Long> entry : lastUseItemTime.entrySet()) {
            ItemEnum itemEnum = ItemEnum.getItemEnum(entry.getKey());

            if (itemEnum == null || calcCooldown.itemStack != itemEnum.getItem() || !itemEnum.getActive().get() || isNotPvP()) {
                continue;
            }

            long time = System.currentTimeMillis() - entry.getValue();
            float timeSetting = itemEnum.getTime().get() * 1000.0F;

            if (time < timeSetting && itemEnum.getActive().get()) {
                calcCooldown.setCooldown(time / timeSetting);
            } else {
                itemsToRemove.add(itemEnum.getItem());
            }
        }

        itemsToRemove.forEach(lastUseItemTime::remove);
    }

    public boolean isNotPvP() {
        return onlyPvP.get() && !ClientUtils.isPvP();
    }

    public boolean canUseItem(Item item) {
        ItemEnum itemEnum = ItemEnum.getItemEnum(item);
        if (itemEnum == null || !itemEnum.getActive().get() || isNotPvP()) {
            return true;
        }

        Long lastUseTime = lastUseItemTime.get(item);
        if (lastUseTime == null) {
            return true;
        }

        long timeElapsed = System.currentTimeMillis() - lastUseTime;
        float cooldownTime = itemEnum.getTime().get() * 1000.0F;
        return timeElapsed >= cooldownTime;
    }

    public void onItemUsed(Item item) {
        ItemEnum itemEnum = ItemEnum.getItemEnum(item);
        if (itemEnum != null && itemEnum.getActive().get() && !isNotPvP()) {
            lastUseItemTime.put(item, System.currentTimeMillis());
        }
    }

    @Getter
    public enum ItemEnum {
        CHORUS(Items.CHORUS_FRUIT,
                () -> items.get(2),
                () -> horusTime.getValue().floatValue()),
        GOLDEN_APPLE(Items.GOLDEN_APPLE,
                () -> items.get(0),
                () -> gappleTime.getValue().floatValue()),
        ENCHANTED_GOLDEN_APPLE(Items.ENCHANTED_GOLDEN_APPLE,
                () -> items.get(3),
                () -> enchantmentGappleTime.getValue().floatValue()),
        ENDER_PEARL(Items.ENDER_PEARL,
                () -> items.get(1),
                () -> pearlTime.getValue().floatValue());

        private final Item item;
        private final Supplier<Boolean> active;
        private final Supplier<Float> time;

        ItemEnum(Item item, Supplier<Boolean> active, Supplier<Float> time) {
            this.item = item;
            this.active = active;
            this.time = time;
        }

        public static ItemEnum getItemEnum(Item item) {
            return Arrays.stream(ItemEnum.values())
                    .filter(e -> e.getItem() == item)
                    .findFirst()
                    .orElse(null);
        }
    }
}