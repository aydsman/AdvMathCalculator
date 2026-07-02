package ui.components;

import backend.models.Question;
import backend.models.Result;
import backend.parser.ASTNode;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A single entry in the left feed: shows the question's category/operation, the input
 * expression, and a short answer (or error) summary. Clicking it selects the bubble,
 * which drives the right panel.
 */
public class QuestionBubble extends VBox {

    private final Question question;
    private Runnable onSelect;

    public QuestionBubble(Question question) {
        this.question = question;
        getStyleClass().add("bubble");

        Label header = new Label(question.getCategory() + "  \u00B7  " + question.getOperation());
        header.getStyleClass().add("bubble-header");

        Label input = new Label(question.getInput());
        input.getStyleClass().add("bubble-input");
        input.setWrapText(true);

        getChildren().addAll(header, input);

        Result result = question.getResult();
        if (result != null) {
            Label summary = new Label(result.isSuccess() ? summarize(result) : result.getError());
            summary.getStyleClass().add(result.isSuccess() ? "bubble-answer" : "bubble-error");
            summary.setWrapText(true);
            getChildren().add(summary);
        }

        setOnMouseClicked(e -> {
            if (onSelect != null) {
                onSelect.run();
            }
        });
    }

    public Question getQuestion() {
        return question;
    }

    public void setOnSelect(Runnable handler) {
        this.onSelect = handler;
    }

    public void setSelected(boolean selected) {
        getStyleClass().remove("bubble-selected");
        if (selected) {
            getStyleClass().add("bubble-selected");
        }
    }

    private static String summarize(Result result) {
        if (!result.getSolutions().isEmpty()) {
            StringBuilder sb = new StringBuilder("= ");
            for (int i = 0; i < result.getSolutions().size(); i++) {
                if (i > 0) {
                    sb.append(",  ");
                }
                sb.append(result.getSolutions().get(i).toDisplay());
            }
            return sb.toString();
        }
        ASTNode exact = result.getExact();
        if (exact != null) {
            String text = "= " + exact.toDisplay();
            if (result.getDecimal() != null) {
                text += "  \u2248 " + trim(result.getDecimal());
            }
            return text;
        }
        if (result.getDecimal() != null) {
            return "\u2248 " + trim(result.getDecimal());
        }
        return "(no answer)";
    }

    private static String trim(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.6g", value);
    }
}
