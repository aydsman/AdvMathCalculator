package ui.panels;

import backend.models.Question;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import ui.components.QuestionBubble;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Question history on the left: title and bubbles only. Expression input lives on
 * the calculate panel ({@link RightPanel}).
 */
public class LeftPanel extends BorderPane {

    private final VBox feed = new VBox();
    private final ScrollPane scroll = new ScrollPane();
    private final List<QuestionBubble> bubbles = new ArrayList<>();

    private Consumer<Question> onSelect;
    private QuestionBubble selectedBubble;

    public LeftPanel() {
        getStyleClass().add("left-panel");

        Label title = new Label("Questions");
        title.getStyleClass().add("panel-title");
        setTop(title);

        feed.getStyleClass().add("feed");
        scroll.setContent(feed);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("feed-scroll");
        setCenter(scroll);
    }

    public void addBubble(Question question) {
        QuestionBubble bubble = new QuestionBubble(question);
        bubble.setOnSelect(() -> select(bubble));
        bubbles.add(bubble);
        feed.getChildren().add(bubble);
        select(bubble);
        scroll.layout();
        scroll.setVvalue(1.0);
    }

    private void select(QuestionBubble bubble) {
        if (selectedBubble != null) {
            selectedBubble.setSelected(false);
        }
        selectedBubble = bubble;
        bubble.setSelected(true);
        if (onSelect != null) {
            onSelect.accept(bubble.getQuestion());
        }
    }

    public void setOnSelect(Consumer<Question> handler) {
        this.onSelect = handler;
    }
}
