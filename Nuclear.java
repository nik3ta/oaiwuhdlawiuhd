package nuclear;

import lombok.Getter;
import mods.proxy.MinecraftProxy;
import mods.proxy.ProxyConnection;
import mods.viaversion.viamcp.ViaMCP;
import mods.voicechat.ForgeVoicechatClientMod;
import mods.voicechat.ForgeVoicechatMod;
import mods.waveycapes.WaveyCapesBase;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import nuclear.control.Manager;
import nuclear.control.handler.HandlerManager;
import nuclear.module.api.Module;
import nuclear.utils.language.Translated;
import nuclear.utils.rpc.DiscordManager;
import nuclear.utils.tps.TPSCalc;
import org.lwjgl.glfw.GLFW;
import nuclear.control.cmd.CmdManager;
import nuclear.control.cmd.macro.MacroManager;
import nuclear.control.config.ConfigManager;
import nuclear.control.config.LastAccountConfig;
import nuclear.control.events.EventManager;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.friend.FriendManager;
import nuclear.control.staff.StaffManager;
import nuclear.control.notif.NotifManager;
import nuclear.ui.alt.AltConfig;
import nuclear.ui.alt.AltManager;
import nuclear.ui.clickgui.Window;
import nuclear.control.drag.DragManager;
import nuclear.control.drag.Dragging;
import nuclear.utils.render.ShaderUtils;
import wtf.wither.Api;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Nuclear {
    public static boolean isServer;

    @Getter
    public static ViaMCP viaMCP;

    @Getter
    private DiscordManager discordManager;

    @Getter
    private static TPSCalc tpsCalc;

    private static final File FIRST_RUN_MARKER = new File("C:\\Nuclear\\client_1_16\\Nuclear\\firstrun.json");
    private HandlerManager handlerManager;

    public final File dir = new File("C:\\Nuclear\\client_1_16\\Nuclear");

    public static TPSCalc getServerTPS() {
        return tpsCalc;
    }

    public void init() {
        if (!FIRST_RUN_MARKER.exists()) {
            openLinks();
            try {
                FIRST_RUN_MARKER.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ShaderUtils.init();
        tpsCalc = new TPSCalc();
        EventManager.register(tpsCalc);
        viaMCP = new ViaMCP();
        EventManager.register(viaMCP);
        Manager.USER_PROFILE = new Api();
        Manager.FUNCTION_MANAGER = new nuclear.module.api.Manager();

        Manager.NOTIFICATION_MANAGER = new NotifManager();
        Manager.WAVYCAPES_BASE = new WaveyCapesBase();
        viaMCP = new ViaMCP();
        EventManager.register(viaMCP);

        handlerManager = new HandlerManager();
        handlerManager.init();
        discordManager = new DiscordManager();
        discordManager.start();
        EventManager.register(discordManager);
        try {

            Manager.ALT = new AltManager();

            ConfigManager configManager = new ConfigManager();
            configManager.init();
            String loadedLang = ConfigManager.loadLanguageSetting();
            Translated.setLanguage(loadedLang);

            if (!dir.exists()) {
                dir.mkdirs();
            }
            Manager.ALT_CONFIG = new AltConfig();
            Manager.ALT_CONFIG.init();

            Manager.FRIEND_MANAGER = new FriendManager();
            Manager.FRIEND_MANAGER.init();

            Manager.COMMAND_MANAGER = new CmdManager();
            Manager.COMMAND_MANAGER.init();

            Manager.STAFF_MANAGER = new StaffManager();
            Manager.STAFF_MANAGER.init();

            Manager.MACRO_MANAGER = new MacroManager();
            Manager.MACRO_MANAGER.init();

            Manager.LAST_ACCOUNT_CONFIG = new LastAccountConfig();
            Manager.LAST_ACCOUNT_CONFIG.init();

            Manager.CONFIG_MANAGER = new ConfigManager();
            Manager.CONFIG_MANAGER.init();

            Manager.CLICK_GUI = new Window(new StringTextComponent("A"));
            DragManager.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Manager.PROXY_CONN = new ProxyConnection();
        MinecraftProxy.initialize();
        Manager.WAVYCAPES_BASE.init();
        new ForgeVoicechatMod();
        new ForgeVoicechatClientMod();
    }

    public static void shutDown() {
        if (Manager.CONFIG_MANAGER != null) {
            Manager.CONFIG_MANAGER.saveConfiguration("default");
        }
        if (Manager.LAST_ACCOUNT_CONFIG != null) {
            Manager.LAST_ACCOUNT_CONFIG.updateFile();
        }
        if (Manager.ALT_CONFIG != null) {
            Manager.ALT_CONFIG.updateFile();
        }
        DragManager.save();
        if (Nuclear.getInstance().getDiscordManager() != null) {
            Nuclear.getInstance().getDiscordManager().stopRPC();
        }
    }

    private void openLinks() {
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec("rundll32 url.dll,FileProtocolHandler https://t.me/nucleardlc");
            rt.exec("rundll32 url.dll,FileProtocolHandler https://discord.gg/bq34naXW94");
            rt.exec("rundll32 url.dll,FileProtocolHandler https://nucleardlc.fun");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void keyPress(int key) {
        EventManager.call(new EventKey(key));

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (Manager.CLICK_GUI == null) {
                Manager.CLICK_GUI = new Window(new StringTextComponent("A"));
            }
            Minecraft.getInstance().displayGuiScreen(Manager.CLICK_GUI);
        }

        if (Manager.MACRO_MANAGER != null) {
            Manager.MACRO_MANAGER.onKeyPressed(key);
        }

        List<Module> modules = Manager.FUNCTION_MANAGER.getFunctions();
        for (Module m : modules) {
            if (m.bind == key) {
                m.toggle();
            }
        }
    }

    public static Dragging createDrag(Module module, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(module, name, x, y));
        return DragManager.draggables.get(name);
    }

    @Getter
    private static final Nuclear instance = new Nuclear();
}
