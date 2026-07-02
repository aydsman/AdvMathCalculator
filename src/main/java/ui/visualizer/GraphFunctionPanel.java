package ui.visualizer;

import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Simplifier;
import backend.math.functions.FunctionUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import ui.components.AddBubbleButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Left sidebar for the free graph: scrollable function bubbles and an input bar
 * (mirrors the main calculator question feed).
 */
public class GraphFunctionPanel extends BorderPane {

    private final VBox feed = new VBox();
    private final ScrollPane scroll = new ScrollPane();
    private final TextField inputField = new TextField();
    private final AddBubbleButton addButton = new AddBubbleButton();
    private final Label errorLabel = new Label();
    private final List<PlottedFunction> functions = new ArrayList<>();
    private final List<FunctionBubble> bubbles = new ArrayList<>();

    private Consumer<List<PlottedFunction>> onFunctionsChanged;
    private FunctionBubble selectedBubble;

    public GraphFunctionPanel() {
        getStyleClass().add("graph-function-panel");

        Label title = new Label("Functions");
        title.getStyleClass().add("panel-title");
        setTop(title);

        feed.getStyleClass().add("feed");
        scroll.setContent(feed);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("feed-scroll");
        setCenter(scroll);

        inputField.setPromptText("e.g. x^2, sin(x), y = 2*x + 1");
        inputField.getStyleClass().add("input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        errorLabel.getStyleClass().add("graph-function-error");
        errorLabel.setWrapText(true);
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        HBox inputBar = new HBox(8, inputField, addButton);
        inputBar.getStyleClass().add("input-bar");
        inputBar.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(6, inputBar, errorLabel);
        setBottom(bottom);

        addButton.setOnAction(e -> addFromInput());
        inputField.setOnAction(e -> addFromInput());
    }

    public List<PlottedFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public void setOnFunctionsChanged(Consumer<List<PlottedFunction>> handler) {
        this.onFunctionsChanged = handler;
    }

    public void clearAll() {
        functions.clear();
        bubbles.clear();
        feed.getChildren().clear();
        inputField.clear();
        clearError();
        notifyChanged();
    }

    private void addFromInput() {
        String text = inputField.getText() == null ? "" : inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            PlottedFunction plotted = parseFunction(text, functions.size());
            functions.add(plotted);

            FunctionBubble bubble = new FunctionBubble(plotted);
            bubble.setOnSelect(() -> select(bubble));
            bubble.setOnChange(this::notifyChanged);
            bubbles.add(bubble);
            feed.getChildren().add(bubble);
            select(bubble);

            inputField.clear();
            clearError();
            scroll.layout();
            scroll.setVvalue(1.0);
            notifyChanged();
        } catch (RuntimeException e) {
            showError(e.getMessage() != null ? e.getMessage() : "Could not plot expression");
        }
    }

    private static PlottedFunction parseFunction(String raw, int index) {
        String text = raw.trim();
        if (text.regionMatches(true, 0, "y=", 0, 2)) {
            text = text.substring(2).trim();
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter a function to plot");
        }
        FunctionUtils.FunctionDef def = ExpressionEvaluator.expressionFromInput(text);
        return new PlottedFunction(
                raw.trim(),
                Simplifier.simplify(def.body()),
                def.variable(),
                PlotColors.defaultForIndex(index));
    }

    private void select(FunctionBubble bubble) {
        if (selectedBubble != null) {
            selectedBubble.setSelected(false);
        }
        selectedBubble = bubble;
        bubble.setSelected(true);
    }

    private void notifyChanged() {
        if (onFunctionsChanged != null) {
            onFunctionsChanged.accept(functions);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }
}
