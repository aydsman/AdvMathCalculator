package backend.engine;

import backend.models.Question;
import backend.models.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches a {@link Question} to the {@link MathOperation} registered under its
 * action key and stores the {@link Result} back on the question.
 *
 * <p>Unknown keys, parse errors, and unexpected runtime exceptions all become a
 * failed result carrying the exception message, so callers never need their own
 * error handling.
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
            result = operation.solve(question.getInput());
        } catch (RuntimeException e) {
            result = Result.failure(e.getMessage() != null ? e.getMessage() : "Error while solving");
        }
        if (result == null) {
            result = Result.failure("Operation produced no result");
        }
        return attach(question, result);
    }

    private static Result attach(Question question, Result result) {
        question.setResult(result);
        return result;
    }
}
