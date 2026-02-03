package nuclear.ui.alt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Account {
    public String accountName;
    public Date creationDate;
    public long dateAdded;
    public ResourceLocation skin;
    public float x, y;
    public boolean pinned = false;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    public Account(String accountName) {
        this(accountName, System.currentTimeMillis());
    }

    public Account(String accountName, long dateAdded) {
        this.accountName = accountName;
        this.dateAdded = dateAdded;
        this.creationDate = new Date();
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8));
        this.skin = DefaultPlayerSkin.getDefaultSkin(uuid);
        resolveUUIDAsync(accountName, uuid);
    }

    private void resolveUUIDAsync(String name, UUID fallbackUUID) {
        EXECUTOR.submit(() -> {
            try (InputStreamReader in = new InputStreamReader(
                    new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream(),
                    StandardCharsets.UTF_8)) {
                JsonObject json = new Gson().fromJson(in, JsonObject.class);
                String id = json.get("id").getAsString();
                UUID uuid = UUID.fromString(id.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                loadSkinAsync(uuid);
            } catch (IOException e) {
                loadSkinAsync(fallbackUUID);
            }
        });
    }

    private void loadSkinAsync(UUID uuid) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getSkinManager().loadProfileTextures(
                    new GameProfile(uuid, accountName),
                    (type, loc, tex) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            skin = loc;
                        }
                    }, false);
        });
    }
}

