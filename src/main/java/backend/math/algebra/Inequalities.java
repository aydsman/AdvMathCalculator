package backend.math.algebra;

import backend.engine.MathOperation;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Solves single-variable inequalities in {@code x}: linear and quadratic,
 * returning interval-style answers.
 */
public class Inequalities implements MathOperation {

    public enum RelationalOp { LT, LE, GT, GE }

    private static final Num ZERO = Num.of(0);
    private static final String VAR = "x";

    public record ParsedInequality(ASTNode lhs, RelationalOp op, ASTNode rhs) {}

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a full inequality such as 2*x + 3 > 7.");
    }

    /** Parses and solves an inequality string such as {@code x^2 - 4 < 0}. */
    public Result solveFromInput(String raw) {
        List<Step> steps = new ArrayList<>();
        ParsedInequality parsed = parse(raw);
        steps.add(new Step("Original inequality",
                new Constant(formatInequality(parsed.lhs(), parsed.op(), parsed.rhs()))));

        ASTNode expr = Simplifier.simplify(new Add(parsed.lhs(), new Neg(parsed.rhs())));
        steps.add(new Step("Move all terms to one side", expr));

        Map<Integer, Num> poly = Factoring.coefficients(expr, VAR);
        if (poly == null) {
            return Result.failure("Only single-variable inequalities in x are supported for now.");
        }

        int degree = poly.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        RelationalOp op = parsed.op();

        if (degree == 0) {
            return solveConstant(poly.getOrDefault(0, ZERO), op, steps);
        }
        if (degree == 1) {
            return solveLinear(expr, poly, op, steps);
        }
        if (degree == 2) {
            return solveQuadratic(poly, expr, op, steps);
        }

        return Result.failure("Inequalities of degree " + degree + " are not supported yet.");
    }

    public static ParsedInequality parse(String input) {
        String s = input.trim();
        int idx = -1;
        RelationalOp op = null;

        if ((idx = s.indexOf("<=")) >= 0) {
            op = RelationalOp.LE;
        } else if ((idx = s.indexOf(">=")) >= 0) {
            op = RelationalOp.GE;
        } else if ((idx = s.indexOf("<")) >= 0) {
            op = RelationalOp.LT;
        } else if ((idx = s.indexOf(">")) >= 0) {
            op = RelationalOp.GT;
        }

        if (op == null) {
            throw new Parser.ParseException("Expected an inequality operator (<, >, <=, >=)");
        }

        ASTNode lhs = Parser.parse(s.substring(0, idx).trim());
        int opLen = op == RelationalOp.LE || op == RelationalOp.GE ? 2 : 1;
        ASTNode rhs = Parser.parse(s.substring(idx + opLen).trim());
        return new ParsedInequality(lhs, op, rhs);
    }

    private static Result solveConstant(Num value, RelationalOp op, List<Step> steps) {
        boolean holds = satisfiesZeroComparison(value, op);
        String answer = holds ? "All real numbers" : "No solution";
        steps.add(new Step(holds ? "The inequality is always true" : "The inequality is never true"));
        return Result.of(answerConstant(answer), null, steps);
    }

    private static Result solveLinear(ASTNode expr, Map<Integer, Num> poly, RelationalOp op, List<Step> steps) {
        Num a = poly.getOrDefault(1, ZERO);
        Num b = poly.getOrDefault(0, ZERO);
        if (a.isZero()) {
            return solveConstant(b, op, steps);
        }

        ASTNode boundary = EquationSolver.computeRealRoots(expr).getFirst();

        boolean aPositive = a.numerator().signum() > 0;
        String answer = formatLinearSolution(op, aPositive, boundary);
        steps.add(new Step("Critical value: x = " + boundary.toDisplay(), boundary));
        steps.add(new Step("Apply the inequality (flip direction if dividing by a negative)", answerConstant(answer)));
        return Result.of(answerConstant(answer), null, steps);
    }

    private static Result solveQuadratic(Map<Integer, Num> poly, ASTNode expr, RelationalOp op, List<Step> steps) {
        Num a = poly.getOrDefault(2, ZERO);
        Num b = poly.getOrDefault(1, ZERO);
        Num c = poly.getOrDefault(0, ZERO);

        if (a.isZero()) {
            return solveLinear(expr, poly, op, steps);
        }

        List<ASTNode> roots = EquationSolver.computeRealRoots(expr);
        roots = new ArrayList<>(roots);
        roots.sort(Comparator.comparingDouble(Inequalities::approximate));

        if (roots.isEmpty()) {
            steps.add(new Step("The quadratic has no real roots — sign is constant"));
            boolean positiveLeading = a.numerator().signum() > 0;
            String answer = intervalNoRoots(op, positiveLeading);
            if ("No solution".equals(answer)) {
                steps.add(new Step("The parabola never satisfies the inequality"));
            } else {
                steps.add(new Step("The parabola always satisfies the inequality"));
            }
            return Result.of(answerConstant(answer), null, steps);
        }

        if (roots.size() == 1) {
            ASTNode r = roots.getFirst();
            steps.add(new Step("Repeated root at x = " + r.toDisplay(), r));
            boolean positiveLeading = a.numerator().signum() > 0;
            String answer = intervalOneRoot(op, positiveLeading, r);
            steps.add(new Step("Use a sign chart with the repeated root", answerConstant(answer)));
            return Result.of(answerConstant(answer), null, steps);
        }

        ASTNode r1 = roots.get(0);
        ASTNode r2 = roots.get(1);
        steps.add(new Step("Roots at x = " + r1.toDisplay() + " and x = " + r2.toDisplay()));
        boolean positiveLeading = a.numerator().signum() > 0;
        String answer = intervalTwoRoots(op, positiveLeading, r1, r2);
        steps.add(new Step("Use a sign chart between the roots", answerConstant(answer)));
        return Result.of(answerConstant(answer), null, steps);
    }

    /** expr op 0  ↔  0 flipOp expr */
    private static boolean satisfiesComparison(Num left, Num right, RelationalOp op) {
        int cmp = left.numerator().multiply(right.denominator())
                .compareTo(right.numerator().multiply(left.denominator()));
        return switch (op) {
            case LT -> cmp < 0;
            case LE -> cmp <= 0;
            case GT -> cmp > 0;
            case GE -> cmp >= 0;
        };
    }

    private static boolean satisfiesZeroComparison(Num value, RelationalOp op) {
        return satisfiesComparison(value, ZERO, op);
    }

    private static String formatLinearSolution(RelationalOp op, boolean aPositive, ASTNode boundary) {
        String b = boundary.toDisplay();
        boolean wantGreater = op == RelationalOp.GT || op == RelationalOp.GE;
        boolean strict = op == RelationalOp.GT || op == RelationalOp.LT;
        if (!aPositive) {
            wantGreater = !wantGreater;
        }
        String symbol = wantGreater ? (strict ? ">" : ">=") : (strict ? "<" : "<=");
        return "x " + symbol + " " + b;
    }

    private static String intervalNoRoots(RelationalOp op, boolean opensUp) {
        boolean wantBelow = op == RelationalOp.LT || op == RelationalOp.LE;
        if (opensUp) {
            return wantBelow ? "No solution" : "All real numbers";
        }
        return wantBelow ? "All real numbers" : "No solution";
    }

    private static String intervalOneRoot(RelationalOp op, boolean opensUp, ASTNode root) {
        String r = root.toDisplay();
        boolean strictBelow = op == RelationalOp.LT;
        boolean weakBelow = op == RelationalOp.LE;
        boolean strictAbove = op == RelationalOp.GT;
        boolean weakAbove = op == RelationalOp.GE;

        if (opensUp) {
            if (strictBelow) {
                return "No solution";
            }
            if (weakBelow) {
                return "x = " + r;
            }
            if (strictAbove) {
                return "x != " + r;
            }
            return "All real numbers";
        }
        if (strictAbove) {
            return "No solution";
        }
        if (weakAbove) {
            return "x = " + r;
        }
        if (strictBelow) {
            return "x != " + r;
        }
        return "All real numbers";
    }

    private static String intervalTwoRoots(RelationalOp op, boolean opensUp,
                                           ASTNode r1, ASTNode r2) {
        String a = r1.toDisplay();
        String b = r2.toDisplay();
        boolean strictBelow = op == RelationalOp.LT;
        boolean weakBelow = op == RelationalOp.LE;
        boolean strictAbove = op == RelationalOp.GT;
        boolean weakAbove = op == RelationalOp.GE;

        if (opensUp) {
            if (strictBelow) {
                return "(" + a + ", " + b + ")";
            }
            if (weakBelow) {
                return "[" + a + ", " + b + "]";
            }
            if (strictAbove) {
                return "(-inf, " + a + ") U (" + b + ", inf)";
            }
            return "(-inf, " + a + "] U [" + b + ", inf)";
        }
        if (strictAbove) {
            return "(" + a + ", " + b + ")";
        }
        if (weakAbove) {
            return "[" + a + ", " + b + "]";
        }
        if (strictBelow) {
            return "(-inf, " + a + ") U (" + b + ", inf)";
        }
        return "(-inf, " + a + "] U [" + b + ", inf)";
    }

    private static String formatInequality(ASTNode lhs, RelationalOp op, ASTNode rhs) {
        return lhs.toDisplay() + " " + symbol(op) + " " + rhs.toDisplay();
    }

    private static String symbol(RelationalOp op) {
        return switch (op) {
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
        };
    }

    private static ASTNode answerConstant(String text) {
        return new Constant(text);
    }

    private static double approximate(ASTNode node) {
        return switch (node) {
            case Num n -> n.toDouble();
            case Neg n -> -approximate(n.operand());
            case backend.parser.ASTNode.Func f when "sqrt".equals(f.name()) && f.argument() instanceof Num n ->
                    Math.sqrt(n.toDouble());
            case backend.parser.ASTNode.Mul m -> approximate(m.left()) * approximate(m.right());
            case backend.parser.ASTNode.Div d -> approximate(d.left()) / approximate(d.right());
            default -> 0;
        };
    }
}
