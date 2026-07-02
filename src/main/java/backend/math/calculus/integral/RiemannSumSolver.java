package backend.math.calculus.integral;

import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Factoring;
import backend.math.algebra.Simplifier;
import backend.math.functions.FunctionUtils;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Symbolic left Riemann sums for polynomial integrands using summation identities. */
final class RiemannSumSolver {

    private static final Var N = new Var("n");
    private static final Var K = new Var("k");
    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);

    private RiemannSumSolver() {}

    static Result solvePolynomial(ASTNode function, String variable, Num a, Num b, int n) {
        Map<Integer, Num> coeffs = Factoring.coefficients(function, variable);
        if (coeffs == null || coeffs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Symbolic Riemann sums currently support polynomial integrands in " + variable);
        }

        int maxPower = coeffs.keySet().stream().max(Integer::compareTo).orElse(0);
        if (maxPower > 4) {
            throw new IllegalArgumentException(
                    "Polynomial degree must be 4 or less for symbolic summation");
        }

        ASTNode deltaX = Simplifier.simplify(div(sub(b, a), N));
        ASTNode expanded = expandInK(coeffs, a, deltaX, maxPower);
        ASTNode riemannSum = buildSummedExpression(coeffs, a, deltaX, maxPower);

        List<Step> steps = new ArrayList<>();
        steps.add(IntegralNotation.definiteIntegralStep(a, b, function, variable));
        steps.add(new Step("Left Riemann sum spacing", "\u0394x = (b-a)/n = " + deltaX.toDisplay()));
        steps.add(new Step("Left endpoints",
                "x_k = a + k\u0394x,  k = 0, 1, ..., n-1"));
        steps.add(new Step("Set up the left Riemann sum",
                "S_n = \u0394x \u00B7 \u03A3_{k=0}^{n-1} f(x_k)"));
        steps.add(new Step("Substitute f(x)",
                "S_n = \u0394x \u00B7 \u03A3_{k=0}^{n-1} (" + function.toDisplay()
                        + ") with x = a + k\u0394x"));
        steps.add(new Step("Expand f(a + k\u0394x) as a polynomial in k",
                "f(a + k\u0394x) = " + expanded.toDisplay()));

        TreeMap<Integer, ASTNode> grouped = groupedCoefficients(coeffs, a, deltaX, maxPower);
        for (int power = 0; power <= maxPower; power++) {
            if (!grouped.containsKey(power)) {
                continue;
            }
            ASTNode coeff = grouped.get(power);
            if (isZero(coeff)) {
                continue;
            }
            steps.add(new Step("Apply summation identity",
                    SummationIdentities.sumNotation(power) + " = "
                            + SummationIdentities.sumKToPowDisplay(power)));
            steps.add(new Step("Replace in the sum",
                    "\u0394x \u00B7 (" + coeff.toDisplay() + ") \u00B7 "
                            + SummationIdentities.sumKToPowDisplay(power)));
        }

        ASTNode simplified = Simplifier.simplify(riemannSum);
        steps.add(new Step("Simplify the symbolic sum", simplified.toDisplay()));
        steps.add(new Step("Evaluate with n = " + n,
                Simplifier.simplify(FunctionUtils.substitute(simplified, "n", Num.of(n))).toDisplay()));

        ASTNode evaluated = Simplifier.simplify(FunctionUtils.substitute(simplified, "n", Num.of(n)));
        Num exact = toNum(evaluated);
        double decimal = exact.toDouble();

        return Result.of(exact, decimal, steps);
    }

    private static ASTNode buildSummedExpression(
            Map<Integer, Num> coeffs, Num a, ASTNode deltaX, int maxPower) {
        ASTNode total = null;
        TreeMap<Integer, ASTNode> grouped = groupedCoefficients(coeffs, a, deltaX, maxPower);
        for (var entry : grouped.entrySet()) {
            int power = entry.getKey();
            ASTNode coeff = entry.getValue();
            if (isZero(coeff)) {
                continue;
            }
            ASTNode term = mul(mul(deltaX, coeff), SummationIdentities.sumKToPow(power));
            total = total == null ? term : new Add(total, term);
        }
        return total == null ? ZERO : total;
    }

    private static TreeMap<Integer, ASTNode> groupedCoefficients(
            Map<Integer, Num> coeffs, Num a, ASTNode deltaX, int maxPower) {
        TreeMap<Integer, ASTNode> grouped = new TreeMap<>();
        for (int m = 0; m <= maxPower; m++) {
            ASTNode coeff = null;
            for (int j = m; j <= maxPower; j++) {
                Num cj = coeffs.get(j);
                if (cj == null || cj.isZero()) {
                    continue;
                }
                ASTNode term = mul(
                        mul(num(cj), binomial(j, m)),
                        mul(pow(a, j - m), pow(deltaX, m)));
                coeff = coeff == null ? term : new Add(coeff, term);
            }
            if (coeff != null) {
                grouped.put(m, Simplifier.simplify(coeff));
            }
        }
        return grouped;
    }

    private static ASTNode expandInK(Map<Integer, Num> coeffs, Num a, ASTNode deltaX, int maxPower) {
        ASTNode total = null;
        for (int j = 0; j <= maxPower; j++) {
            Num cj = coeffs.get(j);
            if (cj == null || cj.isZero()) {
                continue;
            }
            ASTNode expandedPower = expandBinomialInK(a, deltaX, j);
            ASTNode term = mul(num(cj), expandedPower);
            total = total == null ? term : new Add(total, term);
        }
        return total == null ? ZERO : Simplifier.simplify(total);
    }

    private static ASTNode expandBinomialInK(Num a, ASTNode deltaX, int degree) {
        ASTNode total = null;
        for (int m = 0; m <= degree; m++) {
            ASTNode term = mul(
                    mul(binomial(degree, m), pow(a, degree - m)),
                    mul(pow(deltaX, m), pow(K, m)));
            total = total == null ? term : new Add(total, term);
        }
        return total == null ? ZERO : total;
    }

    private static Num binomial(int n, int k) {
        if (k < 0 || k > n) {
            return ZERO;
        }
        long value = 1;
        for (int i = 1; i <= k; i++) {
            value = value * (n - (k - i)) / i;
        }
        return Num.of(value);
    }

    private static Num toNum(ASTNode node) {
        ASTNode simplified = Simplifier.simplify(node);
        if (simplified instanceof Num num) {
            return num;
        }
        return ExpressionEvaluator.evaluateNumeric(simplified);
    }

    private static boolean isZero(ASTNode node) {
        return node instanceof Num n && n.isZero();
    }

    private static ASTNode num(Num value) {
        return value;
    }

    private static ASTNode sub(Num left, Num right) {
        return new Sub(left, right);
    }

    private static ASTNode div(ASTNode left, ASTNode right) {
        return new Div(left, right);
    }

    private static ASTNode mul(ASTNode left, ASTNode right) {
        return new Mul(left, right);
    }

    private static ASTNode mul(Num left, ASTNode right) {
        if (left.isZero()) {
            return ZERO;
        }
        if (left.equals(ONE)) {
            return right;
        }
        return new Mul(left, right);
    }

    private static ASTNode pow(Num base, int exp) {
        if (exp == 0) {
            return ONE;
        }
        if (exp == 1) {
            return base;
        }
        return new Pow(base, Num.of(exp));
    }

    private static ASTNode pow(ASTNode base, int exp) {
        if (exp == 0) {
            return ONE;
        }
        if (exp == 1) {
            return base;
        }
        return new Pow(base, Num.of(exp));
    }
}
