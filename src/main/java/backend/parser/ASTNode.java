package backend.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable symbolic expression tree shared by the parser and every math module.
 *
 * <p>Numbers are exact rationals ({@link Num}). Operators are kept as written
 * rather than normalized, so step-by-step output mirrors the user's input.
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

        public Num plus(Num other) {
            return new Num(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                    denominator.multiply(other.denominator));
        }

        public Num minus(Num other) {
            return plus(other.negate());
        }

        public Num times(Num other) {
            return new Num(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
        }

        /** @throws ArithmeticException if {@code other} is zero */
        public Num dividedBy(Num other) {
            return new Num(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
        }

        public Num negate() {
            return new Num(numerator.negate(), denominator);
        }

        /** @throws ArithmeticException if this is zero and {@code exponent} is negative */
        public Num pow(int exponent) {
            BigInteger num = numerator.pow(Math.abs(exponent));
            BigInteger den = denominator.pow(Math.abs(exponent));
            return exponent < 0 ? new Num(den, num) : new Num(num, den);
        }

        public double toDouble() {
            return new BigDecimal(numerator)
                    .divide(new BigDecimal(denominator), MathContext.DECIMAL64)
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

    /** Human-readable rendering with only the parentheses precedence requires. */
    default String toDisplay() {
        return format(this);
    }

    /** Renders an expression tree to a readable string, adding parentheses only where needed. */
    static String format(ASTNode node) {
        return switch (node) {
            case Num n -> n.isInteger()
                    ? n.numerator().toString()
                    : n.numerator() + "/" + n.denominator();
            case Constant c -> switch (c.name()) {
                case "PI" -> "pi";
                case "E" -> "e";
                default -> c.name();
            };
            case Var v -> v.name();
            case Neg(ASTNode a) -> "-" + wrap(a, 3);
            case Add(ASTNode l, ASTNode r) -> joinSigned(wrap(l, 1), " + ", " - ", wrap(r, 1));
            case Sub(ASTNode l, ASTNode r) -> joinSigned(wrap(l, 1), " - ", " + ", wrap(r, 2));
            case Mul m -> formatProduct(m);
            case Div(ASTNode l, ASTNode r) -> wrap(l, 2) + "/" + wrap(r, 3);
            case Pow(ASTNode b, ASTNode e) -> wrap(b, 5) + "^" + wrap(e, 4);
            case Func(String name, ASTNode arg) -> name + "(" + format(arg) + ")";
        };
    }

    /** Folds a leading minus on the right operand into the operator: {@code a + -b} reads {@code a - b}. */
    private static String joinSigned(String left, String operator, String flipped, String right) {
        return right.startsWith("-") ? left + flipped + right.substring(1) : left + operator + right;
    }

    /**
     * Renders a product as a single fraction: rational coefficients and {@code 1/u}
     * factors move to the denominator, so {@code 1/2*sqrt(3)} reads {@code sqrt(3)/2}
     * and {@code x*1/(x+1)} reads {@code x/(x + 1)}.
     */
    private static String formatProduct(Mul product) {
        List<ASTNode> factors = new ArrayList<>();
        collectFactors(product, factors);

        BigInteger coeffNum = BigInteger.ONE;
        BigInteger coeffDen = BigInteger.ONE;
        List<String> numerator = new ArrayList<>();
        List<ASTNode> denominator = new ArrayList<>();
        for (ASTNode factor : factors) {
            if (factor instanceof Num n) {
                coeffNum = coeffNum.multiply(n.numerator());
                coeffDen = coeffDen.multiply(n.denominator());
            } else if (factor instanceof Div(Num one, ASTNode d) && one.isInteger()
                    && one.numerator().equals(BigInteger.ONE)) {
                denominator.add(d);
            } else {
                numerator.add(wrap(factor, 2));
            }
        }
        if (coeffDen.equals(BigInteger.ONE) && denominator.isEmpty()) {
            return simpleProduct(factors);
        }

        String sign = coeffNum.signum() < 0 ? "-" : "";
        BigInteger absNum = coeffNum.abs();
        if (!absNum.equals(BigInteger.ONE) || numerator.isEmpty()) {
            numerator.add(0, absNum.toString());
        }
        if (!coeffDen.equals(BigInteger.ONE)) {
            denominator.add(0, new Num(coeffDen, BigInteger.ONE));
        }
        ASTNode den = denominator.get(0);
        for (int i = 1; i < denominator.size(); i++) {
            den = new Mul(den, denominator.get(i));
        }
        return sign + String.join("*", numerator) + "/" + wrap(den, 3);
    }

    private static String simpleProduct(List<ASTNode> factors) {
        StringBuilder out = new StringBuilder(wrap(factors.get(0), 2));
        for (int i = 1; i < factors.size(); i++) {
            out.append('*').append(wrap(factors.get(i), 2));
        }
        return out.toString();
    }

    private static void collectFactors(ASTNode node, List<ASTNode> out) {
        if (node instanceof Mul(ASTNode l, ASTNode r)) {
            collectFactors(l, out);
            collectFactors(r, out);
        } else {
            out.add(node);
        }
    }

    private static int precedence(ASTNode node) {
        return switch (node) {
            case Add a -> 1;
            case Sub s -> 1;
            case Mul m -> 2;
            case Div d -> 2;
            case Neg n -> 3;
            case Pow p -> 4;
            case Num num when !num.isInteger() -> 2;
            case Num num -> num.numerator().signum() < 0 ? 3 : 5;
            default -> 5;
        };
    }

    private static String wrap(ASTNode child, int minPrecedence) {
        String rendered = format(child);
        return precedence(child) < minPrecedence ? "(" + rendered + ")" : rendered;
    }
}
