    package nuclear.module.impl.player;

    import net.minecraft.client.gui.screen.ChatScreen;
    import net.minecraft.client.gui.screen.inventory.ChestScreen;
    import net.minecraft.client.gui.screen.inventory.InventoryScreen;
    import net.minecraft.client.settings.KeyBinding;
    import net.minecraft.client.util.InputMappings;
    import net.minecraft.network.IPacket;
    import net.minecraft.network.play.client.CClickWindowPacket;
    import nuclear.control.Manager;
    import nuclear.control.events.Event;
    import nuclear.control.events.impl.game.InventoryCloseEvent;
    import nuclear.control.events.impl.packet.EventPacket;
    import nuclear.control.events.impl.player.EventClickWindow;
    import nuclear.control.events.impl.player.EventUpdate;
    import nuclear.module.TypeList;
    import nuclear.module.api.Annotation;
    import nuclear.module.api.Module;
    import nuclear.module.settings.imp.BooleanSetting;
    import nuclear.module.settings.imp.ModeSetting;
    import nuclear.utils.move.MoveUtil;

    import java.util.ArrayList;
    import java.util.List;

    @Annotation(name = "GuiMove", type = TypeList.Player, desc = "Ходьба в инвентаре")
    public class GuiMove extends Module {
        public static ModeSetting mode = new ModeSetting("Обход", "Vanila", "Vanila", "Spooky", "Grim");
        public static BooleanSetting syncSwap = new BooleanSetting("Синх свапы", false).setVisible(() -> !mode.is("Vanila"));

        public int tick = 0;
        private final List<CClickWindowPacket> pendingPackets = new ArrayList<>();
        public static List<EventClickWindow> packete = new ArrayList<>();
        public static List<EventClickWindow> packete1 = new ArrayList<>();

        public GuiMove() {
            addSettings(mode, syncSwap);
        }

        @Override
        public boolean onEvent(final Event event) {
            if (mc.player == null) return false;

            final KeyBinding[] pressedKeys = {
                    mc.gameSettings.keyBindForward,
                    mc.gameSettings.keyBindBack,
                    mc.gameSettings.keyBindLeft,
                    mc.gameSettings.keyBindRight,
                    mc.gameSettings.keyBindJump,
                    mc.gameSettings.keyBindSprint
            };

            if (event instanceof EventUpdate) {
                if (tick != 0) {
                    for (KeyBinding keyBinding : pressedKeys) {
                        keyBinding.setPressed(false);
                    }
                    tick--;
                    return false;
                }

                if (mc.currentScreen instanceof ChatScreen) {
                    return false;
                }

                if (mc.currentScreen instanceof net.minecraft.client.gui.screen.EditSignScreen) {
                    return false;
                }

                if (mode.is("Grim") && mc.currentScreen instanceof ChestScreen) {
                    return false;
                }

                for (KeyBinding keyBinding : pressedKeys) {
                    boolean isKeyPressed = InputMappings.isKeyDown(mc.getMainWindow().getHandle(), keyBinding.getDefault().getKeyCode());
                    keyBinding.setPressed(isKeyPressed);
                }

            } else if (event instanceof EventPacket) {
                EventPacket packetEvent = (EventPacket) event;
                IPacket<?> p = packetEvent.getPacket();

                if (p instanceof CClickWindowPacket clickPacket) {
                    if (MoveUtil.isMoving()) {
                        if (mc.currentScreen instanceof InventoryScreen) {
                            if (mode.is("Grim")) {
                                pendingPackets.add(clickPacket);
                                packetEvent.cancel();
                            }

                            if (mode.is("Spooky")) {
                                pendingPackets.add(clickPacket);
                                packetEvent.cancel();
                            }
                        }
                    }
                }

            } else if (event instanceof InventoryCloseEvent) {
                InventoryCloseEvent closeEvent = (InventoryCloseEvent) event;

                if (mc.currentScreen instanceof InventoryScreen && !pendingPackets.isEmpty() && MoveUtil.isMoving()) {
                    if (mode.is("Grim") || mode.is("Spooky")) {
                        new Thread(() -> {
                            tick = 5;
                            try {
                                Thread.sleep(mode.is("Spooky") ? 90 : 40);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                            for (CClickWindowPacket pkt : pendingPackets) {
                                if (mc.player != null && mc.player.connection != null) {
                                    mc.player.connection.sendPacketWithoutEvent(pkt);
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                            pendingPackets.clear();
                        }).start();
                        closeEvent.cancel();
                    }
                } else {
                    pendingPackets.clear();
                }
            }

            return false;
        }

        public static void stopMovementTemporarily(float timeInSeconds) {
            GuiMove guiMove = Manager.FUNCTION_MANAGER.guiMove;
            if (guiMove == null || !guiMove.state) return;

            int ticks = Math.max(1, (int) Math.ceil(timeInSeconds * 20.0f));

            guiMove.tick = Math.max(guiMove.tick, ticks);
        }

        @Override
        public void onDisable() {
            super.onDisable();
            pendingPackets.clear();
            packete.clear();
            packete1.clear();
        }
    }
