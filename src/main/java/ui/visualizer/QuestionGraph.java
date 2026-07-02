package ui.visualizer;

import backend.math.algebra.ExpressionEvaluator;
import backend.math.calculus.integral.RiemannSum;
import backend.math.functions.FunctionUtils;
import backend.models.Question;
import backend.math.algebra.Simplifier;
import backend.math.calculus.differential.Derivative;
import backend.models.Result;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Builds plotted curves and overlays for calculate-screen mini graphs. */
public final class QuestionGraph {

    private static final Set<String> SUPPORTED = Set.of("derivative", "riemannSum");

    private QuestionGraph() {}

    public static boolean supports(String operation) {
        return operation != null && SUPPORTED.contains(operation);
    }

    public static boolean supports(Question question) {
        return question != null
                && question.getResult() != null
                && question.getResult().isSuccess()
                && supports(question.getOperation());
    }

    public static List<PlottedFunction> plotsFor(Question question) {
        if (!supports(question)) {
            return List.of();
        }
        return switch (question.getOperation()) {
            case "derivative" -> derivativePlots(question);
            case "riemannSum" -> riemannPlots(question);
            default -> List.of();
        };
    }

    public static RiemannSumOverlay riemannOverlay(Question question, String aText, String bText, String nText) {
        if (question == null || !"riemannSum".equals(question.getOperation())) {
            return null;
        }
        if (aText == null || bText == null || nText == null
                || aText.isBlank() || bText.isBlank() || nText.isBlank()) {
            return null;
        }
        try {
            ASTNode function = RiemannSum.parseIntegrand(question.getInput());
            String variable = FunctionUtils.parseDefinition(question.getInput(), "f", "x").variable();
            Num a = ExpressionEvaluator.parseNumeric(aText);
            Num b = ExpressionEvaluator.parseNumeric(bText);
            int n = parseRectangleCount(nText);
            return new RiemannSumOverlay(function, variable, a.toDouble(), b.toDouble(), n);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<PlottedFunction> riemannPlots(Question question) {
        ASTNode function = RiemannSum.parseIntegrand(question.getInput());
        List<PlottedFunction> plots = new ArrayList<>(1);
        plots.add(new PlottedFunction(
                question.getInput(),
                function,
                "x",
                PlotColors.defaultForIndex(0)));
        return plots;
    }

    private static int parseRectangleCount(String text) {
        ASTNode parsed = Simplifier.simplify(backend.parser.Parser.parse(text.trim()));
        if (!(parsed instanceof Num num) || !num.isInteger() || num.numerator().intValueExact() < 1) {
            throw new IllegalArgumentException("n must be a positive integer");
        }
        return num.numerator().intValueExact();
    }

    private static List<PlottedFunction> derivativePlots(Question question) {
        Result result = question.getResult();
        ASTNode original = Derivative.parseInput(question.getInput());
        ASTNode derivative = Simplifier.simplify(result.getExact());

        List<PlottedFunction> plots = new ArrayList<>(2);
        plots.add(new PlottedFunction(
                stripDerivativeNotation(question.getInput()),
                original,
                "x",
                PlotColors.defaultForIndex(0)));
        plots.add(new PlottedFunction(
                derivative.toDisplay(),
                derivative,
                "x",
                PlotColors.defaultForIndex(1)));
        return plots;
    }

    private static String stripDerivativeNotation(String raw) {
        String text = raw.trim();
        String lower = text.toLowerCase();
        if (lower.startsWith("d/dx")) {
            text = text.substring(4).trim();
            if (text.startsWith("(") && text.endsWith(")")) {
                text = text.substring(1, text.length() - 1).trim();
            }
        }
        return text;
    }
}
