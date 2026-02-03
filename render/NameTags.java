package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mods.voicechat.voice.client.ClientManager;
import mods.voicechat.voice.client.ClientPlayerStateManager;
import mods.voicechat.voice.client.ClientVoicechat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.IMinecraft;
import nuclear.utils.font.Fonts;
import nuclear.utils.math.MathUtil;
import nuclear.utils.math.PlayerPositionTracker;
import nuclear.utils.math.TargetUtil;
import nuclear.utils.render.RenderUtils;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static nuclear.utils.render.RenderUtils.IntColor.rgba;

@Annotation(name = "NameTags", type = TypeList.Render, desc = "Показывает информацию игрока")
public class NameTags extends Module {

    private static final int MAX_ITEMS = 33;

    public MultiBoxSetting targets = new MultiBoxSetting("Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Монстров", false),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Животных", false),
            new BooleanSetting("Себя", true),
            new BooleanSetting("Жителей", false),
            new BooleanSetting("Голых", true),
            new BooleanSetting("Предметы", true));

    public MultiBoxSetting elements = new MultiBoxSetting("Настройки",
            new BooleanSetting("Броня", true),
            new BooleanSetting("Зачарование", true),
            new BooleanSetting("Показывать шары", true),
            new BooleanSetting("Отображать VoiceChat", true));

    public SliderSetting size = new SliderSetting("Размер шрифта", 0.6f, 0.5f, 0.7f, 0.02f);

    public NameTags() {
        addSettings(targets, elements, size);
    }

    public Object2ObjectOpenHashMap<Vector4d, Entity> positions = new Object2ObjectOpenHashMap<>();

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render) {
            if (render.isRender3D()) {
                updatePlayerPositions(render.partialTicks);
            }

            if (render.isRender2D()) {
                renderPlayerElements(render.matrixStack);
            }
        }
        return false;
    }

    private void updatePlayerPositions(float partialTicks) {
        this.positions.clear();

        List<ItemEntity> itemEntities = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (!entity.isAlive() || !PlayerPositionTracker.isInView(entity)) {
                continue;
            }

            if (!TargetUtil.isEntityTarget(entity, targets)) {
                continue;
            }

            // Для игроков проверяем дополнительные условия
            if (entity instanceof PlayerEntity player) {
                if (!player.botEntity) {
                    continue;
                }
                if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON && player == Minecraft.player) {
                    continue;
                }
            }

            // Собираем предметы отдельно для ограничения
            if (entity instanceof ItemEntity itemEntity) {
                itemEntities.add(itemEntity);
                continue;
            }

            Vector4d position = PlayerPositionTracker.updatePlayerPositions(entity, partialTicks);
            if (position != null) {
                this.positions.put(position, entity);
            }
        }

        // Ограничиваем количество предметов до MAX_ITEMS, сортируя по расстоянию
        if (!itemEntities.isEmpty()) {
            itemEntities.sort((a, b) -> {
                double distA = mc.player.getDistance(a);
                double distB = mc.player.getDistance(b);
                return Double.compare(distA, distB);
            });

            int itemCount = Math.min(itemEntities.size(), MAX_ITEMS);
            for (int i = 0; i < itemCount; i++) {
                ItemEntity itemEntity = itemEntities.get(i);
                Vector4d position = PlayerPositionTracker.updatePlayerPositions(itemEntity, partialTicks);
                if (position != null) {
                    this.positions.put(position, itemEntity);
                }
            }
        }
    }

    private void renderPlayerElements(MatrixStack stack) {
        if (positions.isEmpty()) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        IMinecraft.BUFFER.begin(7, DefaultVertexFormats.POSITION_COLOR);

        boolean isConnectedToServer = Manager.FUNCTION_MANAGER.scoreboardHealth.state;
        PlayerEntity currentPlayer = getCurrentPlayer();

        List<Map.Entry<Vector4d, Entity>> sortedPositions = new ArrayList<>(positions.entrySet());
        sortedPositions.sort((a, b) -> {
            double distA = mc.player.getDistance(a.getValue());
            double distB = mc.player.getDistance(b.getValue());
            return Double.compare(distB, distA);
        });

        for (Map.Entry<Vector4d, Entity> entry : sortedPositions) {
            Entity entity = entry.getValue();
            if (isConnectedToServer && entity instanceof PlayerEntity player && player != null && !player.equals(currentPlayer)) {
                updatePlayerHealth(player);
            }
        }

        IMinecraft.TESSELLATOR.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (Map.Entry<Vector4d, Entity> entry : sortedPositions) {
            Vector4d position = entry.getKey();
            Entity entity = entry.getValue();

            double x = position.x;
            double y = position.y;
            double endX = position.z;
            double endY = position.w;

            if (entity instanceof ItemEntity itemEntity) {
                renderItemTags(stack, (float) x, (float) y, (float) endX, (float) endY, itemEntity);
            } else if (entity instanceof PlayerEntity player) {
                if (IMinecraft.mc.ingameGUI.getTabList().header == null) {
                    continue;
                }
                renderTags(stack, (float) x, (float) y, (float) endX, (float) endY, player);
            } else if (entity instanceof LivingEntity livingEntity) {
                renderLivingEntityTags(stack, (float) x, (float) y, (float) endX, (float) endY, livingEntity);
            }
        }
    }

    private PlayerEntity getCurrentPlayer() {
        return Minecraft.getInstance().player;
    }

    private void updatePlayerHealth(PlayerEntity player) {
        String myPlayerName = String.valueOf(mc.player.getName());

        if (player.getName().getString().equals(myPlayerName)) {
            return;
        }

        for (Map.Entry<ScoreObjective, Score> entry : IMinecraft.mc.world.getScoreboard().getObjectivesForEntity(player.getName().getString()).entrySet()) {
            Score score = entry.getValue();
            int newHealth = score.getScorePoints();
            player.setHealth(Math.max(newHealth, 1));
        }
    }

    private void renderTags(MatrixStack matrixStack, float posX, float posY, float endPosX, float endPosY, PlayerEntity entity) {
        float maxOffsetY = 0.0F;
        ITextComponent text = entity.getDisplayName();
        TextComponent name = (TextComponent) text;

        String friendPrefix = "";
        ITextComponent friendText = ITextComponent.getTextComponentOrEmpty(friendPrefix);

        TextComponent friendPrefixComponent = (TextComponent) friendText;
        if (Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) && (Manager.FUNCTION_MANAGER.nameProtect.state && Manager.FUNCTION_MANAGER.nameProtect.friends.get())) {
            friendPrefixComponent.append(new StringTextComponent(TextFormatting.RED + (entity.getDisplayName().getString().contains("●") ? "    Protect" : "Protect")));
        } else {
            friendPrefixComponent.append(name);
        }
        name = friendPrefixComponent;

        int health = (int) entity.getHealth();
        name.append(new StringTextComponent(TextFormatting.DARK_GRAY + " [" + TextFormatting.RED + health + "HP" + TextFormatting.DARK_GRAY + "]"));

        TextComponent finalName = name;
        float width = Fonts.blod[19].getWidth(finalName.getString()) + 6;
        float height = 15f;
        int colorsbox2 = Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) ? (new java.awt.Color(41, 228, 28, 55)).getRGB() : rgba(15, 15, 16, 125);

        MathUtil.scaleElements((posX + endPosX) / 2f, posY - height / 2, size.getValue().floatValue(), () -> {
            ClientVoicechat client = ClientManager.getClient();
            RenderUtils.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F, posY - height - 11.0F + 2F + 0.75F, width, height - 2F, 0F, colorsbox2);
            if (client != null) {
                ClientPlayerStateManager manager = ClientManager.getPlayerStateManager();
                boolean isVoiceClient = !manager.isPlayerDisconnected(entity);
                if (elements.get(3) && isVoiceClient) {
                    boolean isSpeak = client.getTalkCache().isWhispering(entity) || client.getTalkCache().isTalking(entity);
                    int color_speak = manager.isPlayerDisabled(entity) ? (new java.awt.Color(84, 0, 0, 255)).getRGB() :
                            isSpeak ? (new java.awt.Color(0, 255, 0, 255)).getRGB() : (new java.awt.Color(255, 0, 0, 255)).getRGB();
                    RenderUtils.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F + width - 0.5f, posY - height - 11.0F + 2F + 0.75F, 2.25f, height - 2F, 0F, color_speak);
                    RenderUtils.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F - 1.5f, posY - height - 11.0F + 2F + 0.75F, 2.25f, height - 2F, 0F, color_speak);
                }
            }
            Fonts.blod[19].drawText(matrixStack, finalName, (posX + endPosX) / 2f - width / 2f + (Fonts.blod[19].getWidth(finalName.getString())) - width + 8.5f, posY - height - 5.5f);
            if (entity.getDisplayName().getString().contains("§a●")) {
                RenderUtils.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new java.awt.Color(84, 252, 84, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("§c●")) {
                RenderUtils.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new java.awt.Color(252, 84, 84, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("§6●")) {
                RenderUtils.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new java.awt.Color(252, 168, 0, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("●")) {
                RenderUtils.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new java.awt.Color(252, 84, 84, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("•")) {
                RenderUtils.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new java.awt.Color(252, 84, 84, 255).getRGB());
            }
        });

        maxOffsetY += 20.5F;

        List<ItemStack> stacks = new ArrayList<>(Collections.singletonList(entity.getHeldItemMainhand()));
        entity.getArmorInventoryList().forEach(stacks::add);
        stacks.removeIf(w -> w.getItem() instanceof AirItem);

        ItemStack offhandItem = entity.getHeldItemOffhand();
        boolean isSkull = offhandItem.getItem() instanceof SkullItem;
        boolean isEnchantedTotem = false;
        if (offhandItem.getItem() == Items.TOTEM_OF_UNDYING) {
            CompoundNBT tag = offhandItem.getTag();
            if (tag != null && tag.contains("Enchantments")) {
                isEnchantedTotem = true;
            }
        }

        int totalSize = stacks.size() * 20;
        maxOffsetY += 7.0f * size.getValue().floatValue() + (size.getValue().floatValue() * 10) + 5;

        ItemStack mainHandItem = entity.getHeldItemMainhand();
        boolean hasMainHand = !mainHandItem.isEmpty();
        boolean hasOffhandBall = elements.get(2) && !offhandItem.isEmpty() && (isSkull || isEnchantedTotem);

        float baseX = (posX + endPosX) / 2.0F;
        float mainHandY = endPosY + 6.0F;
        float offhandY = hasMainHand ? mainHandY + height - 6.0F : mainHandY;

        if (hasMainHand) {
            ITextComponent displayName = mainHandItem.getDisplayName();
            ITextComponent mainHandName;
            String displayNameString = displayName.getString();
            if (displayNameString.contains("Лучший ТГ @StarikZako")) {
                mainHandName = mainHandItem.getItem().getName();
            } else {
                mainHandName = displayName;
            }
            float mainHandWidth = Fonts.blod[17].getWidth(mainHandName.getString()) + 6;
            float finalMainHandY = mainHandY;

            MathUtil.scaleElements(baseX, finalMainHandY + 8.0F, size.getValue().floatValue(), () -> {
                RenderUtils.Render2D.drawRoundedRect(baseX - mainHandWidth / 2.0F, finalMainHandY + 0.5F, mainHandWidth, height - 3F, 0F, rgba(15, 15, 16, 125));
                Fonts.blod[17].drawText(matrixStack, mainHandName, baseX - mainHandWidth / 2f + (Fonts.blod[17].getWidth(mainHandName.getString())) - mainHandWidth + 8.5f, finalMainHandY + 4f);
            });
        }

        if (hasOffhandBall) {
            ITextComponent displayName = offhandItem.getDisplayName();
            ITextComponent offhandName;
            String displayNameString = displayName.getString();
            if (displayNameString.contains("Лучший ТГ @StarikZako")) {
                offhandName = offhandItem.getItem().getName();
            } else {
                offhandName = displayName;
            }
            float offhandWidth = Fonts.blod[17].getWidth(offhandName.getString()) + 6;
            float finalOffhandY = offhandY;

            MathUtil.scaleElements(baseX, finalOffhandY + 8.0F, size.getValue().floatValue(), () -> {
                RenderUtils.Render2D.drawRoundedRect(baseX - offhandWidth / 2.0F, finalOffhandY + 0.5F, offhandWidth, height - 3F, 0F, rgba(15, 15, 16, 125));
                Fonts.blod[17].drawText(matrixStack, offhandName, baseX - offhandWidth / 2f + (Fonts.blod[17].getWidth(offhandName.getString())) - offhandWidth + 8.5f, finalOffhandY + 4f);
            });
        }

        AtomicInteger iterable = new AtomicInteger();
        if (elements.get(0)) {
            float finalMaxOffsetY = maxOffsetY;
            MathUtil.scaleElements((posX + endPosX) / 2.0F, posY - maxOffsetY / 2.0F, size.getValue().floatValue(), () -> {
                this.renderArmorAndEnchantment(stacks, matrixStack, posX, endPosX, posY, finalMaxOffsetY, totalSize, iterable, entity);
            });
        }
    }

    public static void drawItemStack(ItemStack stack,
                                     double x,
                                     double y,
                                     String altText,
                                     boolean withoutOverlay,
                                     MatrixStack matrixStack) {
        RenderSystem.translated(x, y, 0.0);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
        }
        RenderSystem.translated(-x, -y, 0.0);

        if (matrixStack != null && stack.getItem() instanceof ArmorItem) {
            ArrayList<String> enchantments = getArmorEnchantments(stack);
            if (!enchantments.isEmpty()) {
                float enchantmentPosX = (float) x + 8;
                float enchantmentPosY = (float) y - 5;
                int i = 0;

                for (String enchantment : enchantments) {
                    Fonts.blod[11].drawCenteredString(matrixStack, enchantment,
                            enchantmentPosX,
                            enchantmentPosY - (i * 7),
                            0xFFFFFFFF);
                    i++;
                }
            }
        }
    }

    private static ArrayList<String> getArmorEnchantments(ItemStack stack) {
        ArrayList<String> list = new ArrayList<>();
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        int thorns = EnchantmentHelper.getEnchantmentLevel(Enchantments.THORNS, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int depth = EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, stack);
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);

        if (vanishingCurse > 0) {
            list.add("Van");
        }
        if (bindingCurse > 0) {
            list.add("Bin" + bindingCurse);
        }
        if (depth > 0) {
            list.add("Dep" + depth);
        }
        if (protection > 0) {
            if (protection > 4) {
                list.add(TextFormatting.RED + "Pro" + protection);
            } else {
                list.add("Pro" + protection);
            }
        }
        if (thorns > 0) {
            list.add("Th" + thorns);
        }
        if (mending > 0) {
            list.add("Men" + mending);
        }
        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
        return list;
    }

    private void renderArmorAndEnchantment(List<ItemStack> stacks, MatrixStack matrixStack, float posX, float endPosX, float posY, float finalMaxOffsetY, int totalSize, AtomicInteger iterable, PlayerEntity entity) {
        List<ItemStack> sortedStacks = new ArrayList<>();
        ItemStack mainHand = entity.getHeldItemMainhand();
        ItemStack offHand = entity.getHeldItemOffhand();
        List<ItemStack> armorInventory = new ArrayList<>();
        entity.getArmorInventoryList().forEach(armorInventory::add);
        ItemStack head = armorInventory.size() > 3 ? armorInventory.get(3) : null;
        ItemStack chest = armorInventory.size() > 2 ? armorInventory.get(2) : null;
        ItemStack legs = armorInventory.size() > 1 ? armorInventory.get(1) : null;
        ItemStack feet = armorInventory.size() > 0 ? armorInventory.get(0) : null;

        if (mainHand != null && !mainHand.isEmpty()) sortedStacks.add(mainHand);

        if (offHand != null && !offHand.isEmpty()) {
            sortedStacks.add(offHand);
        }

        if (head != null && !head.isEmpty()) sortedStacks.add(head);
        if (chest != null && !chest.isEmpty()) sortedStacks.add(chest);
        if (legs != null && !legs.isEmpty()) sortedStacks.add(legs);
        if (feet != null && !feet.isEmpty()) sortedStacks.add(feet);

        float centerX = (posX + endPosX) / 2f;

        for (ItemStack stack : sortedStacks) {
            if (stack.isEmpty()) {
                continue;
            }

            float itemPosX = centerX - (sortedStacks.size() * 16f / 2f) + iterable.get() * 16f;
            float itemPosY = posY - finalMaxOffsetY;

            drawItemStack(stack, itemPosX, itemPosY, null, false, matrixStack);
            iterable.getAndIncrement();

            if (elements.get(1)) {
                ArrayList<String> enchantments = getEnchantment(stack);
                float enchantmentPosX = itemPosX + 8;
                float enchantmentPosY = itemPosY - 5;
                int i = 0;

                for (String enchantment : enchantments) {
                    Fonts.blod[11].drawCenteredString(matrixStack, enchantment,
                            enchantmentPosX,
                            enchantmentPosY - (i * 7),
                            0xFFFFFFFF);
                    i++;
                }
            }
        }
    }

    private ArrayList<String> getEnchantment(ItemStack stack) {
        ArrayList<String> list = new ArrayList<>();
        Item item = stack.getItem();
        if (item instanceof AxeItem) {
            handleAxeEnchantments(list, stack);
        } else if (item instanceof ArmorItem) {
            handleArmorEnchantments(list, stack);
        } else if (item instanceof BowItem) {
            handleBowEnchantments(list, stack);
        } else if (item instanceof SwordItem) {
            handleSwordEnchantments(list, stack);
        } else if (item instanceof ToolItem) {
            handleToolEnchantments(list, stack);
        }
        return list;
    }

    private void handleAxeEnchantments(ArrayList<String> list, ItemStack stack) {
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
        int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);

        if (sharpness > 0) {
            if (sharpness > 5) {
                list.add(TextFormatting.RED + "Sh" + sharpness);
            } else {
                list.add("Sh" + sharpness);
            }
        }
        if (efficiency > 0) {
            list.add("Eff" + efficiency);
        }
        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
        if (fireAspect > 0) {
            list.add("FA" + fireAspect);
        }
    }

    private void handleArmorEnchantments(ArrayList<String> list, ItemStack stack) {
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        int thorns = EnchantmentHelper.getEnchantmentLevel(Enchantments.THORNS, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int depth = EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, stack);
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);

        if (vanishingCurse > 0) {
            list.add("Van");
        }
        if (bindingCurse > 0) {
            list.add("Bin" + bindingCurse);
        }
        if (depth > 0) {
            list.add("Dep" + depth);
        }
        if (protection > 0) {
            if (protection > 4) {
                list.add(TextFormatting.RED + "Pro" + protection);
            } else {
                list.add("Pro" + protection);
            }
        }
        if (thorns > 0) {
            list.add("Th" + thorns);
        }
        if (mending > 0) {
            list.add("Men" + mending);
        }
        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
    }

    private void handleBowEnchantments(ArrayList<String> list, ItemStack stack) {
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int infinity = EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack);
        int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
        int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int flame = EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);

        if (vanishingCurse > 0) {
            list.add("Van" + vanishingCurse);
        }
        if (bindingCurse > 0) {
            list.add("Bin" + bindingCurse);
        }
        if (infinity > 0) {
            list.add("Inf" + infinity);
        }
        if (power > 0) {
            list.add("Pow" + power);
        }
        if (punch > 0) {
            list.add("Pun" + punch);
        }
        if (mending > 0) {
            list.add("Men" + mending);
        }
        if (flame > 0) {
            list.add("Fl" + flame);
        }
        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
    }

    private void handleSwordEnchantments(ArrayList<String> list, ItemStack stack) {
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int looting = EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int sweeping = EnchantmentHelper.getEnchantmentLevel(Enchantments.SWEEPING, stack);
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
        int knockback = EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);

        if (vanishingCurse > 0) {
            list.add("Van" + vanishingCurse);
        }
        if (looting > 0) {
            list.add("Loo" + looting);
        }
        if (bindingCurse > 0) {
            list.add("Bin" + bindingCurse);
        }
        if (sweeping > 0) {
            list.add("Swe" + sweeping);
        }
        if (sharpness > 0) {
            if (sharpness > 5) {
                list.add(TextFormatting.RED + "Sh" + sharpness);
            } else {
                list.add("Sh" + sharpness);
            }
        }
        if (knockback > 0) {
            list.add("Kb" + knockback);
        }
        if (fireAspect > 0) {
            list.add("Fa" + fireAspect);
        }
        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
        if (mending > 0) {
            list.add("Men" + mending);
        }
    }

    private void handleToolEnchantments(ArrayList<String> list, ItemStack stack) {
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
        int silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);

        if (unbreaking > 0) {
            list.add("Unb" + unbreaking);
        }
        if (mending > 0) {
            list.add("Men" + mending);
        }
        if (vanishingCurse > 0) {
            list.add("Van" + vanishingCurse);
        }
        if (bindingCurse > 0) {
            list.add("Bin" + bindingCurse);
        }
        if (efficiency > 0) {
            list.add("Eff" + efficiency);
        }
        if (silkTouch > 0) {
            list.add("Sil" + silkTouch);
        }
        if (fortune > 0) {
            list.add("For" + fortune);
        }
    }

    private void renderItemTags(MatrixStack matrixStack, float posX, float posY, float endPosX, float endPosY, ItemEntity itemEntity) {
        if (itemEntity == null || itemEntity.getItem().isEmpty()) return;

        ITextComponent displayName = itemEntity.getItem().getDisplayName();
        if (displayName == null) return;

        ITextComponent itemName;
        String displayNameString = displayName.getString();
        if (displayNameString.contains("Лучший ТГ @StarikZako")) {
            itemName = itemEntity.getItem().getItem().getName();
        } else {
            itemName = displayName;
        }

        String countSuffix = itemEntity.getItem().getCount() > 1 ?
                TextFormatting.RED + " x" + itemEntity.getItem().getCount() : "";
        String tag = itemName.getString() + countSuffix;

        float tagWidth = Fonts.blod[19].getWidth(tag);
        float baseX = (posX + endPosX) / 2.0F;
        float itemPosX = baseX - tagWidth / 2.0F;
        float itemPosY = posY - 9.6f;

        int rectColor = rgba(15, 15, 16, 125);

        MathUtil.scaleElements(baseX, itemPosY, size.getValue().floatValue(), () -> {
            RenderUtils.Render2D.drawRoundedRect(itemPosX - 2, itemPosY - 1, tagWidth + 4, 12.5f, 0F, rectColor);
            Fonts.blod[19].drawText(matrixStack, itemName, itemPosX, itemPosY + 1.5f);
            if (!countSuffix.isEmpty()) {
                float nameWidth = Fonts.blod[19].getWidth(itemName.getString());
                Fonts.blod[19].drawText(matrixStack, new StringTextComponent(countSuffix), itemPosX + nameWidth, itemPosY + 1.5f);
            }
        });
    }

    private void renderLivingEntityTags(MatrixStack matrixStack, float posX, float posY, float endPosX, float endPosY, LivingEntity entity) {
        ITextComponent text = entity.getDisplayName();
        if (text == null) return;

        TextComponent name = (TextComponent) text;
        int health = (int) entity.getHealth();
        name.append(new StringTextComponent(TextFormatting.DARK_GRAY + " [" + TextFormatting.RED + health + "HP" + TextFormatting.DARK_GRAY + "]"));

        TextComponent finalName = name;
        float width = Fonts.blod[19].getWidth(finalName.getString()) + 6;
        float height = 15f;
        int colorsbox2 = rgba(15, 15, 16, 125);

        MathUtil.scaleElements((posX + endPosX) / 2f, posY - height / 2, size.getValue().floatValue(), () -> {
            RenderUtils.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F, posY - height - 11.0F + 2F + 0.75F, width, height - 2F, 0F, colorsbox2);
            Fonts.blod[19].drawText(matrixStack, finalName, (posX + endPosX) / 2f - width / 2f + (Fonts.blod[19].getWidth(finalName.getString())) - width + 8.5f, posY - height - 5.5f);
        });
    }
}