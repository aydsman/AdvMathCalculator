package ui.visualizer;

import backend.math.algebra.ExpressionEvaluator;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared canvas drawing for free graph and calculate-screen mini graphs. */
final class GraphRenderer {

    static final Color BG = Color.web("#16161e");
    static final Color GRID = Color.web("#2a2f44");
    static final Color AXIS = Color.web("#565f89");
    static final Color AXIS_LABEL = Color.web("#a9b1d6");
    static final Color RIEMANN_FILL = Color.web("#7aa2f7", 0.28);
    static final Color RIEMANN_STROKE = Color.web("#7aa2f7", 0.75);

    private final Font labelFont = Font.font("Segoe UI", 11);
    private final Map<String, Double> labelWidthCache = new HashMap<>();

    void draw(GraphicsContext gc, GraphViewport viewport, double width, double height,
              List<PlottedFunction> functions, RiemannSumOverlay riemannOverlay) {
        gc.setFill(BG);
        gc.fillRect(0, 0, width, height);

        drawGrid(gc, viewport, width, height);
        drawAxes(gc, viewport, width, height);
        drawTickLabels(gc, viewport, width, height);

        if (riemannOverlay != null) {
            drawRiemannRectangles(gc, viewport, riemannOverlay);
        }

        for (PlottedFunction fn : functions) {
            drawCurve(gc, viewport, width, fn);
        }
    }

    private static void drawRiemannRectangles(GraphicsContext gc, GraphViewport viewport,
                                              RiemannSumOverlay overlay) {
        double delta = overlay.deltaX();
        for (int i = 0; i < overlay.n(); i++) {
            Double height = overlay.sampleHeight(i);
            if (height == null) {
                continue;
            }

            double x0 = overlay.leftSampleX(i);
            double x1 = x0 + delta;
            double sx0 = viewport.toScreenX(x0);
            double sx1 = viewport.toScreenX(x1);
            double syTop = viewport.toScreenY(height);
            double syBase = viewport.toScreenY(0);

            double left = Math.min(sx0, sx1);
            double width = Math.abs(sx1 - sx0);
            double top = Math.min(syTop, syBase);
            double rectHeight = Math.abs(syBase - syTop);
            if (width <= 0 || rectHeight <= 0) {
                continue;
            }

            gc.setFill(RIEMANN_FILL);
            gc.fillRect(left, top, width, rectHeight);
            gc.setStroke(RIEMANN_STROKE);
            gc.setLineWidth(1);
            gc.strokeRect(left + 0.5, top + 0.5, Math.max(0, width - 1), Math.max(0, rectHeight - 1));
        }
    }

    private void drawGrid(GraphicsContext gc, GraphViewport viewport, double w, double h) {
        gc.setStroke(GRID);
        gc.setLineWidth(1);

        boolean yAxisVisible = viewport.yAxisInView();
        boolean xAxisVisible = viewport.xAxisInView();

        for (int x = viewport.gridXStart(); x <= viewport.gridXEnd(); x++) {
            if (x == 0 && yAxisVisible) {
                continue;
            }
            double sx = snapLine(viewport.toScreenX(x));
            if (sx < -1 || sx > w + 1) {
                continue;
            }
            gc.strokeLine(sx, 0, sx, h);
        }

        for (int y = viewport.gridYStart(); y <= viewport.gridYEnd(); y++) {
            if (y == 0 && xAxisVisible) {
                continue;
            }
            double sy = snapLine(viewport.toScreenY(y));
            if (sy < -1 || sy > h + 1) {
                continue;
            }
            gc.strokeLine(0, sy, w, sy);
        }
    }

    private void drawAxes(GraphicsContext gc, GraphViewport viewport, double w, double h) {
        gc.setStroke(AXIS);
        gc.setLineWidth(2);

        if (viewport.xAxisInView()) {
            double y0 = snapLine(viewport.toScreenY(0));
            gc.strokeLine(0, y0, w, y0);
        } else {
            gc.strokeLine(0, snapLine(h - 0.5), w, snapLine(h - 0.5));
        }
        if (viewport.yAxisInView()) {
            double x0 = snapLine(viewport.toScreenX(0));
            gc.strokeLine(x0, 0, x0, h);
        } else {
            gc.strokeLine(snapLine(0.5), 0, snapLine(0.5), h);
        }
    }

    private void drawTickLabels(GraphicsContext gc, GraphViewport viewport, double w, double h) {
        gc.setFill(AXIS_LABEL);
        gc.setFont(labelFont);

        boolean xAxisInView = viewport.xAxisInView();
        boolean yAxisInView = viewport.yAxisInView();
        boolean originVisible = xAxisInView && yAxisInView;

        double xLabelY = xAxisInView ? viewport.toScreenY(0) + 14 : h - 8;
        double yLabelX = yAxisInView ? viewport.toScreenX(0) - 8 : 8;

        int xStep = viewport.labelStepX();
        for (int x = alignTickStart(viewport.gridXStart(), xStep); x <= viewport.gridXEnd(); x += xStep) {
            if (x == 0 && originVisible) {
                continue;
            }
            double sx = viewport.toScreenX(x);
            if (sx < 4 || sx > w - 4) {
                continue;
            }
            String label = GraphViewport.formatInteger(x);
            double textW = measureText(label);
            gc.fillText(label, sx - textW / 2, xLabelY);
        }

        int yStep = viewport.labelStepY();
        for (int y = alignTickStart(viewport.gridYStart(), yStep); y <= viewport.gridYEnd(); y += yStep) {
            if (y == 0 && originVisible) {
                continue;
            }
            double sy = viewport.toScreenY(y);
            if (sy < 10 || sy > h - 4) {
                continue;
            }
            String label = GraphViewport.formatInteger(y);
            double textW = measureText(label);
            gc.fillText(label, yLabelX - textW, sy + 4);
        }

        if (originVisible) {
            double ox = viewport.toScreenX(0);
            double oy = viewport.toScreenY(0);
            double zeroW = measureText("0");
            gc.fillText("0", ox - zeroW - 6, oy + 14);
        }
    }

    private static int alignTickStart(int gridStart, int step) {
        if (step <= 1) {
            return gridStart;
        }
        int rem = Math.floorMod(gridStart, step);
        return rem == 0 ? gridStart : gridStart + (step - rem);
    }

    private static double snapLine(double coordinate) {
        return Math.floor(coordinate) + 0.5;
    }

    private double measureText(String text) {
        return labelWidthCache.computeIfAbsent(text, key -> {
            Text measure = new Text(key);
            measure.setFont(labelFont);
            return measure.getLayoutBounds().getWidth();
        });
    }

    private static void drawCurve(GraphicsContext gc, GraphViewport viewport, double canvasWidth,
                                  PlottedFunction fn) {
        double xLeft = viewport.visibleMathXLeft();
        double xRight = viewport.visibleMathXRight();
        if (xRight <= xLeft) {
            return;
        }

        double xSpan = xRight - xLeft;
        int samples = (int) Math.ceil(Math.max(canvasWidth * 3, xSpan * 12));
        samples = Math.min(Math.max(samples, 400), 12_000);
        double step = xSpan / (samples - 1);

        gc.setStroke(fn.getColor());
        gc.setLineWidth(2.5);

        boolean drawing = false;
        double prevMathY = 0;

        for (int i = 0; i < samples; i++) {
            double mathX = xLeft + i * step;
            Double mathY = tryEval(mathX, fn);
            if (mathY == null) {
                if (drawing) {
                    gc.stroke();
                    drawing = false;
                }
                continue;
            }

            double screenX = viewport.toScreenX(mathX);
            double screenY = viewport.toScreenY(mathY);

            if (!drawing) {
                gc.beginPath();
                gc.moveTo(screenX, screenY);
                drawing = true;
            } else {
                boolean signFlip = Math.signum(mathY) != Math.signum(prevMathY);
                boolean largeOpposite = signFlip && Math.abs(mathY) > 10 && Math.abs(prevMathY) > 10;
                if (largeOpposite) {
                    gc.stroke();
                    gc.beginPath();
                    gc.moveTo(screenX, screenY);
                } else {
                    gc.lineTo(screenX, screenY);
                }
            }

            prevMathY = mathY;
        }

        if (drawing) {
            gc.stroke();
        }
    }

    private static Double tryEval(double x, PlottedFunction fn) {
        try {
            double y = ExpressionEvaluator.evaluateAtDouble(
                    fn.getExpression(), fn.getVariable(), x);
            if (!Double.isFinite(y)) {
                return null;
            }
            return y;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
