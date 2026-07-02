package backend.math.trig;

import backend.engine.MathOperation;
import backend.math.algebra.EquationSolver;
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
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Solves basic trigonometric equations such as {@code sin(x) = 1/2} or {@code 2*cos(x) - 1 = 0}.
 */
public class TrigEquationSolver implements MathOperation {

    private static final String VAR = "x";
    private static final Num ZERO = Num.of(0);
    private static final Num ONE = Num.of(1);
    private static final Num HALF = Num.of(1, 2);

    @Override
    public Result solve(ASTNode input) {
        return solveZeroForm(input);
    }

    public Result solveFromInput(String raw) {
        return solveZeroForm(EquationSolver.toZeroForm(raw));
    }

    private Result solveZeroForm(ASTNode zeroForm) {
        List<Step> steps = new ArrayList<>();
        zeroForm = Simplifier.simplify(zeroForm);
        steps.add(new Step("Standard form f(x) = 0", zeroForm));

        Optional<TrigEquation> equation = extractEquation(zeroForm);
        if (equation.isEmpty()) {
            return Result.failure(
                    "Supported forms: sin(x) = k, cos(x) = k, tan(x) = k, or a*trig(x) + b = 0");
        }

        TrigEquation eq = equation.get();
        if (!isSimpleVariableArgument(eq.argument())) {
            return Result.failure("Only equations in x (such as sin(x) = 1/2) are supported for now.");
        }

        steps.add(new Step("Isolate " + eq.function() + "(x)", eq.rhs()));
        return switch (eq.function()) {
            case "sin" -> solveSin(eq.rhs(), steps);
            case "cos" -> solveCos(eq.rhs(), steps);
            case "tan" -> solveTan(eq.rhs(), steps);
            default -> Result.failure("Unsupported trig function: " + eq.function());
        };
    }

    private static Result solveSin(ASTNode rhs, List<Step> steps) {
        rhs = Simplifier.simplify(rhs);
        if (rhs instanceof Num n && n.equals(HALF)) {
            steps.add(new Step("Reference angle pi/6 on the unit circle"));
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 6), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(5, 6), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isZero(rhs)) {
            steps.add(new Step("sin(x) = 0 when x is an integer multiple of pi"));
            return Result.ofSolutions(List.of(TrigUtils.generalSolution(ZERO, TrigUtils.PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(ONE)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(1, 2), TrigUtils.TWO_PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(negNum(ONE))) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(3, 2), TrigUtils.TWO_PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(negNum(HALF))) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(7, 6), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(11, 6), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isSqrtThreeOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 3), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(2, 3), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isNegSqrtThreeOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(4, 3), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(5, 3), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isSqrtTwoOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 4), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(3, 4), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isNegSqrtTwoOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(5, 4), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(7, 4), TrigUtils.TWO_PI)),
                    steps);
        }
        return Result.failure("No exact solution template for sin(x) = " + rhs.toDisplay());
    }

    private static Result solveCos(ASTNode rhs, List<Step> steps) {
        rhs = Simplifier.simplify(rhs);
        if (rhs instanceof Num n && n.equals(HALF)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 3), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(5, 3), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isZero(rhs)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(1, 2), TrigUtils.PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(ONE)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(ZERO, TrigUtils.TWO_PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(negNum(ONE))) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.PI, TrigUtils.TWO_PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(negNum(HALF))) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(2, 3), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(4, 3), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isSqrtThreeOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 6), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(11, 6), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isNegSqrtThreeOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(5, 6), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(7, 6), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isSqrtTwoOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(1, 4), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(7, 4), TrigUtils.TWO_PI)),
                    steps);
        }
        if (isNegSqrtTwoOverTwo(rhs)) {
            return Result.ofSolutions(
                    List.of(
                            generalSolution(TrigUtils.piFraction(3, 4), TrigUtils.TWO_PI),
                            generalSolution(TrigUtils.piFraction(5, 4), TrigUtils.TWO_PI)),
                    steps);
        }
        return Result.failure("No exact solution template for cos(x) = " + rhs.toDisplay());
    }

    private static Result solveTan(ASTNode rhs, List<Step> steps) {
        rhs = Simplifier.simplify(rhs);
        if (isZero(rhs)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(ZERO, TrigUtils.PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(ONE)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(1, 4), TrigUtils.PI)), steps);
        }
        if (rhs instanceof Num n && n.equals(negNum(ONE))) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(3, 4), TrigUtils.PI)), steps);
        }
        if (isSqrtThree(rhs)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(1, 3), TrigUtils.PI)), steps);
        }
        if (isNegSqrtThree(rhs)) {
            return Result.ofSolutions(
                    List.of(TrigUtils.generalSolution(TrigUtils.piFraction(2, 3), TrigUtils.PI)), steps);
        }
        return Result.failure("No exact solution template for tan(x) = " + rhs.toDisplay());
    }

    private static ASTNode generalSolution(ASTNode principal, ASTNode period) {
        return TrigUtils.generalSolution(principal, period);
    }

    private record TrigEquation(String function, ASTNode argument, ASTNode rhs) {}

    private static Optional<TrigEquation> extractEquation(ASTNode zeroForm) {
        zeroForm = Simplifier.simplify(zeroForm);

        Optional<TrigEquation> scaled = fromScaledTrigSum(zeroForm);
        if (scaled.isPresent()) {
            return scaled;
        }

        if (zeroForm instanceof Func f && TrigUtils.isTrigFunction(f.name())) {
            return Optional.of(new TrigEquation(f.name().toLowerCase(Locale.ROOT), f.argument(), ZERO));
        }

        if (zeroForm instanceof Sub s) {
            Optional<TrigEquation> eq = fromScaledTrigEqual(s.left(), s.right());
            if (eq.isPresent()) {
                return eq;
            }
            return asTrigEqualsValue(s.left(), s.right());
        }

        if (zeroForm instanceof Add a) {
            Optional<TrigEquation> direct = asTrigEqualsValue(a.left(), a.right());
            if (direct.isPresent()) {
                return direct;
            }
            direct = asTrigEqualsValue(a.right(), a.left());
            if (direct.isPresent()) {
                return direct;
            }
        }
        return Optional.empty();
    }

    private static Optional<TrigEquation> fromScaledTrigSum(ASTNode zeroForm) {
        if (zeroForm instanceof Add a) {
            Optional<TrigEquation> eq = fromScaledTrigOffset(a.left(), a.right());
            if (eq.isPresent()) {
                return eq;
            }
            return fromScaledTrigOffset(a.right(), a.left());
        }
        return Optional.empty();
    }

    /** {@code coeff*trig + constant = 0}. */
    private static Optional<TrigEquation> fromScaledTrigOffset(ASTNode trigSide, ASTNode constantSide) {
        constantSide = Simplifier.simplify(constantSide);
        if (!(trigSide instanceof Mul m) || !(m.left() instanceof Num coeff) || coeff.isZero()) {
            return Optional.empty();
        }
        if (!(m.right() instanceof Func f) || !TrigUtils.isTrigFunction(f.name())) {
            return Optional.empty();
        }
        ASTNode rhs = Simplifier.simplify(new Div(new Neg(constantSide), coeff));
        return Optional.of(new TrigEquation(f.name().toLowerCase(Locale.ROOT), f.argument(), rhs));
    }

    /** {@code coeff*trig - constant = 0}. */
    private static Optional<TrigEquation> fromScaledTrigEqual(ASTNode trigSide, ASTNode constantSide) {
        constantSide = Simplifier.simplify(constantSide);
        if (!(trigSide instanceof Mul m) || !(m.left() instanceof Num coeff) || coeff.isZero()) {
            return Optional.empty();
        }
        if (!(m.right() instanceof Func f) || !TrigUtils.isTrigFunction(f.name())) {
            return Optional.empty();
        }
        ASTNode rhs = Simplifier.simplify(new Div(constantSide, coeff));
        return Optional.of(new TrigEquation(f.name().toLowerCase(Locale.ROOT), f.argument(), rhs));
    }

    private static Optional<TrigEquation> fromScaledTrig(ASTNode trigSide, ASTNode constantSide) {
        return fromScaledTrigOffset(trigSide, constantSide);
    }

    private static Optional<TrigEquation> asTrigEqualsValue(ASTNode lhs, ASTNode rhs) {
        lhs = Simplifier.simplify(lhs);
        rhs = Simplifier.simplify(rhs);

        if (lhs instanceof Func f && TrigUtils.isTrigFunction(f.name()) && isZero(rhs)) {
            return Optional.of(new TrigEquation(f.name().toLowerCase(Locale.ROOT), f.argument(), ZERO));
        }

        if (lhs instanceof Func f && TrigUtils.isTrigFunction(f.name())) {
            return Optional.of(new TrigEquation(f.name().toLowerCase(Locale.ROOT), f.argument(), rhs));
        }

        if (lhs instanceof Mul m && m.left() instanceof Num coeff && m.right() instanceof Func f
                && TrigUtils.isTrigFunction(f.name()) && rhs instanceof Num n && !coeff.isZero()) {
            Num value = Simplifier.simplify(new Div(new Neg(n), coeff)) instanceof Num num ? num : null;
            if (value != null) {
                return Optional.of(new TrigEquation(
                        f.name().toLowerCase(Locale.ROOT), f.argument(), value));
            }
        }

        if (lhs instanceof Add a && a.right() instanceof Num n) {
            if (a.left() instanceof Func f && TrigUtils.isTrigFunction(f.name())) {
                Num value = Simplifier.simplify(new Neg(n)) instanceof Num num ? num : null;
                if (value != null) {
                    return Optional.of(new TrigEquation(
                            f.name().toLowerCase(Locale.ROOT), f.argument(), value));
                }
            }
            if (a.left() instanceof Mul m && m.left() instanceof Num coeff && m.right() instanceof Func f
                    && TrigUtils.isTrigFunction(f.name()) && !coeff.isZero()) {
                Num value = Simplifier.simplify(new Div(new Neg(n), coeff)) instanceof Num num ? num : null;
                if (value != null) {
                    return Optional.of(new TrigEquation(
                            f.name().toLowerCase(Locale.ROOT), f.argument(), value));
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isSimpleVariableArgument(ASTNode argument) {
        argument = Simplifier.simplify(argument);
        return argument instanceof Var v && v.name().equals(VAR);
    }

    private static boolean isZero(ASTNode node) {
        return node instanceof Num n && n.isZero();
    }

    private static Num negNum(Num n) {
        return new Num(n.numerator().negate(), n.denominator());
    }

    private static boolean isSqrtThreeOverTwo(ASTNode node) {
        return node instanceof Div d && d.right() instanceof Num den && den.numerator().intValueExact() == 2
                && d.left() instanceof Func f && "sqrt".equals(f.name())
                && f.argument() instanceof Num n && n.numerator().intValueExact() == 3;
    }

    private static boolean isNegSqrtThreeOverTwo(ASTNode node) {
        return node instanceof Neg n && isSqrtThreeOverTwo(n.operand());
    }

    private static boolean isSqrtTwoOverTwo(ASTNode node) {
        return node instanceof Div d && d.right() instanceof Num den && den.numerator().intValueExact() == 2
                && d.left() instanceof Func f && "sqrt".equals(f.name())
                && f.argument() instanceof Num n && n.numerator().intValueExact() == 2;
    }

    private static boolean isNegSqrtTwoOverTwo(ASTNode node) {
        return node instanceof Neg n && isSqrtTwoOverTwo(n.operand());
    }

    private static boolean isSqrtThree(ASTNode node) {
        return node instanceof Func f && "sqrt".equals(f.name())
                && f.argument() instanceof Num n && n.numerator().intValueExact() == 3;
    }

    private static boolean isNegSqrtThree(ASTNode node) {
        return node instanceof Neg n && isSqrtThree(n.operand());
    }
}
