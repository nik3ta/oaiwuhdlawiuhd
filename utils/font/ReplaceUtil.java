package nuclear.utils.font;

import java.util.HashMap;
import java.util.Map;

public class ReplaceUtil {
    private static final Map<Character, String> REPLACEMENT_MAP = new HashMap<>();

    static {
        REPLACEMENT_MAP.put('⚡', "");
        REPLACEMENT_MAP.put('ꔀ', "PLAYER");
        REPLACEMENT_MAP.put('ꔄ', "HERO");
        REPLACEMENT_MAP.put('ꔈ', "TITAN");
        REPLACEMENT_MAP.put('ꔒ', "AVENGER");
        REPLACEMENT_MAP.put('ꔖ', "OVERLORD");
        REPLACEMENT_MAP.put('ꔠ', "MAGISTER");
        REPLACEMENT_MAP.put('ꔤ', "IMPERATOR");
        REPLACEMENT_MAP.put('ꔨ', "DRAGON");
        REPLACEMENT_MAP.put('ꔲ', "BULL");
        REPLACEMENT_MAP.put('ꔶ', "TIGER");
        REPLACEMENT_MAP.put('ꕄ', "DRACULA");
        REPLACEMENT_MAP.put('ꕖ', "BUNNY");
        REPLACEMENT_MAP.put('ꕈ', "COBRA");
        REPLACEMENT_MAP.put('ꕀ', "HYDRA");
        REPLACEMENT_MAP.put('ꕒ', "RABBIT");
        REPLACEMENT_MAP.put('ꕁ', "GOD");
        REPLACEMENT_MAP.put('ꔂ', "GOD");
        REPLACEMENT_MAP.put('ꔸ', "GOD");
        REPLACEMENT_MAP.put('ꕠ', "D.HELPER");
        REPLACEMENT_MAP.put('ꔉ', "HELPER");
        REPLACEMENT_MAP.put('ꔓ', "ML.MODER");
        REPLACEMENT_MAP.put('ꔗ', "MODER");
        REPLACEMENT_MAP.put('ꔡ', "MODER+");
        REPLACEMENT_MAP.put('ꔥ', "ST.MODER");
        REPLACEMENT_MAP.put('ꔩ', "GL.MODER");
        REPLACEMENT_MAP.put('ꔳ', "ML.ADMIN");
        REPLACEMENT_MAP.put('ꔷ', "ADMIN");
        REPLACEMENT_MAP.put('ꔁ', "MEDIA");
        REPLACEMENT_MAP.put('ꕗ', "SPONSOR");
        REPLACEMENT_MAP.put('ꕅ', "VAMPIRE");
        REPLACEMENT_MAP.put('ꔆ', "VAMPIRE");
        REPLACEMENT_MAP.put('ꕉ', "PEGAS");
        REPLACEMENT_MAP.put('ꕓ', "GHOST");
        REPLACEMENT_MAP.put('ꔢ', "D.ST.MODER");
        REPLACEMENT_MAP.put('ꔅ', "YT");
        REPLACEMENT_MAP.put('ᴀ', "A");
        REPLACEMENT_MAP.put('ʙ', "B");
        REPLACEMENT_MAP.put('ᴄ', "C");
        REPLACEMENT_MAP.put('ᴅ', "D");
        REPLACEMENT_MAP.put('ᴇ', "E");
        REPLACEMENT_MAP.put('ꜰ', "F");
        REPLACEMENT_MAP.put('ɢ', "G");
        REPLACEMENT_MAP.put('ʜ', "H");
        REPLACEMENT_MAP.put('ɪ', "I");
        REPLACEMENT_MAP.put('ᴊ', "J");
        REPLACEMENT_MAP.put('ᴋ', "K");
        REPLACEMENT_MAP.put('ʟ', "L");
        REPLACEMENT_MAP.put('ᴍ', "M");
        REPLACEMENT_MAP.put('ɴ', "N");
        REPLACEMENT_MAP.put('ᴏ', "O");
        REPLACEMENT_MAP.put('ᴘ', "P");
        REPLACEMENT_MAP.put('ǫ', "Q");
        REPLACEMENT_MAP.put('ʀ', "R");
        REPLACEMENT_MAP.put('ᴛ', "T");
        REPLACEMENT_MAP.put('ᴜ', "U");
        REPLACEMENT_MAP.put('ꜱ', "S");
        REPLACEMENT_MAP.put('ᴠ', "V");
        REPLACEMENT_MAP.put('ᴡ', "W");
        REPLACEMENT_MAP.put('ᵡ', "X");
        REPLACEMENT_MAP.put('ʏ', "Y");
        REPLACEMENT_MAP.put('ᴢ', "Z");
        REPLACEMENT_MAP.put('●', "-");
        REPLACEMENT_MAP.put('•', "-");
    }

    public static String replaceCustomFonts(String text) {
        if (text == null) return null;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = REPLACEMENT_MAP.get(c);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
