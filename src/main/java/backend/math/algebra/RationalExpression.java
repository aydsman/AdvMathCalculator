package backend.math.algebra;

import backend.parser.ASTNode;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;

import java.util.ArrayList;
import java.util.List;

/** Helpers for single-quotient form {@code numerator / denominator}. */
public final class RationalExpression {

    private static final Num ONE = Num.of(1);

    private RationalExpression() {}

    public record Quotient(ASTNode numerator, ASTNode denominator) {}

    public static Quotient extractQuotient(ASTNode expression) {
        expression = Simplifier.simplify(expression);
        if (expression instanceof Div d) {
            return new Quotient(d.left(), d.right());
        }

        List<ASTNode> numFactors = new ArrayList<>();
        List<ASTNode> denFactors = new ArrayList<>();
        collectQuotientFactors(expression, numFactors, denFactors);
        if (denFactors.isEmpty()) {
            return new Quotient(expression, ONE);
        }
        return new Quotient(rebuildProduct(numFactors), rebuildProduct(denFactors));
    }

    private static void collectQuotientFactors(ASTNode node, List<ASTNode> numFactors, List<ASTNode> denFactors) {
        node = Simplifier.simplify(node);
        if (node instanceof Mul m) {
            collectQuotientFactors(m.left(), numFactors, denFactors);
            collectQuotientFactors(m.right(), numFactors, denFactors);
            return;
        }
        if (node instanceof Div d) {
            numFactors.add(d.left());
            denFactors.add(d.right());
            return;
        }
        if (node instanceof Pow p && p.exponent() instanceof Num n
                && n.isInteger() && n.numerator().intValueExact() == -1) {
            denFactors.add(p.base());
            return;
        }
        numFactors.add(node);
    }

    public static ASTNode quotient(ASTNode numerator, ASTNode denominator) {
        if (denominator instanceof Num n && n.equals(ONE)) {
            return numerator;
        }
        return new Div(numerator, denominator);
    }

    public static ASTNode rebuildProduct(List<ASTNode> factors) {
        if (factors.isEmpty()) {
            return ONE;
        }
        ASTNode product = null;
        for (ASTNode factor : factors) {
            product = product == null ? factor : new Mul(product, factor);
        }
        return Simplifier.simplify(product);
    }
}
