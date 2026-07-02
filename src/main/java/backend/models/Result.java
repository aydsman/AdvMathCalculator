package backend.models;

import backend.parser.ASTNode;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of solving a {@link Question}.
 *
 * <p>Holds the exact symbolic answer plus an optional decimal approximation
 * ("exact + decimal where applicable") and the step-by-step breakdown. An
 * operation with several answers (e.g. roots of an equation) uses
 * {@link #getSolutions()}; a single-valued operation uses {@link #getExact()}.
 * Failures are represented with {@link #failure(String)} rather than exceptions
 * so the UI can render a message in the same flow.
 */
public class Result {

    private final boolean success;
    private final String error;
    private final ASTNode exact;
    private final List<ASTNode> solutions;
    private final Double decimal;
    private final List<Step> steps;

    private Result(boolean success, String error, ASTNode exact,
                   List<ASTNode> solutions, Double decimal, List<Step> steps) {
        this.success = success;
        this.error = error;
        this.exact = exact;
        this.solutions = solutions == null ? List.of() : List.copyOf(solutions);
        this.decimal = decimal;
        this.steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public static Result of(ASTNode exact, Double decimal, List<Step> steps) {
        return new Result(true, null, exact, null, decimal, steps);
    }

    public static Result ofSolutions(List<ASTNode> solutions, List<Step> steps) {
        return new Result(true, null, null, solutions, null, steps);
    }

    public static Result failure(String error) {
        return new Result(false, error, null, null, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }

    /** Primary symbolic answer for single-valued operations; may be {@code null}. */
    public ASTNode getExact() {
        return exact;
    }

    /** Multiple answers (e.g. equation roots); empty for single-valued operations. */
    public List<ASTNode> getSolutions() {
        return solutions;
    }

    /** Decimal approximation when applicable; may be {@code null}. */
    public Double getDecimal() {
        return decimal;
    }

    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
