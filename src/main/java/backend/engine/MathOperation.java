package backend.engine;

import backend.models.Result;

/**
 * A calculator operation that the {@link MathEngine} dispatches to by action key.
 *
 * <p>Implementations receive the user's input text exactly as typed and report
 * problems through {@link Result#failure(String)} or a {@code ParseException};
 * the engine turns any other runtime exception into a failed result.
 */
@FunctionalInterface
public interface MathOperation {

    Result solve(String input);
}
