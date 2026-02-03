package nuclear.module.impl.player;

import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Annotation(name = "ChestStealer", type = TypeList.Player)
public class ChestStealer extends Module {
    private final BooleanSetting randomOrder = new BooleanSetting("Рандомный порядок", false);
    private final BooleanSetting miss = new BooleanSetting("Промах", false);
    private final BooleanSetting closeEmpty = new BooleanSetting("Закрывать пустой", true);
    private final BooleanSetting closeFull = new BooleanSetting("Закрывать полный инв", false);
    private final SliderSetting delay = new SliderSetting("Задержка", 70f, 0f, 150f, 10f);

    private final TimerUtil timer = new TimerUtil();

    public ChestStealer() {
        addSettings(randomOrder, miss, closeEmpty, closeFull, delay);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null || mc.world == null) return false;

        if (event instanceof EventUpdate) {
            if (mc.player.openContainer instanceof ChestContainer) {
                handleChest((ChestContainer) mc.player.openContainer);
            }
        }
        return false;
    }

    private void handleChest(ChestContainer c) {
        List<Integer> slots = new ArrayList<>();
        int chestSize = c.getLowerChestInventory().getSizeInventory();

        for (int i = 0; i < chestSize; i++) {
            if (!c.getLowerChestInventory().getStackInSlot(i).isEmpty()) {
                slots.add(i);
            }
        }

        if (randomOrder.get()) {
            Collections.shuffle(slots, ThreadLocalRandom.current());
        }

        for (int i : slots) {
            if (timer.hasTimeElapsed(delay.getValue().longValue())) {
                mc.playerController.windowClick(c.windowId, i, 0, ClickType.QUICK_MOVE, mc.player);
                timer.reset();
                break;
            }
        }

        if (miss.get()) {
            for (int i = 0; i < chestSize; i++) {
                if (c.getLowerChestInventory().getStackInSlot(i).isEmpty() &&
                        ThreadLocalRandom.current().nextDouble() < 0.1 &&
                        mc.player.ticksExisted % 30 == 0) {
                    mc.playerController.windowClick(c.windowId, i, 0, ClickType.PICKUP, mc.player);
                    break;
                }
            }
        }

        boolean chestEmpty = c.getLowerChestInventory().isEmpty();
        boolean inventoryFull = mc.player.inventory.getFirstEmptyStack() == -1;

        if ((closeEmpty.get() && chestEmpty) || (closeFull.get() && inventoryFull)) {
            mc.player.closeScreen();
        }
    }
}