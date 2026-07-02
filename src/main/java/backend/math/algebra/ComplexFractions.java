package backend.math.algebra;

import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Sub;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites sums of fractions and nested quotients into a single fraction,
 * e.g. {@code 1/(3+x) - 1/(3-x)} over {@code x} becomes one quotient.
 */
public final class ComplexFractions {

    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);

    private ComplexFractions() {}

    public static ASTNode simplify(ASTNode node) {
        node = Simplifier.simplify(node);
        RationalExpression.Quotient quotient = RationalExpression.extractQuotient(node);
        if (isFractionSum(quotient.numerator())) {
            ASTNode combined = combineFractionSum(quotient.numerator());
            node = RationalExpression.quotient(combined, quotient.denominator());
        }
        return Simplifier.simplify(simplifyNode(node));
    }

    private static ASTNode simplifyNode(ASTNode node) {
        if (node instanceof Div d) {
            ASTNode numerator = simplifyNode(d.left());
            ASTNode denominator = simplifyNode(d.right());

            if (isFractionSum(numerator)) {
                numerator = combineFractionSum(numerator);
            }

            if (numerator instanceof Div inner) {
                return Simplifier.simplify(new Div(
                        inner.left(),
                        Simplifier.simplify(new Mul(inner.right(), denominator))));
            }

            if (denominator instanceof Div inner) {
                return Simplifier.simplify(new Div(
                        Simplifier.simplify(new Mul(numerator, inner.right())),
                        inner.left()));
            }

            return Simplifier.simplify(new Div(numerator, denominator));
        }

        if (isFractionSum(node)) {
            return combineFractionSum(node);
        }

        return node;
    }

    private static boolean isFractionSum(ASTNode node) {
        return parseFractionTerms(node).stream().anyMatch(term -> !term.denominator().equals(ONE));
    }

    private static ASTNode combineFractionSum(ASTNode sum) {
        List<FractionTerm> terms = parseFractionTerms(sum);
        ASTNode denominator = ONE;
        for (FractionTerm term : terms) {
            denominator = Simplifier.simplify(new Mul(denominator, term.denominator()));
        }

        ASTNode numerator = null;
        for (FractionTerm term : terms) {
            ASTNode scale = ONE;
            for (FractionTerm other : terms) {
                if (other != term) {
                    scale = Simplifier.simplify(new Mul(scale, other.denominator()));
                }
            }
            ASTNode part = Simplifier.simplify(new Mul(term.numerator(), scale));
            if (term.negative()) {
                numerator = numerator == null ? new Neg(part) : new Sub(numerator, part);
            } else {
                numerator = numerator == null ? part : new Add(numerator, part);
            }
        }

        if (numerator == null) {
            numerator = ZERO;
        }
        return Simplifier.simplify(new Div(numerator, denominator));
    }

    private static List<FractionTerm> parseFractionTerms(ASTNode node) {
        List<FractionTerm> terms = new ArrayList<>();
        for (SignedTerm signed : flattenSum(node)) {
            RationalExpression.Quotient quotient = RationalExpression.extractQuotient(signed.term());
            terms.add(new FractionTerm(
                    quotient.numerator(),
                    quotient.denominator(),
                    signed.negative()));
        }
        return terms;
    }

    private static List<SignedTerm> flattenSum(ASTNode node) {
        List<SignedTerm> out = new ArrayList<>();
        flattenSum(node, false, out);
        return out;
    }

    private static void flattenSum(ASTNode node, boolean negative, List<SignedTerm> out) {
        node = Simplifier.simplify(node);
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
            default -> out.add(new SignedTerm(node, negative));
        }
    }

    private record SignedTerm(ASTNode term, boolean negative) {}

    private record FractionTerm(ASTNode numerator, ASTNode denominator, boolean negative) {}
}
