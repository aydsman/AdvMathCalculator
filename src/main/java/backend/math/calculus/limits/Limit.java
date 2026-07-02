package backend.math.calculus.limits;

import backend.engine.MathOperation;
import backend.math.algebra.ComplexFractions;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Factoring;
import backend.math.algebra.RationalExpression;
import backend.math.algebra.Rationalizer;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Symbolic limits: direct substitution, complex fractions, conjugate rationalization,
 * and polynomial factoring for {@code 0/0} forms.
 */
public class Limit implements MathOperation {

    private static final Num ONE = Num.of(1);
    private static final String LHOPITAL_MSG =
            "This limit needs L'H\u00f4pital's rule or another method not supported yet.";

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a limit such as lim x->2 (x^2-4)/(x-2)");
    }

    public Result solveFromInput(String raw) {
        LimitQuery query = parseInput(raw);
        return evaluateLimit(query);
    }

    private static Result evaluateLimit(LimitQuery query) {
        String variable = query.variable();
        Num approach = query.approach();
        ASTNode expression = Simplifier.simplify(query.expression());

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(
                "Find lim " + variable + " \u2192 " + approach.toDisplay(),
                expression));

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

        if (isRationalZeroOverZero(current, variable, approach)) {
            return Result.failure(indeterminateMessage(current, variable, approach));
        }
        return Result.failure(LHOPITAL_MSG);
    }

    private static Result tryPolynomialFactor(ASTNode expression, String variable, Num approach,
                                            List<Step> steps) {
        RationalExpression.Quotient rational = asPolynomialRational(expression, variable);
        if (rational == null) {
            return null;
        }
        if (!isZeroOverZero(rational.numerator(), rational.denominator(), variable, approach)) {
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

        Result afterCancel = tryDirectSubstitution(cancelled, variable, approach, steps);
        return afterCancel;
    }

    private static Result tryDirectSubstitution(ASTNode expression, String variable, Num approach,
                                                List<Step> steps) {
        if (isRationalZeroOverZero(expression, variable, approach)) {
            return null;
        }
        if (isNonzeroOverZero(expression, variable, approach)) {
            ASTNode substituted = backend.math.functions.FunctionUtils.substitute(
                    expression, variable, approach);
            steps.add(new Step("Substitute " + variable + " = " + approach.toDisplay(), substituted));
            steps.add(new Step("Denominator is 0 after substitution \u2014 the limit is not finite at this point"));
            return Result.failure("The limit does not exist (approaches \u00b1\u221e).");
        }

        ASTNode substituted;
        try {
            substituted = Simplifier.simplify(
                    backend.math.functions.FunctionUtils.substitute(expression, variable, approach));
        } catch (ArithmeticException e) {
            if (isNonzeroOverZero(expression, variable, approach)) {
                substituted = backend.math.functions.FunctionUtils.substitute(expression, variable, approach);
            } else {
                throw e;
            }
        }
        steps.add(new Step("Substitute " + variable + " = " + approach.toDisplay(), substituted));

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
            if (isRationalZeroOverZero(expression, variable, approach)) {
                return null;
            }
            RationalExpression.Quotient rational = asPolynomialRational(expression, variable);
            if (rational != null
                    && isZeroOverZero(rational.numerator(), rational.denominator(), variable, approach)) {
                return null;
            }
            steps.add(new Step("Denominator is 0 after substitution \u2014 the limit is not finite at this point"));
            return Result.failure("The limit does not exist (approaches \u00b1\u221e).");
        } catch (RuntimeException e) {
            if (isRationalZeroOverZero(expression, variable, approach)) {
                return null;
            }
            String message = e.getMessage() != null ? e.getMessage() : "Could not evaluate";
            return Result.failure(message);
        }
    }

    private static boolean sameExpression(ASTNode a, ASTNode b) {
        return Simplifier.simplify(a).equals(Simplifier.simplify(b));
    }

    private static boolean isRationalZeroOverZero(ASTNode expression, String variable, Num approach) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        return isZeroOverZero(parts.numerator(), parts.denominator(), variable, approach);
    }

    private static boolean isZeroOverZero(ASTNode numerator, ASTNode denominator, String variable, Num approach) {
        return isZeroAt(numerator, variable, approach) && isZeroAt(denominator, variable, approach);
    }

    private static boolean isNonzeroOverZero(ASTNode expression, String variable, Num approach) {
        RationalExpression.Quotient parts = RationalExpression.extractQuotient(expression);
        return !isZeroAt(parts.numerator(), variable, approach)
                && isZeroAt(parts.denominator(), variable, approach);
    }

    private static boolean isZeroAt(ASTNode expression, String variable, Num approach) {
        ASTNode value = Simplifier.simplify(
                backend.math.functions.FunctionUtils.substitute(expression, variable, approach));
        return value instanceof Num n && n.isZero();
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
                if (Simplifier.simplify(numFactors.get(n)).equals(denFactor)) {
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
        if (node instanceof Pow p && p.exponent() instanceof Num n && n.isInteger() && n.numerator().intValueExact() == 2) {
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

    private static String indeterminateMessage(ASTNode expression, String variable, Num approach) {
        return "Indeterminate form at " + variable + " = " + approach.toDisplay()
                + ". " + LHOPITAL_MSG;
    }

    /** Parses {@code lim x->2 expr} or {@code limit(x->2) expr}. */
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

        int approachStart = pos;
        if (pos < text.length() && text.charAt(pos) == '(') {
            int close = findMatchingParen(text, pos);
            pos = close + 1;
        } else {
            while (pos < text.length() && !Character.isWhitespace(text.charAt(pos)) && text.charAt(pos) != ')') {
                pos++;
            }
        }
        String approachText = text.substring(approachStart, pos).trim();
        if (approachText.startsWith("(") && approachText.endsWith(")")) {
            approachText = approachText.substring(1, approachText.length() - 1).trim();
        }
        Num approach = parseApproach(approachText);

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

    private static Num parseApproach(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.equals("inf") || lower.equals("infinity") || lower.equals("oo")
                || lower.equals("+inf") || lower.equals("+infinity") || lower.equals("+oo")) {
            throw new Parser.ParseException("Limits at +\u221e are not supported yet.");
        }
        if (lower.equals("-inf") || lower.equals("-infinity") || lower.equals("-oo")) {
            throw new Parser.ParseException("Limits at -\u221e are not supported yet.");
        }
        return ExpressionEvaluator.parseNumeric(text);
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

    public record LimitQuery(String variable, Num approach, ASTNode expression) {}
}
