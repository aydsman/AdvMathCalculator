package backend.math.algebra;

import backend.engine.MathOperation;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factors polynomials using GCF extraction, difference of squares, and quadratic
 * trinomial factoring (monic and {@code ax^2 + bx + c}).
 *
 * <p>Input is expected to already be simplified by {@code MathEngine} before
 * {@link #solve(ASTNode)} is called.
 */
public class Factoring implements MathOperation {

    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);
    private static final String DEFAULT_VAR = "x";

    @Override
    public Result solve(ASTNode input) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Original expression", input));

        ASTNode factored = factorWithSteps(input, steps);
        factored = Simplifier.simplify(factored);

        if (factored.equals(input)) {
            steps.add(new Step("This expression cannot be factored further over the integers."));
        }

        return Result.of(factored, null, steps);
    }

    public static ASTNode factor(ASTNode node) {
        return factorWithSteps(node, null);
    }

    /** Coefficients of a univariate polynomial (degree → coeff), or {@code null} if not univariate. */
    public static Map<Integer, Num> coefficients(ASTNode node, String varName) {
        return polynomialIn(node, varName);
    }

    private static ASTNode factorWithSteps(ASTNode node, List<Step> steps) {
        ASTNode current = node;

        ASTNode afterGcf = pullGcf(current);
        if (!afterGcf.equals(current)) {
            if (steps != null) {
                steps.add(new Step("Factor out the greatest common factor (GCF)", afterGcf));
            }
            current = afterGcf;
        }

        if (current instanceof Mul m) {
            ASTNode left = factorWithSteps(m.left(), null);
            ASTNode right = factorWithSteps(m.right(), null);
            ASTNode product = Simplifier.simplify(new Mul(left, right));
            if (!product.equals(current)) {
                if (steps != null) {
                    steps.add(new Step("Factor the remaining expression", product));
                }
                return product;
            }
        }

        Optional<ASTNode> diffSq = tryDifferenceOfSquares(current);
        if (diffSq.isPresent()) {
            if (steps != null) {
                steps.add(new Step("Recognize a difference of squares: a\u00B2 \u2212 b\u00B2 = (a + b)(a \u2212 b)",
                        diffSq.get()));
            }
            return diffSq.get();
        }

        Optional<ASTNode> quadratic = tryQuadratic(current);
        if (quadratic.isPresent()) {
            if (steps != null) {
                steps.add(new Step("Factor the quadratic trinomial", quadratic.get()));
            }
            return quadratic.get();
        }

        return current;
    }

    // ---- GCF ----------------------------------------------------------------

    private static ASTNode pullGcf(ASTNode node) {
        List<SignedTerm> terms = collectTerms(node);
        if (terms.size() < 2) {
            return node;
        }

        BigInteger coeffGcf = terms.get(0).coeff().numerator().abs();
        for (SignedTerm term : terms) {
            coeffGcf = coeffGcf.gcd(term.coeff().numerator().abs());
        }

        Map<String, Integer> varGcf = commonVarExponents(terms);
        ASTNode gcf = buildMonomial(new Num(coeffGcf, BigInteger.ONE), varGcf);
        if (gcf.equals(ONE)) {
            return node;
        }

        List<ASTNode> quotients = new ArrayList<>();
        for (SignedTerm term : terms) {
            ASTNode quotient = divideTermByGcf(term, coeffGcf, varGcf);
            if (term.negative()) {
                quotient = new Neg(quotient);
            }
            quotients.add(quotient);
        }

        ASTNode inner = assembleSum(quotients);
        return Simplifier.simplify(new Mul(gcf, inner));
    }

    private static Map<String, Integer> commonVarExponents(List<SignedTerm> terms) {
        Map<String, Integer> common = null;
        for (SignedTerm term : terms) {
            if (common == null) {
                common = new HashMap<>(term.vars());
                continue;
            }
            common.keySet().retainAll(term.vars().keySet());
            for (String v : new ArrayList<>(common.keySet())) {
                common.put(v, Math.min(common.get(v), term.vars().get(v)));
            }
        }
        if (common == null || common.isEmpty()) {
            return Map.of();
        }
        common.entrySet().removeIf(e -> e.getValue() == 0);
        return common;
    }

    private static ASTNode divideTermByGcf(SignedTerm term, BigInteger coeffGcf,
                                           Map<String, Integer> varGcf) {
        BigInteger newNum = term.coeff().numerator().divide(coeffGcf);
        Num newCoeff = new Num(newNum, term.coeff().denominator());

        Map<String, Integer> remaining = new HashMap<>(term.vars());
        for (Map.Entry<String, Integer> e : varGcf.entrySet()) {
            remaining.put(e.getKey(), remaining.get(e.getKey()) - e.getValue());
            if (remaining.get(e.getKey()) == 0) {
                remaining.remove(e.getKey());
            }
        }
        return buildMonomial(newCoeff, remaining);
    }

    // ---- Difference of squares --------------------------------------------

    private static Optional<ASTNode> tryDifferenceOfSquares(ASTNode node) {
        ASTNode left;
        ASTNode right;
        if (node instanceof Sub s) {
            left = s.left();
            right = s.right();
        } else if (node instanceof Add a && a.right() instanceof Neg n) {
            left = a.left();
            right = n.operand();
        } else {
            return Optional.empty();
        }

        Optional<ASTNode> rootA = perfectSquareRoot(left);
        Optional<ASTNode> rootB = perfectSquareRoot(right);
        if (rootA.isEmpty() || rootB.isEmpty()) {
            return Optional.empty();
        }

        ASTNode a = rootA.get();
        ASTNode b = rootB.get();
        return Optional.of(Simplifier.simplify(new Mul(new Add(a, b), new Sub(a, b))));
    }

    private static Optional<ASTNode> perfectSquareRoot(ASTNode node) {
        if (node instanceof Pow p && isTwo(p.exponent())) {
            return Optional.of(Simplifier.simplify(p.base()));
        }
        if (node instanceof Num n && n.isInteger() && !n.isZero()) {
            if (n.numerator().signum() < 0) {
                return Optional.empty();
            }
            BigInteger root = intSqrt(n.numerator());
            if (root != null) {
                return Optional.of(new Num(root, BigInteger.ONE));
            }
            return Optional.empty();
        }
        if (node instanceof Mul m) {
            Optional<ASTNode> leftRoot = perfectSquareRoot(m.left());
            Optional<ASTNode> rightRoot = perfectSquareRoot(m.right());
            if (leftRoot.isPresent() && rightRoot.isPresent()) {
                return Optional.of(Simplifier.simplify(new Mul(leftRoot.get(), rightRoot.get())));
            }
        }
        return Optional.empty();
    }

    // ---- Quadratic trinomial ------------------------------------------------

    private static Optional<ASTNode> tryQuadratic(ASTNode node) {
        Map<Integer, Num> poly = polynomialIn(node, DEFAULT_VAR);
        if (poly == null || !poly.containsKey(2) || poly.size() > 3) {
            return Optional.empty();
        }

        Num a = poly.getOrDefault(2, ZERO);
        Num b = poly.getOrDefault(1, ZERO);
        Num c = poly.getOrDefault(0, ZERO);
        if (a.isZero() || !a.isInteger() || !b.isInteger() || !c.isInteger()) {
            return Optional.empty();
        }

        int av = a.numerator().intValueExact();
        int bv = b.numerator().intValueExact();
        int cv = c.numerator().intValueExact();

        Var var = new Var(DEFAULT_VAR);
        List<Integer> aDivisors = positiveDivisors(av);
        List<Integer> cDivisors = positiveDivisors(cv);

        for (int p : withSigns(aDivisors, av)) {
            for (int r : withSigns(aDivisors, av)) {
                if (p * r != av) {
                    continue;
                }
                for (int q : withSigns(cDivisors, cv)) {
                    for (int s : withSigns(cDivisors, cv)) {
                        if (q * s != cv) {
                            continue;
                        }
                        if (p * s + q * r == bv) {
                            ASTNode bin1 = buildLinearBinomial(p, q, var);
                            ASTNode bin2 = buildLinearBinomial(r, s, var);
                            return Optional.of(Simplifier.simplify(new Mul(bin1, bin2)));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static ASTNode buildLinearBinomial(int coeff, int constant, Var var) {
        ASTNode bx = switch (coeff) {
            case 0 -> null;
            case 1 -> var;
            case -1 -> new Neg(var);
            default -> new Mul(Num.of(coeff), var);
        };
        if (bx == null) {
            return Num.of(constant);
        }
        if (constant == 0) {
            return bx;
        }
        if (constant > 0) {
            return new Add(bx, Num.of(constant));
        }
        return new Sub(bx, Num.of(-constant));
    }

    // ---- Term / polynomial helpers ------------------------------------------

    private record SignedTerm(boolean negative, Num coeff, Map<String, Integer> vars) {}

    private static List<SignedTerm> collectTerms(ASTNode node) {
        List<SignedTerm> raw = new ArrayList<>();
        flattenSum(node, false, raw);
        return raw;
    }

    private static void flattenSum(ASTNode node, boolean negative, List<SignedTerm> out) {
        switch (node) {
            case Add a -> {
                flattenSum(a.left(), negative, out);
                flattenSum(a.right(), negative, out);
            }
            case Sub s -> {
                flattenSum(s.left(), negative, out);
                flattenSum(s.right(), !negative, out);
            }
            case Neg n -> flattenSum(n.operand(), !negative, out);
            default -> out.add(toSignedTerm(node, negative));
        }
    }

    private static SignedTerm toSignedTerm(ASTNode node, boolean negative) {
        if (node instanceof Num n) {
            return new SignedTerm(negative, n, Map.of());
        }

        Num coeff = ONE;
        Map<String, Integer> vars = new HashMap<>();
        ASTNode rest = node;

        if (rest instanceof Neg n) {
            negative = !negative;
            rest = n.operand();
        }
        if (rest instanceof Num n) {
            return new SignedTerm(negative, n, Map.of());
        }
        if (rest instanceof Mul m) {
            if (m.left() instanceof Num n) {
                coeff = n;
                rest = m.right();
            } else if (m.right() instanceof Num n) {
                coeff = n;
                rest = m.left();
            }
        }
        collectVarPowers(rest, vars);
        return new SignedTerm(negative, coeff, vars);
    }

    private static void collectVarPowers(ASTNode node, Map<String, Integer> vars) {
        switch (node) {
            case Var v -> vars.merge(v.name(), 1, Integer::sum);
            case Pow p when p.base() instanceof Var v && isTwo(p.exponent()) ->
                    vars.merge(v.name(), 2, Integer::sum);
            case Pow p when p.base() instanceof Var v && p.exponent() instanceof Num n && n.isInteger() ->
                    vars.merge(v.name(), n.numerator().intValueExact(), Integer::sum);
            case Mul m -> {
                collectVarPowers(m.left(), vars);
                collectVarPowers(m.right(), vars);
            }
            default -> { }
        }
    }

    private static Map<Integer, Num> polynomialIn(ASTNode node, String varName) {
        List<SignedTerm> terms = collectTerms(node);
        Map<Integer, Num> poly = new LinkedHashMap<>();
        for (SignedTerm term : terms) {
            if (term.vars().size() > 1 || (term.vars().size() == 1 && !term.vars().containsKey(varName))) {
                return null;
            }
            int degree = term.vars().getOrDefault(varName, 0);
            Num coeff = term.coeff();
            if (term.negative()) {
                coeff = new Num(coeff.numerator().negate(), coeff.denominator());
            }
            poly.merge(degree, coeff, Factoring::numAdd);
        }
        return poly;
    }

    private static ASTNode buildMonomial(Num coeff, Map<String, Integer> vars) {
        if (vars.isEmpty()) {
            return coeff;
        }
        ASTNode product = null;
        for (Map.Entry<String, Integer> e : vars.entrySet()) {
            ASTNode factor = e.getValue() == 1
                    ? new Var(e.getKey())
                    : new Pow(new Var(e.getKey()), Num.of(e.getValue()));
            product = product == null ? factor : new Mul(product, factor);
        }
        if (coeff.equals(ONE)) {
            return product;
        }
        return new Mul(coeff, product);
    }

    private static ASTNode assembleSum(List<ASTNode> terms) {
        ASTNode sum = null;
        for (ASTNode term : terms) {
            if (sum == null) {
                sum = term;
                continue;
            }
            if (term instanceof Neg n) {
                sum = new Sub(sum, n.operand());
            } else {
                sum = new Add(sum, term);
            }
        }
        return sum == null ? ZERO : sum;
    }

    private static Num numAdd(Num a, Num b) {
        BigInteger num = a.numerator().multiply(b.denominator())
                .add(b.numerator().multiply(a.denominator()));
        BigInteger den = a.denominator().multiply(b.denominator());
        return new Num(num, den);
    }

    private static List<Integer> positiveDivisors(int n) {
        int abs = Math.abs(n);
        List<Integer> divisors = new ArrayList<>();
        for (int i = 1; i <= Math.sqrt(abs); i++) {
            if (abs % i == 0) {
                divisors.add(i);
                if (i != abs / i) {
                    divisors.add(abs / i);
                }
            }
        }
        return divisors;
    }

    private static List<Integer> withSigns(List<Integer> divisors, int target) {
        List<Integer> signed = new ArrayList<>();
        for (int d : divisors) {
            signed.add(d);
            signed.add(-d);
        }
        if (target < 0) {
            // ensure we consider factor pairs whose product is negative
            return signed;
        }
        return signed;
    }

    private static boolean isTwo(ASTNode exponent) {
        return exponent instanceof Num n && n.isInteger() && n.numerator().intValueExact() == 2;
    }

    private static BigInteger intSqrt(BigInteger n) {
        if (n.signum() < 0) {
            return null;
        }
        BigInteger root = n.sqrt();
        return root.multiply(root).equals(n) ? root : null;
    }
}
