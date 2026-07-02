package backend.math.calculus.integral;

import backend.parser.ASTNode;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

/**
 * Closed forms for {@code Σ_{k=0}^{n-1} k^m} as expressions in {@code n}.
 */
final class SummationIdentities {

    private static final Var N = new Var("n");

    private SummationIdentities() {}

    static ASTNode sumKToPow(int power) {
        return switch (power) {
            case 0 -> N;
            case 1 -> div(mul(N, sub(N, one())), two());
            case 2 -> div(mul(mul(sub(N, one()), N), sub(mul(two(), N), one())), six());
            case 3 -> div(pow(mul(N, sub(N, one())), 2), four());
            case 4 -> div(
                    mul(mul(mul(sub(N, one()), N), sub(mul(two(), N), one())),
                            sub(sub(mul(three(), pow(N, 2)), mul(three(), N)), one())),
                    Num.of(30));
            default -> throw new IllegalArgumentException(
                    "Summation identity for k^" + power + " is not supported yet");
        };
    }

    static String sumKToPowDisplay(int power) {
        return switch (power) {
            case 0 -> "n";
            case 1 -> "n(n-1)/2";
            case 2 -> "n(n-1)(2n-1)/6";
            case 3 -> "n^2(n-1)^2/4";
            case 4 -> "n(n-1)(2n-1)(3n^2-3n-1)/30";
            default -> "Σ k^" + power;
        };
    }

    static String sumNotation(int power) {
        String superscript = power == 1 ? "" : "^" + power;
        return "\u03A3_{k=0}^{n-1} k" + superscript;
    }

    private static Num one() {
        return Num.of(1);
    }

    private static Num two() {
        return Num.of(2);
    }

    private static Num three() {
        return Num.of(3);
    }

    private static Num four() {
        return Num.of(4);
    }

    private static Num six() {
        return Num.of(6);
    }

    private static ASTNode sub(ASTNode left, ASTNode right) {
        return new Sub(left, right);
    }

    private static ASTNode mul(ASTNode left, ASTNode right) {
        return new Mul(left, right);
    }

    private static ASTNode div(ASTNode left, ASTNode right) {
        return new Div(left, right);
    }

    private static ASTNode pow(ASTNode base, int exp) {
        return exp == 1 ? base : new Pow(base, Num.of(exp));
    }
}
