package backend.math.algebra;

import backend.engine.MathOperation;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import backend.parser.Parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Solves single-variable equations in {@code x}: linear equations and quadratics
 * (factoring first, then the quadratic formula for exact radical roots).
 */
public class EquationSolver implements MathOperation {

    private static final Num ZERO = Num.of(0);
    private static final String VAR = "x";

    /**
     * Parses {@code lhs = rhs} (or an expression assumed equal to zero) and returns
     * {@code lhs - rhs} simplified.
     */
    public static ASTNode toZeroForm(String input) {
        String trimmed = input.trim();
        ASTNode left;
        ASTNode right;
        if (trimmed.contains("=")) {
            int eq = trimmed.indexOf('=');
            left = Parser.parse(trimmed.substring(0, eq).trim());
            right = Parser.parse(trimmed.substring(eq + 1).trim());
        } else {
            left = Parser.parse(trimmed);
            right = ZERO;
        }
        return Simplifier.simplify(new Add(left, new Neg(right)));
    }

    @Override
    public Result solve(ASTNode zeroForm) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Standard form f(x) = 0", zeroForm));

        Map<Integer, Num> poly = Factoring.coefficients(zeroForm, VAR);
        if (poly == null) {
            return Result.failure("Only single-variable equations in x are supported for now.");
        }

        int degree = poly.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        if (degree == 0) {
            Num constant = poly.getOrDefault(0, ZERO);
            if (constant.isZero()) {
                return Result.failure("Infinitely many solutions (the equation is always true).");
            }
            return Result.failure("No solution (contradiction: a nonzero constant equals zero).");
        }

        if (degree == 1) {
            return solveLinear(poly, steps);
        }
        if (degree == 2) {
            return solveQuadratic(poly, zeroForm, steps);
        }

        return Result.failure("Equations of degree " + degree + " are not supported yet.");
    }

    /** Real roots of {@code zeroForm = 0}, for reuse by {@link Inequalities}. */
    public static List<ASTNode> computeRealRoots(ASTNode zeroForm) {
        Map<Integer, Num> poly = Factoring.coefficients(zeroForm, VAR);
        if (poly == null) {
            return List.of();
        }
        int degree = poly.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (degree == 1) {
            Num a = poly.getOrDefault(1, ZERO);
            Num b = poly.getOrDefault(0, ZERO);
            if (a.isZero()) {
                return List.of();
            }
            return List.of(rootFromLinear(a, b));
        }
        if (degree == 2) {
            return computeQuadraticRootsOnly(poly, zeroForm);
        }
        return List.of();
    }

    private static List<ASTNode> computeQuadraticRootsOnly(Map<Integer, Num> poly, ASTNode zeroForm) {
        Num a = poly.getOrDefault(2, ZERO);
        Num b = poly.getOrDefault(1, ZERO);
        Num c = poly.getOrDefault(0, ZERO);
        if (a.isZero()) {
            Num linA = poly.getOrDefault(1, ZERO);
            Num linB = poly.getOrDefault(0, ZERO);
            if (linA.isZero()) {
                return List.of();
            }
            return List.of(rootFromLinear(linA, linB));
        }

        ASTNode factored = Factoring.factor(zeroForm);
        if (!factored.equals(zeroForm)) {
            List<ASTNode> roots = deduplicate(rootsFromFactored(factored));
            if (!roots.isEmpty()) {
                return roots;
            }
        }

        if (!a.isInteger() || !b.isInteger() || !c.isInteger()) {
            return List.of();
        }

        int ai = a.numerator().intValueExact();
        int bi = b.numerator().intValueExact();
        int ci = c.numerator().intValueExact();
        long disc = (long) bi * bi - (long) 4 * ai * ci;
        if (disc < 0) {
            return List.of();
        }

        ASTNode negB = Num.of(-bi);
        ASTNode twoA = Num.of(2L * ai);
        ASTNode sqrtDisc = simplifySqrt(disc);
        if (disc == 0) {
            return List.of(divideCancel(negB, twoA));
        }
        return List.of(
                divideCancel(new Add(negB, sqrtDisc), twoA),
                divideCancel(new Sub(negB, sqrtDisc), twoA));
    }

    private static Result solveLinear(Map<Integer, Num> poly, List<Step> steps) {
        Num a = poly.getOrDefault(1, ZERO);
        Num b = poly.getOrDefault(0, ZERO);
        if (a.isZero()) {
            return b.isZero()
                    ? Result.failure("Infinitely many solutions.")
                    : Result.failure("No solution.");
        }

        ASTNode root = rootFromLinear(a, b);
        steps.add(new Step("Solve ax + b = 0  →  x = −b/a", root));
        return Result.ofSolutions(List.of(root), steps);
    }

    private static Result solveQuadratic(Map<Integer, Num> poly, ASTNode zeroForm, List<Step> steps) {
        Num a = poly.getOrDefault(2, ZERO);
        Num b = poly.getOrDefault(1, ZERO);
        Num c = poly.getOrDefault(0, ZERO);

        if (a.isZero()) {
            return solveLinear(poly, steps);
        }

        ASTNode factored = Factoring.factor(zeroForm);
        if (!factored.equals(zeroForm)) {
            steps.add(new Step("Factor the quadratic", factored));
            List<ASTNode> factoredRoots = rootsFromFactored(factored);
            if (!factoredRoots.isEmpty()) {
                steps.add(new Step("Set each factor equal to zero and solve"));
                return Result.ofSolutions(deduplicate(factoredRoots), steps);
            }
        }

        steps.add(new Step("Cannot factor over the integers — use the quadratic formula"));
        return quadraticFormula(a, b, c, steps);
    }

    private static Result quadraticFormula(Num a, Num b, Num c, List<Step> steps) {
        if (!a.isInteger() || !b.isInteger() || !c.isInteger()) {
            return Result.failure("Quadratic formula currently requires integer coefficients.");
        }

        int ai = a.numerator().intValueExact();
        int bi = b.numerator().intValueExact();
        int ci = c.numerator().intValueExact();

        long discLong = (long) bi * bi - (long) 4 * ai * ci;
        steps.add(new Step("Discriminant D = b² − 4ac = " + discLong));

        if (discLong < 0) {
            return Result.failure("No real solutions (D < 0).");
        }

        ASTNode negB = Num.of(-bi);
        ASTNode twoA = Num.of(2L * ai);
        ASTNode sqrtDisc = simplifySqrt(discLong);

        if (discLong == 0) {
            ASTNode root = divideCancel(negB, twoA);
            steps.add(new Step("One repeated root: x = −b / (2a)", root));
            return Result.ofSolutions(List.of(root), steps);
        }

        ASTNode root1 = divideCancel(new Add(negB, sqrtDisc), twoA);
        ASTNode root2 = divideCancel(new Sub(negB, sqrtDisc), twoA);
        steps.add(new Step("x = (−b + √D) / (2a)", root1));
        steps.add(new Step("x = (−b − √D) / (2a)", root2));
        return Result.ofSolutions(List.of(root1, root2), steps);
    }

    private static List<ASTNode> rootsFromFactored(ASTNode factored) {
        List<ASTNode> roots = new ArrayList<>();
        for (ASTNode factor : flattenMul(factored)) {
            rootOfFactor(factor).ifPresent(roots::add);
        }
        return roots;
    }

    private static List<ASTNode> flattenMul(ASTNode node) {
        if (node instanceof Mul m) {
            List<ASTNode> out = new ArrayList<>();
            out.addAll(flattenMul(m.left()));
            out.addAll(flattenMul(m.right()));
            return out;
        }
        if (node instanceof Pow p && isTwo(p.exponent())) {
            List<ASTNode> out = new ArrayList<>();
            out.add(p.base());
            out.add(p.base());
            return out;
        }
        return List.of(node);
    }

    private static Optional<ASTNode> rootOfFactor(ASTNode factor) {
        factor = Simplifier.simplify(factor);

        if (factor instanceof Num n) {
            return n.isZero() ? Optional.empty() : Optional.empty();
        }

        Map<Integer, Num> poly = Factoring.coefficients(factor, VAR);
        if (poly == null) {
            return Optional.empty();
        }

        if (!poly.containsKey(1)) {
            if (poly.containsKey(0) && poly.get(0).isZero() && poly.size() == 1) {
                return Optional.empty();
            }
            return Optional.empty();
        }

        Num a = poly.get(1);
        Num b = poly.getOrDefault(0, ZERO);
        if (a.isZero()) {
            return Optional.empty();
        }
        return Optional.of(rootFromLinear(a, b));
    }

    private static ASTNode rootFromLinear(Num a, Num b) {
        BigInteger num = b.numerator().negate().multiply(a.denominator());
        BigInteger den = b.denominator().multiply(a.numerator());
        return Simplifier.simplify(new Num(num, den));
    }

    private static ASTNode divideCancel(ASTNode numerator, ASTNode denominator) {
        numerator = Simplifier.simplify(numerator);
        denominator = Simplifier.simplify(denominator);
        if (numerator instanceof Mul m && m.left() instanceof Num num && denominator instanceof Num den && !den.isZero()) {
            Num scaled = new Num(num.numerator().multiply(den.denominator()),
                    num.denominator().multiply(den.numerator()));
            ASTNode rest = m.right();
            if (scaled.equals(Num.of(1))) {
                return Simplifier.simplify(rest);
            }
            if (scaled.equals(Num.of(-1))) {
                return Simplifier.simplify(new Neg(rest));
            }
            return Simplifier.simplify(new Mul(scaled, rest));
        }
        return Simplifier.simplify(new Div(numerator, denominator));
    }

    private static ASTNode simplifySqrt(long discriminant) {
        if (discriminant == 0) {
            return ZERO;
        }
        int coeff = 1;
        long radicand = discriminant;
        for (int i = 2; i * i <= radicand; i++) {
            while (radicand % (i * i) == 0) {
                coeff *= i;
                radicand /= i * i;
            }
        }
        if (radicand == 1) {
            return Num.of(coeff);
        }
        ASTNode sqrt = new Func("sqrt", Num.of(radicand));
        return coeff == 1 ? sqrt : Simplifier.simplify(new Mul(Num.of(coeff), sqrt));
    }

    private static List<ASTNode> deduplicate(List<ASTNode> roots) {
        Set<String> seen = new LinkedHashSet<>();
        List<ASTNode> unique = new ArrayList<>();
        for (ASTNode root : roots) {
            String key = root.toDisplay();
            if (seen.add(key)) {
                unique.add(root);
            }
        }
        return unique;
    }

    private static boolean isTwo(ASTNode exponent) {
        return exponent instanceof Num n && n.isInteger() && n.numerator().intValueExact() == 2;
    }
}
