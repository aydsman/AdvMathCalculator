package backend.math.calculus.differential;

import backend.engine.MathOperation;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Simplifier;
import backend.math.functions.FunctionUtils;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the tangent line to {@code y = f(x)} at a given point.
 *
 * <p>Accepted input forms:
 * <ul>
 *   <li>{@code x^2 at x=2}</li>
 *   <li>{@code x^2 @ x=2}</li>
 *   <li>{@code f(x)=x^2, x=2}</li>
 *   <li>{@code y=x^2 at x=2}</li>
 * </ul>
 */
public class TangentLine implements MathOperation {

    private static final String DEFAULT_VAR = "x";
    private static final Pattern AT_POINT = Pattern.compile(
            "(?i)(?:\\s+at\\s+|\\s*@\\s*|\\s*,\\s*)" + DEFAULT_VAR + "\\s*=\\s*(.+)$");

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a function and point, e.g. x^2 at x=2");
    }

    public Result solveFromInput(String raw) {
        ParsedInput parsed = parseInput(raw);
        return tangentAt(parsed.expression(), parsed.variable(), parsed.xValue());
    }

    private static Result tangentAt(ASTNode expression, String variable, Num a) {
        List<Step> steps = new ArrayList<>();
        ASTNode f = Simplifier.simplify(expression);
        steps.add(new Step("Function", f));
        steps.add(new Step("Point of tangency: " + variable + " = " + a.toDisplay(), a));

        ASTNode faNode = Simplifier.simplify(FunctionUtils.substitute(f, variable, a));
        steps.add(new Step("Evaluate f(" + a.toDisplay() + ")", faNode));

        Num fa;
        try {
            ExpressionEvaluator.NumericEvaluation eval = ExpressionEvaluator.evaluate(faNode);
            if (!eval.hasExact()) {
                return Result.failure("f(" + a.toDisplay() + ") is not an exact value: "
                        + faNode.toDisplay());
            }
            fa = eval.exact();
        } catch (RuntimeException e) {
            return Result.failure("Could not evaluate f(" + a.toDisplay() + "): "
                    + (e.getMessage() != null ? e.getMessage() : "error"));
        }

        Result derivativeResult = Derivative.differentiate(f, variable);
        ASTNode fPrime = derivativeResult.getExact();
        if (fPrime == null) {
            return Result.failure(derivativeResult.getError() != null
                    ? derivativeResult.getError()
                    : "Could not differentiate the function.");
        }
        steps.add(new Step("Differentiate: f'(" + variable + ")", fPrime));

        ASTNode fpaNode = Simplifier.simplify(FunctionUtils.substitute(fPrime, variable, a));
        steps.add(new Step("Evaluate f'(" + a.toDisplay() + ")", fpaNode));

        Num fpa;
        try {
            ExpressionEvaluator.NumericEvaluation eval = ExpressionEvaluator.evaluate(fpaNode);
            if (!eval.hasExact()) {
                return Result.failure("f'(" + a.toDisplay() + ") is not an exact value: "
                        + fpaNode.toDisplay());
            }
            fpa = eval.exact();
        } catch (RuntimeException e) {
            return Result.failure("Could not evaluate f'(" + a.toDisplay() + "): "
                    + (e.getMessage() != null ? e.getMessage() : "error"));
        }

        // Point-slope: y - f(a) = f'(a)(x - a)
        ASTNode pointSlope = new Constant(
                "y - " + fa.toDisplay() + " = " + fpa.toDisplay()
                        + "*(" + variable + " - " + a.toDisplay() + ")");
        steps.add(new Step("Point-slope form", pointSlope));

        // Slope-intercept: y = f'(a)*x + (f(a) - f'(a)*a)
        ASTNode slopeTerm = Simplifier.simplify(new Mul(fpa, new Var(variable)));
        Num intercept;
        try {
            intercept = ExpressionEvaluator.evaluateNumeric(
                    Simplifier.simplify(new Sub(fa, new Mul(fpa, a))));
        } catch (RuntimeException e) {
            return Result.failure("Could not simplify the tangent-line intercept.");
        }

        ASTNode rhs = intercept.isZero()
                ? slopeTerm
                : Simplifier.simplify(new Add(slopeTerm, intercept));
        steps.add(new Step("Slope-intercept form: y = " + rhs.toDisplay(), rhs));

        ASTNode answer = new Constant("y = " + rhs.toDisplay());
        return Result.of(answer, null, steps);
    }

    static ParsedInput parseInput(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new Parser.ParseException("Enter a function and point, e.g. x^2 at x=2");
        }

        Matcher matcher = AT_POINT.matcher(text);
        if (!matcher.find()) {
            throw new Parser.ParseException(
                    "Enter a point of tangency, e.g. x^2 at x=2 or f(x)=x^2, x=2");
        }

        String xText = matcher.group(1).trim();
        String exprPart = text.substring(0, matcher.start()).trim();
        if (exprPart.isEmpty()) {
            throw new Parser.ParseException("Enter a function before the point, e.g. x^2 at x=2");
        }

        String lower = exprPart.toLowerCase(Locale.ROOT);
        if (lower.startsWith("y=")) {
            exprPart = exprPart.substring(2).trim();
        }

        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(exprPart, "f", DEFAULT_VAR);
        Num xValue = ExpressionEvaluator.parseNumeric(xText);
        return new ParsedInput(Simplifier.simplify(def.body()), def.variable(), xValue);
    }

    record ParsedInput(ASTNode expression, String variable, Num xValue) {}
}
