package backend.math.functions;

import backend.engine.MathOperation;
import backend.math.algebra.Factoring;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the domain and range of {@code f(x)} from a function definition or expression.
 */
public class DomainRange implements MathOperation {

    private static final String VAR = "x";

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a function such as f(x) = x^2 + 1 or sqrt(x).");
    }

    public Result solveFromInput(String raw) {
        List<Step> steps = new ArrayList<>();
        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(raw, "f", VAR);
        ASTNode body = Simplifier.simplify(def.body());

        steps.add(new Step("Function", new Constant(def.name() + "(" + def.variable() + ") = " + body.toDisplay())));

        String domain = findDomain(body, steps);
        String range = findRange(body, steps);

        steps.add(new Step("Domain", new Constant(domain)));
        steps.add(new Step("Range", new Constant(range)));

        return Result.ofSolutions(
                List.of(new Constant("Domain: " + domain), new Constant("Range: " + range)),
                steps);
    }

    private static String findDomain(ASTNode body, List<Step> steps) {
        List<String> restrictions = new ArrayList<>();
        FunctionUtils.collectDomainRestrictions(body, restrictions);
        Set<String> unique = new LinkedHashSet<>(restrictions);
        if (unique.isEmpty()) {
            steps.add(new Step("No variable restrictions — all real numbers are allowed"));
            return "All real numbers";
        }
        steps.add(new Step("Apply domain restrictions from denominators and even roots"));
        return String.join(", and ", unique);
    }

    private static String findRange(ASTNode body, List<Step> steps) {
        body = Simplifier.simplify(body);

        if (body instanceof Func f) {
            return rangeOfFunction(f, steps);
        }

        if (body instanceof backend.parser.ASTNode.Div d) {
            steps.add(new Step("Rational function — range excludes values the function never reaches"));
            if (d.left() instanceof Num n && !n.isZero()) {
                if (d.right() instanceof Var v && v.name().equals(VAR)) {
                    return "All real numbers except 0";
                }
                if (d.right() instanceof backend.parser.ASTNode.Sub s
                        && s.left() instanceof Var v && v.name().equals(VAR)
                        && s.right() instanceof Num) {
                    return "All real numbers except 0";
                }
            }
            return "Unable to determine";
        }

        String powerRange = rangeOfPowerExpression(body, steps);
        if (powerRange != null) {
            return powerRange;
        }

        Map<Integer, Num> poly = Factoring.coefficients(body, VAR);
        if (poly != null && isPolynomialIn(body, VAR)) {
            return rangeOfPolynomial(poly, steps);
        }

        steps.add(new Step("Range could not be determined symbolically for this form"));
        return "Unable to determine";
    }

    private static String rangeOfFunction(Func f, List<Step> steps) {
        return switch (f.name()) {
            case "sin", "cos", "sec", "csc" -> {
                steps.add(new Step("Standard trigonometric range"));
                yield "[-1, 1]";
            }
            case "tan", "cot" -> {
                steps.add(new Step("Standard trigonometric range"));
                yield "All real numbers";
            }
            case "sqrt" -> {
                steps.add(new Step("Square root outputs are never negative"));
                yield "[0, inf)";
            }
            case "ln", "log" -> {
                steps.add(new Step("Logarithm outputs are all real numbers"));
                yield "All real numbers";
            }
            default -> "Unable to determine";
        };
    }

    /** Handles {@code (x - h)^n}, {@code (ln(x) - 1)^3}, {@code 2*(x - h)^2}, etc. */
    private static String rangeOfPowerExpression(ASTNode body, List<Step> steps) {
        Num scale = Num.of(1);
        ASTNode core = body;

        if (body instanceof Neg neg) {
            scale = Num.of(-1);
            core = neg.operand();
        } else if (body instanceof Mul m && m.left() instanceof Num n) {
            scale = n;
            core = m.right();
        }

        if (!(core instanceof Pow p) || !(p.exponent() instanceof Num expNum) || !expNum.isInteger()) {
            return null;
        }

        int exp = expNum.numerator().intValueExact();
        if (exp == 0) {
            steps.add(new Step("Constant power"));
            return "{1}";
        }

        boolean linearBase = isLinearInX(p.base());
        boolean unboundedBase = spansAllRealNumbers(p.base());
        if (!linearBase && !unboundedBase) {
            return null;
        }

        if (exp % 2 == 1) {
            steps.add(new Step("Odd power of an expression that spans all real numbers"));
            return "All real numbers";
        }

        steps.add(new Step("Even power — outputs are never negative (times a scale)"));
        return scale.numerator().signum() > 0 ? "[0, inf)" : "(-inf, 0]";
    }

    private static boolean isLinearInX(ASTNode node) {
        Map<Integer, Num> poly = Factoring.coefficients(node, VAR);
        return poly != null && poly.containsKey(1) && !poly.containsKey(2);
    }

    /** Whether the expression can take on every real value (on its natural domain). */
    private static boolean spansAllRealNumbers(ASTNode node) {
        return switch (node) {
            case Var v -> v.name().equals(VAR);
            case Num n -> false;
            case Neg n -> spansAllRealNumbers(n.operand());
            case Add a -> spansAllRealNumbers(a.left()) && spansAllRealNumbers(a.right());
            case Sub s -> spansAllRealNumbers(s.left()) && s.right() instanceof Num;
            case Mul m -> false;
            case backend.parser.ASTNode.Div d -> false;
            case Pow p -> false;
            case Func f -> switch (f.name()) {
                case "ln", "log", "tan", "cot" -> true;
                default -> false;
            };
            case Constant c -> false;
            default -> false;
        };
    }

    /** True only when the tree is a polynomial in {@code var} (no function calls). */
    private static boolean isPolynomialIn(ASTNode node, String var) {
        return switch (node) {
            case Num n -> true;
            case Var v -> v.name().equals(var);
            case Neg n -> isPolynomialIn(n.operand(), var);
            case Add a -> isPolynomialIn(a.left(), var) && isPolynomialIn(a.right(), var);
            case Sub s -> isPolynomialIn(s.left(), var) && isPolynomialIn(s.right(), var);
            case Mul m -> isPolynomialIn(m.left(), var) && isPolynomialIn(m.right(), var);
            case Pow p -> isPolynomialIn(p.base(), var)
                    && p.exponent() instanceof Num n && n.isInteger() && !n.isZero();
            case Func f -> false;
            case Constant c -> true;
            case backend.parser.ASTNode.Div d -> false;
            default -> false;
        };
    }

    private static String rangeOfPolynomial(Map<Integer, Num> poly, List<Step> steps) {
        int degree = poly.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        if (degree == 0) {
            Num c = poly.getOrDefault(0, Num.of(0));
            steps.add(new Step("Constant function"));
            return "{" + c.toDisplay() + "}";
        }

        if (degree == 1) {
            steps.add(new Step("Non-constant linear functions cover all real numbers"));
            return "All real numbers";
        }

        if (degree == 2) {
            Num a = poly.getOrDefault(2, Num.of(0));
            Num b = poly.getOrDefault(1, Num.of(0));
            Num c = poly.getOrDefault(0, Num.of(0));
            if (!a.isInteger() || !b.isInteger() || !c.isInteger()) {
                return "Unable to determine";
            }
            int ai = a.numerator().intValueExact();
            int bi = b.numerator().intValueExact();
            int ci = c.numerator().intValueExact();
            double vertexY = ai * Math.pow(-bi / (2.0 * ai), 2) + bi * (-bi / (2.0 * ai)) + ci;
            steps.add(new Step("Quadratic range is bounded by the vertex y-value"));
            if (ai > 0) {
                return "[" + formatNum(vertexY) + ", inf)";
            }
            return "(-inf, " + formatNum(vertexY) + "]";
        }

        if (degree == 3) {
            steps.add(new Step("Cubic polynomials with real coefficients have range all real numbers"));
            return "All real numbers";
        }

        return "Unable to determine";
    }

    private static String formatNum(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.4g", value);
    }
}
