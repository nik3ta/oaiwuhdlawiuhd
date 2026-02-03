package nuclear.module.impl.combat;

import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "ItemRelease",
        type = TypeList.Combat, desc = "Автоматически выпускает предмет, когда он полностью натянут"
)
public class ItemRelease extends Module {

    private final MultiBoxSetting items = new MultiBoxSetting("Предметы",
            new BooleanSetting("Лук", true),
            new BooleanSetting("Трезубец", false),
            new BooleanSetting("Арбалет", true));

    private final SliderSetting tickBow = new SliderSetting("Задержка выстрела",  2.5F, 2.0F, 5F, 0.05F).setVisible(() -> items.get("Лук"));

    public ItemRelease() {
        this.addSettings(items, tickBow);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate eventUpdate) {
            if (items.get("Лук")) {
                if (mc.player.inventory.getCurrentItem().getItem() instanceof BowItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= tickBow.getValue().floatValue()) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                }
            }

            if (items.get("Трезубец")) {
                if (mc.player.inventory.getCurrentItem().getItem() instanceof TridentItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= 10) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                }
            }

            if (items.get("Арбалет")) {
                if (mc.player.inventory.getCurrentItem().getItem() instanceof CrossbowItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= CrossbowItem.getChargeTime(mc.player.inventory.getCurrentItem())) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                }
            }
        }
        return false;
    }
}