package nuclear.utils.rpc.callbacks;

import com.sun.jna.Callback;
import nuclear.utils.rpc.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser var1);
}