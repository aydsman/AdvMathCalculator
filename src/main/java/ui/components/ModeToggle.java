package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * Segmented Calculate / Visualize switch that drives what the {@code RightPanel} shows.
 * Exactly one mode is always selected.
 */
public class ModeToggle extends HBox {

    public enum Mode { CALCULATE, VISUALIZE }

    private final ToggleButton calculateButton = new ToggleButton("Calculate");
    private final ToggleButton visualizeButton = new ToggleButton("Visualize");
    private Consumer<Mode> onModeChange;

    public ModeToggle() {
        getStyleClass().add("mode-toggle");
        setAlignment(Pos.CENTER);

        ToggleGroup group = new ToggleGroup();
        calculateButton.setToggleGroup(group);
        visualizeButton.setToggleGroup(group);
        calculateButton.setSelected(true);
        calculateButton.getStyleClass().add("mode-button");
        visualizeButton.getStyleClass().add("mode-button");
        calculateButton.setFocusTraversable(false);
        visualizeButton.setFocusTraversable(false);

        getChildren().addAll(calculateButton, visualizeButton);

        group.selectedToggleProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                old.setSelected(true);
                return;
            }
            if (onModeChange != null) {
                onModeChange.accept(getMode());
            }
        });
    }

    public Mode getMode() {
        return calculateButton.isSelected() ? Mode.CALCULATE : Mode.VISUALIZE;
    }

    public void setOnModeChange(Consumer<Mode> handler) {
        this.onModeChange = handler;
    }

    public void setVisualizeVisible(boolean visible) {
        visualizeButton.setVisible(visible);
        visualizeButton.setManaged(visible);
        if (!visible && visualizeButton.isSelected()) {
            calculateButton.setSelected(true);
        }
    }
}
