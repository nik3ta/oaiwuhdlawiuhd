package nuclear.module.impl.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.player.GuiMove;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.ui.autoswap.AutoSwapUI;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Annotation(name = "AutoSwap", type = TypeList.Combat, desc = "Авто свап предметов левой руки")
public class AutoSwap extends Module {

    public static BindSetting swapKey = new BindSetting("Свап", 0);
    final ModeSetting swapMode = new ModeSetting("Режим", "Обычный", "Обычный", "Колесо");

    public static net.minecraft.item.ItemStack[] wheelSegmentItems = new net.minecraft.item.ItemStack[3];

    private final ModeSetting itemType = new ModeSetting("Свапать с", "Щит", "Щит", "Геплы", "Тотем", "Шар ");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Геплы", "Щит", "Геплы", "Тотем", "Шар ");
    public final BooleanSetting notif = new BooleanSetting("Отображать свап предметов", true);
    private boolean waitingForSwap = false;
    private int targetSlot = -1;
    private int targetInventorySlot = -1;
    private int desiredWeaponSlot = -1;
    private boolean shouldRestoreWeaponSlot = false;

    private final TimerUtil stopWatch = new TimerUtil();
    private boolean isSwapping = false;

    public AutoSwap() {
        if (swapKey == null) {
            swapKey = new BindSetting("Свап", 0);
        }
        addSettings(swapMode, swapKey, itemType, swapType, notif);

        itemType.setVisible(() -> swapMode.is("Обычный"));
        swapType.setVisible(() -> swapMode.is("Обычный"));

        for (int i = 0; i < wheelSegmentItems.length; i++) {
            if (wheelSegmentItems[i] == null) {
                wheelSegmentItems[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventKey e) {
            if (swapMode.is("Колесо")) {
                if (e.key == swapKey.getKey()) {
                    if (mc.currentScreen == null || !(mc.currentScreen instanceof AutoSwapUI)) {
                        mc.displayGuiScreen(new AutoSwapUI());
                    }
                }
            } else {
                if (e.key == swapKey.getKey()) {
                    if (stopWatch.isReached(85) && !isSwapping && !waitingForSwap) {
                        isSwapping = true;

                        ItemStack offhandItemStack = mc.player.getHeldItemOffhand();
                        Item selectedItem = getSelectedItem();
                        Item swapItem = getSwapItem();

                        int fromSlot = -1;
                        int toSlot = 45;

                        if (offhandItemStack.getItem() == selectedItem) {
                            fromSlot = getSlot(swapItem);
                        } else {
                            fromSlot = getSlot(selectedItem);
                        }

                        if (fromSlot >= 0 && swapItem != Items.AIR && selectedItem != Items.AIR) {
                            int fromSlotInHotbar = fromSlot >= 36 ? fromSlot - 36 : fromSlot;
                            shouldRestoreWeaponSlot = fromSlotInHotbar >= 0 && fromSlotInHotbar < 9 && desiredWeaponSlot != -1;
                            if (GuiMove.syncSwap.get() && Manager.FUNCTION_MANAGER.guiMove.state && !GuiMove.mode.is("Vanila")) {
                                waitingForSwap = true;
                                targetSlot = fromSlot;
                                targetInventorySlot = toSlot;

                                GuiMove.stopMovementTemporarily(0.06f);
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(45);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }

                                    mc.execute(() -> {
                                        if (mc.player != null && waitingForSwap) {
                                            InventoryUtils.moveItem(targetSlot, targetInventorySlot);
                                            notifySwap(mc.player.getHeldItemOffhand());
                                            stopWatch.reset();
                                            waitingForSwap = false;
                                            isSwapping = false;

                                        }
                                    });
                                }).start();
                            } else {
                                InventoryUtils.moveItem(fromSlot, toSlot);
                                notifySwap(mc.player.getHeldItemOffhand());
                            }

                                
                            stopWatch.reset();
                            isSwapping = false;
                        }
                    } else {
                        isSwapping = false;
                    }
                    }
                }
            }

        return false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private void notifySwap(ItemStack stack) {
        if (notif.get() && stack.getItem() != Items.AIR) {
            Manager.NOTIFICATION_MANAGER.add(
                    stack.getDisplayName().getString(),
                    "Function Debug",
                    3,
                    this.category,
                    stack
            );
        }
    }

    private Item getSwapItem() {
        return getItemByType(swapType.get());
    }

    private Item getSelectedItem() {
        return getItemByType(itemType.get());
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар " -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }

    private int getSlot(Item item) {
        int finalSlot = -1;
        
        // Если ищем тотем, приоритет зачарованным тотемам
        if (item == Items.TOTEM_OF_UNDYING) {
            // Сначала ищем зачарованный тотем
            for (int i = 0; i < 36; ++i) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == item && stack.isEnchanted()) {
                    finalSlot = i;
                    break;
                }
            }
            
            // Если зачарованный тотем не найден, ищем обычный
            if (finalSlot == -1) {
                for (int i = 0; i < 36; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                        finalSlot = i;
                        break;
                    }
                }
            }
        } else {
            // Для остальных предметов обычная логика
            for (int i = 0; i < 36; ++i) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    finalSlot = i;
                    break;
                }
            }
        }

        if (finalSlot < 9 && finalSlot != -1) {
            finalSlot += 36;
        }

        return finalSlot;
    }

    @Override
    public JsonObject save() {
        JsonObject object = super.save();
        
        JsonArray wheelItemsArray = new JsonArray();
        for (ItemStack stack : wheelSegmentItems) {
            if (stack == null || stack.isEmpty()) {
                wheelItemsArray.add("");
            } else {
                try {
                    CompoundNBT nbt = new CompoundNBT();
                    stack.write(nbt);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    CompressedStreamTools.writeCompressed(nbt, baos);
                    String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    wheelItemsArray.add(base64);
                } catch (Exception e) {
                    wheelItemsArray.add("");
                }
            }
        }
        object.add("wheelSegmentItems", wheelItemsArray);
        
        return object;
    }

    @Override
    public void load(JsonObject object, boolean start) {
        super.load(object, start);
        
        if (object.has("wheelSegmentItems")) {
            JsonArray wheelItemsArray = object.getAsJsonArray("wheelSegmentItems");
            for (int i = 0; i < wheelSegmentItems.length && i < wheelItemsArray.size(); i++) {
                JsonElement element = wheelItemsArray.get(i);
                if (element.isJsonPrimitive() && !element.getAsString().isEmpty()) {
                    try {
                        String base64 = element.getAsString();
                        byte[] bytes = Base64.getDecoder().decode(base64);
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                        CompoundNBT nbt = CompressedStreamTools.readCompressed(bais);
                        ItemStack loadedStack = ItemStack.read(nbt);
                        wheelSegmentItems[i] = loadedStack != null ? loadedStack : ItemStack.EMPTY;
                    } catch (Exception e) {
                        wheelSegmentItems[i] = ItemStack.EMPTY;
                    }
                } else {
                    wheelSegmentItems[i] = ItemStack.EMPTY;
                }
            }
        }
    }
}