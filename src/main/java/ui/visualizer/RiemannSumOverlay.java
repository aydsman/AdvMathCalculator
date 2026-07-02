package ui.visualizer;

import backend.math.algebra.ExpressionEvaluator;
import backend.parser.ASTNode;

/** Left Riemann-sum rectangles drawn under a curve on the graph. */
public record RiemannSumOverlay(ASTNode function, String variable, double a, double b, int n) {

    public double deltaX() {
        return (b - a) / n;
    }

    public double leftSampleX(int index) {
        return a + index * deltaX();
    }

    public Double sampleHeight(int index) {
        try {
            double y = ExpressionEvaluator.evaluateAtDouble(function, variable, leftSampleX(index));
            return Double.isFinite(y) ? y : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
