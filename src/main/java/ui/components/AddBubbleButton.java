package ui.components;

import javafx.scene.control.Button;

/** The "+" button that submits the current input as a new question bubble. */
public class AddBubbleButton extends Button {

    public AddBubbleButton() {
        super("+");
        getStyleClass().add("add-button");
        setFocusTraversable(false);
    }
}
