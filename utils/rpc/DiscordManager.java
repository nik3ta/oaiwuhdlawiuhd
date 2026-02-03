package nuclear.utils.rpc;

import lombok.Getter;
import nuclear.control.Manager;
import nuclear.utils.rpc.utils.DiscordEventHandlers;
import nuclear.utils.rpc.utils.DiscordRPC;
import nuclear.utils.rpc.utils.DiscordRichPresence;
import nuclear.utils.rpc.utils.RPCButton;

import java.util.concurrent.CompletableFuture;

@Getter
public class DiscordManager {

    private DiscordDaemonThread discordDaemonThread;
    private long APPLICATION_ID;

    private volatile boolean running;
    private volatile boolean initialized;

    private String image;
    private String telegram;
    private String site;

    private void cppInit() {
        discordDaemonThread = new DiscordDaemonThread();
        APPLICATION_ID = 1454550485871628410L;
        running = true;
        image = "https://s12.gifyu.com/images/bhzrg.gif";
        telegram = "https://t.me/nucleardlc/";
        site = "https://nucleardlc.xyz/";
    }

    public void init() {
        cppInit();
        
        CompletableFuture.runAsync(() -> {
            try {
                DiscordRichPresence.Builder builder = new DiscordRichPresence.Builder();
                DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
                DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(APPLICATION_ID), handlers, true, "");
                builder.setStartTimestamp((System.currentTimeMillis() / 1000));
                String uid = Manager.USER_PROFILE.getUID();
                String username = Manager.USER_PROFILE.getName();

                builder.setState("Uid » " + uid);
                builder.setLargeImage(image, username);
                builder.setButtons(RPCButton.create("Купить", site), RPCButton.create("Телеграм", telegram));
                DiscordRPC.INSTANCE.Discord_UpdatePresence(builder.build());
                initialized = true;
                discordDaemonThread.start();
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        });
    }

    public DiscordManager start() {
        init();
        return this;
    }

    public void stopRPC() {
        if (initialized) {
            try {
                DiscordRPC.INSTANCE.Discord_Shutdown();
            } catch (Exception ignored) {}
        }
        if (discordDaemonThread != null) {
            discordDaemonThread.interrupt();
        }
        this.running = false;
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15 * 1000);
                }
            } catch (Exception exception) {
                stopRPC();
            }

            super.run();
        }
    }
}