package nuclear.module.impl.other;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EffectNotifyEvent;
import nuclear.control.events.impl.player.EventDestroyTotem;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.control.events.impl.player.PotionExplosionEvent;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.render.ColorUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "UseTracker", type = TypeList.Other, desc = "Показывает какой предмет использует игрок")
public class UseTracker extends Module {
    public final BooleanSetting usetracker = new BooleanSetting("Исп предметов", true);
    public final BooleanSetting totemtracker = new BooleanSetting("Исп тотема", true);
    public final BooleanSetting usepotion = new BooleanSetting("Исп зелья", true);
    public final BooleanSetting ftBypass = new BooleanSetting("Обход FunTime", false).setVisible(() -> usepotion.get());
    private static final Map<String, List<TrackedEffect>> playerEffects = new ConcurrentHashMap<>();

    public UseTracker() {
        addSettings(usetracker, totemtracker, usepotion, ftBypass);
    }

    @Override
    public boolean onEvent(Event event) {
        if (usetracker.get() && event instanceof EventPacket) {
            onPacket((EventPacket) event);
        }
        if (usetracker.get() && event instanceof EventUpdate) {
            onUpdate((EventUpdate) event);
        }
        if (totemtracker.get() && event instanceof EventDestroyTotem) {
            EventDestroyTotem e = (EventDestroyTotem) event;
            if (e.entity instanceof LivingEntity) {
                String playerName = e.entity.getName().getString();
                if (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(playerName)) {
                    return false;
                }
                totem_tracker((LivingEntity) e.entity);
            }
        }
        if (usepotion.get() && event instanceof PotionExplosionEvent) {
            onPotionExplosion((PotionExplosionEvent) event);
        }
        if (usepotion.get() && event instanceof EffectNotifyEvent) {
            onEffectNotify((EffectNotifyEvent) event);
        }
        return false;
    }

    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SEntityStatusPacket packet) {
            if (!(packet.getEntity(mc.world) instanceof PlayerEntity player) || packet.getOpCode() != 9) return;
            playerUsing.put(player.getEntityId(), true);
        }
    }

    private final Map<Integer, Boolean> playerUsing = new HashMap<>();
    private final Map<Integer, String> lastUsingItemName = new HashMap<>();

    private static class StartUseData {
        public final Hand usedHand;
        public final ItemStack startStack;

        public StartUseData(Hand usedHand, ItemStack startStack) {
            this.usedHand = usedHand;
            this.startStack = startStack.copy();
        }
    }

    private final Map<Integer, StartUseData> startData = new HashMap<>();

    private void onUpdate(EventUpdate e) {
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player || (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(other.getName().getString()))) continue;

            int id = other.getEntityId();
            boolean isUsing = other.isHandActive() || other.isUsingItem();
            boolean wasUsing = playerUsing.getOrDefault(id, false);

            if (isUsing) {
                if (!wasUsing) {
                    Hand usedHand = other.getActiveHand();
                    if (usedHand == null) usedHand = Hand.MAIN_HAND;
                    ItemStack start = usedHand == Hand.MAIN_HAND ? other.getHeldItemMainhand() : other.getHeldItemOffhand();
                    startData.put(id, new StartUseData(usedHand, start));
                    if (!start.isEmpty()) {
                        String itemName;
                        if (start.getItem() instanceof PotionItem) {
                            ITextComponent displayName = start.getDisplayName();
                            String displayNameString = displayName.getString();
                            if (displayNameString.contains("Лучший ТГ @StarikZako")) {
                                itemName = start.getItem().getName().getString();
                            } else {
                                itemName = displayNameString;
                            }
                        } else {
                            itemName = start.getItem().getName().getString();
                        }
                        lastUsingItemName.put(id, itemName);
                    }
                } else {
                    StartUseData started = startData.get(id);
                    if (started != null) {
                        ItemStack cur = started.usedHand == Hand.MAIN_HAND ? other.getHeldItemMainhand() : other.getHeldItemOffhand();
                        if (!cur.isEmpty()) {
                            String itemName;
                            if (cur.getItem() instanceof PotionItem) {
                                ITextComponent displayName = cur.getDisplayName();
                                String displayNameString = displayName.getString();
                                if (displayNameString.contains("Лучший ТГ @StarikZako")) {
                                    itemName = cur.getItem().getName().getString();
                                } else {
                                    itemName = displayNameString;
                                }
                            } else {
                                itemName = cur.getItem().getName().getString();
                            }
                            lastUsingItemName.put(id, itemName);
                        }
                    }
                }
            }

            if (!isUsing && wasUsing) {
                boolean finished = false;
                StartUseData started = startData.get(id);
                ItemStack usedItemStack = null;
                if (started != null) {
                    ItemStack curNow = started.usedHand == Hand.MAIN_HAND ? other.getHeldItemMainhand() : other.getHeldItemOffhand();
                    ItemStack start = started.startStack;
                    usedItemStack = start;
                    if (!start.isEmpty()) {
                        if (start.getItem() instanceof PotionItem) {
                            finished = (curNow.getItem() == Items.GLASS_BOTTLE)
                                    || (curNow.getItem() == start.getItem() && curNow.getCount() < start.getCount())
                                    || (start.getCount() == 1 && curNow.isEmpty());
                        } else if (start.isFood()) {
                            finished = (curNow.getItem() == start.getItem() && curNow.getCount() < start.getCount())
                                    || (start.getCount() == 1 && curNow.isEmpty());
                        } else if (start.getItem() == Items.SUSPICIOUS_STEW || start.getItem() == Items.MUSHROOM_STEW || start.getItem() == Items.RABBIT_STEW) {
                            finished = (curNow.getItem() == Items.BOWL) || (start.getCount() == 1 && curNow.isEmpty());
                        } else if (start.getItem() == Items.MILK_BUCKET) {
                            finished = (curNow.getItem() == Items.BUCKET) || (curNow.getItem() == Items.MILK_BUCKET);
                        }
                    }
                }

                if (finished) {
                    String itemName = lastUsingItemName.getOrDefault(id, "предмет");
                    String playerName = other.getDisplayName().getString();
                    String fullText = playerName + " использовал " + itemName;
                    ITextComponent message = new StringTextComponent("")
                            .append(other.getDisplayName())
                            .append(new StringTextComponent(" использовал ")
                                    .setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY)))
                            .append(new StringTextComponent(itemName)
                                    .setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE)));
                    ClientUtils.sendMessage(message);

                    // Отправка уведомления через NotifManager
                    if (Manager.NOTIFICATION_MANAGER != null && usedItemStack != null && !usedItemStack.isEmpty()) {
                        Manager.NOTIFICATION_MANAGER.add(
                                fullText,
                                "",
                                3,
                                this.category,
                                other.getDisplayName(),
                                usedItemStack.copy()
                        );
                    }
                }

                lastUsingItemName.remove(id);
                startData.remove(id);
            }

            if (isUsing != wasUsing) {
                playerUsing.put(id, isUsing);
            }
        }
    }

    private void totem_tracker(LivingEntity entity) {
        if (entity.getHeldItemMainhand().getItem() == Items.TOTEM_OF_UNDYING) {
            totem_tracker(entity.getHeldItemMainhand(), entity);
        } else if (entity.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING && entity.getHeldItemMainhand().getItem() != Items.TOTEM_OF_UNDYING) {
            totem_tracker(entity.getHeldItemOffhand(), entity);
        }
    }

    private void totem_tracker(ItemStack stack, LivingEntity entity) {

        ITextComponent message = new StringTextComponent("")
                .append(entity.getDisplayName())
                .append(new StringTextComponent(" потерял тотем бессмертия, зачарован:")
                        .setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY)))
                .append(new StringTextComponent(" ●")
                        .setStyle(new StringTextComponent("").getStyle().applyFormatting(
                                stack.isEnchanted() ? TextFormatting.GREEN : TextFormatting.RED)));
        ClientUtils.sendMessage(message);

        // Отправка уведомления через NotifManager
        if (Manager.NOTIFICATION_MANAGER != null && Manager.NOTIFICATION_MANAGER.useTotems.get()) {
            String fullText = entity.getDisplayName().getString() + " потерял тотем бессмертия, зачарован: " + (stack.isEnchanted() ? "●" : "●");
            Manager.NOTIFICATION_MANAGER.add(
                    fullText,
                    "",
                    3,
                    this.category,
                    entity.getDisplayName(),
                    stack.copy()
            );
        }
    }

    public void onEffectNotify(EffectNotifyEvent event) {
        if (!usepotion.get() || ftBypass.get())
            return;

        if (!(event.getEntity() instanceof PlayerEntity player)) {
            return;
        }

        List<EffectInstance> effects = event.getEffects();
        ItemStack itemStack = event.getItemStack();

        double sumAppliedDur = 0;
        double sumOriginalDur = 0;

        StringBuilder effectsBuilder = new StringBuilder();

        int effectCount = 0;
        List<EffectInstance> appliedEffects = new ArrayList<>();

        for (EffectInstance e : effects) {
            if (e.getPotion().isInstant()) continue;

            int appliedDur = (int) (event.getDistanceFactor() * e.getDuration() + 0.5);
            int originalDur = e.getDuration();

            if (appliedDur <= 20) continue;

            sumAppliedDur += appliedDur;
            sumOriginalDur += originalDur;
            effectCount++;

            Effect effect = e.getPotion();
            String effectName = effect.getDisplayName().getString();
            int amplifier = e.getAmplifier();
            int duration = appliedDur / 20;
            int originalDuration = originalDur / 20;

            appliedEffects.add(new EffectInstance(
                    e.getPotion(),
                    appliedDur,
                    e.getAmplifier(),
                    false,
                    true
            ));

            effectsBuilder.append(TextFormatting.GRAY)
                    .append(effectName)
                    .append(TextFormatting.WHITE)
                    .append(" ")
                    .append(getRomanNumeral(amplifier + 1))
                    .append(TextFormatting.GRAY)
                    .append(" (")
                    .append(formatTime(duration))
                    .append("/")
                    .append(formatTime(originalDuration))
                    .append(")")
                    .append(TextFormatting.RESET)
                    .append("\n");
        }

        if (effectCount == 0) return;

        if (sumOriginalDur > 0) {
            int percentage = (int) ((sumAppliedDur / sumOriginalDur) * 100);

            trackPlayerEffects(player, appliedEffects);

            displayPotionNotificationFromEffect(player, itemStack, appliedEffects, percentage, effectsBuilder.toString().trim());
        }
    }

    public void onPotionExplosion(PotionExplosionEvent event) {
        if (!usepotion.get() || ftBypass.get())
            return;

        int potionColor = event.getRgbColor();
        BlockPos pos = event.getPosition();

        Optional<SpecialPotionType> potionTypeOpt = findClosestPotionType(potionColor);

        if (potionTypeOpt.isEmpty())
            return;

        SpecialPotionType potionType = potionTypeOpt.get();

        double posX = pos.getX();
        double posY = pos.getY();
        double posZ = pos.getZ();

        boolean hitBlock = isNearSolidBlock(posX, posY, posZ);

        AxisAlignedBB areaOfEffect = new AxisAlignedBB(
                posX - 4.0, posY - 2.0, posZ - 4.0,
                posX + 4.0, posY + 2.0, posZ + 4.0
        );

        List<PlayerEntity> playersInArea = mc.world.getEntitiesWithinAABB(
                PlayerEntity.class,
                areaOfEffect,
                player -> true
        );

        PlayerEntity directHitPlayer = null;
        double minDistance = Double.MAX_VALUE;

        if (!hitBlock && !playersInArea.isEmpty()) {
            for (PlayerEntity player : playersInArea) {
                double distance = getDistance(posX, posY, posZ, player);
                if (distance < minDistance) {
                    minDistance = distance;
                    directHitPlayer = player;
                }
            }
        }

        for (PlayerEntity player : playersInArea) {
            if (!hitBlock && player == directHitPlayer && minDistance < 2.0) {
                List<EffectInstance> effectInstances = new CopyOnWriteArrayList<>();

                for (SpecialPotionType.PotionEffectData effectData : potionType.getEffects()) {
                    effectInstances.add(new EffectInstance(
                            effectData.getEffect(),
                            effectData.getDurationTicks(),
                            effectData.getAmplifier(),
                            false,
                            true
                    ));
                }

                trackPlayerEffects(player, effectInstances);
                displayPotionNotification(player, potionType, effectInstances, 100);

            } else {
                double distanceSq = getDistanceSq(posX, posY, posZ, player);

                if (distanceSq < 16.0) {
                    double factor = 1.0 - Math.sqrt(distanceSq) / 4.0;

                    List<EffectInstance> appliedEffects = new CopyOnWriteArrayList<>();

                    for (SpecialPotionType.PotionEffectData effectData : potionType.getEffects()) {
                        int originalDuration = effectData.getDurationTicks();
                        int appliedDuration = (int) (factor * originalDuration + 0.5);

                        if (appliedDuration > 20) {
                            appliedEffects.add(new EffectInstance(
                                    effectData.getEffect(),
                                    appliedDuration,
                                    effectData.getAmplifier(),
                                    false,
                                    true
                            ));
                        }
                    }

                    if (!appliedEffects.isEmpty()) {
                        int percentage = (int) (factor * 100);

                        trackPlayerEffects(player, appliedEffects);
                        displayPotionNotification(player, potionType, appliedEffects, percentage);
                    }
                }
            }
        }
    }

    private double getDistance(double x1, double y1, double z1, Entity entity) {
        double dx = x1 - entity.getPosX();
        double dy = y1 - entity.getPosY();
        double dz = z1 - entity.getPosZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double getDistanceSq(double x1, double y1, double z1, Entity entity) {
        double dx = x1 - entity.getPosX();
        double dy = y1 - entity.getPosY();
        double dz = z1 - entity.getPosZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean isNearSolidBlock(double x, double y, double z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (isSolidBlock(pos)) return true;

        if (isSolidBlock(pos.down())) return true;
        if (isSolidBlock(pos.north())) return true;
        if (isSolidBlock(pos.south())) return true;
        if (isSolidBlock(pos.east())) return true;
        if (isSolidBlock(pos.west())) return true;

        return false;
    }

    private boolean isSolidBlock(BlockPos pos) {
        BlockState blockState = mc.world.getBlockState(pos);
        return blockState.getMaterial().isSolid() && !blockState.getMaterial().isLiquid();
    }

    private void displayPotionNotification(PlayerEntity player, SpecialPotionType potionType,
                                           List<EffectInstance> appliedEffects, int percentage) {
        ITextComponent headerComponent = new StringTextComponent(potionType.getDisplayName());
        StringBuilder effectsBuilder = new StringBuilder();
        StringBuilder simpleEffectsBuilder = new StringBuilder();

        for (int i = 0; i < appliedEffects.size(); i++) {
            EffectInstance effect = appliedEffects.get(i);
            SpecialPotionType.PotionEffectData originalData = findEffectData(potionType, effect.getPotion());
            String effectName = effect.getPotion().getDisplayName().getString();
            int amplifier = effect.getAmplifier();
            int duration = effect.getDuration() / 20;
            int originalDuration = originalData != null ? originalData.getDurationSeconds() : duration;

            effectsBuilder.append(TextFormatting.BLUE)
                    .append(effectName)
                    .append(TextFormatting.BLUE)
                    .append(" ")
                    .append(getRomanNumeral(amplifier + 1))
                    .append(TextFormatting.BLUE)
                    .append(" (")
                    .append(formatTime(originalDuration))
                    .append(")")
                    .append(TextFormatting.RESET)
                    .append("\n");

            simpleEffectsBuilder.append(TextFormatting.GRAY)
                    .append("● ")
                    .append(TextFormatting.RED)
                    .append(effectName)
                    .append(" ")
                    .append(getRomanNumeral(amplifier + 1))
                    .append(TextFormatting.GRAY)
                    .append(" ")
                    .append(formatTime(duration))
                    .append(TextFormatting.RESET);

            if (i < appliedEffects.size() - 1) {
                simpleEffectsBuilder.append("\n");
            }
        }

        ITextComponent playerName = player.getName().deepCopy().mergeStyle(TextFormatting.RED);

        IFormattableTextComponent grayReceived = new StringTextComponent(getSpacePrefix(playerName) + "получил эффект из ").mergeStyle(TextFormatting.GRAY);
        IFormattableTextComponent itemNameComponent = new StringTextComponent(potionType.getDisplayName()).deepCopy();

        IFormattableTextComponent successText = new StringTextComponent("\n● Успешность: ")
                .mergeStyle(TextFormatting.GRAY)
                .append(new StringTextComponent(percentage + "%").setStyle(getPercentageColor(percentage)));

        ITextComponent tooltipText = new StringTextComponent("")
                .append(headerComponent)
                .append(ITextComponent.getTextComponentOrEmpty("\n"))
                .append(new StringTextComponent(effectsBuilder.toString().trim()));

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipText);
        itemNameComponent = itemNameComponent.modifyStyle(style -> style.setHoverEvent(hoverEvent));

        ITextComponent msg = new StringTextComponent("")
                .append(playerName)
                .append(grayReceived)
                .append(itemNameComponent)
                .append(successText)
                .append(new StringTextComponent("\n" + simpleEffectsBuilder.toString()));

        ClientUtils.sendMessage(msg);
    }

    private static int interpolateColor(int startColor, int endColor, float fraction) {
        int r1 = (startColor >> 16) & 0xFF;
        int g1 = (startColor >> 8) & 0xFF;
        int b1 = startColor & 0xFF;

        int r2 = (endColor >> 16) & 0xFF;
        int g2 = (endColor >> 8) & 0xFF;
        int b2 = endColor & 0xFF;

        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Возвращает стиль цвета от зелёного (100%) до красного (0%)
     */
    private static Style getPercentageColor(int percentage) {
        percentage = Math.max(0, Math.min(100, percentage)); // ограничиваем 0–100
        float fraction = 1f - (percentage / 100f); // 0 — зелёный, 1 — красный

        int green = 0x00FF00; // зелёный
        int red = 0xFF0000;   // красный
        int color = interpolateColor(green, red, fraction);

        return Style.EMPTY.setColor(Color.fromInt(color));
    }


    private SpecialPotionType.PotionEffectData findEffectData(SpecialPotionType potionType, Effect effect) {
        for (SpecialPotionType.PotionEffectData data : potionType.getEffects()) {
            if (data.getEffect() == effect) {
                return data;
            }
        }
        return null;
    }

    private Optional<SpecialPotionType> findClosestPotionType(int color) {
        for (SpecialPotionType type : SpecialPotionType.values()) {
            if (type.getBaseColor() == color || type.getColorVariations().contains(color)) {
                return Optional.of(type);
            }
        }

        SpecialPotionType closestType = null;
        double minDistance = Double.MAX_VALUE;
        final double THRESHOLD = 30.0;

        for (SpecialPotionType type : SpecialPotionType.values()) {
            double distance = colorDistance(color, type.getBaseColor());
            if (distance < minDistance && distance < THRESHOLD) {
                minDistance = distance;
                closestType = type;
            }
        }

        return Optional.ofNullable(closestType);
    }

    private double colorDistance(int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        double rmean = (r1 + r2) / 2.0;
        double r = r1 - r2;
        double g = g1 - g2;
        double b = b1 - b2;

        double weightR = 2 + rmean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256.0;

        return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
    }

    private Optional<SpecialPotionType> findPotionTypeByName(String displayName) {
        for (SpecialPotionType type : SpecialPotionType.values()) {
            if (type.getDisplayName().equals(displayName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private void displayPotionNotificationFromEffect(PlayerEntity player, ItemStack itemStack, List<EffectInstance> appliedEffects, int percentage, String effectsText) {
        ITextComponent headerComponent = gradient("Активные эффекты:");

        ITextComponent playerName = player.getDisplayName();
        IFormattableTextComponent grayReceived = new StringTextComponent(getSpacePrefix(playerName) + "получил ").mergeStyle(TextFormatting.GRAY);
        ITextComponent displayName = itemStack.getDisplayName();
        IFormattableTextComponent itemNameComponent;
        String displayNameString = displayName.getString();
        if (displayNameString.contains("Лучший ТГ @StarikZako")) {
            itemNameComponent = itemStack.getItem().getName().deepCopy();
        } else {
            itemNameComponent = displayName.deepCopy();
        }

        IFormattableTextComponent percentageText = gradient(" (" + percentage + "%)");

        ITextComponent tooltipText = new StringTextComponent("")
                .append(headerComponent)
                .append(ITextComponent.getTextComponentOrEmpty("\n"))
                .append(new StringTextComponent(effectsText));

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipText);

        itemNameComponent = itemNameComponent.modifyStyle(style -> style.setHoverEvent(hoverEvent));
        percentageText = percentageText.modifyStyle(style -> style.setHoverEvent(hoverEvent));

        ITextComponent msg = new StringTextComponent("")
                .append(playerName)
                .append(grayReceived)
                .append(itemNameComponent)
                .append(percentageText);

        mc.player.sendMessage(msg, Util.DUMMY_UUID);
    }

    public static StringTextComponent gradient(String message) {
        StringTextComponent text = new StringTextComponent("");
        for (int i = 0; i < message.length(); i++) {
            text.append(new StringTextComponent(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.setColor(new Color(ColorUtils.gradient(3, i * 8, getColorByName("primaryColor"), getColorByName("primaryColor"), (int) 0.5f)))));
        }

        return text;
    }

    private String getSpacePrefix(ITextComponent playerName) {
        String plainText = playerName.getUnformattedComponentText();

        if (plainText.endsWith(" ")) {
            return "";
        }

        String stringRepresentation = playerName.toString();

        if (stringRepresentation.matches(".*(§[0-9a-fk-or]| |\\s)$")) {
            return "";
        }

        if (playerName.getSiblings().size() > 0) {
            ITextComponent lastSibling = playerName.getSiblings().get(playerName.getSiblings().size() - 1);
            String lastSiblingText = lastSibling.getString();
            if (lastSiblingText.endsWith(" ") || lastSiblingText.endsWith("§r")) {
                return "";
            }
        }

        return " ";
    }

    private void trackPlayerEffects(PlayerEntity player, List<EffectInstance> effects) {
        String playerUUID = player.getUniqueID().toString();
        List<TrackedEffect> trackedEffects = playerEffects.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>());

        for (EffectInstance effect : effects) {
            if (effect.getPotion() == Effects.STRENGTH) {
                trackedEffects.removeIf(e -> e.getEffect() == effect.getPotion());
                trackedEffects.add(new TrackedEffect(
                        effect.getPotion(),
                        effect.getAmplifier(),
                        System.currentTimeMillis(),
                        effect.getDuration() * 50L
                ));
            }
        }
    }

    private String getRomanNumeral(int num) {
        if (num <= 0) return "I";
        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return num <= 10 ? romanNumerals[num] : String.valueOf(num);
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return String.format("0:%02d", seconds);
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }

    @Getter
    public static class TrackedEffect {
        private final Effect effect;
        private final int amplifier;
        private final long startTime;
        private final long durationMillis;

        public TrackedEffect(Effect effect, int amplifier, long startTime, long durationMillis) {
            this.effect = effect;
            this.amplifier = amplifier;
            this.startTime = startTime;
            this.durationMillis = durationMillis;
        }

        public int getRemainingTicks() {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = durationMillis - elapsed;
            return (int) (remaining / 50);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum SpecialPotionType {
        FLASH(16777215, "§6§l[★] §e§lВспышка", Arrays.asList(
                new PotionEffectData(Effects.BLINDNESS, 20, 0),
                new PotionEffectData(Effects.GLOWING, 4 * 60, 0)
        ), Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)),

        KILLER(13369344, "§4§l[★] §c§lЗелье Киллера", Arrays.asList(
                new PotionEffectData(Effects.RESISTANCE, 3 * 60, 0),
                new PotionEffectData(Effects.STRENGTH, 90, 3)
        ), Arrays.asList(12582912, 15007744, 14024704)),

        KILLER2(11796480, "§4§l[★] §c§lЗелье Киллера", Arrays.asList(
                new PotionEffectData(Effects.RESISTANCE, 3 * 60, 0),
                new PotionEffectData(Effects.STRENGTH, 3 * 60, 3)
        ), Arrays.asList(12582912, 15007744, 14024704)),

        BURP(16737792, "§c§l[★] §6§lЗелье Отрыжки", Arrays.asList(
                new PotionEffectData(Effects.BLINDNESS, 10, 0),
                new PotionEffectData(Effects.GLOWING, 3 * 60, 0),
                new PotionEffectData(Effects.HUNGER, 90, 9),
                new PotionEffectData(Effects.SLOWNESS, 3 * 60, 2),
                new PotionEffectData(Effects.WITHER, 30, 4)
        ), Arrays.asList(16727040, 16733184, 16739072)),

        SULFURIC_ACID(10092339, "§2§l[★] §a§lСерная кислота", Arrays.asList(
                new PotionEffectData(Effects.POISON, 50, 1),
                new PotionEffectData(Effects.SLOWNESS, 90, 3),
                new PotionEffectData(Effects.WEAKNESS, 90, 2),
                new PotionEffectData(Effects.WITHER, 30, 4)
        ), Arrays.asList(9961472, 10223411, 10027007)),

        MEDIC(16711935, "§5§l[★] §d§lЗелье Медика", Arrays.asList(
                new PotionEffectData(Effects.HEALTH_BOOST, 45, 2),
                new PotionEffectData(Effects.REGENERATION, 45, 2)
        ), Arrays.asList(16716287, 16707583, 14680063)),

        AGENT(16776960, "§6§l[★] §e§lЗелье агента", Arrays.asList(
                new PotionEffectData(Effects.FIRE_RESISTANCE, 15 * 60, 0),
                new PotionEffectData(Effects.HASTE, 3 * 60, 0),
                new PotionEffectData(Effects.INVISIBILITY, 15 * 60, 0),
                new PotionEffectData(Effects.SPEED, 15 * 60, 2),
                new PotionEffectData(Effects.STRENGTH, 5 * 60, 2)
        ), Arrays.asList(16772608, 16773376, 16775892, 15663104)),

        WINNER(65280, "§2§l[★] §a§lЗелье Победителя", Arrays.asList(
                new PotionEffectData(Effects.HEALTH_BOOST, 3 * 60, 1),
                new PotionEffectData(Effects.INVISIBILITY, 15 * 60, 0),
                new PotionEffectData(Effects.REGENERATION, 60, 1),
                new PotionEffectData(Effects.RESISTANCE, 60, 0)
        ), Arrays.asList(59136, 57088, 63232, 65310, 65350)),

        URINE(65280, "§3§l[★] §b§lМоча Флеша", Arrays.asList(
                new PotionEffectData(Effects.JUMP_BOOST, 2 * 60, 1),
                new PotionEffectData(Effects.SPEED, 2 * 60, 2)
        ), Arrays.asList(65535));

        private final Integer baseColor;
        private final String displayName;
        private final List<PotionEffectData> effects;
        private final List<Integer> colorVariations;

        @Getter
        @RequiredArgsConstructor
        public static class PotionEffectData {
            private final Effect effect;
            private final Integer durationSeconds;
            private final Integer amplifier;

            public Integer getDurationTicks() {
                return durationSeconds * 20;
            }
        }
    }
}
