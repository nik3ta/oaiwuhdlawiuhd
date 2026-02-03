package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "TargetEsp", type = TypeList.Render, desc = "Отображает таргет")
public class TargetEsp extends Module {
    public static ModeSetting targetesp = new ModeSetting("Мод", "Кольцо", "Ромб", "Кольцо", "Призраки", "Кристаллы");
    public static ModeSetting ghostType = new ModeSetting("Тип Призраков", "Кастом", "Кастом", "Тонкий", "Тянущуюся").setVisible(() -> targetesp.is("Призраки"));
    public static ModeSetting type = new ModeSetting("Ромб", "Тип 1", "Тип 1", "Тип 2", "Тип 3", "Тип 4").setVisible(() -> targetesp.is("Ромб"));
    public final SliderSetting size = new SliderSetting("Размeр", 85f, 60F, 100F, 5F).setVisible(() -> targetesp.is("Ромб"));
    public final SliderSetting distancee = new SliderSetting("Дистанция", 0.8f, 0.3F, 0.8F, 0.05F).setVisible(() -> targetesp.is("Призраки") && (ghostType.is("Кастом") || ghostType.is("Тонкий")));
    public final SliderSetting sizee = new SliderSetting("Размер", 0.15f, 0.1F, 0.3F, 0.05F).setVisible(() -> targetesp.is("Призраки") && (ghostType.is("Кастом") || ghostType.is("Тонкий") || ghostType.is("Тянущуюся")));
    public final SliderSetting distance = new SliderSetting("Длина", 17f, 1F, 20F, 1F).setVisible(() -> targetesp.is("Призраки") && (ghostType.is("Кастом") || ghostType.is("Тонкий")));
    public final SliderSetting alpha = new SliderSetting("Прозрачность", 14f, 0F, 20F, 1F).setVisible(() -> targetesp.is("Призраки") && (ghostType.is("Кастом") || ghostType.is("Тонкий") || ghostType.is("Тянущуюся")));
    public final SliderSetting speed = new SliderSetting("Скорость", 45f, 30F, 80F, 5F).setVisible(() -> targetesp.is("Призраки") && (ghostType.is("Кастом") || ghostType.is("Тонкий") || ghostType.is("Тянущуюся")));
    public final SliderSetting bmwGhostCount = new SliderSetting("Кол призраков", 3f, 2F, 5F, 1F).setVisible(() -> targetesp.is("Призраки") && ghostType.is("Тянущуюся"));
    public final SliderSetting bmwGhostTimer = new SliderSetting("Время жизни", 350f, 150F, 500F, 25F).setVisible(() -> targetesp.is("Призраки") && ghostType.is("Тянущуюся"));
    public final SliderSetting bmwStrengthXZ = new SliderSetting("Цикл по XZ", 2000f, 1000F, 5000F, 100F).setVisible(() -> targetesp.is("Призраки") && ghostType.is("Тянущуюся"));
    public final SliderSetting bmwStrengthY = new SliderSetting("Цикл по Y", 1700f, 1000F, 5000F, 100F).setVisible(() -> targetesp.is("Призраки") && ghostType.is("Тянущуюся"));
    public final SliderSetting crystalFade = new SliderSetting("Затухание", 1.0f, 0.0F, 1.0F, 0.01F).setVisible(() -> targetesp.is("Кристаллы"));
    public final SliderSetting crystalSize = new SliderSetting("Размер", 2.0f, 0.5F, 3.0F, 0.1F).setVisible(() -> targetesp.is("Кристаллы"));
    public final BooleanSetting hitred = new BooleanSetting("Краснеть при ударе", true).setVisible(() -> !targetesp.is("Кольцо"));

    public TargetEsp() {
        this.addSettings(targetesp, type, ghostType, size, alpha, sizee, distance, speed, distancee, bmwGhostCount, bmwGhostTimer, bmwStrengthXZ, bmwStrengthY, crystalFade, crystalSize, hitred);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
