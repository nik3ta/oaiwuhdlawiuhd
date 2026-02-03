package nuclear.control;

import com.google.common.eventbus.EventBus;
import mods.proxy.ProxyConnection;
import mods.waveycapes.WaveyCapesBase;
import nuclear.control.cmd.CmdManager;
import nuclear.control.cmd.macro.MacroManager;
import nuclear.control.config.ConfigManager;
import nuclear.control.config.LastAccountConfig;
import nuclear.control.friend.FriendManager;
import nuclear.control.staff.StaffManager;
import nuclear.control.notif.NotifManager;
import nuclear.ui.alt.AltConfig;
import nuclear.ui.alt.AltManager;
import nuclear.ui.clickgui.Window;
import nuclear.control.user.UserManager;
import wtf.wither.Api;

public class Manager {

    public static nuclear.module.api.Manager FUNCTION_MANAGER;
    public static CmdManager COMMAND_MANAGER;
    public static FriendManager FRIEND_MANAGER;
    public static MacroManager MACRO_MANAGER;
    public static LastAccountConfig LAST_ACCOUNT_CONFIG;
    public static WaveyCapesBase WAVYCAPES_BASE;
    public static StaffManager STAFF_MANAGER;
    public static Window CLICK_GUI;
    public static ConfigManager CONFIG_MANAGER;
    public static Api USER_PROFILE;
    public static NotifManager NOTIFICATION_MANAGER;
    public static AltManager ALT;
    public static EventBus EVENT_BUS;
    public static AltConfig ALT_CONFIG;
    public static ProxyConnection PROXY_CONN;

    static {
        EVENT_BUS = new EventBus();
    }
}
