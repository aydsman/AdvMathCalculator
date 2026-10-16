package backend.math;

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
import backend.parser.ASTNode;

import java.util.ArrayList;
import java.util.List;

/** Structural queries on expression trees shared across the math modules. */
public final class Expressions {

    private Expressions() {}

    /**
     * Splits a sum into its terms. A subtracted operand becomes a single
     * {@link Neg} term, so {@code a - (b + c)} yields {@code [a, -(b + c)]}.
     */
    public static List<ASTNode> terms(ASTNode node) {
        List<ASTNode> out = new ArrayList<>();
        collectTerms(node, out);
        return out;
    }

    private static void collectTerms(ASTNode node, List<ASTNode> out) {
        switch (node) {
            case Add(ASTNode l, ASTNode r) -> {
                collectTerms(l, out);
                collectTerms(r, out);
            }
            case Sub(ASTNode l, ASTNode r) -> {
                collectTerms(l, out);
                out.add(new Neg(r));
            }
            default -> out.add(node);
        }
    }

    /** Splits a product into its factors, e.g. {@code 2*x*sin(x)} yields {@code [2, x, sin(x)]}. */
    public static List<ASTNode> factors(ASTNode node) {
        List<ASTNode> out = new ArrayList<>();
        collectFactors(node, out);
        return out;
    }

    private static void collectFactors(ASTNode node, List<ASTNode> out) {
        if (node instanceof Mul(ASTNode l, ASTNode r)) {
            collectFactors(l, out);
            collectFactors(r, out);
        } else {
            out.add(node);
        }
    }

    public static boolean containsVariable(ASTNode node, String variable) {
        return switch (node) {
            case Var v -> v.name().equals(variable);
            case Num n -> false;
            case Constant c -> false;
            case Neg(ASTNode a) -> containsVariable(a, variable);
            case Add(ASTNode l, ASTNode r) -> containsVariable(l, variable) || containsVariable(r, variable);
            case Sub(ASTNode l, ASTNode r) -> containsVariable(l, variable) || containsVariable(r, variable);
            case Mul(ASTNode l, ASTNode r) -> containsVariable(l, variable) || containsVariable(r, variable);
            case Div(ASTNode l, ASTNode r) -> containsVariable(l, variable) || containsVariable(r, variable);
            case Pow(ASTNode b, ASTNode e) -> containsVariable(b, variable) || containsVariable(e, variable);
            case Func(String name, ASTNode arg) -> containsVariable(arg, variable);
        };
    }
}
