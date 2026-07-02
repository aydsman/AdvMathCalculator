package backend.math.calculus.integral;

import backend.engine.MathOperation;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Factoring;
import backend.math.algebra.Simplifier;
import backend.math.functions.FunctionUtils;
import backend.models.DefiniteIntegral;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Num;
import backend.parser.Parser;

import java.util.List;
import java.util.Map;

/**
 * Riemann-sum setup and symbolic evaluation for polynomial integrands.
 */
public class RiemannSum implements MathOperation {

    private static final String DEFAULT_VAR = "x";

    @Override
    public Result solve(ASTNode input) {
        return Result.of(Simplifier.simplify(input), null, List.of());
    }

    /** Parses the integrand; bounds are supplied later from the calculate screen. */
    public Result solveFromInput(String raw) {
        ASTNode body = parseIntegrand(raw);
        return Result.of(body, null, List.of());
    }

    public Result solveWithBounds(ASTNode function, String variable, String aText, String bText, String nText) {
        function = Simplifier.simplify(function);
        Num a = ExpressionEvaluator.parseNumeric(aText);
        Num b = ExpressionEvaluator.parseNumeric(bText);
        int n = parseRectangleCount(nText);

        if (a.toDouble() == b.toDouble()) {
            throw new IllegalArgumentException("a and b must be different");
        }

        Map<Integer, Num> coeffs = Factoring.coefficients(function, variable);
        if (coeffs != null && !coeffs.isEmpty()) {
            return RiemannSumSolver.solvePolynomial(function, variable, a, b, n);
        }

        return numericFallback(function, variable, a, b, n);
    }

    private static Result numericFallback(ASTNode function, String variable, Num a, Num b, int n) {
        double sum = leftRiemannSum(function, variable, a.toDouble(), b.toDouble(), n);
        return Result.of(null, sum, List.of(
                IntegralNotation.definiteIntegralStep(a, b, function, variable),
                new Step("Numeric left Riemann sum (non-polynomial integrand)",
                        "n = " + n + " rectangles")));
    }

    public static ASTNode parseIntegrand(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new Parser.ParseException("Enter a function such as x^2 or sin(x)");
        }
        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(raw.trim(), "f", DEFAULT_VAR);
        return Simplifier.simplify(def.body());
    }

    static String formatIntegralNotation(Num a, Num b, ASTNode function, String variable) {
        DefiniteIntegral integral = new DefiniteIntegral(
                a.toDisplay(),
                b.toDisplay(),
                IntegralNotation.parenthesizeIfNeeded(function.toDisplay()),
                variable);
        return "\u222B_" + integral.lower() + "^" + integral.upper()
                + " " + integral.integrand() + " d" + integral.variable();
    }

    private static int parseRectangleCount(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter n (number of rectangles)");
        }
        ASTNode parsed = Simplifier.simplify(Parser.parse(text.trim()));
        if (!(parsed instanceof Num num) || !num.isInteger() || num.numerator().intValueExact() < 1) {
            throw new IllegalArgumentException("n must be a positive integer");
        }
        return num.numerator().intValueExact();
    }

    private static double leftRiemannSum(ASTNode function, String variable, double a, double b, int n) {
        double delta = (b - a) / n;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            double x = a + i * delta;
            sum += ExpressionEvaluator.evaluateAtDouble(function, variable, x);
        }
        return sum * delta;
    }
}
