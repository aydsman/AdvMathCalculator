package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * Optional x-value input shown in Calculate mode for algebra expressions.
 */
public class EvaluateAtBar extends HBox {

    private final TextField xField = new TextField();
    private final Button evaluateButton = new Button("Evaluate");
    private final CheckBox decimalCheck = new CheckBox("Decimal");
    private Consumer<String> onEvaluate;
    private Runnable onDecimalToggle;

    public EvaluateAtBar() {
        getStyleClass().add("evaluate-bar");
        setAlignment(Pos.CENTER);
        setSpacing(8);

        Label label = new Label("Evaluate at x =");
        label.getStyleClass().add("evaluate-label");

        xField.setPromptText("e.g. 2, -1, 3/4");
        xField.getStyleClass().add("evaluate-field");
        xField.setPrefWidth(120);

        evaluateButton.getStyleClass().add("evaluate-button");
        evaluateButton.setFocusTraversable(false);
        evaluateButton.setOnAction(e -> fireEvaluate());
        xField.setOnAction(e -> fireEvaluate());

        decimalCheck.getStyleClass().add("evaluate-decimal-check");
        decimalCheck.setFocusTraversable(false);
        decimalCheck.setOnAction(e -> {
            if (onDecimalToggle != null) {
                onDecimalToggle.run();
            }
        });

        getChildren().addAll(label, xField, evaluateButton, decimalCheck);
        setVisible(false);
        setManaged(false);
    }

    public void setVisibleForQuestion(boolean visible) {
        setVisible(visible);
        setManaged(visible);
        if (!visible) {
            xField.clear();
            decimalCheck.setSelected(false);
        }
    }

    public void setOnEvaluate(Consumer<String> handler) {
        this.onEvaluate = handler;
    }

    public void setOnDecimalToggle(Runnable handler) {
        this.onDecimalToggle = handler;
    }

    public boolean isShowDecimal() {
        return decimalCheck.isSelected();
    }

    public void showError(String message) {
        xField.setPromptText(message);
    }

    public void clearError() {
        xField.setPromptText("e.g. 2, -1, 3/4");
    }

    private void fireEvaluate() {
        if (onEvaluate != null) {
            onEvaluate.accept(xField.getText());
        }
    }
}
