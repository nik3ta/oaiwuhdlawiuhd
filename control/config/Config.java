package nuclear.control.config;

import com.google.gson.JsonObject;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.ui.clickgui.Panel;
import nuclear.utils.ClientUtils;

import java.io.File;

public final class Config {

    private final File file;
    public String author;

    public Config(String name) {
        this.file = new File(ConfigManager.CONFIG_DIR, name + ".cfg");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();

        JsonObject modulesObject = new JsonObject();
        Manager.FUNCTION_MANAGER.getFunctions().forEach(module -> modulesObject.add(module.name, module.save()));
        jsonObject.add("Features", modulesObject);

        JsonObject stylesObject = new JsonObject();
        jsonObject.add("styles", stylesObject);

        JsonObject otherObject = new JsonObject();
        if (!otherObject.has("author"))
            otherObject.addProperty("author", Manager.USER_PROFILE.getName());
        if (!otherObject.has("time"))
            otherObject.addProperty("time", System.currentTimeMillis());

        jsonObject.add("Others", otherObject);

        JsonObject colorsObject = Panel.saveColors();
        jsonObject.add("Colors", colorsObject);

        return jsonObject;
    }

    public void load(JsonObject object, String configuration, boolean start) {
        // Обрабатываем модули - если секция "Features" отсутствует или модуль отсутствует в ней,
        // модуль сбрасывается к дефолтным значениям
        Manager.FUNCTION_MANAGER.getFunctions().forEach(module -> {
            if (!start && module.isState()) {
                module.setState(false);
            }
            JsonObject moduleObject = null;
            if (object != null && object.has("Features")) {
                JsonObject modulesObject = object.getAsJsonObject("Features");
                if (modulesObject.has(module.name)) {
                    moduleObject = modulesObject.getAsJsonObject(module.name);
                }
            }
            // Если модуль отсутствует в конфиге, moduleObject будет null,
            // и метод load установит все настройки в дефолтные значения
            module.load(moduleObject, start);
        });

        try {
            if (object.has("Colors")) {
                JsonObject colorsObject = object.getAsJsonObject("Colors");
                Panel.loadColors(colorsObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClientUtils.sendMessage("Конфигурация " + TextFormatting.RED + configuration + TextFormatting.RESET + " загружена");
    }

    public File getFile() {
        return file;
    }
}