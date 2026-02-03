package nuclear.utils.font;

import lombok.SneakyThrows;
import nuclear.utils.font.common.Lang;
import nuclear.utils.font.styled.StyledFont;

public class Fonts {
    public static final String FONT_DIR = "/assets/minecraft/nuclear/font/";

    public static volatile StyledFont[] gilroyBold = new StyledFont[25];
    public static volatile StyledFont[] msMedium = new StyledFont[25];
    public static volatile StyledFont[] msLight = new StyledFont[25];
    public static volatile StyledFont[] msRegular = new StyledFont[25];
    public static volatile StyledFont[] msSemiBold = new StyledFont[25];

    public static volatile StyledFont[] icon = new StyledFont[101];

    public static volatile StyledFont[] icon2 = new StyledFont[25];
    public static volatile StyledFont[] newcode = new StyledFont[25];
    public static volatile StyledFont[] blod = new StyledFont[25];

    @SneakyThrows
    public static void init() {
        for (int i = 8; i < 24; i++) {
            blod[i] = new StyledFont("nunito-bold.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            newcode[i] = new StyledFont("newcode.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            icon2[i] = new StyledFont("icon.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            gilroyBold[i] = new StyledFont("gilroy-bold.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            msLight[i] = new StyledFont("Montserrat-Light.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            msMedium[i] = new StyledFont("Montserrat-Medium.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            msRegular[i] = new StyledFont("Montserrat-Regular.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
            msSemiBold[i] = new StyledFont("Montserrat-SemiBold.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
        }

        for (int i = 8; i < 101; i++) {
            icon[i] = new StyledFont("icomoon.ttf", i, 0.0f, -0.25f, 0.0f, true, Lang.ENG_RU);
        }
    }
}