package ui.visualizer;

import javafx.scene.paint.Color;

/** Default curve colors for the free graph (Tokyo Night palette). */
public final class PlotColors {

    public static final Color[] PALETTE = {
            Color.web("#7aa2f7"),
            Color.web("#9ece6a"),
            Color.web("#f7768e"),
            Color.web("#e0af68"),
            Color.web("#bb9af7"),
            Color.web("#7dcfff"),
            Color.web("#ff9e64"),
            Color.web("#73daca"),
    };

    private PlotColors() {}

    public static Color defaultForIndex(int index) {
        return PALETTE[Math.floorMod(index, PALETTE.length)];
    }

    public static String toCss(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
