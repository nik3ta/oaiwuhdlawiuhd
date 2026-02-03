package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Annotation(name = "NoInteract", type = TypeList.Player)
public class NoInteract extends Module {
    public BooleanSetting allBlocks = new BooleanSetting("Все блоки", "All blocks", false);
    public MultiBoxSetting ignoreInteract = new MultiBoxSetting("Игнорить",
            new BooleanSetting("Стойки", "Stands", true),
            new BooleanSetting("Сундуки", "Chests", true),
            new BooleanSetting("Двери", "Doors", true),
            new BooleanSetting("Кнопки", "Buttons", true),
            new BooleanSetting("Воронки", "Funnels", true),
            new BooleanSetting("Раздатчики", "Distributors", true),
            new BooleanSetting("Нотные блоки", "Note blocks", true),
            new BooleanSetting("Верстаки", "Workbenches", true),
            new BooleanSetting("Люки", "Hatches", true),
            new BooleanSetting("Печки", "Stoves", true),
            new BooleanSetting("Калитки", "Wickets", true),
            new BooleanSetting("Наковальни", "Anvils", true),
            new BooleanSetting("Рычаги", "Levers", true)).setVisible(() -> !allBlocks.get());

    public NoInteract() {
        addSettings(allBlocks, ignoreInteract);
    }


    public Set<Integer> getBlocks() {
        Set<Integer> blocks = new HashSet<>();
        addBlocksForInteractionType(blocks, 1, 147, 329, 270);
        addBlocksForInteractionType(blocks, 2, 173, 161, 485, 486, 487, 488, 489, 720, 721);
        addBlocksForInteractionType(blocks, 3, 183, 308, 309, 310, 311, 312, 313, 718, 719, 758);
        addBlocksForInteractionType(blocks, 4, 336);
        addBlocksForInteractionType(blocks, 5, 70, 342, 508);
        addBlocksForInteractionType(blocks, 6, 74);
        addBlocksForInteractionType(blocks, 7, 151);
        addBlocksForInteractionType(blocks, 8, 222, 223, 224, 225, 226, 227, 712, 713, 379);
        addBlocksForInteractionType(blocks, 9, 154, 670);
        addBlocksForInteractionType(blocks, 10, 250, 475, 476, 477, 478, 479, 714, 715);
        addBlocksForInteractionType(blocks, 11, 328, 327, 326);
        addBlocksForInteractionType(blocks, 12, 171);
        return blocks;
    }

    private void addBlocksForInteractionType(Set<Integer> blocks, int interactionType, Integer... blockIds) {
        if (ignoreInteract.get(interactionType)) {
            blocks.addAll(Arrays.asList(blockIds));
        }
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
