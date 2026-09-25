package backend.math.calculus.integral;

import backend.engine.MathOperation;
import backend.math.algebra.Simplifier;
import backend.math.functions.FunctionUtils;
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
 * Indefinite integrals of the form {@code ∫ a·f(bx + c) dx} using the constant-multiple
 * and linear-composition antiderivative properties:
 *
 * <pre>
 *   ∫ a · f(bx + c) dx = (a/b) · F(bx + c) + C   (b ≠ 0)
 * </pre>
 *
 * <p>Supports a base table for powers, reciprocal, sin, cos, sqrt, sec², and csc².
 */
public class Antiderivative implements MathOperation {

    private static final String DEFAULT_VAR = "x";
    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);
    private static final Num NEG_ONE = Num.of(-1);
    private static final Constant PLUS_C = new Constant("C");

    @Override
    public Result solve(ASTNode input) {
        return integrate(Simplifier.simplify(input), DEFAULT_VAR);
    }

    public Result solveFromInput(String raw) {
        return integrate(parseInput(raw), DEFAULT_VAR);
    }

    /** Strips optional {@code integral ... dx} / {@code ∫ ... dx} wrappers. */
    public static ASTNode parseInput(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            throw new Parser.ParseException("Enter an integrand such as 6*sin(2*x+1) or x^2");
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("integral")) {
            text = text.substring("integral".length()).trim();
        } else if (text.startsWith("\u222B") || lower.startsWith("int ")) {
            text = text.substring(text.startsWith("\u222B") ? 1 : 4).trim();
        }

        if (lower.endsWith(" dx") || text.endsWith(" dx") || text.endsWith(" d" + DEFAULT_VAR)) {
            int d = text.toLowerCase(Locale.ROOT).lastIndexOf(" d");
            if (d > 0) {
                text = text.substring(0, d).trim();
            }
        }

        if (text.startsWith("(") && text.endsWith(")") && balancedOuterParens(text)) {
            text = text.substring(1, text.length() - 1).trim();
        }

        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(text, "f", DEFAULT_VAR);
        return Simplifier.simplify(def.body());
    }

    private static boolean balancedOuterParens(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0 && i < text.length() - 1) {
                    return false;
                }
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static Result integrate(ASTNode integrand, String variable) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Integrate with respect to " + variable, integrand));

        Peeled peeled = peelConstantFactor(integrand);
        Num a = peeled.factor();
        ASTNode core = peeled.core();

        if (!a.equals(ONE)) {
            steps.add(new Step(
                    "Constant multiple: ∫ " + a.toDisplay() + "·g(" + variable + ") d" + variable
                            + " = " + a.toDisplay() + " · ∫ g(" + variable + ") d" + variable,
                    core));
        }

        // Pure constant (no variable): ∫ a dx = a*x + C
        if (!containsVariable(core, variable)) {
            return constantIntegral(a, core, variable, steps);
        }

        MatchedForm match = matchAfBxC(core, variable);
        if (match == null) {
            return Result.failure(
                    "Only antiderivatives of the form a·f(bx + c) are supported so far "
                            + "(powers, 1/u, sin, cos, sqrt, sec^2, csc^2).");
        }

        Linear u = match.linear();
        steps.add(new Step(
                "Identify linear inside: u = " + formatLinear(u, variable)
                        + "  (b = " + u.b().toDisplay() + ", c = " + u.c().toDisplay() + ")",
                new Constant("f(u) = " + match.fDescription())));

        if (u.b().isZero()) {
            // f(c) is constant in x
            ASTNode value = tableF(match, new Linear(ZERO, u.c()), variable);
            ASTNode scaled = Simplifier.simplify(new Mul(a, value));
            ASTNode answer = Simplifier.simplify(new Mul(scaled, new Var(variable)));
            steps.add(new Step("b = 0, so the integrand is constant in " + variable, answer));
            return finish(answer, steps);
        }

        steps.add(new Step(
                "Linear composition: ∫ f(bx + c) d" + variable
                        + " = (1/b) · F(bx + c) + C"));

        ASTNode F = tableF(match, u, variable);
        steps.add(new Step("Antiderivative F(u) evaluated at u = " + formatLinear(u, variable), F));

        // (a/b) * F
        ASTNode scale = Simplifier.simplify(new Div(a, u.b()));
        ASTNode antideriv = Simplifier.simplify(new Mul(scale, F));
        if (!scale.equals(ONE)) {
            steps.add(new Step(
                    "Multiply by a/b = " + scale.toDisplay(),
                    antideriv));
        } else {
            steps.add(new Step("Simplify", antideriv));
        }

        return finish(antideriv, steps);
    }

    private static Result constantIntegral(Num a, ASTNode core, String variable, List<Step> steps) {
        ASTNode value = Simplifier.simplify(core);
        ASTNode scaled = a.equals(ONE) ? value : Simplifier.simplify(new Mul(a, value));
        ASTNode answer = scaled.equals(ONE)
                ? new Var(variable)
                : Simplifier.simplify(new Mul(scaled, new Var(variable)));
        steps.add(new Step("Constant rule: ∫ k d" + variable + " = k·" + variable + " + C", answer));
        return finish(answer, steps);
    }

    private static Result finish(ASTNode antideriv, List<Step> steps) {
        ASTNode withC = Simplifier.simplify(new Add(antideriv, PLUS_C));
        steps.add(new Step("Add the constant of integration", withC));
        return Result.of(withC, null, steps);
    }

    // ---- Matching a·f(bx+c) ------------------------------------------------

    private record Peeled(Num factor, ASTNode core) {}

    private static Peeled peelConstantFactor(ASTNode node) {
        node = Simplifier.simplify(node);
        if (node instanceof Neg n) {
            Peeled inner = peelConstantFactor(n.operand());
            return new Peeled(numMul(NEG_ONE, inner.factor()), inner.core());
        }
        if (node instanceof Mul m) {
            List<ASTNode> factors = flattenMul(m);
            Num coeff = ONE;
            List<ASTNode> rest = new ArrayList<>();
            for (ASTNode f : factors) {
                if (f instanceof Num num) {
                    coeff = numMul(coeff, num);
                } else if (f instanceof Neg neg && neg.operand() instanceof Num num) {
                    coeff = numMul(coeff, numNeg(num));
                } else {
                    rest.add(f);
                }
            }
            if (rest.isEmpty()) {
                return new Peeled(coeff, ONE);
            }
            ASTNode core = rest.get(0);
            for (int i = 1; i < rest.size(); i++) {
                core = new Mul(core, rest.get(i));
            }
            return new Peeled(coeff, Simplifier.simplify(core));
        }
        if (node instanceof Div d && d.left() instanceof Num num) {
            Peeled den = peelConstantFactor(d.right());
            // num / (k * rest) = (num/k) * (1/rest) — only if rest is the full denominator core
            return new Peeled(numDiv(num, den.factor()), Simplifier.simplify(new Div(ONE, den.core())));
        }
        return new Peeled(ONE, node);
    }

    private static List<ASTNode> flattenMul(ASTNode node) {
        node = Simplifier.simplify(node);
        if (node instanceof Mul m) {
            List<ASTNode> out = new ArrayList<>();
            out.addAll(flattenMul(m.left()));
            out.addAll(flattenMul(m.right()));
            return out;
        }
        return List.of(node);
    }

    private enum FormKind {
        POWER, RECIPROCAL, SIN, COS, SQRT, SEC2, CSC2
    }

    private record Linear(Num b, Num c) {}

    private record MatchedForm(FormKind kind, Linear linear, Num exponent, String fDescription) {}

    private static MatchedForm matchAfBxC(ASTNode core, String variable) {
        core = Simplifier.simplify(core);

        // sec(u)^2 / csc(u)^2
        if (core instanceof Pow p && p.exponent() instanceof Num exp && exp.equals(Num.of(2))
                && p.base() instanceof Func f) {
            Linear linear = asLinear(f.argument(), variable);
            if (linear != null) {
                String name = f.name().toLowerCase(Locale.ROOT);
                if ("sec".equals(name)) {
                    return new MatchedForm(FormKind.SEC2, linear, null, "sec(u)^2");
                }
                if ("csc".equals(name)) {
                    return new MatchedForm(FormKind.CSC2, linear, null, "csc(u)^2");
                }
            }
        }

        if (core instanceof Func f) {
            Linear linear = asLinear(f.argument(), variable);
            if (linear == null) {
                return null;
            }
            return switch (f.name().toLowerCase(Locale.ROOT)) {
                case "sin" -> new MatchedForm(FormKind.SIN, linear, null, "sin(u)");
                case "cos" -> new MatchedForm(FormKind.COS, linear, null, "cos(u)");
                case "sqrt" -> new MatchedForm(FormKind.SQRT, linear, null, "sqrt(u)");
                default -> null;
            };
        }

        // 1/(bx+c)
        if (core instanceof Div d) {
            ASTNode num = Simplifier.simplify(d.left());
            if (num instanceof Num n && n.equals(ONE)) {
                Linear linear = asLinear(d.right(), variable);
                if (linear != null) {
                    return new MatchedForm(FormKind.RECIPROCAL, linear, null, "1/u");
                }
            }
        }

        // (bx+c)^n  or  x^n
        if (core instanceof Pow p && p.exponent() instanceof Num exp) {
            if (exp.equals(NEG_ONE)) {
                Linear linear = asLinear(p.base(), variable);
                if (linear != null) {
                    return new MatchedForm(FormKind.RECIPROCAL, linear, null, "1/u");
                }
            }
            if (!exp.equals(NEG_ONE)) {
                Linear linear = asLinear(p.base(), variable);
                if (linear != null) {
                    return new MatchedForm(FormKind.POWER, linear, exp, "u^" + exp.toDisplay());
                }
            }
        }

        // bare linear: bx+c  →  treat as u^1
        Linear linear = asLinear(core, variable);
        if (linear != null && !linear.b().isZero()) {
            return new MatchedForm(FormKind.POWER, linear, ONE, "u");
        }

        return null;
    }

    /**
     * Parses an expression as {@code b·x + c}. Returns {@code null} if it is not linear
     * in {@code variable}.
     */
    static Linear asLinear(ASTNode node, String variable) {
        node = Simplifier.simplify(node);
        Num b = ZERO;
        Num c = ZERO;

        List<ASTNode> terms = flattenSum(node);
        for (ASTNode term : terms) {
            term = Simplifier.simplify(term);
            if (term instanceof Num n) {
                c = numAdd(c, n);
                continue;
            }
            if (term instanceof Var v && v.name().equals(variable)) {
                b = numAdd(b, ONE);
                continue;
            }
            if (term instanceof Neg n) {
                ASTNode inner = Simplifier.simplify(n.operand());
                if (inner instanceof Num num) {
                    c = numAdd(c, numNeg(num));
                    continue;
                }
                if (inner instanceof Var v && v.name().equals(variable)) {
                    b = numAdd(b, NEG_ONE);
                    continue;
                }
                if (inner instanceof Mul m) {
                    NumCoeffVar cv = coeffTimesVar(m, variable);
                    if (cv != null) {
                        b = numAdd(b, numNeg(cv.coeff()));
                        continue;
                    }
                }
                return null;
            }
            if (term instanceof Mul m) {
                NumCoeffVar cv = coeffTimesVar(m, variable);
                if (cv != null) {
                    b = numAdd(b, cv.coeff());
                    continue;
                }
                return null;
            }
            return null;
        }
        return new Linear(b, c);
    }

    private record NumCoeffVar(Num coeff) {}

    private static NumCoeffVar coeffTimesVar(Mul m, String variable) {
        List<ASTNode> factors = flattenMul(m);
        Num coeff = ONE;
        int varCount = 0;
        for (ASTNode f : factors) {
            if (f instanceof Num n) {
                coeff = numMul(coeff, n);
            } else if (f instanceof Var v && v.name().equals(variable)) {
                varCount++;
            } else if (f instanceof Neg n && n.operand() instanceof Num num) {
                coeff = numMul(coeff, numNeg(num));
            } else {
                return null;
            }
        }
        if (varCount != 1) {
            return null;
        }
        return new NumCoeffVar(coeff);
    }

    private static List<ASTNode> flattenSum(ASTNode node) {
        List<ASTNode> out = new ArrayList<>();
        flattenSumInto(Simplifier.simplify(node), out);
        return out;
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
            default -> out.add(node);
        }
    }

    // ---- Table F(u) --------------------------------------------------------

    private static ASTNode tableF(MatchedForm match, Linear u, String variable) {
        ASTNode uExpr = linearNode(u, variable);
        return switch (match.kind()) {
            case POWER -> {
                Num n = match.exponent();
                Num nPlus1 = numAdd(n, ONE);
                // u^(n+1) / (n+1)
                yield Simplifier.simplify(new Div(new Pow(uExpr, nPlus1), nPlus1));
            }
            case RECIPROCAL -> Simplifier.simplify(new Func("ln", new Func("abs", uExpr)));
            case SIN -> Simplifier.simplify(new Neg(new Func("cos", uExpr)));
            case COS -> new Func("sin", uExpr);
            case SQRT -> {
                // ∫ u^(1/2) = u^(3/2) / (3/2) = (2/3) u^(3/2)
                ASTNode power = new Pow(uExpr, Num.of(3, 2));
                yield Simplifier.simplify(new Mul(Num.of(2, 3), power));
            }
            case SEC2 -> new Func("tan", uExpr);
            case CSC2 -> Simplifier.simplify(new Neg(new Func("cot", uExpr)));
        };
    }

    private static ASTNode linearNode(Linear linear, String variable) {
        ASTNode bx = linear.b().equals(ONE)
                ? new Var(variable)
                : linear.b().equals(NEG_ONE)
                ? new Neg(new Var(variable))
                : new Mul(linear.b(), new Var(variable));
        if (linear.c().isZero()) {
            return Simplifier.simplify(bx);
        }
        if (linear.b().isZero()) {
            return linear.c();
        }
        if (linear.c().numerator().signum() < 0) {
            return Simplifier.simplify(new Sub(bx, numNeg(linear.c())));
        }
        return Simplifier.simplify(new Add(bx, linear.c()));
    }

    private static String formatLinear(Linear linear, String variable) {
        return linearNode(linear, variable).toDisplay();
    }

    private static boolean containsVariable(ASTNode node, String variable) {
        return switch (node) {
            case Var v -> v.name().equals(variable);
            case Num n -> false;
            case Constant c -> false;
            case Neg n -> containsVariable(n.operand(), variable);
            case Add a -> containsVariable(a.left(), variable) || containsVariable(a.right(), variable);
            case Sub s -> containsVariable(s.left(), variable) || containsVariable(s.right(), variable);
            case Mul m -> containsVariable(m.left(), variable) || containsVariable(m.right(), variable);
            case Div d -> containsVariable(d.left(), variable) || containsVariable(d.right(), variable);
            case Pow p -> containsVariable(p.base(), variable) || containsVariable(p.exponent(), variable);
            case Func f -> containsVariable(f.argument(), variable);
        };
    }

    // ---- Rational helpers --------------------------------------------------

    private static Num numAdd(Num a, Num b) {
        return new Num(
                a.numerator().multiply(b.denominator()).add(b.numerator().multiply(a.denominator())),
                a.denominator().multiply(b.denominator()));
    }

    private static Num numMul(Num a, Num b) {
        return new Num(a.numerator().multiply(b.numerator()), a.denominator().multiply(b.denominator()));
    }

    private static Num numDiv(Num a, Num b) {
        if (b.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        return new Num(a.numerator().multiply(b.denominator()), a.denominator().multiply(b.numerator()));
    }

    private static Num numNeg(Num a) {
        return new Num(a.numerator().negate(), a.denominator());
    }
}
