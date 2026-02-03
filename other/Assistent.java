package nuclear.module.impl.other;

import net.minecraft.client.Minecraft;
import net.minecraft.item.AirItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.InfoSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.world.InventoryUtils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Annotation(name = "Assistent", type = TypeList.Other, desc = "Автоматизирует действие")
public class Assistent extends Module {
    public ModeSetting mode = new ModeSetting("Выбор режимов", "FunTime", "FunTime", "HolyWorld", "ReallyWorld");
    public InfoSetting binding = new InfoSetting("Бинды", () -> {
    });
    private BindSetting trap = new BindSetting("Трапа", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting diz = new BindSetting("Дезорент", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting plast = new BindSetting("Пласт", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting yaw = new BindSetting("Явная пыль", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting aura = new BindSetting("Божья аура", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting death = new BindSetting("Огнен смерч", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting snowball = new BindSetting("Снежок заморозка", 0).setVisible(() -> mode.is("FunTime"));
    private BindSetting stick = new BindSetting("Взрыв штучка", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting gul = new BindSetting("Прощальный гул", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting stun = new BindSetting("Стан", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting trapka = new BindSetting("Взрыв трап", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting snow = new BindSetting("Ком снега", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting trapk = new BindSetting("Трaпкa", 0).setVisible(() -> mode.is("HolyWorld"));
    private BindSetting antipolet = new BindSetting("Анти Полет", 0).setVisible(() -> mode.is("ReallyWorld"));
    private BindSetting dropKey = new BindSetting("Выброс корд", 0);
    public BooleanSetting blockmsg = new BooleanSetting("Блок запрет слова", true).setVisible(() -> mode.is("ReallyWorld"));
    public BooleanSetting coloserp = new BooleanSetting("Не скачивать рп", true).setVisible(() -> mode.is("ReallyWorld"));
    public BooleanSetting sell = new BooleanSetting("Выделять цены", true).setVisible(() -> mode.is("FunTime"));
    InventoryUtils.Hands handUtil = new InventoryUtils.Hands();
    long delay;
    private final String[] forbiddenWords = {
            "экспа", "экспенсив", "экспой", "нуриком", "целкой", "нурлан", "newcode", "ньюкод",
            "нурсултан", "целестиал", "целка", "нурик", "атернос", "aternos", "celka", "nurik",
            "expensive", "celestial", "nursultan", "фанпей", "funpay", "fluger", "акриен", "akrien",
            "фантайм", "funtime", "безмамный", "rich", "рич", "wild", "вилд", "excellent",
            "экселлент", "matix", "impact", "матикс", "импакт", "wurst", "бесплатно донат"};
    int x = -1, z = -1;
    public static CopyOnWriteArrayList<Usables> usables = new CopyOnWriteArrayList<>();
    public static int oldItem;
    public static Item lastItem;
    public static boolean invSwap;
    public static int invSlot;

    public Assistent() {
        addSettings(mode, blockmsg, coloserp, sell, binding, trap, diz, plast, yaw, aura, death, snowball, stick, gul, stun, snow, trapka, trapk, antipolet, dropKey);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mode.is("HolyWorld")) {
            if (event instanceof EventKey e) {
                if (e.key == trapka.getKey()) {
                    InventoryUtils.holySwapClick(Items.PRISMARINE_SHARD, false);
                }

                if (e.key == gul.getKey()) {
                    InventoryUtils.holySwapClick(Items.FIREWORK_STAR, false);
                }

                if (e.key == stun.getKey()) {
                    InventoryUtils.holySwapClick(Items.NETHER_STAR, false);
                }

                if (e.key == snow.getKey()) {
                    InventoryUtils.holySwapClick(Items.SNOWBALL, false);
                }

                if (e.key == stick.getKey()) {
                    InventoryUtils.holySwapClick(Items.FIRE_CHARGE, false);
                }

                if (e.key == trapk.getKey()) {
                    InventoryUtils.holySwapClick(Items.POPPED_CHORUS_FRUIT, false);
                }
            }
        }

        if (event instanceof EventKey e) {
            handleKeyEvent(e);
        } else if (event instanceof EventPacket e) {
            handlePacketEvent(e);
        }
        return false;
    }

    private void handleKeyEvent(EventKey e) {
        if (mode.is("FunTime")) {
            if (e.key == snowball.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("снежок заморозка", true);
                    int invSlot = getItemForName("снежок заморозка", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Снежок заморозка" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.SNOWBALL != null && !mc.player.getCooldownTracker().hasCooldown(Items.SNOWBALL)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Снежок заморозка был" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.SNOWBALL) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Снежок заморозка" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.SNOWBALL, false);
                    }
                }
            }
            if (e.key == diz.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("дезориентация", true);
                    int invSlot = getItemForName("дезориентация", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Дезориентация" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.ENDER_EYE != null && !mc.player.getCooldownTracker().hasCooldown(Items.ENDER_EYE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Дезориентация была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.ENDER_EYE) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Дезориентация" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.ENDER_EYE, false);
                    }
                }
            }
            if (e.key == trap.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("трапка", true);
                    int invSlot = getItemForName("трапка", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Трапка" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.NETHERITE_SCRAP != null && !mc.player.getCooldownTracker().hasCooldown(Items.NETHERITE_SCRAP)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Трапка была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.NETHERITE_SCRAP) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Трапка" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.NETHERITE_SCRAP, false);
                    }
                }
            }
            if (e.key == plast.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("пласт", true);
                    int invSlot = getItemForName("пласт", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Пласт" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    }

                    if (Items.DRIED_KELP != null && !mc.player.getCooldownTracker().hasCooldown(Items.DRIED_KELP)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Пласт был" + TextFormatting.RED + " использован");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.DRIED_KELP) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Пласт" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.DRIED_KELP, false);
                    }
                }
            }
            if (e.key == yaw.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("явная", true);
                    int invSlot = getItemForName("явная", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Явная пыль" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.SUGAR != null && !mc.player.getCooldownTracker().hasCooldown(Items.SUGAR)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Явная пыль была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.SUGAR) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Явная пыль" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.SUGAR, false);
                    }
                }
            }
            if (e.key == aura.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("божья", true);
                    int invSlot = getItemForName("божья", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Божья аура" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    }

                    if (Items.PHANTOM_MEMBRANE != null && !mc.player.getCooldownTracker().hasCooldown(Items.PHANTOM_MEMBRANE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Божья аура была" + TextFormatting.RED + " использована");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.PHANTOM_MEMBRANE) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Божья аура" + TextFormatting.RED + " не была" + TextFormatting.WHITE + " найдена");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.PHANTOM_MEMBRANE, false);
                    }
                }
            }
            if (e.key == death.getKey()) {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);
                    int hbSlot = getItemForName("огненный", true);
                    int invSlot = getItemForName("огненный", false);

                    if (invSlot == -1 && hbSlot == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Огненный смерч" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    }

                    if (Items.FIRE_CHARGE != null && !mc.player.getCooldownTracker().hasCooldown(Items.FIRE_CHARGE)) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                            ClientUtils.sendMessage(TextFormatting.WHITE + "Огненный смерч был" + TextFormatting.RED + " использован");
                        }
                    }
                } else {
                    if (InventoryUtils.getItemSlot(Items.FIRE_CHARGE) == -1) {
                        ClientUtils.sendMessage(TextFormatting.WHITE + "Огненный смерч" + TextFormatting.RED + " не был" + TextFormatting.WHITE + " найден");
                    } else {
                        InventoryUtils.inventorySwapClick(Items.FIRE_CHARGE, false);
                    }
                }
            }
        }
        if (mode.is("ReallyWorld")) {
            if (e.key == antipolet.getKey()) {
                antipolet();
            }
        }
        if (e.key == dropKey.getKey()) {
            mc.player.sendChatMessage("!корды " + (int) mc.player.getPosX() + " " + (int) mc.player.getPosZ());
        }
    }

    private void handlePacketEvent(EventPacket e) {
        if (mode.is("ReallyWorld") && coloserp.get()) {
            if (e.getPacket() instanceof SSendResourcePackPacket) {
                Minecraft.getInstance().getConnection().sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
                Minecraft.getInstance().getConnection().sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
                e.setCancel(true);
            }
        }

        if (e.getPacket() instanceof CChatMessagePacket) {
            CChatMessagePacket chatPacket = (CChatMessagePacket) e.getPacket();
            String message = chatPacket.getMessage().toLowerCase();

            if (mode.is("ReallyWorld") && blockmsg.get()) {
                for (String word : forbiddenWords) {
                    if (message.matches("(?i)\\b" + Pattern.quote(word) + "\\b")) {
                        ClientUtils.sendMessage("" + TextFormatting.GRAY + "Ваше сообщение содержит " + TextFormatting.RED + "запрещенное" + TextFormatting.GRAY + " слово, сообщение не было отправлено");
                        e.setCancel(true);
                        return;
                    }
                }
            }
        }
    }

    private void antipolet() {
        if (InventoryUtils.getItemSlot(Items.FIREWORK_STAR) == -1) {
            ClientUtils.sendMessage(TextFormatting.RED + "У вас отсутствует анти полет!");
        } else {
            InventoryUtils.antipolet(Items.FIREWORK_STAR);
        }
    }


    private int getItemForName(String name, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AirItem) {
                continue;
            }
            String displayName = TextFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName().getString());
            if (displayName != null && displayName.toLowerCase().contains(name)) {
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

    public static class Usables {
        public long time;
        public int time2;
        public float spawnX;
        public float spawnY;
        public float spawnZ;
        public String text;

        public Usables(long time, int time2, float spawnX, float spawnY, float spawnZ, String text) {
            this.time = time;
            this.time2 = time2;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.spawnZ = spawnZ;
            this.text = text;
        }
    }
}