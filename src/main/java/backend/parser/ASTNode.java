package backend.parser;

import java.math.BigInteger;

/**
 * Symbolic expression tree node.
 *
 * <p>The whole math backend operates on this tree. It is a {@code sealed}
 * interface so the math modules can use exhaustive pattern-matching
 * {@code switch} over every node kind, e.g.
 *
 * <pre>{@code
 * switch (node) {
 *     case ASTNode.Num n           -> ...
 *     case ASTNode.Add(var l, var r) -> ...
 *     // every case must be handled (compiler-checked)
 * }
 * }</pre>
 *
 * <p>Numbers are stored exactly as rationals (see {@link Num}) to support the
 * "exact + decimal" answers the project requires. Operators are kept explicit
 * (rather than a canonical {@code Add}/{@code Mul} normal form) so the original
 * structure of a user's expression is preserved for clean step-by-step display.
 */
public sealed interface ASTNode
        permits ASTNode.Num, ASTNode.Constant, ASTNode.Var,
                ASTNode.Add, ASTNode.Sub, ASTNode.Mul, ASTNode.Div,
                ASTNode.Pow, ASTNode.Neg, ASTNode.Func {

    /** Exact rational number, always stored in lowest terms with a positive denominator. */
    record Num(BigInteger numerator, BigInteger denominator) implements ASTNode {
        public Num {
            if (denominator.signum() == 0) {
                throw new ArithmeticException("Num denominator cannot be zero");
            }
            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }
            BigInteger gcd = numerator.gcd(denominator);
            if (gcd.compareTo(BigInteger.ONE) > 0) {
                numerator = numerator.divide(gcd);
                denominator = denominator.divide(gcd);
            }
        }

        public static Num of(long value) {
            return new Num(BigInteger.valueOf(value), BigInteger.ONE);
        }

        public static Num of(long numerator, long denominator) {
            return new Num(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
        }

        public boolean isInteger() {
            return denominator.equals(BigInteger.ONE);
        }

        public boolean isZero() {
            return numerator.signum() == 0;
        }

        public double toDouble() {
            return new java.math.BigDecimal(numerator)
                    .divide(new java.math.BigDecimal(denominator), java.math.MathContext.DECIMAL64)
                    .doubleValue();
        }
    }

    /** Named mathematical constant such as {@code PI} or {@code E}. */
    record Constant(String name) implements ASTNode {}

    /** Variable such as {@code x}, {@code y}, or {@code t}. */
    record Var(String name) implements ASTNode {}

    record Add(ASTNode left, ASTNode right) implements ASTNode {}

    record Sub(ASTNode left, ASTNode right) implements ASTNode {}

    record Mul(ASTNode left, ASTNode right) implements ASTNode {}

    record Div(ASTNode left, ASTNode right) implements ASTNode {}

    record Pow(ASTNode base, ASTNode exponent) implements ASTNode {}

    record Neg(ASTNode operand) implements ASTNode {}

    /** Single-argument function application such as {@code sin(x)}, {@code ln(x)}, {@code sqrt(x)}. */
    record Func(String name, ASTNode argument) implements ASTNode {}

    /** Convenience: a human-readable, precedence-aware rendering of this node. */
    default String toDisplay() {
        return format(this);
    }

    /** Renders an expression tree to a readable string, adding parentheses only where needed. */
    static String format(ASTNode node) {
        return switch (node) {
            case Num n -> n.isInteger()
                    ? n.numerator().toString()
                    : n.numerator() + "/" + n.denominator();
            case Constant c -> c.name();
            case Var v -> v.name();
            case Neg(ASTNode a) -> "-" + wrap(a, 3);
            case Add(ASTNode l, ASTNode r) -> wrap(l, 1) + " + " + wrap(r, 1);
            case Sub(ASTNode l, ASTNode r) -> wrap(l, 1) + " - " + wrap(r, 2);
            case Mul(ASTNode l, ASTNode r) -> wrap(l, 2) + "*" + wrap(r, 2);
            case Div(ASTNode l, ASTNode r) -> wrap(l, 2) + "/" + wrap(r, 3);
            case Pow(ASTNode b, ASTNode e) -> wrap(b, 5) + "^" + wrap(e, 4);
            case Func(String name, ASTNode arg) -> name + "(" + format(arg) + ")";
        };
    }

    private static int precedence(ASTNode node) {
        return switch (node) {
            case Add a -> 1;
            case Sub s -> 1;
            case Mul m -> 2;
            case Div d -> 2;
            case Neg n -> 3;
            case Pow p -> 4;
            case Num num -> num.numerator().signum() < 0 ? 3 : 5;
            default -> 5;
        };
    }

    private static String wrap(ASTNode child, int minPrecedence) {
        String rendered = format(child);
        return precedence(child) < minPrecedence ? "(" + rendered + ")" : rendered;
    }
}
