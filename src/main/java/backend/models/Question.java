package backend.models;

/**
 * A single user request flowing through the app.
 *
 * <p>Carries the {@code category} (e.g. "Calculus") and {@code operation}
 * (the action key chosen from the button tree, e.g. "derivative") together
 * with the raw {@code input} expression. The {@link Result} is attached once
 * the {@code MathEngine} has solved it, so a Question represents both the ask
 * and, later, its answer.
 */
public class Question {

    private final String category;
    private final String operation;
    private final String input;
    private Result result;

    public Question(String category, String operation, String input) {
        this.category = category;
        this.operation = operation;
        this.input = input;
    }

    public String getCategory() {
        return category;
    }

    public String getOperation() {
        return operation;
    }

    public String getInput() {
        return input;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public boolean isSolved() {
        return result != null;
    }
}
