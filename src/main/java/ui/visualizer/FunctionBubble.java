package ui.visualizer;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.components.ColorPickerButton;

/** One function entry in the free-graph sidebar. */
public class FunctionBubble extends HBox {

    private final PlottedFunction function;
    private Runnable onSelect;
    private Runnable onChange;

    public FunctionBubble(PlottedFunction function) {
        this.function = function;
        getStyleClass().add("function-bubble");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);

        Label expression = new Label(function.getInput());
        expression.getStyleClass().add("function-bubble-input");
        expression.setWrapText(true);
        HBox.setHgrow(expression, Priority.ALWAYS);

        ColorPickerButton colorButton = new ColorPickerButton();
        colorButton.setColor(function.getColor());
        colorButton.setOnColorChange(c -> {
            function.setColor(c);
            updateAccent();
            if (onChange != null) {
                onChange.run();
            }
        });

        getChildren().addAll(expression, colorButton);
        updateAccent();

        setOnMouseClicked(e -> {
            if (onSelect != null) {
                onSelect.run();
            }
        });
    }

    public PlottedFunction getFunction() {
        return function;
    }

    public void setOnSelect(Runnable handler) {
        this.onSelect = handler;
    }

    public void setOnChange(Runnable handler) {
        this.onChange = handler;
    }

    public void setSelected(boolean selected) {
        getStyleClass().remove("function-bubble-selected");
        if (selected) {
            getStyleClass().add("function-bubble-selected");
        }
    }

    private void updateAccent() {
        String hex = PlotColors.toCss(function.getColor());
        setStyle("-fx-border-color: " + hex + ";");
    }
}
