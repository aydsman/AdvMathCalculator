package ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import ui.visualizer.PlotColors;

import java.util.function.Consumer;

/** Small swatch that opens a palette of preset colors. */
public class ColorPickerButton extends Button {

    private Color color = PlotColors.PALETTE[0];
    private Consumer<Color> onColorChange;

    public ColorPickerButton() {
        getStyleClass().add("color-picker-button");
        setMinSize(26, 26);
        setMaxSize(26, 26);
        setFocusTraversable(false);
        applyColor(color);
        setOnAction(e -> showPalette());
    }

    public void setColor(Color color) {
        this.color = color;
        applyColor(color);
    }

    public Color getColor() {
        return color;
    }

    public void setOnColorChange(Consumer<Color> handler) {
        this.onColorChange = handler;
    }

    private void applyColor(Color c) {
        String hex = PlotColors.toCss(c);
        setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 6;");
    }

    private void showPalette() {
        ContextMenu menu = new ContextMenu();
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setPadding(new Insets(6));

        Color[] palette = PlotColors.PALETTE;
        for (int i = 0; i < palette.length; i++) {
            Color option = palette[i];
            Button swatch = new Button();
            swatch.getStyleClass().add("color-swatch");
            swatch.setMinSize(22, 22);
            swatch.setMaxSize(22, 22);
            String hex = PlotColors.toCss(option);
            swatch.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 4;");
            swatch.setOnAction(ev -> {
                setColor(option);
                if (onColorChange != null) {
                    onColorChange.accept(option);
                }
                menu.hide();
            });
            grid.add(swatch, i % 4, i / 4);
        }

        CustomMenuItem item = new CustomMenuItem(grid);
        item.setHideOnClick(true);
        menu.getItems().add(item);
        menu.show(this, Side.BOTTOM, 0, 4);
    }
}
