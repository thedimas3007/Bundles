package thedimas.bundle;

import arc.graphics.Color;

@SuppressWarnings("unused")
public enum Level {
    system(Color.gold, "\uE80F"),
    success(Color.valueOf("#38d667"), "\uE800"),
    info(Color.sky, "\uE837"),
    error(Color.valueOf("#e55454"), "\u26A0"), // client error (wrong input)
    fatal(Color.scarlet, "\uE810"); // server error

    final Color color;
    final String icon;

    Level(Color color, String icon) {
        this.color = color;
        this.icon = icon;
    }

    public String format(String string) {
        return String.format("[#%s]%s %s[]", color, icon, string);
    }
}
