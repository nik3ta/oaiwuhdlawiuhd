package nuclear.module;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TypeList {
    Combat("d"),
    Movement("y"),
    Render("w"),
    Player("U"),
    Other("Q");

    public final String icon;
}
