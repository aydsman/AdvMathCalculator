package backend.models;

import backend.parser.ASTNode;

/**
 * One entry in a step-by-step solution.
 */
public class Step {

    private final String description;
    private final String mathLine;
    private final ASTNode expression;
    private final DefiniteIntegral definiteIntegral;

    public Step(String description) {
        this(description, null, null, null);
    }

    public Step(String description, ASTNode expression) {
        this(description, null, expression, null);
    }

    /** Narrative step with a pre-formatted math line (no {@link ASTNode}). */
    public Step(String description, String mathLine) {
        this(description, mathLine, null, null);
    }

    public static Step definiteIntegral(String description, DefiniteIntegral integral) {
        return new Step(description, null, null, integral);
    }

    private Step(String description, String mathLine, ASTNode expression, DefiniteIntegral definiteIntegral) {
        this.description = description;
        this.mathLine = mathLine;
        this.expression = expression;
        this.definiteIntegral = definiteIntegral;
    }

    public String getDescription() {
        return description;
    }

    public String getMathLine() {
        return mathLine;
    }

    public boolean hasMathLine() {
        return mathLine != null && !mathLine.isBlank();
    }

    public ASTNode getExpression() {
        return expression;
    }

    public boolean hasExpression() {
        return expression != null;
    }

    public DefiniteIntegral getDefiniteIntegral() {
        return definiteIntegral;
    }

    public boolean hasDefiniteIntegral() {
        return definiteIntegral != null;
    }
}
