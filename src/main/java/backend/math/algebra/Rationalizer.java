package backend.math.algebra;

import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;

import java.util.Optional;

/**
 * Clears radicals from {@code 0/0} limits by multiplying by a conjugate,
 * e.g. {@code (sqrt(x+1)-2)/(x^2-9)}.
 */
public final class Rationalizer {

    private Rationalizer() {}

    public static Optional<ASTNode> rationalize(ASTNode expression) {
        RationalExpression.Quotient quotient = RationalExpression.extractQuotient(expression);
        return findDifference(quotient.numerator())
                .map(diff -> buildRationalized(quotient.denominator(), diff));
    }

    private static ASTNode buildRationalized(ASTNode denominator, Difference diff) {
        ASTNode conjugate = Simplifier.simplify(new Add(diff.a(), diff.b()));
        ASTNode newNumerator = differenceOfSquares(diff.a(), diff.b());
        ASTNode newDenominator = Simplifier.simplify(new Mul(denominator, conjugate));
        return RationalExpression.quotient(newNumerator, newDenominator);
    }

    private static ASTNode differenceOfSquares(ASTNode a, ASTNode b) {
        return Simplifier.simplify(new Sub(square(a), square(b)));
    }

    private static ASTNode square(ASTNode node) {
        node = Simplifier.simplify(node);
        if (node instanceof Func f && "sqrt".equals(f.name())) {
            return f.argument();
        }
        if (node instanceof Pow p && p.exponent() instanceof Num n && n.equals(Num.of(1))) {
            return p.base();
        }
        return Simplifier.simplify(new Pow(node, Num.of(2)));
    }

    private static Optional<Difference> findDifference(ASTNode numerator) {
        numerator = Simplifier.simplify(numerator);
        if (numerator instanceof Sub s) {
            return differenceIfRadical(s.left(), s.right());
        }
        if (numerator instanceof Add a && a.right() instanceof Neg n) {
            return differenceIfRadical(a.left(), n.operand());
        }
        if (numerator instanceof Neg n && n.operand() instanceof Sub s) {
            return differenceIfRadical(s.right(), s.left());
        }
        return Optional.empty();
    }

    private static Optional<Difference> differenceIfRadical(ASTNode a, ASTNode b) {
        a = Simplifier.simplify(a);
        b = Simplifier.simplify(b);
        if (!containsSqrt(a) && !containsSqrt(b)) {
            return Optional.empty();
        }
        return Optional.of(new Difference(a, b));
    }

    private static boolean containsSqrt(ASTNode node) {
        return switch (node) {
            case Func f when "sqrt".equals(f.name()) -> true;
            case Neg n -> containsSqrt(n.operand());
            case Add a -> containsSqrt(a.left()) || containsSqrt(a.right());
            case Sub s -> containsSqrt(s.left()) || containsSqrt(s.right());
            case Mul m -> containsSqrt(m.left()) || containsSqrt(m.right());
            case Div d -> containsSqrt(d.left()) || containsSqrt(d.right());
            case Pow p -> containsSqrt(p.base()) || containsSqrt(p.exponent());
            default -> false;
        };
    }

    private record Difference(ASTNode a, ASTNode b) {}
}
