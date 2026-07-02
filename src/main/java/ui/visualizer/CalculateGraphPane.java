package ui.visualizer;

import backend.models.Question;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Graph panel for the calculate screen. Compact mode: legend + square plot beside steps.
 * Full mode: plot only, filling the visualize view.
 */
public class CalculateGraphPane extends VBox {

    private final GraphPlotPane plotPane = new GraphPlotPane(GraphPlotPane.ViewportMode.MINI);
    private final HBox legend = new HBox(14);
    private final StackPane squareHost = new StackPane(plotPane);

    private boolean compact = true;

    public CalculateGraphPane() {
        getStyleClass().add("calculate-graph-pane");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);
        setFillWidth(true);
        legend.getStyleClass().add("calculate-graph-legend");
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(0, 8, 0, 8));
        legend.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(plotPane, Pos.TOP_CENTER);
        squareHost.getStyleClass().add("calculate-graph-square");
        squareHost.getStyleClass().add("calculate-graph-mini-frame");
        squareHost.setAlignment(Pos.TOP_CENTER);
        plotPane.getStyleClass().add("calculate-graph-mini-plot");

        getChildren().addAll(legend, squareHost);

        widthProperty().addListener((obs, old, width) -> updateSquareSize());
        layoutBoundsProperty().addListener((obs, old, bounds) -> updateSquareSize());
    }

    public GraphPlotPane getPlotPane() {
        return plotPane;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
        legend.setVisible(compact);
        legend.setManaged(compact);
        if (!compact) {
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            getStyleClass().add("calculate-graph-full");
            squareHost.getStyleClass().remove("calculate-graph-mini-frame");
            plotPane.getStyleClass().remove("calculate-graph-mini-plot");
            squareHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            squareHost.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            squareHost.setMinSize(0, 0);
            plotPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            plotPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            plotPane.setMinSize(160, 120);
            VBox.setVgrow(squareHost, Priority.ALWAYS);
        } else {
            setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            getStyleClass().remove("calculate-graph-full");
            if (!squareHost.getStyleClass().contains("calculate-graph-mini-frame")) {
                squareHost.getStyleClass().add("calculate-graph-mini-frame");
            }
            if (!plotPane.getStyleClass().contains("calculate-graph-mini-plot")) {
                plotPane.getStyleClass().add("calculate-graph-mini-plot");
            }
            VBox.setVgrow(squareHost, Priority.NEVER);
            updateSquareSize();
        }
        plotPane.requestRedraw();
    }

    public void update(Question question) {
        update(question, null, null, null);
    }

    public void update(Question question, String riemannA, String riemannB, String riemannN) {
        if (!QuestionGraph.supports(question)) {
            plotPane.setFunctions(java.util.List.of());
            plotPane.setRiemannOverlay(null);
            legend.getChildren().clear();
            return;
        }

        var plots = QuestionGraph.plotsFor(question);
        plotPane.setFunctions(plots);

        RiemannSumOverlay overlay = QuestionGraph.riemannOverlay(question, riemannA, riemannB, riemannN);
        plotPane.setRiemannOverlay(overlay);
        if (overlay != null) {
            plotPane.fitToRiemannOverlay(overlay);
        } else {
            plotPane.resetView();
        }

        rebuildLegend(question, plots, overlay != null);
        plotPane.requestRedraw();
    }

    private void rebuildLegend(Question question, java.util.List<PlottedFunction> plots, boolean showRectangles) {
        legend.getChildren().clear();
        if (plots.isEmpty()) {
            return;
        }

        if ("derivative".equals(question.getOperation()) && plots.size() >= 2) {
            legend.getChildren().add(legendItem("f(x)", plots.get(0).getColor()));
            legend.getChildren().add(legendItem("f\u2032(x)", plots.get(1).getColor()));
            return;
        }

        if ("riemannSum".equals(question.getOperation())) {
            legend.getChildren().add(legendItem("f(x)", plots.get(0).getColor()));
            if (showRectangles) {
                legend.getChildren().add(legendItem("Rectangles", Color.web("#7aa2f7", 0.55)));
            }
            return;
        }

        for (PlottedFunction fn : plots) {
            legend.getChildren().add(legendItem(fn.getInput(), fn.getColor()));
        }
    }

    private static HBox legendItem(String text, Color color) {
        Rectangle swatch = new Rectangle(12, 12);
        swatch.setFill(color);
        swatch.setArcWidth(3);
        swatch.setArcHeight(3);

        Label label = new Label(text);
        label.getStyleClass().add("calculate-graph-legend-label");

        HBox row = new HBox(6, swatch, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void updateSquareSize() {
        if (!compact) {
            return;
        }
        double size = getWidth();
        if (size <= 0 || !Double.isFinite(size)) {
            return;
        }
        plotPane.setMaxSize(size, size);
        plotPane.setPrefSize(size, size);
        plotPane.setMinSize(size, size);
        squareHost.setMaxSize(size, size);
        squareHost.setPrefSize(size, size);
        squareHost.setMinSize(size, size);
    }

    public void resetView() {
        plotPane.resetView();
    }
}
