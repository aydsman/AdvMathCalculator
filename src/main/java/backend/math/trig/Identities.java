package backend.math.trig;

import backend.engine.MathOperation;
import backend.math.algebra.Simplifier;
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
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Applies standard trigonometric identities to simplify an expression.
 */
public class Identities implements MathOperation {

    private static final Num ONE = Num.of(1);
    private static final Num TWO = Num.of(2);

    @Override
    public Result solve(ASTNode input) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Original expression", input));

        ASTNode current = Simplifier.simplify(input);
        boolean changed;
        do {
            changed = false;
            ASTNode next = applyIdentities(current);
            next = Simplifier.simplify(next);
            if (!next.equals(current)) {
                changed = true;
                current = next;
            }
        } while (changed);

        if (!current.equals(input)) {
            steps.add(new Step("Apply trigonometric identities", current));
        } else {
            steps.add(new Step("No further identities apply to this form"));
        }

        return Result.of(current, null, steps);
    }

    private static ASTNode applyIdentities(ASTNode node) {
        node = rewriteBottomUp(node);
        node = applyPythagoreanInSum(node);
        return node;
    }

    private static ASTNode rewriteBottomUp(ASTNode node) {
        return switch (node) {
            case Add a -> applyPythagoreanInSum(new Add(
                    rewriteBottomUp(a.left()), rewriteBottomUp(a.right())));
            case Sub s -> new Sub(rewriteBottomUp(s.left()), rewriteBottomUp(s.right()));
            case Mul m -> new Mul(rewriteBottomUp(m.left()), rewriteBottomUp(m.right()));
            case Div d -> new Div(rewriteBottomUp(d.left()), rewriteBottomUp(d.right()));
            case Pow p -> new Pow(rewriteBottomUp(p.base()), rewriteBottomUp(p.exponent()));
            case Neg n -> new Neg(rewriteBottomUp(n.operand()));
            case Func f -> rewriteTrigFunction(f);
            default -> node;
        };
    }

    private static ASTNode rewriteTrigFunction(Func f) {
        ASTNode arg = rewriteBottomUp(f.argument());
        String name = f.name().toLowerCase(Locale.ROOT);

        if ("tan".equals(name)) {
            return Simplifier.simplify(new Div(new Func("sin", arg), new Func("cos", arg)));
        }
        if ("cot".equals(name)) {
            return Simplifier.simplify(new Div(new Func("cos", arg), new Func("sin", arg)));
        }
        if ("sec".equals(name)) {
            return Simplifier.simplify(new Div(ONE, new Func("cos", arg)));
        }
        if ("csc".equals(name)) {
            return Simplifier.simplify(new Div(ONE, new Func("sin", arg)));
        }
        if ("sin".equals(name) && arg instanceof Mul m && m.left() instanceof Num n && n.isInteger()
                && n.numerator().intValueExact() == 2) {
            ASTNode inner = m.right();
            return Simplifier.simplify(new Mul(TWO, new Mul(new Func("sin", inner), new Func("cos", inner))));
        }
        if ("cos".equals(name) && arg instanceof Mul m && m.left() instanceof Num n && n.isInteger()
                && n.numerator().intValueExact() == 2) {
            ASTNode inner = m.right();
            ASTNode cos2 = new Pow(new Func("cos", inner), TWO);
            ASTNode sin2 = new Pow(new Func("sin", inner), TWO);
            return Simplifier.simplify(new Sub(cos2, sin2));
        }
        return new Func(name, arg);
    }

    private static ASTNode applyPythagoreanInSum(ASTNode node) {
        if (!(node instanceof Add)) {
            return node;
        }

        List<SignedTerm> terms = flattenSum(node);
        Map<String, Integer> sin2 = new HashMap<>();
        Map<String, Integer> cos2 = new HashMap<>();
        Map<String, Integer> sec2 = new HashMap<>();
        Map<String, Integer> tan2 = new HashMap<>();
        List<SignedTerm> other = new ArrayList<>();
        int constant = 0;

        for (SignedTerm term : terms) {
            TrigPower power = asTrigPower(term.body());
            if (power != null && power.exponent() == 2) {
                int sign = term.positive() ? 1 : -1;
                switch (power.name()) {
                    case "sin" -> sin2.merge(power.argKey(), sign, Integer::sum);
                    case "cos" -> cos2.merge(power.argKey(), sign, Integer::sum);
                    case "sec" -> sec2.merge(power.argKey(), sign, Integer::sum);
                    case "tan" -> tan2.merge(power.argKey(), sign, Integer::sum);
                    default -> other.add(term);
                }
            } else if (term.body() instanceof Num n && n.isInteger()) {
                constant += term.positive() ? n.numerator().intValueExact() : -n.numerator().intValueExact();
            } else {
                other.add(term);
            }
        }

        for (String key : new ArrayList<>(sin2.keySet())) {
            int s = sin2.getOrDefault(key, 0);
            int c = cos2.getOrDefault(key, 0);
            if (s == 1 && c == 1) {
                sin2.remove(key);
                cos2.remove(key);
                constant += 1;
            } else if (s == 1 && c == -1) {
                sin2.remove(key);
                cos2.remove(key);
                other.add(new SignedTerm(true, new Pow(new Func("sin", parseArg(key)), TWO)));
                other.add(new SignedTerm(true, new Pow(new Func("sin", parseArg(key)), TWO)));
            } else if (s == -1 && c == 1) {
                sin2.remove(key);
                cos2.remove(key);
                other.add(new SignedTerm(true, new Pow(new Func("cos", parseArg(key)), TWO)));
                other.add(new SignedTerm(true, new Pow(new Func("cos", parseArg(key)), TWO)));
            }
        }

        for (String key : new ArrayList<>(sec2.keySet())) {
            int sec = sec2.getOrDefault(key, 0);
            int tan = tan2.getOrDefault(key, 0);
            if (sec == 1 && tan == -1) {
                sec2.remove(key);
                tan2.remove(key);
                constant += 1;
            }
        }

        if (constant != 0) {
            other.add(new SignedTerm(constant > 0, Num.of(Math.abs(constant))));
        }
        for (var e : sin2.entrySet()) {
            if (e.getValue() != 0) {
                other.add(trigPowerTerm("sin", e.getKey(), e.getValue()));
            }
        }
        for (var e : cos2.entrySet()) {
            if (e.getValue() != 0) {
                other.add(trigPowerTerm("cos", e.getKey(), e.getValue()));
            }
        }
        for (var e : sec2.entrySet()) {
            if (e.getValue() != 0) {
                other.add(trigPowerTerm("sec", e.getKey(), e.getValue()));
            }
        }
        for (var e : tan2.entrySet()) {
            if (e.getValue() != 0) {
                other.add(trigPowerTerm("tan", e.getKey(), e.getValue()));
            }
        }

        return rebuildSum(other);
    }

    private static SignedTerm trigPowerTerm(String fn, String argKey, int coeff) {
        ASTNode power = new Pow(new Func(fn, parseArg(argKey)), TWO);
        ASTNode body = Math.abs(coeff) == 1 ? power : new Mul(Num.of(Math.abs(coeff)), power);
        return new SignedTerm(coeff > 0, body);
    }

    private static ASTNode parseArg(String key) {
        return Simplifier.simplify(Parser.parse(key));
    }

    private record SignedTerm(boolean positive, ASTNode body) {}

    private record TrigPower(String name, String argKey, int exponent) {}

    private static TrigPower asTrigPower(ASTNode node) {
        if (node instanceof Pow p && p.exponent() instanceof Num n && n.isInteger()
                && n.numerator().intValueExact() == 2 && p.base() instanceof Func f
                && TrigUtils.isTrigFunction(f.name())) {
            return new TrigPower(f.name().toLowerCase(Locale.ROOT), f.argument().toDisplay(), 2);
        }
        return null;
    }

    private static List<SignedTerm> flattenSum(ASTNode node) {
        List<SignedTerm> out = new ArrayList<>();
        flattenSumInto(node, true, out);
        return out;
    }

    private static void flattenSumInto(ASTNode node, boolean positive, List<SignedTerm> out) {
        switch (node) {
            case Add a -> {
                flattenSumInto(a.left(), positive, out);
                flattenSumInto(a.right(), positive, out);
            }
            case Sub s -> {
                flattenSumInto(s.left(), positive, out);
                flattenSumInto(s.right(), !positive, out);
            }
            case Neg n -> flattenSumInto(n.operand(), !positive, out);
            default -> out.add(new SignedTerm(positive, node));
        }
    }

    private static ASTNode rebuildSum(List<SignedTerm> terms) {
        if (terms.isEmpty()) {
            return Num.of(0);
        }
        ASTNode sum = null;
        for (SignedTerm term : terms) {
            ASTNode piece = term.positive() ? term.body() : Simplifier.simplify(new Neg(term.body()));
            sum = sum == null ? piece : Simplifier.simplify(new Add(sum, piece));
        }
        return sum;
    }
}
