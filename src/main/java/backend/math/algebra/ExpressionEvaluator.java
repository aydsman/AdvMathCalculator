package backend.math.algebra;

import backend.engine.MathOperation;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes a numeric value for a variable and evaluates the expression.
 */
public class ExpressionEvaluator implements MathOperation {

    private static final String DEFAULT_VAR = "x";
    private static final Pattern AT_X = Pattern.compile("@\\s*" + DEFAULT_VAR + "\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMA_X = Pattern.compile(",\\s*" + DEFAULT_VAR + "\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public Result solve(ASTNode input) {
        return Result.of(Simplifier.simplify(input), null, List.of());
    }

    /** Parses {@code expr}, {@code expr @ x=2}, or {@code expr, x=2}. */
    public Result solveFromInput(String raw) {
        ParsedInput parsed = parseInput(raw);
        ASTNode body = Simplifier.simplify(parsed.expression().body());
        if (parsed.xValue() != null) {
            return evaluateAt(body, parsed.expression().variable(), parsed.xValue());
        }
        return solve(body);
    }

    public static FunctionUtils.FunctionDef expressionFromInput(String raw) {
        return parseInput(raw).expression();
    }

    private static ParsedInput parseInput(String raw) {
        String trimmed = raw.trim();
        String xValue = null;
        String exprPart = trimmed;

        Matcher at = AT_X.matcher(trimmed);
        if (at.find()) {
            xValue = at.group(1).trim();
            exprPart = trimmed.substring(0, at.start()).trim();
        } else {
            Matcher comma = COMMA_X.matcher(trimmed);
            if (comma.find()) {
                xValue = comma.group(1).trim();
                exprPart = trimmed.substring(0, comma.start()).trim();
            }
        }

        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(exprPart, "f", DEFAULT_VAR);
        return new ParsedInput(def, xValue);
    }

    private record ParsedInput(FunctionUtils.FunctionDef expression, String xValue) {}

    public Result evaluateAt(ASTNode expression, String variable, String xValueText) {
        List<Step> steps = new ArrayList<>();
        ASTNode simplified = Simplifier.simplify(expression);
        steps.add(new Step("Expression", simplified));

        Num xValue = parseNumeric(xValueText);
        steps.add(new Step("Substitute " + variable + " = " + xValue.toDisplay(), xValue));

        ASTNode substituted = FunctionUtils.substitute(simplified, variable, xValue);
        substituted = Simplifier.simplify(substituted);
        steps.add(new Step("Evaluate", substituted));

        try {
            Num exact = evaluateNumeric(substituted);
            Double decimal = exact.denominator().equals(BigInteger.ONE) ? null : exact.toDouble();
            return Result.of(exact, decimal, steps);
        } catch (ApproximateEvaluationException e) {
            return Result.of(substituted, e.value, steps);
        }
    }

    /** Pulls the expression to evaluate from a prior question's input and result. */
    public static ASTNode expressionFromQuestion(String input, Result priorResult, String operation) {
        if ("evaluate".equals(operation)) {
            return Simplifier.simplify(expressionFromInput(input).body());
        }
        if (priorResult != null && priorResult.isSuccess() && priorResult.getExact() != null) {
            return priorResult.getExact();
        }
        return Simplifier.simplify(expressionFromInput(input).body());
    }

    public static Num parseNumeric(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter a value for x");
        }
        ASTNode parsed = Simplifier.simplify(Parser.parse(text.trim()));
        if (!(parsed instanceof Num num)) {
            throw new IllegalArgumentException("x must be a number, got: " + parsed.toDisplay());
        }
        return num;
    }

    /** Exact rational evaluation, or a decimal when the value is not rational (e.g. trig). */
    public static NumericEvaluation evaluate(ASTNode node) {
        try {
            return NumericEvaluation.exact(evaluateNumeric(node));
        } catch (ApproximateEvaluationException e) {
            return NumericEvaluation.approximate(e.value);
        }
    }

    public record NumericEvaluation(Num exact, Double approximate) {
        public static NumericEvaluation exact(Num value) {
            return new NumericEvaluation(value, null);
        }

        public static NumericEvaluation approximate(double value) {
            return new NumericEvaluation(null, value);
        }

        public boolean hasExact() {
            return exact != null;
        }
    }

    public static Num evaluateNumeric(ASTNode node) {
        return switch (node) {
            case Num n -> n;
            case Var v -> throw new IllegalArgumentException("Undefined variable: " + v.name());
            case Constant c -> throw new IllegalArgumentException("Cannot evaluate constant: " + c.name());
            case Neg n -> numNeg(evaluateNumeric(n.operand()));
            case Add a -> numAdd(evaluateNumeric(a.left()), evaluateNumeric(a.right()));
            case Sub s -> numSub(evaluateNumeric(s.left()), evaluateNumeric(s.right()));
            case Mul m -> numMul(evaluateNumeric(m.left()), evaluateNumeric(m.right()));
            case Div d -> {
                Num divisor = evaluateNumeric(d.right());
                if (divisor.isZero()) {
                    throw new ArithmeticException("Division by zero");
                }
                yield numDiv(evaluateNumeric(d.left()), divisor);
            }
            case Pow p -> evaluatePow(evaluateNumeric(p.base()), p.exponent());
            case Func f -> evaluateFunction(f.name(), evaluateNumeric(f.argument()));
        };
    }

    /** Floating-point evaluation for graphing and other numeric sampling. */
    public static double evaluateAtDouble(ASTNode expression, String variable, double xValue) {
        return evaluateDouble(Simplifier.simplify(expression), variable, xValue);
    }

    private static double evaluateDouble(ASTNode node, String variable, double xValue) {
        return switch (node) {
            case Num n -> n.toDouble();
            case Var v -> {
                if (v.name().equals(variable)) {
                    yield xValue;
                }
                throw new IllegalArgumentException("Undefined variable: " + v.name());
            }
            case Constant c -> switch (c.name()) {
                case "PI" -> Math.PI;
                case "E" -> Math.E;
                default -> throw new IllegalArgumentException("Cannot evaluate constant: " + c.name());
            };
            case Neg n -> -evaluateDouble(n.operand(), variable, xValue);
            case Add a -> evaluateDouble(a.left(), variable, xValue) + evaluateDouble(a.right(), variable, xValue);
            case Sub s -> evaluateDouble(s.left(), variable, xValue) - evaluateDouble(s.right(), variable, xValue);
            case Mul m -> evaluateDouble(m.left(), variable, xValue) * evaluateDouble(m.right(), variable, xValue);
            case Div d -> {
                double divisor = evaluateDouble(d.right(), variable, xValue);
                if (divisor == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield evaluateDouble(d.left(), variable, xValue) / divisor;
            }
            case Pow p -> Math.pow(
                    evaluateDouble(p.base(), variable, xValue),
                    evaluateDouble(p.exponent(), variable, xValue));
            case Func f -> evaluateFunctionDouble(f.name(), evaluateDouble(f.argument(), variable, xValue));
        };
    }

    private static double evaluateFunctionDouble(String name, double argument) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sin" -> Math.sin(argument);
            case "cos" -> Math.cos(argument);
            case "tan" -> Math.tan(argument);
            case "sec" -> 1 / Math.cos(argument);
            case "csc" -> 1 / Math.sin(argument);
            case "cot" -> 1 / Math.tan(argument);
            case "abs" -> Math.abs(argument);
            case "sqrt" -> {
                if (argument < 0) {
                    throw new ArithmeticException("Square root of a negative number");
                }
                yield Math.sqrt(argument);
            }
            case "ln", "log" -> {
                if (argument <= 0) {
                    throw new ArithmeticException("Logarithm requires a positive argument");
                }
                yield Math.log(argument);
            }
            default -> throw new IllegalArgumentException("Cannot evaluate function: " + name);
        };
    }

    private static Num evaluatePow(Num base, ASTNode exponentNode) {
        if (exponentNode instanceof Num expNum && expNum.isInteger()) {
            int exp = expNum.numerator().intValueExact();
            if (Math.abs(exp) <= 16) {
                return numIntPow(base, exp);
            }
        }
        double value = Math.pow(base.toDouble(), evaluateNumeric(exponentNode).toDouble());
        throw new ApproximateEvaluationException(value);
    }

    private static Num evaluateFunction(String name, Num argument) {
        if ("abs".equalsIgnoreCase(name)) {
            return argument.numerator().signum() < 0
                    ? new Num(argument.numerator().negate(), argument.denominator())
                    : argument;
        }
        double x = argument.toDouble();
        double value = switch (name.toLowerCase(Locale.ROOT)) {
            case "sin" -> Math.sin(x);
            case "cos" -> Math.cos(x);
            case "tan" -> Math.tan(x);
            case "sqrt" -> {
                if (x < 0) {
                    throw new ArithmeticException("Square root of a negative number");
                }
                yield Math.sqrt(x);
            }
            case "ln", "log" -> {
                if (x <= 0) {
                    throw new ArithmeticException("Logarithm requires a positive argument");
                }
                yield Math.log(x);
            }
            default -> throw new IllegalArgumentException("Cannot evaluate function: " + name);
        };
        throw new ApproximateEvaluationException(value);
    }

    private static Num numIntPow(Num base, int exp) {
        if (exp == 0) {
            return Num.of(1);
        }
        BigInteger num = base.numerator().pow(Math.abs(exp));
        BigInteger den = base.denominator().pow(Math.abs(exp));
        if (exp < 0) {
            return new Num(den, num);
        }
        return new Num(num, den);
    }

    private static Num numAdd(Num a, Num b) {
        return new Num(
                a.numerator().multiply(b.denominator()).add(b.numerator().multiply(a.denominator())),
                a.denominator().multiply(b.denominator()));
    }

    private static Num numSub(Num a, Num b) {
        return new Num(
                a.numerator().multiply(b.denominator()).subtract(b.numerator().multiply(a.denominator())),
                a.denominator().multiply(b.denominator()));
    }

    private static Num numMul(Num a, Num b) {
        return new Num(a.numerator().multiply(b.numerator()), a.denominator().multiply(b.denominator()));
    }

    private static Num numDiv(Num a, Num b) {
        return new Num(a.numerator().multiply(b.denominator()), a.denominator().multiply(b.numerator()));
    }

    private static Num numNeg(Num a) {
        return new Num(a.numerator().negate(), a.denominator());
    }

    private static final class ApproximateEvaluationException extends RuntimeException {
        private final double value;

        private ApproximateEvaluationException(double value) {
            this.value = value;
        }
    }
}
