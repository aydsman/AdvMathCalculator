package ui.visualizer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Full-window graphing workspace with a function sidebar and multi-curve plotting.
 */
public class FreeGraph extends BorderPane {

    private final GraphPlotPane plotPane = new GraphPlotPane();
    private final GraphFunctionPanel functionPanel = new GraphFunctionPanel();

    private Runnable onBack;

    public FreeGraph() {
        getStyleClass().add("free-graph-screen");
        setFocusTraversable(true);

        Button backButton = new Button("\u2190 Calculator");
        backButton.getStyleClass().add("free-graph-back");
        backButton.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        Label title = new Label("Free Graph");
        title.getStyleClass().add("free-graph-title");

        HBox toolbar = new HBox(12, backButton, title);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("free-graph-toolbar");
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        setTop(toolbar);

        SplitPane split = new SplitPane(functionPanel, plotPane);
        split.setDividerPositions(0.32);
        split.getStyleClass().add("free-graph-split");
        setCenter(split);

        visibleProperty().addListener((obs, wasVisible, visible) -> {
            if (visible) {
                Platform.runLater(() -> {
                    plotPane.requestFocus();
                    plotPane.requestRedraw();
                });
            }
        });

        functionPanel.setOnFunctionsChanged(functions -> plotPane.setFunctions(functions));
    }

    public void refreshAfterShow() {
        Platform.runLater(() -> {
            plotPane.requestFocus();
            plotPane.requestRedraw();
        });
    }

    public void setOnBack(Runnable handler) {
        this.onBack = handler;
    }

    public void reset() {
        functionPanel.clearAll();
        plotPane.resetView();
        refreshAfterShow();
    }
}
