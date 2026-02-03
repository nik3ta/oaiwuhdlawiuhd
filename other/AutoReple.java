package nuclear.module.impl.other;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.misc.TimerUtil;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Annotation(name = "AutoReple", type = TypeList.Player)
public class AutoReple extends Module {

    private static final int TARGET_GAPPLES = 7;
    private static final int MIN_GAPPLES = 3;
    private static final int TARGET_GOLDEN_APPLES = 64;
    private static final int MIN_GOLDEN_APPLES = 16;
    private static final int GOLDEN_APPLE_BATCH = 16;

    private final MultiBoxSetting items = new MultiBoxSetting("Закупать",
            new BooleanSetting("Тотемы", true),
            new BooleanSetting("Яблоки", false),
            new BooleanSetting("Чарки", false));

    private int stage = 0;
    private final TimerUtil actionTimer = new TimerUtil();
    private final TimerUtil buyTimer = new TimerUtil();
    private final TimerUtil closeTimer = new TimerUtil();
    private final TimerUtil buyGappleTimer = new TimerUtil();
    private final TimerUtil buyGoldenAppleTimer = new TimerUtil();
    private boolean pendingClose = false;
    private int boughtThisRun = 0;
    private int boughtGoldenApplesThisRun = 0;

    public AutoReple() {
        addSettings(items);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            onUpdate();
        }
        return false;
    }

    private void onUpdate() {
        if (Minecraft.player == null || mc.world == null) {
            return;
        }

        if (isBrawlikLow()) {
            if (stage != 0) {
                if (Minecraft.player != null) {
                    Minecraft.player.closeScreen();
                }
                resetState();
            }
            return;
        }

        if (mc.player.isHandActive()) {
            if (stage != 0) {
                if (Minecraft.player != null) {
                    Minecraft.player.closeScreen();
                }
                resetState();
            }
            return;
        }

        int totalTotems = countTotems();
        int totalGapples = countGapples();
        int totalGoldenApples = countGoldenApples();

        boolean hasFreeSlot = IntStream.range(0, Minecraft.player.inventory.getSizeInventory()).anyMatch(i -> Minecraft.player.inventory.getStackInSlot(i).isEmpty());

        // Проверка на необходимость закупки (только если включена хотя бы одна настройка)
        boolean needTotems = items.get("Тотемы") && totalTotems < 3;
        // Чарки нужны только если их меньше 3 И тотемы в порядке (>= 5)
        boolean needGapples = items.get("Чарки") && totalGapples < MIN_GAPPLES && totalGapples < TARGET_GAPPLES && totalTotems >= 5;
        // Золотые яблоки нужны если их меньше 16 (независимо от тотемов и чарок)
        boolean needGoldenApples = items.get("Яблоки") && totalGoldenApples < MIN_GOLDEN_APPLES && totalGoldenApples < TARGET_GOLDEN_APPLES;
        
        if (!needTotems && !needGapples && !needGoldenApples) {
            // Если ничего не нужно закупать, не начинаем процесс закупки
            if (stage == 0) {
                return;
            }
        }

        if (items.get("Тотемы")) {
            if (stage == 0 && totalTotems >= 4 && totalTotems <= 6) {
                return;
            }

            if (stage == 0 && !needGapples && !needGoldenApples) {
                return;
            }
        }

        if (stage == 0 && !hasFreeSlot) {
            return;
        }

        if (!isWearingFullNetherite()) {
            resetState();
            return;
        }

        // Проверка на игроков в радиусе - только при начале закупки (stage == 0)
        // или если уже идет процесс закупки, чтобы прервать его
        if (hasNearbyPlayers(11.0)) {
            if (stage != 0) {
                // Если уже идет процесс закупки, закрываем магазин
                if (Minecraft.player != null) {
                    Minecraft.player.closeScreen();
                }
                resetState();
            }
            // Если stage == 0, просто не начинаем закупку
            return;
        }

        if (pendingClose) {
            if (closeTimer.hasTimeElapsed(30L)) {
                if (Minecraft.player != null) {
                    Minecraft.player.closeScreen();
                }
                resetState();
                pendingClose = false;
            }
            return;
        }

        if (stage == 0) {
            if (mc.currentScreen == null) {
                Minecraft.player.sendChatMessage("/shop");
                boughtThisRun = 0;
                boughtGoldenApplesThisRun = 0;
                stage = 1;
                actionTimer.reset();
            }
            return;
        }

        if (actionTimer.hasTimeElapsed(TimeUnit.SECONDS.toMillis(5))) {
            resetState();
            return;
        }

        if (!(Minecraft.player.openContainer instanceof ChestContainer container)) {
            return;
        }


        switch (stage) {
            case 1 -> {
                if (mc.currentScreen != null) {
                    mc.displayGuiScreen(null);
                }
                int swordSlot = findNamedItemInChest(container, Items.NETHERITE_SWORD, "FFA");
                if (swordSlot != -1) {
                    clickSlot(container, swordSlot, ClickType.PICKUP);
                    stage = 2;
                    actionTimer.reset();
                }
            }
            case 2 -> {
                if (mc.currentScreen != null) {
                    mc.displayGuiScreen(null);
                }
                int xpSlot = findItemInChest(container, Items.EXPERIENCE_BOTTLE);
                if (xpSlot != -1) {
                    clickSlot(container, xpSlot, ClickType.PICKUP);
                    stage = 3;
                    actionTimer.reset();
                }
            }
            case 3 -> {
                if (mc.currentScreen != null) {
                    mc.displayGuiScreen(null);
                }
                
                boolean shouldClose = true;
                
                // Определяем текущее состояние
                int currentTotems = countTotems();
                int currentGapples = countGapples();
                int currentGoldenApples = countGoldenApples();
                boolean shouldBuyTotems = items.get("Тотемы") && currentTotems < 4;
                boolean shouldBuyGapples = items.get("Чарки") && currentGapples < MIN_GAPPLES && currentGapples < TARGET_GAPPLES && currentTotems >= 4;
                // Золотые яблоки покупаются независимо от тотемов и чарок
                boolean shouldBuyGoldenApples = items.get("Яблоки") && currentGoldenApples < MIN_GOLDEN_APPLES && currentGoldenApples < TARGET_GOLDEN_APPLES;
                
                // Сначала проверяем и закупаем тотемы (приоритет)
                // Покупаем тотемы ТОЛЬКО если их меньше 4
                if (shouldBuyTotems) {
                    shouldClose = false;
                    
                    if (hasFreeSlot && boughtThisRun < 3) {
                        int totemSlot = 33;
                        if (totemSlot >= 0 && totemSlot < container.inventorySlots.size()) {
                            Slot slot = container.inventorySlots.get(totemSlot);
                            ItemStack stack = slot.getStack();
                            if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING) {
                                if (buyTimer.hasTimeElapsed(100L)) {
                                    clickSlot(container, totemSlot, ClickType.PICKUP);
                                    boughtThisRun++;
                                    buyTimer.reset();
                                    actionTimer.reset();
                                    return;
                                }
                            }
                        }
                    }
                    // Если нужно покупать тотемы, не покупаем чарки (но яблоки покупаем независимо)
                    // Не делаем return, чтобы проверить яблоки
                }
                
                // Проверка и закупка золотых яблок (независимо от тотемов и чарок)
                // Яблоки имеют приоритет над чарками, если тотемы в порядке
                if (shouldBuyGoldenApples) {
                    shouldClose = false;
                    
                    // Покупаем по 16 штук за раз, максимум до 64
                    if (hasFreeSlot && boughtGoldenApplesThisRun < GOLDEN_APPLE_BATCH && currentGoldenApples + boughtGoldenApplesThisRun < TARGET_GOLDEN_APPLES) {
                        int goldenAppleSlot = 30; // На 3 меньше, чем тотем (33 - 3 = 30)
                        if (goldenAppleSlot >= 0 && goldenAppleSlot < container.inventorySlots.size()) {
                            Slot slot = container.inventorySlots.get(goldenAppleSlot);
                            ItemStack stack = slot.getStack();
                            if (!stack.isEmpty() && stack.getItem() == Items.GOLDEN_APPLE) {
                                if (buyGoldenAppleTimer.hasTimeElapsed(100L)) {
                                    clickSlot(container, goldenAppleSlot, ClickType.PICKUP);
                                    boughtGoldenApplesThisRun++;
                                    buyGoldenAppleTimer.reset();
                                    actionTimer.reset();
                                    return;
                                }
                            }
                        }
                    }
                }
                
                // Проверка и закупка чарок (только если тотемы в порядке и яблоки в порядке)
                if (shouldBuyGapples) {
                    shouldClose = false;
                    
                    if (hasFreeSlot) {
                        int gappleSlot = 32; // На 1 меньше, чем тотем (33 - 1 = 32)
                        if (gappleSlot >= 0 && gappleSlot < container.inventorySlots.size()) {
                            Slot slot = container.inventorySlots.get(gappleSlot);
                            ItemStack stack = slot.getStack();
                            if (!stack.isEmpty() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                                if (buyGappleTimer.hasTimeElapsed(100L)) {
                                    clickSlot(container, gappleSlot, ClickType.PICKUP);
                                    buyGappleTimer.reset();
                                    actionTimer.reset();
                                    return;
                                }
                            }
                        }
                    }
                }
                
                // Закрываем магазин, если ничего не нужно закупать
                if (shouldClose) {
                    if (!pendingClose) {
                        pendingClose = true;
                        closeTimer.reset();
                    }
                }
            }
            default -> {
                resetState();
            }
        }
    }

    private void clickSlot(ChestContainer container, int slotId, ClickType clickType) {
        if (Minecraft.player == null || mc.playerController == null) {
            return;
        }
        mc.playerController.windowClick(container.windowId, slotId, 0, clickType, Minecraft.player);
    }

    private int findItemInChest(ChestContainer container, net.minecraft.item.Item target) {
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && stack.getItem() == target) {
                return i;
            }
        }
        return -1;
    }

    private int findNamedItemInChest(ChestContainer container, net.minecraft.item.Item target, String namePart) {
        String lowerNamePart = namePart.toLowerCase();
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && stack.getItem() == target) {
                String displayName = stack.getDisplayName().getString();
                if (displayName.toLowerCase().contains(lowerNamePart)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int countTotems() {
        if (Minecraft.player == null) {
            return 0;
        }

        int inInventory = (int) IntStream.range(0, Minecraft.player.inventory.getSizeInventory())
                .mapToObj(i -> Minecraft.player.inventory.getStackInSlot(i))
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING)
                .count();

        ItemStack offhand = Minecraft.player.getHeldItemOffhand();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            inInventory++;
        }

        return inInventory;
    }

    private int countGapples() {
        if (Minecraft.player == null) {
            return 0;
        }

        int inInventory = (int) IntStream.range(0, Minecraft.player.inventory.getSizeInventory())
                .mapToObj(i -> Minecraft.player.inventory.getStackInSlot(i))
                .filter(s -> s.getItem() == Items.ENCHANTED_GOLDEN_APPLE)
                .mapToInt(ItemStack::getCount)
                .sum();

        ItemStack offhand = Minecraft.player.getHeldItemOffhand();
        if (offhand.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            inInventory += offhand.getCount();
        }

        return inInventory;
    }

    private int countGoldenApples() {
        if (Minecraft.player == null) {
            return 0;
        }

        int inInventory = (int) IntStream.range(0, Minecraft.player.inventory.getSizeInventory())
                .mapToObj(i -> Minecraft.player.inventory.getStackInSlot(i))
                .filter(s -> s.getItem() == Items.GOLDEN_APPLE)
                .mapToInt(ItemStack::getCount)
                .sum();

        ItemStack offhand = Minecraft.player.getHeldItemOffhand();
        if (offhand.getItem() == Items.GOLDEN_APPLE) {
            inInventory += offhand.getCount();
        }

        return inInventory;
    }

    private boolean isWearingFullNetherite() {
        for (EquipmentSlotType slotType : new EquipmentSlotType[]{
                EquipmentSlotType.HEAD,
                EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS,
                EquipmentSlotType.FEET
        }) {
            ItemStack stack = Minecraft.player.getItemStackFromSlot(slotType);
            if (stack.isEmpty()) {
                return false;
            }

            if (slotType == EquipmentSlotType.CHEST) {
                if (stack.getItem() == Items.ELYTRA) {
                    continue;
                }
            }

            if (!(stack.getItem() instanceof ArmorItem armorItem)) {
                return false;
            }

            if (armorItem.getArmorMaterial() != ArmorMaterial.NETHERITE) {
                return false;
            }
        }
        return true;
    }

    private void resetState() {
        stage = 0;
        actionTimer.reset();
        buyTimer.reset();
        buyGappleTimer.reset();
        buyGoldenAppleTimer.reset();
        closeTimer.reset();
        pendingClose = false;
        boughtThisRun = 0;
        boughtGoldenApplesThisRun = 0;
    }

    private boolean isBrawlikLow() {
        if (mc == null || mc.world == null) {
            return false;
        }

        // Определяем, что нужно покупать и сколько это стоит
        int totalTotems = countTotems();
        int totalGapples = countGapples();
        int totalGoldenApples = countGoldenApples();
        boolean needTotems = items.get("Тотемы") && totalTotems < 3;
        // Чарки нужны только если их меньше 3 И тотемы в порядке (>= 5)
        boolean needGapples = items.get("Чарки") && totalGapples < MIN_GAPPLES && totalGapples < TARGET_GAPPLES && totalTotems >= 5;
        // Золотые яблоки нужны если их меньше 16 (независимо от тотемов и чарок)
        boolean needGoldenApples = items.get("Яблоки") && totalGoldenApples < MIN_GOLDEN_APPLES && totalGoldenApples < TARGET_GOLDEN_APPLES;
        
        // Если ничего не нужно покупать, не проверяем бравлики
        if (!needTotems && !needGapples && !needGoldenApples) {
            return false;
        }
        
        // Определяем минимальную стоимость покупки
        // Приоритет тотемам - если нужны тотемы, проверяем их стоимость
        int requiredBravliks = 0;
        if (needTotems) {
            requiredBravliks = 25; // Стоимость тотема
        } else if (needGapples) {
            // Чарки только если тотемы в порядке
            requiredBravliks = 75; // Стоимость чарки
        } else if (needGoldenApples) {
            // Золотые яблоки только если тотемы в порядке
            // Покупаем по 16 штук, каждая стоит 10, но проверяем минимум на одну покупку (10)
            requiredBravliks = 10; // Стоимость одного золотого яблока
        }

        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) {
            return false;
        }

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return false;
        }

        // Ищем строку, содержащую слово "Бравлики"
        for (Score score : scoreboard.getSortedScores(objective)) {
            String playerName = score.getPlayerName();
            if (playerName == null || playerName.startsWith("#")) {
                continue;
            }

            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            ITextComponent lineComponent = ScorePlayerTeam.func_237500_a_(team, new StringTextComponent(playerName));
            String line = lineComponent.getString();

            if (line.contains("Бравлики")) {
                // Нашли строку с Бравликами — пытаемся вытащить число (может быть с запятыми, например 10,000)
                // Ищем все числа в строке, включая запятые
                Matcher matcher = Pattern.compile("[\\d,]+").matcher(line);
                if (matcher.find()) {
                    try {
                        String numberStr = matcher.group().replace(",", ""); // Убираем запятые
                        int value = Integer.parseInt(numberStr);
                        // Возвращаем true только если не хватает бравликов на покупку
                        return value < requiredBravliks;
                    } catch (NumberFormatException ignored) {
                        // Если число не распарсилось — считаем, что всё ок
                        return false;
                    }
                } else {
                    // Есть слово "Бравлики", но нет числа — считаем, что всё нормально
                    return false;
                }
            }
        }

        // Если не нашли строку с "Бравлики" — не блокируем закупку
        return false;
    }

    private boolean hasNearbyPlayers(double radius) {
        if (Minecraft.player == null || mc.world == null || Manager.FRIEND_MANAGER == null) {
            return false;
        }

        try {
            double radiusSq = radius * radius;
            // Проверяем только игроков в радиусе
            for (Entity entity : mc.world.getEntitiesWithinAABB(PlayerEntity.class, 
                    Minecraft.player.getBoundingBox().grow(radius))) {
                if (!(entity instanceof PlayerEntity)) {
                    continue; // Пропускаем не-игроков (на всякий случай)
                }
                
                PlayerEntity player = (PlayerEntity) entity;
                
                // Пропускаем самого себя
                if (player == Minecraft.player) {
                    continue;
                }
                
                // Пропускаем друзей
                String playerName = player.getName().getString();
                if (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(playerName)) {
                    continue;
                }
                
                // Пропускаем спектаторов и мертвых
                if (player.isSpectator() || !player.isAlive()) {
                    continue;
                }
                
                // Проверяем точное расстояние (используем getPosX/Y/Z для более точной проверки)
                double dx = player.getPosX() - Minecraft.player.getPosX();
                double dy = player.getPosY() - Minecraft.player.getPosY();
                double dz = player.getPosZ() - Minecraft.player.getPosZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;
                
                if (distanceSq <= radiusSq) {
                    return true; // Найден игрок в радиусе (не друг)
                }
            }
        } catch (Exception e) {
            // В случае ошибки считаем, что игроков нет (безопаснее для закупки)
            return false;
        }
        
        return false; // Игроков в радиусе нет
    }
}
