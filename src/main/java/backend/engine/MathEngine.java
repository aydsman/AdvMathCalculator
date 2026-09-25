package backend.engine;

import backend.math.algebra.EquationSolver;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Inequalities;
import backend.math.algebra.Simplifier;
import backend.math.calculus.differential.Derivative;
import backend.math.calculus.differential.ImplicitDerivative;
import backend.math.calculus.integral.Antiderivative;
import backend.math.calculus.integral.RiemannSum;
import backend.math.calculus.limits.Limit;
import backend.math.functions.Composition;
import backend.math.functions.DomainRange;
import backend.math.functions.InverseFunction;
import backend.math.functions.Transformations;
import backend.math.trig.TrigEquationSolver;
import backend.math.trig.UnitCircle;
import backend.models.Question;
import backend.models.Result;
import backend.parser.ASTNode;
import backend.parser.Parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central switchboard: routes a {@link Question} to the {@link MathOperation}
 * registered under its operation key, then attaches and returns the {@link Result}.
 *
 * <p>The engine owns the shared concerns so individual operations don't have to:
 * parsing the raw input, normalizing it with {@link Simplifier} (except for the
 * simplify operation itself), handling unknown operations, and converting parse errors
 * or unexpected exceptions into a failed {@code Result}. Math modules register
 * themselves via {@link #register(String, MathOperation)}, keeping the engine
 * decoupled from any specific module.
 */
public class MathEngine {

    private final Map<String, MathOperation> operations = new HashMap<>();

    /** Registers (or replaces) the operation handled under {@code actionKey}. */
    public void register(String actionKey, MathOperation operation) {
        if (actionKey == null || operation == null) {
            throw new IllegalArgumentException("actionKey and operation must be non-null");
        }
        operations.put(actionKey, operation);
    }

    public boolean supports(String actionKey) {
        return operations.containsKey(actionKey);
    }

    public Set<String> registeredOperations() {
        return Set.copyOf(operations.keySet());
    }

    /**
     * Solves the question: looks up its operation, parses its input, and runs it.
     * The resulting {@link Result} is also stored on the question via
     * {@link Question#setResult(Result)}.
     */
    public Result solve(Question question) {
        if (question == null) {
            return Result.failure("No question provided");
        }

        MathOperation operation = operations.get(question.getOperation());
        if (operation == null) {
            return attach(question, Result.failure("Unknown operation: " + question.getOperation()));
        }

        Result result;
        try {
            result = route(question, operation);
        } catch (RuntimeException e) {
            result = Result.failure(
                    e.getMessage() != null ? e.getMessage() : "Error while solving");
        }
        if (result == null) {
            result = Result.failure("Operation produced no result");
        }
        return attach(question, result);
    }

    private static Result route(Question question, MathOperation operation) {
        String op = question.getOperation();
        String input = question.getInput();

        return switch (op) {
            case "inequalities" -> new Inequalities().solveFromInput(input);
            case "domainRange" -> new DomainRange().solveFromInput(input);
            case "composition" -> new Composition().solveFromInput(input);
            case "inverse" -> new InverseFunction().solveFromInput(input);
            case "transformations" -> new Transformations().solveFromInput(input);
            case "evaluate" -> new ExpressionEvaluator().solveFromInput(input);
            case "unitCircle" -> new UnitCircle().solveFromInput(input);
            case "trigEquation" -> new TrigEquationSolver().solveFromInput(input);
            case "derivative" -> new Derivative().solveFromInput(input);
            case "implicitDerivative" -> new ImplicitDerivative().solveFromInput(input);
            case "limit" -> new Limit().solveFromInput(input);
            case "riemannSum" -> new RiemannSum().solveFromInput(input);
            case "indefiniteIntegral" -> new Antiderivative().solveFromInput(input);
            default -> parseAndSolve(op, input, operation);
        };
    }

    private static Result parseAndSolve(String operationKey, String rawInput, MathOperation operation) {
        ASTNode input;
        if ("solveEquation".equals(operationKey)) {
            input = EquationSolver.toZeroForm(rawInput);
        } else {
            input = Parser.parse(rawInput);
        }

        if (!"simplify".equals(operationKey)) {
            input = Simplifier.simplify(input);
        }

        return operation.solve(input);
    }

    private static Result attach(Question question, Result result) {
        question.setResult(result);
        return result;
    }
}
