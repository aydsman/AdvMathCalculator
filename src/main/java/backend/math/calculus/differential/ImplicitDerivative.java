package backend.math.calculus.differential;

import backend.engine.MathOperation;
import backend.math.algebra.EquationSolver;
import backend.math.algebra.Simplifier;
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
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds dy/dx for an equation F(x, y) = 0 using implicit differentiation.
 *
 * <p>Input examples: {@code x^2 + y^2 = 1}, {@code x*y = 5}, {@code x^2 + y^2 - 1}.
 */
public class ImplicitDerivative implements MathOperation {

    private static final String X = "x";
    private static final String Y = "y";
    private static final Constant DYDX = new Constant("dy/dx");
    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);

    @Override
    public Result solve(ASTNode input) {
        return differentiateEquation(input);
    }

    public Result solveFromInput(String raw) {
        return differentiateEquation(parseEquation(raw));
    }

    private static ASTNode parseEquation(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new Parser.ParseException("Enter an equation such as x^2 + y^2 = 1.");
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("dy/dx") && text.contains(",")) {
            int comma = text.indexOf(',');
            text = text.substring(comma + 1).trim();
        }
        return Simplifier.simplify(EquationSolver.toZeroForm(text));
    }

    private static Result differentiateEquation(ASTNode zeroForm) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Write the equation as F(x, y) = 0", zeroForm));
        steps.add(new Step("Differentiate both sides with respect to x (treat y as y(x))"));

        ASTNode differentiated = Simplifier.simplify(implicitDiff(zeroForm, X, Y));
        steps.add(new Step("dF/dx = 0 becomes", differentiated));

        DyDxForm form = splitDyDx(differentiated);
        ASTNode coeff = Simplifier.simplify(form.coeff());
        ASTNode rest = Simplifier.simplify(form.without());

        if (coeff.equals(ZERO)) {
            if (rest.equals(ZERO)) {
                return Result.failure("dy/dx is undefined (0 = 0).");
            }
            return Result.failure("dy/dx does not appear in the differentiated equation.");
        }

        steps.add(new Step("Collect dy/dx terms: " + rest.toDisplay() + " + (" + coeff.toDisplay() + ")*dy/dx = 0"));
        ASTNode dyDx = fullySimplify(new Div(new Neg(rest), coeff));
        steps.add(new Step("Solve for dy/dx", dyDx));
        steps.add(new Step("Answer", new Constant("dy/dx = " + dyDx.toDisplay())));

        return Result.of(dyDx, null, steps);
    }

    private static ASTNode implicitDiff(ASTNode node, String x, String y) {
        node = Simplifier.simplify(node);
        return switch (node) {
            case Num n -> ZERO;
            case Constant c -> ZERO;
            case Var v -> {
                if (v.name().equals(x)) {
                    yield ONE;
                }
                if (v.name().equals(y)) {
                    yield DYDX;
                }
                yield ZERO;
            }
            case Neg n -> Simplifier.simplify(new Neg(implicitDiff(n.operand(), x, y)));
            case Add a -> Simplifier.simplify(new Add(
                    implicitDiff(a.left(), x, y),
                    implicitDiff(a.right(), x, y)));
            case Sub s -> Simplifier.simplify(new Sub(
                    implicitDiff(s.left(), x, y),
                    implicitDiff(s.right(), x, y)));
            case Mul m -> {
                ASTNode left = Simplifier.simplify(m.left());
                ASTNode right = Simplifier.simplify(m.right());
                ASTNode dLeft = implicitDiff(left, x, y);
                ASTNode dRight = implicitDiff(right, x, y);
                yield Simplifier.simplify(new Add(new Mul(dLeft, right), new Mul(left, dRight)));
            }
            case Div d -> {
                ASTNode f = Simplifier.simplify(d.left());
                ASTNode g = Simplifier.simplify(d.right());
                ASTNode df = implicitDiff(f, x, y);
                ASTNode dg = implicitDiff(g, x, y);
                ASTNode numerator = new Sub(new Mul(df, g), new Mul(f, dg));
                yield Simplifier.simplify(new Div(numerator, new Pow(g, Num.of(2))));
            }
            case Pow p -> implicitDiffPower(p, x, y);
            case Func f -> implicitDiffFunction(f, x, y);
        };
    }

    private static ASTNode implicitDiffPower(Pow p, String x, String y) {
        ASTNode base = Simplifier.simplify(p.base());
        ASTNode exponent = Simplifier.simplify(p.exponent());

        if (base instanceof Var v && v.name().equals(x) && exponent instanceof Num n) {
            return Derivative.derivativeOf(new Pow(base, n), x);
        }
        if (base instanceof Var v && v.name().equals(y) && exponent instanceof Num n) {
            if (n.isZero()) {
                return ZERO;
            }
            Num lowered = n.isInteger()
                    ? Num.of(n.numerator().intValueExact() - 1)
                    : new Num(n.numerator().subtract(n.denominator()), n.denominator());
            ASTNode power = n.equals(ONE) ? ONE : new Pow(base, lowered);
            return Simplifier.simplify(new Mul(n, new Mul(power, DYDX)));
        }
        if (!(exponent instanceof Num expNum)) {
            throw new UnsupportedOperationException(
                    "Use logarithmic derivative for variable exponents: " + p.toDisplay());
        }

        ASTNode dBase = implicitDiff(base, x, y);
        Num loweredExp = expNum.isInteger()
                ? Num.of(expNum.numerator().intValueExact() - 1)
                : new Num(expNum.numerator().subtract(expNum.denominator()), expNum.denominator());
        ASTNode lowered = new Pow(base, loweredExp);
        return Simplifier.simplify(new Mul(new Mul(expNum, lowered), dBase));
    }

    private static ASTNode implicitDiffFunction(Func f, String x, String y) {
        String name = f.name().toLowerCase(Locale.ROOT);
        ASTNode arg = Simplifier.simplify(f.argument());
        ASTNode inner = implicitDiff(arg, x, y);

        ASTNode outer = switch (name) {
            case "sin" -> new Func("cos", arg);
            case "cos" -> Simplifier.simplify(new Neg(new Func("sin", arg)));
            case "tan" -> Simplifier.simplify(new Pow(new Func("sec", arg), Num.of(2)));
            case "ln", "log" -> Simplifier.simplify(new Div(ONE, arg));
            case "sqrt" -> Simplifier.simplify(new Div(ONE, new Mul(Num.of(2), new Func("sqrt", arg))));
            default -> throw new UnsupportedOperationException("Implicit derivative not supported for: " + name);
        };

        if (inner.equals(ZERO)) {
            return ZERO;
        }
        if (inner.equals(ONE)) {
            return outer;
        }
        if (inner.equals(DYDX)) {
            return outer;
        }
        return Simplifier.simplify(new Mul(outer, inner));
    }

    private static ASTNode fullySimplify(ASTNode node) {
        ASTNode current = node;
        for (int pass = 0; pass < 8; pass++) {
            ASTNode next = Simplifier.simplify(current);
            if (next.equals(current)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private record DyDxForm(ASTNode without, ASTNode coeff) {}

    private static DyDxForm splitDyDx(ASTNode node) {
        ASTNode without = ZERO;
        ASTNode coeff = ZERO;
        for (ASTNode term : flattenSum(node)) {
            if (containsDyDx(term)) {
                coeff = Simplifier.simplify(new Add(coeff, dyDxCoefficient(term)));
            } else {
                without = Simplifier.simplify(new Add(without, term));
            }
        }
        return new DyDxForm(without, coeff);
    }

    private static ASTNode dyDxCoefficient(ASTNode term) {
        term = Simplifier.simplify(term);
        if (isDyDx(term)) {
            return ONE;
        }
        if (term instanceof Mul m) {
            if (isDyDx(m.left())) {
                return Simplifier.simplify(m.right());
            }
            if (isDyDx(m.right())) {
                return Simplifier.simplify(m.left());
            }
            if (containsDyDx(m.left())) {
                return Simplifier.simplify(new Mul(dyDxCoefficient(m.left()), m.right()));
            }
            if (containsDyDx(m.right())) {
                return Simplifier.simplify(new Mul(m.left(), dyDxCoefficient(m.right())));
            }
        }
        if (term instanceof Neg n) {
            return Simplifier.simplify(new Neg(dyDxCoefficient(n.operand())));
        }
        throw new UnsupportedOperationException("Could not isolate dy/dx in: " + term.toDisplay());
    }

    private static boolean containsDyDx(ASTNode node) {
        node = Simplifier.simplify(node);
        if (isDyDx(node)) {
            return true;
        }
        return switch (node) {
            case Add a -> containsDyDx(a.left()) || containsDyDx(a.right());
            case Sub s -> containsDyDx(s.left()) || containsDyDx(s.right());
            case Neg n -> containsDyDx(n.operand());
            case Mul m -> containsDyDx(m.left()) || containsDyDx(m.right());
            default -> false;
        };
    }

    private static List<ASTNode> flattenSum(ASTNode node) {
        List<ASTNode> terms = new ArrayList<>();
        flattenSumInto(node, terms);
        return terms;
    }

    private static void flattenSumInto(ASTNode node, List<ASTNode> out) {
        switch (node) {
            case Add a -> {
                flattenSumInto(a.left(), out);
                flattenSumInto(a.right(), out);
            }
            case Sub s -> {
                flattenSumInto(s.left(), out);
                out.add(Simplifier.simplify(new Neg(s.right())));
            }
            case Neg n -> out.add(Simplifier.simplify(node));
            default -> out.add(node);
        }
    }

    private static boolean isDyDx(ASTNode node) {
        return node instanceof Constant c && "dy/dx".equals(c.name());
    }
}
