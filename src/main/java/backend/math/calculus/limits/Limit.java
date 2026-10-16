package backend.math.calculus.limits;

import backend.engine.MathOperation;
import backend.math.algebra.ComplexFractions;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Factoring;
import backend.math.algebra.RationalExpression;
import backend.math.algebra.Rationalizer;
import backend.math.algebra.Simplifier;
import backend.math.calculus.differential.Derivative;
import backend.math.functions.FunctionUtils;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Symbolic limits: direct substitution, complex fractions, conjugate rationalization,
 * polynomial factoring for {@code 0/0}, one-sided limits, limits at infinity,
 * absolute values, and L'Hôpital's rule.
 */
public class Limit implements MathOperation {

    private static final Num ONE = Num.of(1);
    private static final Num ZERO = Num.of(0);
    private static final Constant POS_INF = new Constant("\u221e");
    private static final Constant NEG_INF = new Constant("-\u221e");
    private static final int MAX_DEPTH = 8;
    private static final double SAMPLE_EPS = 1e-4;
    private static final double LARGE = 1e6;

    public enum Side {
        BOTH, LEFT, RIGHT
    }

    public sealed interface Approach permits Approach.Finite, Approach.Infinity {
        String toDisplay();

        record Finite(Num value, Side side) implements Approach {
            @Override
            public String toDisplay() {
                String base = value.toDisplay();
                return switch (side) {
                    case BOTH -> base;
                    case LEFT -> base + "\u207b";
                    case RIGHT -> base + "\u207a";
                };
            }
        }

        record Infinity(boolean positive) implements Approach {
            @Override
            public String toDisplay() {
                return positive ? "\u221e" : "-\u221e";
            }
        }
    }

    public record LimitQuery(String variable, Approach approach, ASTNode expression) {}

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a limit such as lim x->2 (x^2-4)/(x-2)");
    }

    public Result solveFromInput(String raw) {
        LimitQuery query = parseInput(raw);
        return evaluateLimit(query, 0, new ArrayList<>());
    }

    private static Result evaluateLimit(LimitQuery query, int depth, List<Step> priorSteps) {
        if (depth > MAX_DEPTH) {
            return failWith(priorSteps, "Could not resolve this limit after repeated attempts.");
        }

        String variable = query.variable();
        Approach approach = query.approach();
        ASTNode expression = Simplifier.simplify(query.expression());

        List<Step> steps = new ArrayList<>(priorSteps);
        if (depth == 0) {
            steps.add(new Step(
                    "Find lim " + variable + " \u2192 " + approach.toDisplay(),
                    expression));
        }

        AbsRewrite absRewrite = rewriteAbs(expression, variable, approach);
        if (absRewrite.changed()) {
            expression = absRewrite.expression();
            steps.add(new Step("Rewrite absolute value near the approach", expression));
        }

        if (approach instanceof Approach.Infinity inf) {
            return evaluateAtInfinity(variable, inf, expression, steps, depth);
        }

        Approach.Finite finite = (Approach.Finite) approach;

        if (finite.side() == Side.BOTH && containsAbs(expression)) {
            Result twoSided = resolveTwoSidedWithAbs(variable, finite, expression, steps, depth);
            if (twoSided != null) {
                return twoSided;
            }
        }

        return evaluateAtFinite(variable, finite, expression, steps, depth);
    }

    private static Result evaluateAtFinite(String variable, Approach.Finite approach,
                                           ASTNode expression, List<Step> steps, int depth) {
        ASTNode current = expression;

        Result direct = tryDirectSubstitution(current, variable, approach, steps);
        if (direct != null) {
            return direct;
        }

        for (int pass = 0; pass < 4; pass++) {
            boolean changed = false;

            ASTNode combined = ComplexFractions.simplify(current);
            if (!sameExpression(combined, current)) {
                current = cancelQuotientFactors(Simplifier.simplify(combined));
                steps.add(new Step("Combine into a single fraction", current));
                changed = true;
            } else {
                Optional<ASTNode> rationalized = Rationalizer.rationalize(current);
                if (rationalized.isPresent() && !sameExpression(rationalized.get(), current)) {
                    current = cancelQuotientFactors(Simplifier.simplify(rationalized.get()));
                    steps.add(new Step("Multiply by the conjugate", current));
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }

            direct = tryDirectSubstitution(current, variable, approach, steps);
            if (direct != null) {
                return direct;
            }

            Result factored = tryPolynomialFactor(current, variable, approach, steps);
            if (factored != null) {
                return factored;
            }
        }

        Result factored = tryPolynomialFactor(current, variable, approach, steps);
        if (factored != null) {
            return factored;
        }

        if (isIndeterminateForm(current, variable, approach)) {
            Result lhopital = tryLhopital(current, variable, approach, steps, depth);
            if (lhopital != null) {
                return lhopital;
            }
            return failWith(steps, indeterminateMessage(current, variable, approach));
        }

        if (isNonzeroOverZero(current, variable, approach.value())) {
            return divergeFromSide(current, variable, approach, steps);
        }

        return failWith(steps, "Could not evaluate this limit with the available methods.");
    }

    private static Result evaluateAtInfinity(String variable, Approach.Infinity approach,
                                             ASTNode expression, List<Step> steps, int depth) {
        ASTNode current = Simplifier.simplify(expression);

        Optional<ASTNode> rationalized = Rationalizer.rationalize(current);
        if (rationalized.isPresent() && !sameExpression(rationalized.get(), current)) {
            current = cancelQuotientFactors(Simplifier.simplify(rationalized.get()));
            steps.add(new Step("Multiply by the conjugate", current));
        }

        Result rational = tryRationalAtInfinity(current, variable, approach, steps);
        if (rational != null) {
            return rational;
        }

        // x = 1/t, then lim t -> 0+/-
        Num zero = ZERO;
        Side side = approach.positive() ? Side.RIGHT : Side.LEFT;
        ASTNode substituted = FunctionUtils.substitute(current, variable, new Div(ONE, new Var(variable)));
        substituted = Simplifier.simplify(substituted);
        steps.add(new Step(
                "Substitute " + variable + " = 1/t with t \u2192 0"
                        + (approach.positive() ? "\u207a" : "\u207b"),
                substituted));

        Approach.Finite tApproach = new Approach.Finite(zero, side);
        // Keep using the same variable name as t for the nested limit.
        return evaluateAtFinite(variable, tApproach, substituted, steps, depth + 1);
    }

    private static Result tryRationalAtInfinity(ASTNode expression, String variable,
                                                Approach.Infinity approach, List<Step> steps) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        Map<Integer, Num> numCoeffs = Factoring.coefficients(parts.numerator(), variable);
        Map<Integer, Num> denCoeffs = Factoring.coefficients(parts.denominator(), variable);
        if (numCoeffs == null || denCoeffs == null || denCoeffs.isEmpty()) {
            return null;
        }

        int degNum = degreeOf(numCoeffs);
        int degDen = degreeOf(denCoeffs);
        Num leadNum = numCoeffs.getOrDefault(degNum, ZERO);
        Num leadDen = denCoeffs.getOrDefault(degDen, ZERO);
        if (leadDen.isZero()) {
            return null;
        }

        steps.add(new Step("Compare degrees at infinity: deg(num) = " + degNum
                + ", deg(den) = " + degDen));

        if (degNum < degDen) {
            steps.add(new Step("Degree of numerator is smaller \u2014 limit is 0", ZERO));
            return Result.of(ZERO, null, steps);
        }
        if (degNum == degDen) {
            ASTNode ratio = Simplifier.simplify(new Div(leadNum, leadDen));
            steps.add(new Step("Leading-coefficient ratio", ratio));
            return finishNumeric(ratio, steps);
        }

        // degNum > degDen → ±∞ depending on leading signs and side
        boolean positiveLead = leadNum.numerator().signum() * leadDen.numerator().signum() > 0;
        int sign = positiveLead ? 1 : -1;
        if (!approach.positive() && ((degNum - degDen) % 2 != 0)) {
            sign = -sign;
        }
        Constant inf = sign > 0 ? POS_INF : NEG_INF;
        steps.add(new Step("Degree of numerator is larger \u2014 limit is " + inf.toDisplay(), inf));
        return Result.of(inf, null, steps);
    }

    private static int degreeOf(Map<Integer, Num> coeffs) {
        int deg = Integer.MIN_VALUE;
        for (Map.Entry<Integer, Num> e : coeffs.entrySet()) {
            if (!e.getValue().isZero()) {
                deg = Math.max(deg, e.getKey());
            }
        }
        return deg == Integer.MIN_VALUE ? 0 : deg;
    }

    private static Result tryLhopital(ASTNode expression, String variable, Approach approach,
                                      List<Step> steps, int depth) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        if (parts.denominator().equals(ONE)) {
            return null;
        }

        String formLabel = isZeroOverZero(parts.numerator(), parts.denominator(), variable, approach)
                ? "0/0"
                : "\u221e/\u221e";
        steps.add(new Step("Indeterminate form " + formLabel + " \u2014 apply L'H\u00f4pital's rule"));

        ASTNode numPrime;
        ASTNode denPrime;
        try {
            numPrime = Derivative.derivativeOf(parts.numerator(), variable);
            denPrime = Derivative.derivativeOf(parts.denominator(), variable);
        } catch (RuntimeException e) {
            steps.add(new Step("Could not differentiate for L'H\u00f4pital: "
                    + (e.getMessage() != null ? e.getMessage() : "unsupported")));
            return null;
        }

        steps.add(new Step("Differentiate the numerator", numPrime));
        steps.add(new Step("Differentiate the denominator", denPrime));

        if (isZeroAtApproach(denPrime, variable, approach)
                && isZeroAtApproach(numPrime, variable, approach) == false
                && !isInfiniteAtApproach(denPrime, variable, approach)) {
            // still ok — may be another 0/0 after simplify
        }

        ASTNode next = Simplifier.simplify(RationalExpression.quotient(numPrime, denPrime));
        steps.add(new Step("New limit expression", next));

        return evaluateLimit(new LimitQuery(variable, approach, next), depth + 1, steps);
    }

    private static Result tryPolynomialFactor(ASTNode expression, String variable,
                                              Approach.Finite approach, List<Step> steps) {
        RationalExpression.Quotient rational = asPolynomialRational(expression, variable);
        if (rational == null) {
            return null;
        }
        if (!isZeroOverZero(rational.numerator(), rational.denominator(), variable, approach.value())) {
            return null;
        }

        steps.add(new Step("Indeterminate form 0/0 \u2014 factor and cancel common terms"));

        ASTNode factoredNum = Simplifier.simplify(Factoring.factor(rational.numerator()));
        if (!factoredNum.equals(rational.numerator())) {
            steps.add(new Step("Factor the numerator", factoredNum));
        }

        ASTNode factoredDen = Simplifier.simplify(Factoring.factor(rational.denominator()));
        if (!factoredDen.equals(rational.denominator())) {
            steps.add(new Step("Factor the denominator", factoredDen));
        }

        ASTNode cancelled = cancelCommonFactors(factoredNum, factoredDen);
        if (!cancelled.equals(RationalExpression.quotient(factoredNum, factoredDen))) {
            steps.add(new Step("Cancel common factors", cancelled));
        } else if (factoredNum.equals(rational.numerator()) && factoredDen.equals(rational.denominator())) {
            return null;
        }

        return tryDirectSubstitution(cancelled, variable, approach, steps);
    }

    private static Result tryDirectSubstitution(ASTNode expression, String variable,
                                                Approach.Finite approach, List<Step> steps) {
        Num value = approach.value();

        if (isRationalZeroOverZero(expression, variable, value)) {
            return null;
        }
        if (isNonzeroOverZero(expression, variable, value)) {
            return divergeFromSide(expression, variable, approach, steps);
        }

        ASTNode substituted;
        try {
            substituted = Simplifier.simplify(FunctionUtils.substitute(expression, variable, value));
        } catch (ArithmeticException e) {
            if (isNonzeroOverZero(expression, variable, value)) {
                return divergeFromSide(expression, variable, approach, steps);
            }
            return null;
        }
        steps.add(new Step("Substitute " + variable + " = " + value.toDisplay(), substituted));

        if (!isDefinedNear(expression, variable, approach)) {
            steps.add(new Step("Expression is not defined from this side near the approach"));
            return failWith(steps, "The limit does not exist from this side.");
        }

        try {
            ExpressionEvaluator.NumericEvaluation evaluation = ExpressionEvaluator.evaluate(substituted);
            if (evaluation.hasExact()) {
                Num exact = evaluation.exact();
                steps.add(new Step("Limit", exact));
                Double decimal = exact.denominator().equals(java.math.BigInteger.ONE) ? null : exact.toDouble();
                return Result.of(exact, decimal, steps);
            }
            steps.add(new Step("Limit (approximate)"));
            return Result.of(substituted, evaluation.approximate(), steps);
        } catch (ArithmeticException divisionByZero) {
            if (isRationalZeroOverZero(expression, variable, value)) {
                return null;
            }
            RationalExpression.Quotient rational = asPolynomialRational(expression, variable);
            if (rational != null
                    && isZeroOverZero(rational.numerator(), rational.denominator(), variable, value)) {
                return null;
            }
            return divergeFromSide(expression, variable, approach, steps);
        } catch (RuntimeException e) {
            if (isRationalZeroOverZero(expression, variable, value)) {
                return null;
            }
            String message = e.getMessage() != null ? e.getMessage() : "Could not evaluate";
            return failWith(steps, message);
        }
    }

    private static Result divergeFromSide(ASTNode expression, String variable,
                                          Approach.Finite approach, List<Step> steps) {
        steps.add(new Step("Substitute " + variable + " = " + approach.value().toDisplay(),
                FunctionUtils.substitute(expression, variable, approach.value())));

        int sign = infinitySign(expression, variable, approach);
        if (approach.side() == Side.BOTH) {
            Approach.Finite left = new Approach.Finite(approach.value(), Side.LEFT);
            Approach.Finite right = new Approach.Finite(approach.value(), Side.RIGHT);
            int leftSign = infinitySign(expression, variable, left);
            int rightSign = infinitySign(expression, variable, right);
            if (leftSign != 0 && rightSign != 0 && leftSign != rightSign) {
                steps.add(new Step("Left- and right-hand limits diverge to opposite infinities"));
                return failWith(steps, "The limit does not exist (approaches \u00b1\u221e).");
            }
            if (sign == 0) {
                sign = leftSign != 0 ? leftSign : rightSign;
            }
        }

        if (sign > 0) {
            steps.add(new Step("Approaches +\u221e", POS_INF));
            return failWith(steps, "The limit does not exist (approaches \u221e).");
        }
        if (sign < 0) {
            steps.add(new Step("Approaches -\u221e", NEG_INF));
            return failWith(steps, "The limit does not exist (approaches -\u221e).");
        }
        steps.add(new Step("Denominator is 0 after substitution \u2014 the limit is not finite at this point"));
        return failWith(steps, "The limit does not exist (approaches \u00b1\u221e).");
    }

    private static int infinitySign(ASTNode expression, String variable, Approach.Finite approach) {
        double sample = samplePoint(approach);
        try {
            double value = ExpressionEvaluator.evaluateAtDouble(expression, variable, sample);
            if (Double.isInfinite(value)) {
                return value > 0 ? 1 : -1;
            }
            if (Double.isNaN(value)) {
                return 0;
            }
            // Very large magnitude near a pole
            if (Math.abs(value) > 1e8) {
                return value > 0 ? 1 : -1;
            }
            // Compare with a closer sample to see direction
            double closer = samplePoint(new Approach.Finite(approach.value(), approach.side()),
                    SAMPLE_EPS / 10);
            double closerVal = ExpressionEvaluator.evaluateAtDouble(expression, variable, closer);
            if (Math.abs(closerVal) > Math.abs(value) && Math.abs(closerVal) > 1e4) {
                return closerVal > 0 ? 1 : -1;
            }
            return 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static double samplePoint(Approach.Finite approach) {
        return samplePoint(approach, SAMPLE_EPS);
    }

    private static double samplePoint(Approach.Finite approach, double eps) {
        double a = approach.value().toDouble();
        return switch (approach.side()) {
            case LEFT -> a - eps;
            case RIGHT -> a + eps;
            case BOTH -> a + eps;
        };
    }

    private static boolean isDefinedNear(ASTNode expression, String variable, Approach.Finite approach) {
        if (approach.side() == Side.BOTH) {
            return isDefinedAtSample(expression, variable, samplePoint(
                    new Approach.Finite(approach.value(), Side.LEFT)))
                    || isDefinedAtSample(expression, variable, samplePoint(
                    new Approach.Finite(approach.value(), Side.RIGHT)))
                    || isDefinedAtSample(expression, variable, approach.value().toDouble());
        }
        return isDefinedAtSample(expression, variable, samplePoint(approach));
    }

    private static boolean isDefinedAtSample(ASTNode expression, String variable, double x) {
        try {
            double v = ExpressionEvaluator.evaluateAtDouble(expression, variable, x);
            return !Double.isNaN(v);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Result resolveTwoSidedWithAbs(String variable, Approach.Finite approach,
                                                 ASTNode expression, List<Step> steps, int depth) {
        // Only special-case when abs argument vanishes at the approach (kink / indeterminate).
        if (!absArgumentVanishes(expression, variable, approach.value())) {
            return null;
        }
        List<Step> leftSteps = new ArrayList<>(steps);
        List<Step> rightSteps = new ArrayList<>(steps);
        leftSteps.add(new Step("Absolute value changes at the approach \u2014 compare one-sided limits"));
        rightSteps.add(new Step("Absolute value changes at the approach \u2014 compare one-sided limits"));

        Result left = evaluateAtFinite(variable, new Approach.Finite(approach.value(), Side.LEFT),
                rewriteAbs(expression, variable, new Approach.Finite(approach.value(), Side.LEFT)).expression(),
                leftSteps, depth + 1);
        Result right = evaluateAtFinite(variable, new Approach.Finite(approach.value(), Side.RIGHT),
                rewriteAbs(expression, variable, new Approach.Finite(approach.value(), Side.RIGHT)).expression(),
                rightSteps, depth + 1);

        if (left.isSuccess() && right.isSuccess() && sameAnswer(left, right)) {
            steps.addAll(left.getSteps().subList(steps.size(), left.getSteps().size()));
            steps.add(new Step("Left- and right-hand limits agree"));
            return Result.of(left.getExact(), left.getDecimal(), steps);
        }
        if (!left.isSuccess() && !right.isSuccess()) {
            return null; // fall through to normal path
        }
        steps.add(new Step("Left- and right-hand limits differ"));
        return failWith(steps, "The limit does not exist (left and right limits differ).");
    }

    private static boolean sameAnswer(Result a, Result b) {
        if (a.getExact() != null && b.getExact() != null) {
            return Simplifier.simplify(a.getExact()).equals(Simplifier.simplify(b.getExact()));
        }
        if (a.getDecimal() != null && b.getDecimal() != null) {
            return Math.abs(a.getDecimal() - b.getDecimal()) < 1e-9;
        }
        return false;
    }

    private static boolean containsAbs(ASTNode node) {
        return switch (node) {
            case Func f -> "abs".equalsIgnoreCase(f.name()) || containsAbs(f.argument());
            case Neg n -> containsAbs(n.operand());
            case Add a -> containsAbs(a.left()) || containsAbs(a.right());
            case Sub s -> containsAbs(s.left()) || containsAbs(s.right());
            case Mul m -> containsAbs(m.left()) || containsAbs(m.right());
            case Div d -> containsAbs(d.left()) || containsAbs(d.right());
            case Pow p -> containsAbs(p.base()) || containsAbs(p.exponent());
            default -> false;
        };
    }

    private static boolean absArgumentVanishes(ASTNode node, String variable, Num approach) {
        return switch (node) {
            case Func f -> {
                if ("abs".equalsIgnoreCase(f.name())) {
                    yield isZeroAt(f.argument(), variable, approach);
                }
                yield absArgumentVanishes(f.argument(), variable, approach);
            }
            case Neg n -> absArgumentVanishes(n.operand(), variable, approach);
            case Add a -> absArgumentVanishes(a.left(), variable, approach)
                    || absArgumentVanishes(a.right(), variable, approach);
            case Sub s -> absArgumentVanishes(s.left(), variable, approach)
                    || absArgumentVanishes(s.right(), variable, approach);
            case Mul m -> absArgumentVanishes(m.left(), variable, approach)
                    || absArgumentVanishes(m.right(), variable, approach);
            case Div d -> absArgumentVanishes(d.left(), variable, approach)
                    || absArgumentVanishes(d.right(), variable, approach);
            case Pow p -> absArgumentVanishes(p.base(), variable, approach)
                    || absArgumentVanishes(p.exponent(), variable, approach);
            default -> false;
        };
    }

    private record AbsRewrite(ASTNode expression, boolean changed) {}

    private static AbsRewrite rewriteAbs(ASTNode expression, String variable, Approach approach) {
        ASTNode rewritten = rewriteAbsNode(expression, variable, approach);
        ASTNode simplified = Simplifier.simplify(rewritten);
        return new AbsRewrite(simplified, !sameExpression(simplified, expression));
    }

    private static ASTNode rewriteAbsNode(ASTNode node, String variable, Approach approach) {
        return switch (node) {
            case Func f -> {
                ASTNode arg = rewriteAbsNode(f.argument(), variable, approach);
                if ("abs".equalsIgnoreCase(f.name())) {
                    Integer sign = signNear(arg, variable, approach);
                    if (sign != null && sign > 0) {
                        yield arg;
                    }
                    if (sign != null && sign < 0) {
                        yield new Neg(arg);
                    }
                }
                yield new Func(f.name(), arg);
            }
            case Neg n -> new Neg(rewriteAbsNode(n.operand(), variable, approach));
            case Add a -> new Add(rewriteAbsNode(a.left(), variable, approach),
                    rewriteAbsNode(a.right(), variable, approach));
            case Sub s -> new Sub(rewriteAbsNode(s.left(), variable, approach),
                    rewriteAbsNode(s.right(), variable, approach));
            case Mul m -> new Mul(rewriteAbsNode(m.left(), variable, approach),
                    rewriteAbsNode(m.right(), variable, approach));
            case Div d -> new Div(rewriteAbsNode(d.left(), variable, approach),
                    rewriteAbsNode(d.right(), variable, approach));
            case Pow p -> new Pow(rewriteAbsNode(p.base(), variable, approach),
                    rewriteAbsNode(p.exponent(), variable, approach));
            default -> node;
        };
    }

    private static Integer signNear(ASTNode expression, String variable, Approach approach) {
        try {
            if (approach instanceof Approach.Infinity inf) {
                double sample = inf.positive() ? LARGE : -LARGE;
                double v = ExpressionEvaluator.evaluateAtDouble(expression, variable, sample);
                if (v > 0) {
                    return 1;
                }
                if (v < 0) {
                    return -1;
                }
                return null;
            }
            Approach.Finite finite = (Approach.Finite) approach;
            if (finite.side() == Side.BOTH) {
                Integer left = signNear(expression, variable, new Approach.Finite(finite.value(), Side.LEFT));
                Integer right = signNear(expression, variable, new Approach.Finite(finite.value(), Side.RIGHT));
                if (left != null && left.equals(right)) {
                    return left;
                }
                // Continuous away from zero: use value at the point when nonzero
                ASTNode at = Simplifier.simplify(FunctionUtils.substitute(expression, variable, finite.value()));
                if (at instanceof Num n && !n.isZero()) {
                    return n.numerator().signum() > 0 ? 1 : -1;
                }
                return null;
            }
            double v = ExpressionEvaluator.evaluateAtDouble(expression, variable, samplePoint(finite));
            if (v > 0) {
                return 1;
            }
            if (v < 0) {
                return -1;
            }
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Result finishNumeric(ASTNode node, List<Step> steps) {
        try {
            ExpressionEvaluator.NumericEvaluation evaluation = ExpressionEvaluator.evaluate(node);
            if (evaluation.hasExact()) {
                Num exact = evaluation.exact();
                steps.add(new Step("Limit", exact));
                Double decimal = exact.denominator().equals(java.math.BigInteger.ONE) ? null : exact.toDouble();
                return Result.of(exact, decimal, steps);
            }
            steps.add(new Step("Limit (approximate)"));
            return Result.of(node, evaluation.approximate(), steps);
        } catch (RuntimeException e) {
            steps.add(new Step("Limit", node));
            return Result.of(node, null, steps);
        }
    }

    private static Result failWith(List<Step> steps, String message) {
        // Preserve steps in the failure path by returning a failed Result
        // (Result.failure does not carry steps; include message only).
        return Result.failure(message);
    }

    private static boolean sameExpression(ASTNode a, ASTNode b) {
        return Simplifier.simplify(a).equals(Simplifier.simplify(b));
    }

    private static boolean isIndeterminateForm(ASTNode expression, String variable, Approach approach) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        if (parts.denominator().equals(ONE)) {
            return false;
        }
        return isZeroOverZero(parts.numerator(), parts.denominator(), variable, approach)
                || isInfinityOverInfinity(parts.numerator(), parts.denominator(), variable, approach);
    }

    private static boolean isRationalZeroOverZero(ASTNode expression, String variable, Num approach) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        return isZeroOverZero(parts.numerator(), parts.denominator(), variable, approach);
    }

    private static boolean isZeroOverZero(ASTNode numerator, ASTNode denominator,
                                          String variable, Approach approach) {
        return isZeroAtApproach(numerator, variable, approach)
                && isZeroAtApproach(denominator, variable, approach);
    }

    private static boolean isZeroOverZero(ASTNode numerator, ASTNode denominator,
                                          String variable, Num approach) {
        return isZeroAt(numerator, variable, approach) && isZeroAt(denominator, variable, approach);
    }

    private static boolean isInfinityOverInfinity(ASTNode numerator, ASTNode denominator,
                                                  String variable, Approach approach) {
        return isInfiniteAtApproach(numerator, variable, approach)
                && isInfiniteAtApproach(denominator, variable, approach);
    }

    private static boolean isNonzeroOverZero(ASTNode expression, String variable, Num approach) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        return !isZeroAt(parts.numerator(), variable, approach)
                && isZeroAt(parts.denominator(), variable, approach);
    }

    private static boolean isZeroAtApproach(ASTNode expression, String variable, Approach approach) {
        if (approach instanceof Approach.Finite finite) {
            return isZeroAt(expression, variable, finite.value());
        }
        // At infinity: treat polynomials of positive degree as infinite, constants as themselves
        return false;
    }

    private static boolean isInfiniteAtApproach(ASTNode expression, String variable, Approach approach) {
        if (approach instanceof Approach.Infinity) {
            Map<Integer, Num> coeffs = Factoring.coefficients(expression, variable);
            if (coeffs != null) {
                return degreeOf(coeffs) > 0;
            }
            try {
                Approach.Infinity inf = (Approach.Infinity) approach;
                double sample = inf.positive() ? LARGE : -LARGE;
                double v = ExpressionEvaluator.evaluateAtDouble(expression, variable, sample);
                return Double.isInfinite(v) || Math.abs(v) > 1e8;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isZeroAt(ASTNode expression, String variable, Num approach) {
        try {
            ASTNode value = Simplifier.simplify(FunctionUtils.substitute(expression, variable, approach));
            return value instanceof Num n && n.isZero();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static RationalExpression.Quotient asPolynomialRational(ASTNode expression, String variable) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        if (!isPolynomialIn(parts.numerator(), variable) || !isPolynomialIn(parts.denominator(), variable)) {
            return null;
        }
        return parts;
    }

    private static boolean isPolynomialIn(ASTNode expression, String variable) {
        Map<Integer, Num> coeffs = Factoring.coefficients(expression, variable);
        return coeffs != null;
    }

    private static ASTNode cancelQuotientFactors(ASTNode expression) {
        RationalExpression.Quotient parts = flattenNestedQuotient(RationalExpression.extractQuotient(expression));
        return cancelCommonFactors(parts.numerator(), parts.denominator());
    }

    private static RationalExpression.Quotient flattenNestedQuotient(RationalExpression.Quotient quotient) {
        ASTNode numerator = quotient.numerator();
        ASTNode denominator = quotient.denominator();
        while (true) {
            RationalExpression.Quotient inner = RationalExpression.extractQuotient(numerator);
            if (inner.denominator().equals(ONE)) {
                break;
            }
            numerator = inner.numerator();
            denominator = Simplifier.simplify(new Mul(inner.denominator(), denominator));
        }
        return new RationalExpression.Quotient(numerator, denominator);
    }

    private static ASTNode cancelCommonFactors(ASTNode numerator, ASTNode denominator) {
        List<ASTNode> numFactors = new ArrayList<>(flattenMul(numerator));
        List<ASTNode> denFactors = new ArrayList<>(flattenMul(denominator));

        for (int d = 0; d < denFactors.size(); d++) {
            ASTNode denFactor = Simplifier.simplify(denFactors.get(d));
            for (int n = 0; n < numFactors.size(); n++) {
                if (Simplifier.simplify(numFactors.get(n)).equals(denFactor) && !denFactor.equals(ONE)) {
                    numFactors.remove(n);
                    denFactors.remove(d);
                    d--;
                    break;
                }
            }
        }

        return RationalExpression.quotient(
                RationalExpression.rebuildProduct(numFactors),
                RationalExpression.rebuildProduct(denFactors));
    }

    private static List<ASTNode> flattenMul(ASTNode node) {
        node = Simplifier.simplify(node);
        if (node instanceof Mul m) {
            List<ASTNode> out = new ArrayList<>();
            out.addAll(flattenMul(m.left()));
            out.addAll(flattenMul(m.right()));
            return out;
        }
        if (node instanceof Neg n) {
            List<ASTNode> inner = flattenMul(n.operand());
            if (inner.size() == 1 && inner.get(0) instanceof Num num) {
                return List.of(Simplifier.simplify(new Mul(Num.of(-1), num)));
            }
            List<ASTNode> out = new ArrayList<>();
            out.add(Num.of(-1));
            out.addAll(inner);
            return out;
        }
        if (node instanceof Pow p && p.exponent() instanceof Num n && n.isInteger()
                && n.numerator().intValueExact() == 2) {
            List<ASTNode> out = new ArrayList<>();
            out.add(p.base());
            out.add(p.base());
            return out;
        }
        if (node instanceof Num num && num.equals(ONE)) {
            return List.of();
        }
        return List.of(node);
    }

    private static String indeterminateMessage(ASTNode expression, String variable, Approach approach) {
        return "Indeterminate form at " + variable + " = " + approach.toDisplay()
                + ". This limit needs a method not supported yet.";
    }

    /** Parses {@code lim x->2 expr}, {@code lim x->1- expr}, {@code lim x->inf expr}. */
    public static LimitQuery parseInput(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new Parser.ParseException("Enter a limit such as lim x->2 (x^2-4)/(x-2)");
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("lim")) {
            throw new Parser.ParseException("Enter a limit such as lim x->2 (x^2-4)/(x-2)");
        }

        int pos = lower.startsWith("limit") ? 5 : 3;
        pos = skipWhitespace(text, pos);
        if (pos < text.length() && text.charAt(pos) == '(') {
            pos++;
        }
        pos = skipWhitespace(text, pos);

        int varStart = pos;
        while (pos < text.length() && Character.isLetter(text.charAt(pos))) {
            pos++;
        }
        if (varStart == pos) {
            throw new Parser.ParseException("Expected a variable after lim, such as lim x->2 ...");
        }
        String variable = text.substring(varStart, pos).trim();
        pos = skipWhitespace(text, pos);

        if (pos + 1 >= text.length() || text.charAt(pos) != '-' || text.charAt(pos + 1) != '>') {
            throw new Parser.ParseException("Expected -> after the variable, such as lim x->2 ...");
        }
        pos += 2;
        pos = skipWhitespace(text, pos);

        ApproachParse approachParse = parseApproach(text, pos);
        pos = approachParse.end();
        Approach approach = approachParse.approach();

        pos = skipWhitespace(text, pos);
        if (pos < text.length() && text.charAt(pos) == ')') {
            pos++;
        }
        pos = skipWhitespace(text, pos);

        String exprPart = text.substring(pos).trim();
        if (exprPart.isEmpty()) {
            throw new Parser.ParseException("Enter an expression after the approach value");
        }

        ASTNode expression = Simplifier.simplify(Parser.parse(exprPart));
        return new LimitQuery(variable, approach, expression);
    }

    private record ApproachParse(Approach approach, int end) {}

    private static ApproachParse parseApproach(String text, int pos) {
        int start = pos;
        if (pos >= text.length()) {
            throw new Parser.ParseException("Expected an approach value after ->");
        }

        if (text.charAt(pos) == '(') {
            int close = findMatchingParen(text, pos);
            String inside = text.substring(pos + 1, close).trim();
            Approach approach = parseApproachToken(inside);
            return new ApproachParse(approach, close + 1);
        }

        // Read until whitespace or ')' — then peel off optional one-sided +/- from a finite value.
        while (pos < text.length() && !Character.isWhitespace(text.charAt(pos)) && text.charAt(pos) != ')') {
            pos++;
        }
        String token = text.substring(start, pos).trim();
        Approach approach = parseApproachToken(token);
        return new ApproachParse(approach, pos);
    }

    private static Approach parseApproachToken(String text) {
        String raw = text.trim();
        if (raw.isEmpty()) {
            throw new Parser.ParseException("Expected an approach value after ->");
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        if (isPositiveInfinity(lower)) {
            return new Approach.Infinity(true);
        }
        if (isNegativeInfinity(lower)) {
            return new Approach.Infinity(false);
        }

        Side side = Side.BOTH;
        String numberText = raw;
        if (raw.length() >= 2) {
            char last = raw.charAt(raw.length() - 1);
            if (last == '+' || last == '-') {
                String without = raw.substring(0, raw.length() - 1).trim();
                String withoutLower = without.toLowerCase(Locale.ROOT);
                // Don't treat the sign of a pure signed infinity / negative number start as a side
                // when the token is just "-" or the side would leave an empty / invalid number —
                // but "1-" / "2+" / "-3+" are one-sided.
                if (!without.isEmpty() && !isPositiveInfinity(withoutLower) && !isNegativeInfinity(withoutLower)) {
                    // Negative numbers like "-3" must not become side from a lone leading minus.
                    // Only treat trailing +/- as side when the prefix parses as a number.
                    try {
                        ExpressionEvaluator.parseNumeric(without);
                        numberText = without;
                        side = last == '+' ? Side.RIGHT : Side.LEFT;
                    } catch (RuntimeException ignored) {
                        // keep full token as the number (e.g. malformed)
                    }
                }
            }
        }

        Num value = ExpressionEvaluator.parseNumeric(numberText);
        return new Approach.Finite(value, side);
    }

    private static boolean isPositiveInfinity(String lower) {
        return lower.equals("inf") || lower.equals("infinity") || lower.equals("oo")
                || lower.equals("+inf") || lower.equals("+infinity") || lower.equals("+oo")
                || lower.equals("\u221e") || lower.equals("+\u221e");
    }

    private static boolean isNegativeInfinity(String lower) {
        return lower.equals("-inf") || lower.equals("-infinity") || lower.equals("-oo")
                || lower.equals("-\u221e");
    }

    private static int skipWhitespace(String text, int pos) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int findMatchingParen(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new Parser.ParseException("Unmatched parenthesis in limit input");
    }
}
