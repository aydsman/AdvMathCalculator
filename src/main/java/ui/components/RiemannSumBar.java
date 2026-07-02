package ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/** Bounds and rectangle count for Riemann sum questions on the calculate screen. */
public class RiemannSumBar extends HBox {

    private final TextField aField = new TextField();
    private final TextField bField = new TextField();
    private final TextField nField = new TextField();
    private final Button computeButton = new Button("Compute");
    private Consumer<RiemannSumParams> onCompute;

    public RiemannSumBar() {
        getStyleClass().add("riemann-bar");
        setAlignment(Pos.CENTER);
        setSpacing(8);

        aField.setPromptText("a");
        bField.setPromptText("b");
        nField.setPromptText("n");
        aField.getStyleClass().add("riemann-field");
        bField.getStyleClass().add("riemann-field");
        nField.getStyleClass().add("riemann-field");
        aField.setPrefWidth(72);
        bField.setPrefWidth(72);
        nField.setPrefWidth(56);

        Label aLabel = label("a =");
        Label bLabel = label("b =");
        Label nLabel = label("n =");

        computeButton.getStyleClass().add("riemann-button");
        computeButton.setFocusTraversable(false);
        computeButton.setOnAction(e -> fireCompute());
        aField.setOnAction(e -> fireCompute());
        bField.setOnAction(e -> fireCompute());
        nField.setOnAction(e -> fireCompute());

        getChildren().addAll(aLabel, aField, bLabel, bField, nLabel, nField, computeButton);
        setVisible(false);
        setManaged(false);
    }

    public void setVisibleForQuestion(boolean visible) {
        setVisible(visible);
        setManaged(visible);
        if (!visible) {
            clearFields();
        }
    }

    public void setOnCompute(Consumer<RiemannSumParams> handler) {
        this.onCompute = handler;
    }

    public void showError(String message) {
        aField.setPromptText(message);
    }

    public void clearError() {
        aField.setPromptText("a");
    }

    public void clearFields() {
        aField.clear();
        bField.clear();
        nField.clear();
        clearError();
    }

    public RiemannSumParams params() {
        return new RiemannSumParams(aField.getText(), bField.getText(), nField.getText());
    }

    private void fireCompute() {
        if (onCompute != null) {
            onCompute.accept(params());
        }
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("riemann-label");
        return label;
    }

    public record RiemannSumParams(String a, String b, String n) {}
}
