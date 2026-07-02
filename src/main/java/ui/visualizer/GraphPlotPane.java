package ui.visualizer;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable interactive graph canvas (pan, zoom, grid, multi-curve).
 * Used by {@link FreeGraph} and the calculate-screen mini graph.
 */
public class GraphPlotPane extends StackPane {

    private static final double ZOOM_SCROLL = 1.12;
    private static final double ZOOM_KEY = 1.15;

    public enum ViewportMode {
        FULL, MINI
    }

    private final Canvas canvas = new Canvas();
    private final GraphViewport viewport = new GraphViewport();
    private final GraphRenderer renderer = new GraphRenderer();

    private final List<PlottedFunction> functions = new ArrayList<>();
    private RiemannSumOverlay riemannOverlay;

    private double dragStartX;
    private double dragStartY;
    private boolean dragging;
    private double lastDrawWidth;
    private double lastDrawHeight;
    private boolean dirty = true;

    public GraphPlotPane() {
        this(ViewportMode.FULL);
    }

    public GraphPlotPane(ViewportMode mode) {
        if (mode == ViewportMode.MINI) {
            viewport.useMiniDefaults();
        } else {
            viewport.useFullDefaults();
        }
        getStyleClass().add("graph-plot-host");
        getChildren().add(canvas);
        StackPane.setAlignment(canvas, Pos.CENTER);
        setCursor(Cursor.OPEN_HAND);
        setFocusTraversable(true);
        setMinSize(160, 120);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener((obs, old, width) -> scheduleRedraw());
        heightProperty().addListener((obs, old, height) -> scheduleRedraw());

        visibleProperty().addListener((obs, wasVisible, visible) -> {
            if (visible) {
                lastDrawWidth = 0;
                lastDrawHeight = 0;
                Platform.runLater(this::requestRedraw);
            }
        });

        setOnMouseEntered(e -> requestFocus());
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        addEventHandler(ScrollEvent.SCROLL, this::onScroll);
        addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    public void setFunctions(List<PlottedFunction> plots) {
        functions.clear();
        if (plots != null) {
            functions.addAll(plots);
        }
        dirty = true;
        requestRedraw();
    }

    public void setRiemannOverlay(RiemannSumOverlay overlay) {
        this.riemannOverlay = overlay;
        dirty = true;
        requestRedraw();
    }

    public void fitToRiemannOverlay(RiemannSumOverlay overlay) {
        if (overlay == null) {
            return;
        }
        double delta = overlay.deltaX();
        double ymin = Double.POSITIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < overlay.n(); i++) {
            Double y = overlay.sampleHeight(i);
            if (y == null) {
                continue;
            }
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }

        double midSamples = 8;
        for (int i = 0; i <= midSamples; i++) {
            double x = overlay.a() + (overlay.b() - overlay.a()) * i / midSamples;
            try {
                double y = backend.math.algebra.ExpressionEvaluator.evaluateAtDouble(
                        overlay.function(), overlay.variable(), x);
                if (Double.isFinite(y)) {
                    ymin = Math.min(ymin, y);
                    ymax = Math.max(ymax, y);
                }
            } catch (RuntimeException ignored) {
            }
        }

        if (!Double.isFinite(ymin) || !Double.isFinite(ymax)) {
            viewport.resetToDefault();
            return;
        }

        ymin = Math.min(ymin, 0);
        ymax = Math.max(ymax, 0);
        double xPad = Math.max(0.5, (overlay.b() - overlay.a()) * 0.08);
        double ySpan = Math.max(1, ymax - ymin);
        double yPad = Math.max(0.5, ySpan * 0.12);
        viewport.fitToRange(overlay.a() - xPad, overlay.b() + xPad, ymin - yPad, ymax + yPad);
        dirty = true;
    }

    public GraphViewport getViewport() {
        return viewport;
    }

    public List<PlottedFunction> getFunctions() {
        return List.copyOf(functions);
    }

    public void resetView() {
        viewport.resetToDefault();
        dirty = true;
        lastDrawWidth = 0;
        lastDrawHeight = 0;
        requestRedraw();
    }

    public void requestRedraw() {
        if (!isVisible()) {
            return;
        }
        Platform.runLater(this::redrawIfReady);
    }

    private void scheduleRedraw() {
        if (!isVisible()) {
            return;
        }
        redrawIfReady();
    }

    private void redrawIfReady() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 40 || h < 40) {
            return;
        }
        if (!dirty && Math.abs(w - lastDrawWidth) < 0.5 && Math.abs(h - lastDrawHeight) < 0.5) {
            return;
        }
        lastDrawWidth = w;
        lastDrawHeight = h;
        dirty = false;
        redraw();
    }

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        viewport.updateLayout(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        renderer.draw(gc, viewport, w, h, functions, riemannOverlay);
    }

    private void onMousePressed(MouseEvent event) {
        requestFocus();
        if (event.isPrimaryButtonDown()) {
            dragging = true;
            dragStartX = event.getX();
            dragStartY = event.getY();
            setCursor(Cursor.CLOSED_HAND);
            event.consume();
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (!dragging) {
            return;
        }
        double dx = event.getX() - dragStartX;
        double dy = event.getY() - dragStartY;
        dragStartX = event.getX();
        dragStartY = event.getY();
        viewport.panPixels(dx, dy);
        dirty = true;
        redraw();
        event.consume();
    }

    private void onMouseReleased(MouseEvent event) {
        dragging = false;
        setCursor(Cursor.OPEN_HAND);
    }

    private void onScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? ZOOM_SCROLL : 1 / ZOOM_SCROLL;
        viewport.zoomAbout(event.getX(), event.getY(), factor);
        dirty = true;
        redraw();
        event.consume();
    }

    private void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.R) {
            viewport.resetToDefault();
            dirty = true;
            redraw();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.UP) {
            viewport.zoomAboutCenter(ZOOM_KEY);
            dirty = true;
            redraw();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            viewport.zoomAboutCenter(1 / ZOOM_KEY);
            dirty = true;
            redraw();
        }
    }
}
