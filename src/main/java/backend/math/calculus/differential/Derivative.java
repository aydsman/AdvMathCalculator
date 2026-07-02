package backend.math.calculus.differential;

import backend.engine.MathOperation;
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
 * Symbolic first derivatives in {@code x} with step-by-step rule application.
 *
 * <p>Supports sum/difference, constant multiple, power, product, quotient, chain,
 * and standard trig / log / sqrt rules.
 */
public class Derivative implements MathOperation {

    private static final String DEFAULT_VAR = "x";
    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);
    private static final Constant E = new Constant("E");

    @Override
    public Result solve(ASTNode input) {
        return differentiate(input, DEFAULT_VAR);
    }

    public Result solveFromInput(String raw) {
        return differentiate(parseInput(raw), DEFAULT_VAR);
    }

    /** Strips optional {@code d/dx(...)} / {@code d/dx ...} wrappers. */
    public static ASTNode parseInput(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new Parser.ParseException("Enter an expression such as x^2 + 3*x or d/dx(sin(x)).");
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("d/dx")) {
            text = text.substring(4).trim();
            if (text.startsWith("(") && text.endsWith(")")) {
                text = text.substring(1, text.length() - 1).trim();
            }
        } else if (lower.startsWith("f(x)=") || lower.contains("(x)=")) {
            int eq = text.indexOf('=');
            text = text.substring(eq + 1).trim();
        }

        return Simplifier.simplify(Parser.parse(text));
    }

    public static Result differentiate(ASTNode expression, String variable) {
        expression = Simplifier.simplify(expression);
        return finishDerivative(expression, variable, diff(expression, variable));
    }

    private static Result finishDerivative(ASTNode expression, String variable, DiffResult result) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Find d/d" + variable + " of", expression));
        steps.addAll(result.steps());

        ASTNode unsimplified = result.derivative();
        ASTNode simplified = fullySimplify(unsimplified);

        steps.add(new Step("Derivative (before simplifying)", unsimplified));
        if (simplified.equals(unsimplified)) {
            steps.add(new Step("Simplify — already in simplest form", simplified));
        } else {
            steps.add(new Step("Simplify", simplified));
        }

        return Result.of(simplified, null, steps);
    }

    /** Package helper for sibling differential modules. */
    static ASTNode derivativeOf(ASTNode expression, String variable) {
        return fullySimplify(diff(Simplifier.simplify(expression), variable).derivative());
    }

    /** Applies {@link Simplifier} until the expression stops changing. */
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

    private record DiffResult(ASTNode derivative, List<Step> steps) {
        static DiffResult of(ASTNode derivative, String description) {
            return new DiffResult(derivative, List.of(new Step(description, derivative)));
        }

        static DiffResult of(ASTNode derivative, String description, ASTNode shown) {
            return new DiffResult(derivative, List.of(new Step(description, shown)));
        }

        static DiffResult silent(ASTNode derivative) {
            return new DiffResult(derivative, List.of());
        }

        DiffResult prepend(String description, ASTNode newDerivative) {
            List<Step> merged = new ArrayList<>();
            merged.add(new Step(description, newDerivative));
            merged.addAll(steps);
            return new DiffResult(newDerivative, merged);
        }

        DiffResult merge(DiffResult other, ASTNode combined, String combineDescription) {
            List<Step> merged = new ArrayList<>(steps);
            merged.addAll(other.steps());
            merged.add(new Step(combineDescription, combined));
            return new DiffResult(combined, merged);
        }
    }

    private static DiffResult diff(ASTNode node, String variable) {
        node = Simplifier.simplify(node);
        return switch (node) {
            case Num n -> DiffResult.of(ZERO, "Constant rule: d/d" + variable + "[" + n.toDisplay() + "] = 0");
            case Constant c -> DiffResult.of(ZERO, "Constant rule: d/d" + variable + "[" + c.name() + "] = 0");
            case Var v -> diffVariable(v, variable);
            case Neg n -> diffNeg(n.operand(), variable);
            case Add a -> diffSum(flattenSum(node), variable);
            case Sub s -> diffSum(flattenSum(new Add(s.left(), new Neg(s.right()))), variable);
            case Mul m -> diffProduct(m, variable);
            case Div d -> diffQuotient(d, variable);
            case Pow p -> diffPower(p, variable);
            case Func f -> diffFunction(f, variable);
        };
    }

    private static DiffResult diffVariable(Var v, String variable) {
        if (v.name().equals(variable)) {
            return DiffResult.of(ONE, "Variable rule: d/d" + variable + "[" + variable + "] = 1");
        }
        return DiffResult.of(ZERO, "Variable rule: d/d" + variable + "[" + v.name() + "] = 0  (treat as constant)");
    }

    private static DiffResult diffNeg(ASTNode operand, String variable) {
        DiffResult inner = diff(operand, variable);
        ASTNode derivative = Simplifier.simplify(new Neg(inner.derivative()));
        return inner.prepend(
                "Constant multiple: d/d" + variable + "[-" + operand.toDisplay() + "] = -("
                        + inner.derivative().toDisplay() + ")",
                derivative);
    }

    private static DiffResult diffSum(List<ASTNode> terms, String variable) {
        if (terms.isEmpty()) {
            return DiffResult.silent(ZERO);
        }
        if (terms.size() == 1) {
            return diff(terms.get(0), variable);
        }

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Sum rule: differentiate term-by-term"));

        ASTNode total = null;
        for (ASTNode term : terms) {
            DiffResult part = diff(term, variable);
            steps.addAll(part.steps());
            total = total == null ? part.derivative() : new Add(total, part.derivative());
        }
        steps.add(new Step("Add the term derivatives", total));
        return new DiffResult(total, steps);
    }

    private static DiffResult diffProduct(Mul m, String variable) {
        ASTNode left = Simplifier.simplify(m.left());
        ASTNode right = Simplifier.simplify(m.right());

        if (left instanceof Num coeff) {
            DiffResult inner = diff(right, variable);
            ASTNode derivative = Simplifier.simplify(new Mul(coeff, inner.derivative()));
            return inner.prepend(
                    "Constant multiple: d/d" + variable + "[" + coeff.toDisplay() + "*("
                            + right.toDisplay() + ")] = " + coeff.toDisplay() + " * ("
                            + inner.derivative().toDisplay() + ")",
                    derivative);
        }
        if (right instanceof Num coeff) {
            DiffResult inner = diff(left, variable);
            ASTNode derivative = Simplifier.simplify(new Mul(inner.derivative(), coeff));
            return inner.prepend(
                    "Constant multiple: d/d" + variable + "[(" + left.toDisplay() + ")*"
                            + coeff.toDisplay() + "] = (" + inner.derivative().toDisplay()
                            + ") * " + coeff.toDisplay(),
                    derivative);
        }

        DiffResult df = diff(left, variable);
        DiffResult dg = diff(right, variable);
        ASTNode term1 = new Mul(df.derivative(), right);
        ASTNode term2 = new Mul(left, dg.derivative());
        ASTNode derivative = new Add(term1, term2);

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Product rule: d/d" + variable + "[f*g] = f'*g + f*g'",
                new Constant("f = " + left.toDisplay() + ",  g = " + right.toDisplay())));
        steps.addAll(df.steps());
        steps.addAll(dg.steps());
        steps.add(new Step("f'*g = " + term1.toDisplay(), term1));
        steps.add(new Step("f*g' = " + term2.toDisplay(), term2));
        steps.add(new Step("Combine product-rule terms", derivative));
        return new DiffResult(derivative, steps);
    }

    private static DiffResult diffQuotient(Div d, String variable) {
        ASTNode f = Simplifier.simplify(d.left());
        ASTNode g = Simplifier.simplify(d.right());

        DiffResult df = diff(f, variable);
        DiffResult dg = diff(g, variable);

        ASTNode numerator = new Sub(new Mul(df.derivative(), g), new Mul(f, dg.derivative()));
        ASTNode denominator = new Pow(g, Num.of(2));
        ASTNode derivative = new Div(numerator, denominator);

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Quotient rule: d/d" + variable + "[f/g] = (f'*g - f*g') / g^2",
                new Constant("f = " + f.toDisplay() + ",  g = " + g.toDisplay())));
        steps.addAll(df.steps());
        steps.addAll(dg.steps());
        steps.add(new Step("f'*g - f*g' = " + numerator.toDisplay(), numerator));
        steps.add(new Step("Divide by g^2 = " + denominator.toDisplay(), derivative));
        return new DiffResult(derivative, steps);
    }

    private static DiffResult diffPower(Pow p, String variable) {
        ASTNode base = Simplifier.simplify(p.base());
        ASTNode exponent = Simplifier.simplify(p.exponent());

        if (base instanceof Var v && v.name().equals(variable) && exponent instanceof Num n) {
            return diffVariablePower(n, variable);
        }

        if (base instanceof Constant c && c.name().equals("E") && exponent instanceof Var v
                && v.name().equals(variable)) {
            ASTNode derivative = Simplifier.simplify(new Pow(E, v));
            return DiffResult.of(derivative,
                    "Exponential rule: d/d" + variable + "[E^" + variable + "] = E^" + variable);
        }

        if (!(exponent instanceof Num expNum)) {
            return logDiffPower(p, variable);
        }

        DiffResult inner = diff(base, variable);
        Num loweredExp = new Num(
                expNum.numerator().subtract(expNum.denominator()), expNum.denominator());
        ASTNode lowered = new Pow(base, loweredExp);
        ASTNode chain = new Mul(new Mul(expNum, lowered), inner.derivative());

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Chain rule on power: d/d" + variable + "[u^n] = n*u^(n-1) * du/d" + variable,
                new Constant("u = " + base.toDisplay() + ",  n = " + expNum.toDisplay())));
        steps.addAll(inner.steps());
        steps.add(new Step("n*u^(n-1) * du/d" + variable + " = " + chain.toDisplay(), chain));
        return new DiffResult(chain, steps);
    }

    private static DiffResult logDiffPower(Pow p, String variable) {
        ASTNode base = Simplifier.simplify(p.base());
        ASTNode exponent = Simplifier.simplify(p.exponent());
        ASTNode original = new Pow(base, exponent);

        DiffResult du = diff(base, variable);
        DiffResult dv = diff(exponent, variable);

        ASTNode lnBase = new Func("ln", base);
        ASTNode term1 = new Mul(dv.derivative(), lnBase);
        ASTNode term2 = new Mul(exponent, new Div(du.derivative(), base));
        ASTNode bracket = Simplifier.simplify(new Add(term1, term2));
        ASTNode derivative = Simplifier.simplify(new Mul(original, bracket));

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Logarithmic differentiation: let y = " + original.toDisplay()));
        steps.add(new Step("ln(y) = " + exponent.toDisplay() + " * ln(" + base.toDisplay() + ")"));
        steps.add(new Step("Differentiate implicitly: y'/y = v' * ln(u) + v * u'/u",
                new Constant("u = " + base.toDisplay() + ",  v = " + exponent.toDisplay())));
        steps.addAll(du.steps());
        steps.addAll(dv.steps());
        steps.add(new Step("v' * ln(u) + v * u'/u = " + bracket.toDisplay(), bracket));
        steps.add(new Step("y' = y * (v' * ln(u) + v * u'/u) = " + derivative.toDisplay(), derivative));
        return new DiffResult(derivative, steps);
    }

    private static DiffResult diffVariablePower(Num exponent, String variable) {
        if (exponent.isZero()) {
            return DiffResult.of(ZERO, "Constant rule: d/d" + variable + "[" + variable + "^0] = 0");
        }
        if (exponent.equals(ONE)) {
            return DiffResult.of(ONE, "Variable rule: d/d" + variable + "[" + variable + "] = 1");
        }

        Num lowered = new Num(exponent.numerator().subtract(exponent.denominator()),
                exponent.denominator());
        if (exponent.denominator().equals(exponent.numerator())) {
            lowered = ONE;
        } else if (exponent.isInteger()) {
            lowered = Num.of(exponent.numerator().intValueExact() - 1);
        }

        ASTNode coeff = exponent;
        ASTNode powerPart = exponent.equals(ONE) ? ONE : new Pow(new Var(variable), lowered);
        ASTNode derivative = Simplifier.simplify(new Mul(coeff, powerPart));

        return DiffResult.of(
                derivative,
                "Power rule: d/d" + variable + "[" + variable + "^" + exponent.toDisplay()
                        + "] = " + exponent.toDisplay() + "*" + variable + "^"
                        + (exponent.isInteger() && exponent.numerator().intValueExact() == 2
                        ? "1" : lowered.toDisplay()));
    }

    private static DiffResult diffFunction(Func f, String variable) {
        String name = f.name().toLowerCase(Locale.ROOT);
        ASTNode arg = Simplifier.simplify(f.argument());
        DiffResult inner = diff(arg, variable);

        ASTNode outerDerivative = switch (name) {
            case "sin" -> new Func("cos", arg);
            case "cos" -> Simplifier.simplify(new Neg(new Func("sin", arg)));
            case "tan" -> Simplifier.simplify(new Pow(new Func("sec", arg), Num.of(2)));
            case "sec" -> Simplifier.simplify(new Mul(new Func("sec", arg), new Func("tan", arg)));
            case "csc" -> Simplifier.simplify(new Neg(new Mul(new Func("csc", arg), new Func("cot", arg))));
            case "cot" -> Simplifier.simplify(new Neg(new Pow(new Func("csc", arg), Num.of(2))));
            case "ln", "log" -> Simplifier.simplify(new Div(ONE, arg));
            case "sqrt" -> Simplifier.simplify(new Div(ONE, new Mul(Num.of(2), new Func("sqrt", arg))));
            default -> throw new UnsupportedOperationException("Derivative not supported for: " + name);
        };

        String ruleName = switch (name) {
            case "sin" -> "sin";
            case "cos" -> "cos";
            case "tan" -> "tan";
            case "sec" -> "sec";
            case "csc" -> "csc";
            case "cot" -> "cot";
            case "ln", "log" -> "ln";
            case "sqrt" -> "sqrt";
            default -> name;
        };

        ASTNode derivative = inner.derivative().equals(ONE)
                ? outerDerivative
                : new Mul(outerDerivative, inner.derivative());

        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Chain rule on " + ruleName + ": differentiate outer, then multiply by du/d"
                + variable, new Constant("u = " + arg.toDisplay())));
        if (!arg.equals(new Var(variable))) {
            steps.addAll(inner.steps());
        }
        steps.add(new Step("Outer derivative: d/du[" + ruleName + "(u)] evaluated at u = "
                + arg.toDisplay(), outerDerivative));
        if (!inner.derivative().equals(ONE)) {
            steps.add(new Step("Multiply by du/d" + variable + " = " + inner.derivative().toDisplay(), derivative));
        }
        return new DiffResult(derivative, steps);
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
                flattenSumInto(new Neg(s.right()), out);
            }
            case Neg n -> flattenSumInto(n.operand(), out);
            default -> out.add(node);
        }
    }
}
