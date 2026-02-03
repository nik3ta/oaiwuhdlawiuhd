package nuclear.module.impl.other;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.player.EventContainerRender;
import nuclear.control.events.impl.player.EventCooldown;
import nuclear.control.events.impl.player.EventHotbarRender;
import nuclear.control.events.impl.player.EventRenderTooltip;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.player.GuiMove;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ColorSetting;
import nuclear.module.settings.imp.InfoSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.render.AnimationUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.world.InventoryUtils;

import java.util.List;

@Annotation(name = "ItemHelper", type = TypeList.Other, desc = "Авто свап предметов левой руки")
public class ItemHelper extends Module {
    private final InfoSetting bind = new InfoSetting("Исп по кнопки",()-> {});
    public BindSetting horus = new BindSetting("Хорус", 0);
    public BindSetting charGapple = new BindSetting("Чарка", 0);
    public BindSetting potion = new BindSetting("Все взр зелье", 0);
    private final InfoSetting health = new InfoSetting("Здоровье",()-> {});
    private final BooleanSetting show_instant_health = new BooleanSetting("Подсвет зелье исцеления", false);
    private final ColorSetting color_instant_health = new ColorSetting("Цвет зелья исцеления",  ColorUtils.hex("#FF2AB9")).setVisible(show_instant_health::get);
    private final BooleanSetting show_enchant_golden_apple = new BooleanSetting("Подсвет чарки", true);
    private final ColorSetting color_enchant_golden_apple = new ColorSetting("Цвет чарки",  ColorUtils.hex("#FFAC93")).setVisible(show_enchant_golden_apple::get);
    private final BooleanSetting show_golden_apple = new BooleanSetting("Подсвет золотые яблоки", false);
    private final ColorSetting color_golden_apple = new ColorSetting("Цвет золотого яблока",  ColorUtils.hex("#E7EB56")).setVisible(show_golden_apple::get);
    private final BooleanSetting show_golden_carrot = new BooleanSetting("Подсвет золотая морковка", false);
    private final ColorSetting color_golden_carrot = new ColorSetting("Цвет золотой морковки",  ColorUtils.hex("#FFA500")).setVisible(show_golden_carrot::get);

    private final InfoSetting other = new InfoSetting("Остальное",()-> {});

    private final BooleanSetting decreaseCooldown = new BooleanSetting("Уменьшать кд предметов", true);
    private final BooleanSetting show_nbt = new BooleanSetting("Показывать nbt предметов", false);

    private final AnimationUtil anim = new AnimationUtil(0.0f, 4);

    InventoryUtils.Hands handUtil = new InventoryUtils.Hands();
    long delay;
    private int prevChorusSlot = -1;
    private int originalChorusInventorySlot = -1;
    private boolean isChorusSelected = false;
    private int prevGappleSlot = -1;
    private int originalGappleInventorySlot = -1;
    private boolean isGappleSelected = false;

    public ItemHelper() {
        addSettings(bind, horus, charGapple, potion, health, show_instant_health, color_instant_health, show_enchant_golden_apple, color_enchant_golden_apple, show_golden_apple, color_golden_apple, show_golden_carrot, color_golden_carrot, other, decreaseCooldown, show_nbt);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            handleKeyEvent(e);
        }

        if (event instanceof EventCooldown e) {
            onCooldown(e);
        }

        if (event instanceof EventContainerRender.Pre e) {
            onContainerRenderPre(e);
        }

        if (event instanceof EventHotbarRender.Pre e) {
            onHotbarRender(e);
        }

        if (event instanceof EventRenderTooltip e) {
            onRenderTooltip(e);
        }
        return false;
    }

    private void handleKeyEvent(EventKey e) {
        if (e.key == potion.getKey()) {
            potion();
        }
        if (e.key == horus.getKey()) {
            handleChorusSwitch();
        }
        if (e.key == charGapple.getKey()) {
            handleGappleSwitch();
        }
    }

    public void onCooldown(EventCooldown e) {
        Item item = e.getItem();
        ItemStack itemStack = new ItemStack(item);
        if (decreaseCooldown.get() && itemStack.isFood()) {
            int reduction = item.getFood().isFastEating() ? 16 : 32;
            int originalTicks = e.getTicks();

            if (originalTicks > reduction && itemStack.getItem() != Items.DRIED_KELP) {
                e.setTicks(originalTicks - reduction);
                IFormattableTextComponent message = new StringTextComponent("Задержка на ").mergeStyle(TextFormatting.GRAY)
                        .append(new StringTextComponent(item.getName().getString()).mergeStyle(TextFormatting.RED))
                        .append(new StringTextComponent(" уменьшена на ").mergeStyle(TextFormatting.GRAY))
                        .append(new StringTextComponent(String.valueOf(reduction / 20.0)).mergeStyle(TextFormatting.RED))
                        .append(new StringTextComponent(" сек").mergeStyle(TextFormatting.GRAY));
                ClientUtils.sendMessage(message);
            }
        }
    }

    public void onContainerRenderPre(EventContainerRender.Pre e) {
        if (mc.currentScreen instanceof CreativeScreen || !(e.getContainer() instanceof PlayerContainer)) return;

        for (Slot slot : e.getContainer().inventorySlots) {
            if (slot == null || !slot.getHasStack()) continue;
            renderItemHighlight(e.getStack(), slot.getStack(), e.getGuiLeft() + slot.xPos, e.getGuiTop() + slot.yPos);
        }
    }

    public void onHotbarRender(EventHotbarRender.Pre e) {
        if (mc.currentScreen instanceof CreativeScreen) return;

        int centerX = mc.getMainWindow().getScaledWidth() / 2;
        int baseY = mc.getMainWindow().getScaledHeight() - 16 - 3;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (stack.isEmpty()) continue;
            renderItemHighlight(e.getStack(), stack, centerX - 90 + i * 20 + 2, baseY);
        }
    }

    private void renderItemHighlight(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        int color = getItemHighlightColor(stack);
        if (color != 0) {
            float norm = (anim.getValue() + 0.75f) / 1.5f;
            int alpha = (int) (75 + norm * 100);
            RenderUtils.Render2D.drawMinecraftRectangle(matrixStack, x, y, 16, 16, ColorUtils.applyOpacity(color, alpha));
        }
    }

    private int getItemHighlightColor(ItemStack stack) {
        if (show_enchant_golden_apple.get() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            return color_enchant_golden_apple.get();
        }
        if (show_golden_apple.get() && stack.getItem() == Items.GOLDEN_APPLE) {
            return color_golden_apple.get();
        }
        if (show_golden_carrot.get() && stack.getItem() == Items.GOLDEN_CARROT) {
            return color_golden_carrot.get();
        }
        if (show_instant_health.get() && InventoryUtils.stackHasAnyEffect(stack, true, false, false, Effects.INSTANT_HEALTH)) {
            return color_instant_health.get();
        }
        return 0;
    }

    public void onRenderTooltip(EventRenderTooltip e) {
        if (!show_nbt.get() || !e.stack.hasTag()) return;
        e.setCancel(true);

        List<ITextComponent> tooltip = mc.currentScreen.getTooltipFromItem(e.stack);
        tooltip.add(StringTextComponent.EMPTY);
        addTag(tooltip, e.stack.getTag(), 0);

        mc.currentScreen.func_243308_b(e.matrixStack, tooltip, e.mouseX, e.mouseY);
    }

    private void addTag(List<ITextComponent> tooltip, CompoundNBT tag, int depth) {
        String indent = "  ".repeat(depth);
        for (String key : tag.keySet()) {
            INBT base = tag.get(key);
            String type = getNbtType(base);
            tooltip.add(new StringTextComponent(indent + "- " + type + ": " + key).mergeStyle(TextFormatting.DARK_GRAY));
        }
    }

    private String getNbtType(INBT nbt) {
        if (nbt instanceof ByteNBT) return "byte";
        if (nbt instanceof ShortNBT) return "short";
        if (nbt instanceof IntNBT) return "int";
        if (nbt instanceof LongNBT) return "long";
        if (nbt instanceof FloatNBT) return "float";
        if (nbt instanceof DoubleNBT) return "double";
        if (nbt instanceof StringNBT) return "string";
        if (nbt instanceof ByteArrayNBT) return "byte[]";
        if (nbt instanceof IntArrayNBT) return "int[]";
        if (nbt instanceof LongArrayNBT) return "long[]";
        if (nbt instanceof ListNBT) return "list";
        if (nbt instanceof CompoundNBT) return "compound";
        return "unknown";
    }

    private void potion() {
        if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
            handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            int hbSlot = findItem(Items.SPLASH_POTION, true);
            int invSlot = findItem(Items.SPLASH_POTION, false);

            if (invSlot == -1 && hbSlot == -1) {
                ClientUtils.sendMessage(TextFormatting.WHITE + "Взрывное зелье не было найдено");
            }

            if (Items.SPLASH_POTION != null) {
                int slot = findAndTrowItem(hbSlot, invSlot);
                if (slot != -1 && slot > 8) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }
                    mc.playerController.pickItem(slot);
                    ClientUtils.sendMessage(TextFormatting.WHITE + "Взрывное зелье было" + TextFormatting.RED + " использовано");
                }
            }
        } else {

            handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.SPLASH_POTION) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }
                    InventoryUtils.inventorySwapClick(Items.SPLASH_POTION, false);
                    return;
                }
            }
            ClientUtils.sendMessage(TextFormatting.WHITE + "Взрывное зелье не было найдено");
        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }

    private void handleChorusSwitch() {
        if (isChorusSelected) {
            ItemStack currentStack = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
            if (currentStack.getItem() == Items.CHORUS_FRUIT) {
                if (originalChorusInventorySlot != -1) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }
                    InventoryUtils.moveItem(mc.player.inventory.currentItem, originalChorusInventorySlot);
                } else if (prevChorusSlot != -1) {
                    mc.player.inventory.currentItem = prevChorusSlot;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(prevChorusSlot));
                }
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
                return;
            } else {
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
            }
        }

        int chorusSlot = findItem(Items.CHORUS_FRUIT, true);
        int currentHotbarSlot = mc.player.inventory.currentItem;

        if (chorusSlot != -1) {
            if (chorusSlot != currentHotbarSlot) {
                prevChorusSlot = currentHotbarSlot;
                mc.player.inventory.currentItem = chorusSlot;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(chorusSlot));
            }
            isChorusSelected = true;
            originalChorusInventorySlot = -1;
        } else {
            chorusSlot = findItem(Items.CHORUS_FRUIT, false);
            if (chorusSlot != -1) {
                int targetHotbarSlot = currentHotbarSlot + 36;
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    GuiMove.stopMovementTemporarily(0.085f);
                }
                InventoryUtils.moveItem(chorusSlot, targetHotbarSlot);
                mc.player.connection.sendPacket(new CHeldItemChangePacket(currentHotbarSlot));
                isChorusSelected = true;
                originalChorusInventorySlot = chorusSlot;
                prevChorusSlot = -1;
            } else {
                ClientUtils.sendMessage(TextFormatting.RED + "Хорус не найден!");
                isChorusSelected = false;
                prevChorusSlot = -1;
                originalChorusInventorySlot = -1;
            }
        }
    }

    private void handleGappleSwitch() {
        if (isGappleSelected) {
            ItemStack currentStack = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
            if (currentStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                if (originalGappleInventorySlot != -1) {
                    if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                        GuiMove.stopMovementTemporarily(0.085f);
                    }
                    InventoryUtils.moveItem(mc.player.inventory.currentItem, originalGappleInventorySlot);
                } else if (prevGappleSlot != -1) {
                    mc.player.inventory.currentItem = prevGappleSlot;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(prevGappleSlot));
                }
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
                return;
            } else {
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
            }
        }

        int gappleSlot = findItem(Items.ENCHANTED_GOLDEN_APPLE, true);
        int currentHotbarSlot = mc.player.inventory.currentItem;

        if (gappleSlot != -1) {
            if (gappleSlot != currentHotbarSlot) {
                prevGappleSlot = currentHotbarSlot;
                mc.player.inventory.currentItem = gappleSlot;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(gappleSlot));
            }
            isGappleSelected = true;
            originalGappleInventorySlot = -1;
        } else {
            gappleSlot = findItem(Items.ENCHANTED_GOLDEN_APPLE, false);
            if (gappleSlot != -1) {
                int targetHotbarSlot = currentHotbarSlot + 36;
                if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                    GuiMove.stopMovementTemporarily(0.085f);
                }
                InventoryUtils.moveItem(gappleSlot, targetHotbarSlot);
                mc.player.connection.sendPacket(new CHeldItemChangePacket(currentHotbarSlot));
                isGappleSelected = true;
                originalGappleInventorySlot = gappleSlot;
                prevGappleSlot = -1;
            } else {
                ClientUtils.sendMessage(TextFormatting.RED + "Чарка не найдена!");
                isGappleSelected = false;
                prevGappleSlot = -1;
                originalGappleInventorySlot = -1;
            }
        }
    }

    private int findItemNoChanges(int endSlot, Item item) {
        for (int i = 0; i < endSlot; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
