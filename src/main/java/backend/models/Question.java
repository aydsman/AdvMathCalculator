package backend.models;

/**
 * A user request: the operation's action key (e.g. {@code "derivative"}), its menu
 * label for display, and the raw input. The engine attaches the {@link Result}.
 */
public class Question {

    private final String category;
    private final String operation;
    private final String operationLabel;
    private final String input;
    private Result result;

    public Question(String category, String operation, String input) {
        this(category, operation, operation, input);
    }

    public Question(String category, String operation, String operationLabel, String input) {
        this.category = category;
        this.operation = operation;
        this.operationLabel = operationLabel;
        this.input = input;
    }

    public String getCategory() {
        return category;
    }

    public String getOperation() {
        return operation;
    }

    public String getOperationLabel() {
        return operationLabel;
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
