package backend.engine;

import backend.models.Result;
import backend.parser.ASTNode;

/**
 * The contract every math module implements so the {@link MathEngine} can route to it.
 *
 * <p>An operation receives the already-parsed input expression and returns a
 * {@link Result} (which carries the answer plus the step-by-step breakdown).
 * Implementations should report problems by returning {@link Result#failure(String)}
 * rather than throwing; the engine also guards against unexpected exceptions.
 *
 * <p>Single-variable operations (derivatives, integrals, ...) assume the variable
 * {@code x} for now. If multi-variable support is needed later, this contract can
 * grow an operation-context parameter without affecting callers.
 */
@FunctionalInterface
public interface MathOperation {

    Result solve(ASTNode input);
}
