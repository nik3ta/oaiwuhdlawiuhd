package nuclear.control.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    public static final File CONFIG_DIR = new File("C:\\Nuclear\\client_1_16\\Nuclear\\config");
    private final File autoCfgDir = new File("C:\\Nuclear\\client_1_16\\Nuclear\\config\\default.cfg");
    private static final JsonParser jsonParser = new JsonParser();

    public void init() throws Exception {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        if (!autoCfgDir.exists()) {
            autoCfgDir.createNewFile();
        }
        if (autoCfgDir.exists()) {
            // ��������, ��� ���� �� ������
            if (autoCfgDir.length() > 0) {
                loadConfiguration("default", true);
            } else {
                // ���� ���� ������, �������������� ���
                saveConfiguration("default");
            }
        }
    }

    public static void saveLanguageSetting(String language) {
        try {
            File langFile = new File("C:\\Nuclear\\client_1_16\\Nuclear", "language.json");
            JsonObject languageConfig = new JsonObject();
            languageConfig.addProperty("language", language);
            // Keep backward compatibility
            languageConfig.addProperty("isRussian", "RUS".equals(language));

            writeJsonToFile(langFile, languageConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Backward compatibility
    public static void saveLanguageSetting(boolean isRussian) {
        saveLanguageSetting(isRussian ? "RUS" : "ENG");
    }

    public static String loadLanguageSetting() {
        try {
            File langFile = new File("C:\\Nuclear\\client_1_16\\Nuclear", "language.json");
            if (langFile.exists()) {
                JsonElement element = readFileAsJson(langFile);
                if (element != null && element.isJsonObject()) {
                    JsonObject languageConfig = element.getAsJsonObject();
                    // Try new format first
                    if (languageConfig.has("language")) {
                        String lang = languageConfig.get("language").getAsString();
                        if (lang != null && (lang.equals("RUS") || lang.equals("ENG") || lang.equals("PL") || lang.equals("UKR"))) {
                            return lang;
                        }
                    }
                    // Fallback to old format
                    if (languageConfig.has("isRussian")) {
                        return languageConfig.get("isRussian").getAsBoolean() ? "RUS" : "ENG";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ENG"; // Default to ENG
    }

    // Backward compatibility
    public static boolean loadLanguageSettingBoolean() {
        String lang = loadLanguageSetting();
        return "RUS".equals(lang);
    }

    public List<String> getAllConfigurations() {
        List<String> configurations = new ArrayList<>();
        File[] files = CONFIG_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".cfg")) {
                    String configName = file.getName().substring(0, file.getName().lastIndexOf(".cfg"));
                    configurations.add(configName);
                }
            }
        }
        return configurations;
    }

    public void loadConfiguration(String configuration, boolean start) {
        Config config = findConfig(configuration);
        if (config == null) {
            return;
        }

        try {
            JsonElement element = readFileAsJson(config.getFile());
            if (element != null) {
                JsonObject object = element.getAsJsonObject();

                config.load(object, configuration, start);
            } else {
                saveConfiguration(configuration);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private static JsonElement readFileAsJson(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return jsonParser.parse(content);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static void writeJsonToFile(File file, JsonElement jsonElement) {
        Thread thread = new Thread(() -> {
            try {
                String content = new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void saveConfiguration(String configuration) {
        Config config = findConfig(configuration);
        if (config == null) {
            config = new Config(configuration);
        }

        writeJsonToFile(config.getFile(), config.save());
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        if (new File(CONFIG_DIR, configName + ".cfg").exists())
            return new Config(configName);

        return null;
    }

    public void deleteConfig(String configName) {
        if (configName == null)
            return;
        Config config = findConfig(configName);
        if (config != null) {
            File file = config.getFile();
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                }
            }
        }
    }
}
