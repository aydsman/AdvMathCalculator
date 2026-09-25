package backend.math.algebra;

import backend.engine.MathOperation;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algebraic simplifier. Folds exact rational arithmetic, applies identity rules
 * (x+0, x*1, x^1, x^0, ...), combines like terms in sums, and combines powers of a
 * common base in products.
 *
 * <p>The static {@link #simplify(ASTNode)} method is the workhorse and is reused by
 * other modules (e.g. to tidy a derivative's output); {@link #solve(ASTNode)} wraps it
 * with a {@link Result} for the engine.
 */
public class Simplifier implements MathOperation {

    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);
    private static final Num NEG_ONE = Num.of(-1);

    @Override
    public Result solve(ASTNode input) {
        ASTNode simplified = simplify(input);

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Original expression", input));
        if (input.equals(simplified)) {
            steps.add(new Step("The expression is already in simplest form."));
        } else {
            steps.add(new Step("Combine like terms, fold constants, and apply identities", simplified));
        }

        Double decimal = simplified instanceof Num n && !n.isInteger() ? n.toDouble() : null;
        return Result.of(simplified, decimal, steps);
    }

    /** Recursively simplifies an expression tree. Safe to call on any node. */
    public static ASTNode simplify(ASTNode node) {
        return switch (node) {
            case Num n -> n;
            case Var v -> v;
            case Constant c -> c;
            case Neg neg -> simplifyNeg(neg.operand());
            case Add add -> simplifySum(new Add(add.left(), add.right()));
            case Sub sub -> simplifySum(new Add(sub.left(), new Neg(sub.right())));
            case Mul mul -> simplifyProduct(mul);
            case Div div -> simplifyDiv(div.left(), div.right());
            case Pow pow -> simplifyPow(pow.base(), pow.exponent());
            case Func f -> simplifyFunc(f.name(), f.argument());
        };
    }

    private static ASTNode simplifyNeg(ASTNode operand) {
        ASTNode a = simplify(operand);
        if (a instanceof Num n) {
            return numNeg(n);
        }
        if (a instanceof Neg inner) {
            return inner.operand();
        }
        if (a instanceof Mul m && m.left() instanceof Num c && isNegative(c)) {
            return simplify(new Mul(numNeg(c), m.right()));
        }
        if (a instanceof Div d) {
            if (d.left() instanceof Neg) {
                return a;
            }
            return simplifyDiv(simplify(new Neg(d.left())), d.right());
        }
        return new Neg(a);
    }

    // ---- Sums (handles Add and, via rewriting, Sub) ----------------------

    private static ASTNode simplifySum(Add node) {
        List<ASTNode> terms = new ArrayList<>();
        collectSumTerms(node, terms);

        Num constant = ZERO;
        Map<ASTNode, Num> combined = new LinkedHashMap<>();

        for (ASTNode term : terms) {
            ASTNode simplified = simplify(term);
            if (simplified instanceof Num n) {
                constant = numAdd(constant, n);
                continue;
            }
            Term split = splitCoefficient(simplified);
            ASTNode base = simplify(split.base());
            if (base instanceof Num bn) {
                constant = numAdd(constant, numMul(split.coeff(), bn));
                continue;
            }
            combined.merge(base, split.coeff(), Simplifier::numAdd);
        }

        List<ASTNode> resultTerms = new ArrayList<>();
        for (Map.Entry<ASTNode, Num> entry : combined.entrySet()) {
            Num coeff = entry.getValue();
            if (coeff.isZero()) {
                continue;
            }
            ASTNode base = entry.getKey();
            if (coeff.equals(ONE)) {
                resultTerms.add(base);
            } else if (coeff.equals(NEG_ONE)) {
                resultTerms.add(new Neg(base));
            } else {
                resultTerms.add(new Mul(coeff, base));
            }
        }
        if (!constant.isZero()) {
            resultTerms.add(constant);
        }
        return assembleSum(resultTerms);
    }

    private static void collectSumTerms(ASTNode node, List<ASTNode> out) {
        switch (node) {
            case Add a -> {
                collectSumTerms(a.left(), out);
                collectSumTerms(a.right(), out);
            }
            case Sub s -> {
                collectSumTerms(s.left(), out);
                collectSumTerms(new Neg(s.right()), out);
            }
            case Neg n when n.operand() instanceof Add a -> {
                collectSumTerms(new Neg(a.left()), out);
                collectSumTerms(new Neg(a.right()), out);
            }
            case Neg n when n.operand() instanceof Sub s -> {
                collectSumTerms(new Neg(s.left()), out);
                collectSumTerms(s.right(), out);
            }
            default -> out.add(node);
        }
    }

    /** Builds a readable sum, using subtraction for negative terms (so "x + -3" shows as "x - 3"). */
    private static ASTNode assembleSum(List<ASTNode> terms) {
        ASTNode sum = null;
        for (ASTNode term : terms) {
            if (sum == null) {
                sum = term;
                continue;
            }
            if (term instanceof Neg neg) {
                sum = new Sub(sum, neg.operand());
            } else if (term instanceof Mul m && m.left() instanceof Num c && isNegative(c)) {
                sum = new Sub(sum, new Mul(numNeg(c), m.right()));
            } else if (term instanceof Num n && isNegative(n)) {
                sum = new Sub(sum, numNeg(n));
            } else {
                sum = new Add(sum, term);
            }
        }
        return sum == null ? ZERO : sum;
    }

    private record Term(Num coeff, ASTNode base) {}

    private static Term splitCoefficient(ASTNode node) {
        if (node instanceof Neg neg) {
            Term inner = splitCoefficient(neg.operand());
            return new Term(numNeg(inner.coeff()), inner.base());
        }
        if (node instanceof Mul m) {
            if (m.left() instanceof Num c) {
                return new Term(c, m.right());
            }
            if (m.right() instanceof Num c) {
                return new Term(c, m.left());
            }
        }
        return new Term(ONE, node);
    }

    // ---- Products --------------------------------------------------------

    private static ASTNode simplifyProduct(Mul node) {
        List<ASTNode> factors = new ArrayList<>();
        collectProductFactors(node, factors);
        factors = cancelReciprocalPairs(factors);

        Num coefficient = ONE;
        Map<ASTNode, ASTNode> powers = new LinkedHashMap<>();

        for (ASTNode factor : factors) {
            ASTNode simplified = simplify(factor);
            if (simplified instanceof Num n) {
                if (n.isZero()) {
                    return ZERO;
                }
                coefficient = numMul(coefficient, n);
                continue;
            }
            ASTNode base;
            ASTNode exponent;
            if (simplified instanceof Pow p) {
                base = simplify(p.base());
                exponent = p.exponent();
            } else {
                base = simplified;
                exponent = ONE;
            }
            powers.merge(base, exponent, (a, b) -> simplify(new Add(a, b)));
        }

        List<ASTNode> factorNodes = new ArrayList<>();
        for (Map.Entry<ASTNode, ASTNode> entry : powers.entrySet()) {
            ASTNode factor = simplifyPow(entry.getKey(), simplify(entry.getValue()));
            if (factor.equals(ONE)) {
                continue;
            }
            factorNodes.add(factor);
        }

        ASTNode product = null;
        for (ASTNode factor : factorNodes) {
            product = product == null ? factor : new Mul(product, factor);
        }

        if (product == null) {
            return coefficient;
        }
        if (coefficient.equals(ONE)) {
            return product;
        }
        if (coefficient.equals(NEG_ONE)) {
            return new Neg(product);
        }
        return new Mul(coefficient, product);
    }

    private static void collectProductFactors(ASTNode node, List<ASTNode> out) {
        if (node instanceof Mul m) {
            collectProductFactors(m.left(), out);
            collectProductFactors(m.right(), out);
        } else if (node instanceof Neg n) {
            out.add(NEG_ONE);
            collectProductFactors(n.operand(), out);
        } else {
            out.add(node);
        }
    }

    /** Cancels pairs such as {@code x * (1/x)} in a flattened product. */
    private static List<ASTNode> cancelReciprocalPairs(List<ASTNode> factors) {
        List<ASTNode> remaining = new ArrayList<>();
        for (ASTNode factor : factors) {
            ASTNode simplified = simplify(factor);
            if (tryCancelReciprocal(remaining, simplified)) {
                continue;
            }
            remaining.add(simplified);
        }
        return remaining;
    }

    private static boolean tryCancelReciprocal(List<ASTNode> factors, ASTNode candidate) {
        if (candidate instanceof Div d && simplify(d.left()).equals(ONE)) {
            ASTNode denom = simplify(d.right());
            return factors.removeIf(existing -> simplify(existing).equals(denom));
        }
        for (int i = 0; i < factors.size(); i++) {
            ASTNode existing = factors.get(i);
            if (existing instanceof Div d && simplify(d.left()).equals(ONE)
                    && simplify(d.right()).equals(candidate)) {
                factors.remove(i);
                return true;
            }
        }
        return false;
    }

    // ---- Division, powers, functions ------------------------------------

    private static ASTNode simplifyDiv(ASTNode left, ASTNode right) {
        ASTNode a = simplify(left);
        ASTNode b = simplify(right);
        if (b.equals(ONE)) {
            return simplify(a);
        }
        if (a instanceof Num an && an.isZero()) {
            return ZERO;
        }
        if (a instanceof Num an && b instanceof Num bn && !bn.isZero()) {
            return numDiv(an, bn);
        }
        if (a.equals(b)) {
            return ONE;
        }

        ScaledTerm numerator = extractScaledTerm(a);
        ScaledTerm denominator = extractScaledTerm(b);
        if (!numerator.scale().equals(ONE) || !denominator.scale().equals(ONE)) {
            Num scale = numDiv(numerator.scale(), denominator.scale());
            ASTNode inner = simplifyDiv(numerator.rest(), denominator.rest());
            if (scale.equals(ONE)) {
                return inner;
            }
            if (scale.equals(NEG_ONE)) {
                if (inner instanceof Div d) {
                    return new Div(simplify(new Neg(d.left())), d.right());
                }
                return simplify(new Neg(inner));
            }
            return simplify(new Mul(scale, inner));
        }

        return new Div(a, b);
    }

    private record ScaledTerm(Num scale, ASTNode rest) {}

    private static ScaledTerm extractScaledTerm(ASTNode node) {
        node = simplify(node);
        if (node instanceof Num n) {
            return new ScaledTerm(n, ONE);
        }
        if (node instanceof Neg n) {
            ScaledTerm inner = extractScaledTerm(n.operand());
            return new ScaledTerm(numNeg(inner.scale()), inner.rest());
        }
        if (node instanceof Mul m) {
            if (m.left() instanceof Num c) {
                ScaledTerm inner = extractScaledTerm(m.right());
                return new ScaledTerm(numMul(c, inner.scale()), inner.rest());
            }
            if (m.right() instanceof Num c) {
                ScaledTerm inner = extractScaledTerm(m.left());
                return new ScaledTerm(numMul(c, inner.scale()), inner.rest());
            }
        }
        return new ScaledTerm(ONE, node);
    }

    private static ASTNode simplifyPow(ASTNode base, ASTNode exponent) {
        ASTNode b = simplify(base);
        ASTNode e = simplify(exponent);
        if (e instanceof Num en) {
            if (en.isZero()) {
                return ONE;
            }
            if (en.equals(ONE)) {
                return b;
            }
        }
        if (b.equals(ONE)) {
            return ONE;
        }
        if (b instanceof Num bn && bn.isZero() && e instanceof Num en && isPositive(en)) {
            return ZERO;
        }
        if (b instanceof Num bn && e instanceof Num en && en.isInteger()) {
            return numPow(bn, en);
        }
        if (b instanceof Func f && "sqrt".equals(f.name()) && e instanceof Num en
                && en.isInteger() && en.numerator().intValueExact() == 2) {
            return f.argument();
        }
        if (b instanceof Pow inner) {
            return simplifyPow(inner.base(), simplify(new Mul(inner.exponent(), e)));
        }
        return new Pow(b, e);
    }

    private static ASTNode simplifyFunc(String name, ASTNode argument) {
        ASTNode a = simplify(argument);
        switch (name) {
            case "sqrt" -> {
                if (a instanceof Num n && n.isInteger() && !isNegative(n)) {
                    BigInteger root = n.numerator().sqrt();
                    if (root.multiply(root).equals(n.numerator())) {
                        return new Num(root, BigInteger.ONE);
                    }
                }
            }
            case "abs" -> {
                if (a instanceof Num n) {
                    return isNegative(n) ? numNeg(n) : n;
                }
                if (a instanceof Neg n) {
                    return simplifyFunc("abs", n.operand());
                }
            }
            case "sin", "tan" -> {
                if (a instanceof Num n && n.isZero()) {
                    return ZERO;
                }
            }
            case "cos" -> {
                if (a instanceof Num n && n.isZero()) {
                    return ONE;
                }
            }
            case "ln" -> {
                if (a.equals(ONE)) {
                    return ZERO;
                }
                if (a instanceof Constant c && c.name().equals("E")) {
                    return ONE;
                }
            }
            default -> { }
        }
        return new Func(name, a);
    }

    // ---- Exact rational helpers -----------------------------------------

    private static Num numAdd(Num a, Num b) {
        BigInteger numerator = a.numerator().multiply(b.denominator())
                .add(b.numerator().multiply(a.denominator()));
        BigInteger denominator = a.denominator().multiply(b.denominator());
        return new Num(numerator, denominator);
    }

    private static Num numMul(Num a, Num b) {
        return new Num(a.numerator().multiply(b.numerator()),
                a.denominator().multiply(b.denominator()));
    }

    private static Num numDiv(Num a, Num b) {
        return new Num(a.numerator().multiply(b.denominator()),
                a.denominator().multiply(b.numerator()));
    }

    private static Num numNeg(Num a) {
        return new Num(a.numerator().negate(), a.denominator());
    }

    private static ASTNode numPow(Num base, Num integerExponent) {
        BigInteger exp = integerExponent.numerator();
        if (exp.signum() == 0) {
            return ONE;
        }
        if (exp.abs().bitLength() > 16) {
            return new Pow(base, integerExponent);
        }
        int e = exp.abs().intValueExact();
        BigInteger numerator = base.numerator().pow(e);
        BigInteger denominator = base.denominator().pow(e);
        if (exp.signum() < 0) {
            return new Num(denominator, numerator);
        }
        return new Num(numerator, denominator);
    }

    private static boolean isNegative(Num n) {
        return n.numerator().signum() < 0;
    }

    private static boolean isPositive(Num n) {
        return n.numerator().signum() > 0;
    }
}
