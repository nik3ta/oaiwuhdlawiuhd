package nuclear.utils.language;

import nuclear.control.config.ConfigManager;

public class Translated {
    private static String currentLanguage = "ENG"; // Default to ENG

    public static boolean isRussian() {
        return "RUS".equals(currentLanguage);
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(String language) {
        if (language != null && (language.equals("RUS") || language.equals("ENG") || language.equals("PL") || language.equals("UKR"))) {
            currentLanguage = language;
            ConfigManager.saveLanguageSetting(currentLanguage);
        }
    }

    // Backward compatibility
    public static void setRussian(boolean value) {
        setLanguage(value ? "RUS" : "ENG");
    }
}
