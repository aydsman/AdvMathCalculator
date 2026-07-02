package ui.components;

import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Renders a {@link Result} as a numbered, step-by-step breakdown followed by the final
 * answer. Shown in the right panel while in Calculate mode.
 */
public class StepDisplay extends VBox {

    public StepDisplay() {
        getStyleClass().add("step-display");
        setResult(null);
    }

    public void setResult(Result result) {
        setResult(result, null, false);
    }

    public void setResult(Result result, Result evaluationAt, boolean showDecimal) {
        getChildren().clear();

        if (result == null && evaluationAt == null) {
            getChildren().add(placeholder("Select a question on the left to see its steps."));
            return;
        }

        if (evaluationAt != null) {
            appendEvaluation(evaluationAt, showDecimal);
            return;
        }

        if (result == null) {
            return;
        }

        if (!result.isSuccess()) {
            Label error = new Label(result.getError());
            error.getStyleClass().add("bubble-error");
            error.setWrapText(true);
            getChildren().add(error);
            return;
        }

        int index = 1;
        for (Step step : result.getSteps()) {
            VBox stepBox = new VBox();
            stepBox.getStyleClass().add("step");

            Label description = new Label(index++ + ".  " + step.getDescription());
            description.getStyleClass().add("step-desc");
            description.setWrapText(true);
            stepBox.getChildren().add(description);

            if (step.hasDefiniteIntegral()) {
                IntegralDisplay integral = new IntegralDisplay(step.getDefiniteIntegral());
                stepBox.getChildren().add(integral);
            } else if (step.hasExpression()) {
                Label expression = new Label(step.getExpression().toDisplay());
                expression.getStyleClass().add("step-expr");
                expression.setWrapText(true);
                stepBox.getChildren().add(expression);
            } else if (step.hasMathLine()) {
                Label expression = new Label(step.getMathLine());
                expression.getStyleClass().add("step-expr");
                expression.setWrapText(true);
                stepBox.getChildren().add(expression);
            }
            getChildren().add(stepBox);
        }

        VBox answerBox = new VBox();
        answerBox.getStyleClass().add("final-answer");
        Label label = new Label("Answer");
        label.getStyleClass().add("final-answer-label");
        Label value = new Label(answerText(result, showDecimal));
        value.getStyleClass().add("final-answer-value");
        value.setWrapText(true);
        answerBox.getChildren().addAll(label, value);
        getChildren().add(answerBox);
    }

    /** Waiting state for Evaluate before the user picks an x value. */
    public void setPendingEvaluation() {
        getChildren().clear();
        getChildren().add(placeholder("Enter a value for x and click Evaluate."));
    }

    /** Waiting state for Riemann sum before bounds and n are entered. */
    public void setPendingRiemannSum() {
        getChildren().clear();
        getChildren().add(placeholder("Enter bounds a, b and rectangle count n, then click Compute."));
    }

    /** Waiting state for Unit Circle before the user picks an angle. */
    public void setPendingUnitCircle() {
        getChildren().clear();
        getChildren().add(placeholder("Pick a quick angle below, or enter one on the left and submit."));
    }

    private void appendEvaluation(Result evaluation, boolean showDecimal) {
        VBox evalBox = new VBox();
        evalBox.getStyleClass().add("evaluation-box");

        Label title = new Label("Evaluation at x");
        title.getStyleClass().add("evaluation-title");
        evalBox.getChildren().add(title);

        if (!evaluation.isSuccess()) {
            Label error = new Label(evaluation.getError());
            error.getStyleClass().add("bubble-error");
            error.setWrapText(true);
            evalBox.getChildren().add(error);
        } else {
            for (Step step : evaluation.getSteps()) {
                Label line = new Label(step.getDescription()
                        + (step.hasExpression() ? ": " + step.getExpression().toDisplay() : ""));
                line.getStyleClass().add("evaluation-step");
                line.setWrapText(true);
                evalBox.getChildren().add(line);
            }
            Label value = new Label("f(x) = " + answerText(evaluation, showDecimal));
            value.getStyleClass().add("evaluation-value");
            value.setWrapText(true);
            evalBox.getChildren().add(value);
        }
        getChildren().add(evalBox);
    }

    private Label placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("step-empty");
        label.setWrapText(true);
        return label;
    }

    private static String answerText(Result result, boolean showDecimal) {
        if (!result.getSolutions().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < result.getSolutions().size(); i++) {
                if (i > 0) {
                    sb.append(",   ");
                }
                sb.append(result.getSolutions().get(i).toDisplay());
            }
            return sb.toString();
        }
        ASTNode exact = result.getExact();
        if (exact != null) {
            String text = exact.toDisplay();
            if (result.getDecimal() != null) {
                text += "   \u2248 " + formatDecimal(result.getDecimal());
            } else if (showDecimal && result.getDecimal() != null) {
                text += "   \u2248 " + formatDecimal(result.getDecimal());
            }
            return text;
        }
        if (result.getDecimal() != null) {
            return "\u2248 " + formatDecimal(result.getDecimal());
        }
        return "(no answer)";
    }

    private static String formatDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        String text = String.format("%.10f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.isEmpty() ? "0" : text;
    }
}
