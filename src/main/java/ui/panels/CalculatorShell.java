package ui.panels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Main calculator layout: collapsible questions sidebar + workspace.
 */
public class CalculatorShell extends BorderPane {

    private static final double EXPANDED_DIVIDER = 0.30;
    private static final double COLLAPSED_DIVIDER = 0.0;
    private static final double COLLAPSED_STRIP_WIDTH = 32;

    private final Region leftPanel;
    private final Region rightPanel;
    private final SplitPane split = new SplitPane();
    private final StackPane leftSlot = new StackPane();
    private final Button toggle = new Button("\u2039");
    private final VBox collapsedStrip = new VBox(toggle);

    private boolean expanded = true;
    private double savedDivider = EXPANDED_DIVIDER;

    public CalculatorShell(Region leftPanel, Region rightPanel) {
        this.leftPanel = leftPanel;
        this.rightPanel = rightPanel;

        leftSlot.getChildren().addAll(leftPanel, toggle);
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, -13, 0, 0));

        collapsedStrip.setAlignment(Pos.TOP_CENTER);
        collapsedStrip.setPrefWidth(COLLAPSED_STRIP_WIDTH);
        collapsedStrip.setMinWidth(COLLAPSED_STRIP_WIDTH);
        collapsedStrip.setMaxWidth(COLLAPSED_STRIP_WIDTH);
        collapsedStrip.getStyleClass().add("collapsed-sidebar-strip");
        collapsedStrip.setPadding(new Insets(10, 0, 0, 0));

        toggle.getStyleClass().add("panel-collapse-toggle");
        toggle.setFocusTraversable(false);
        toggle.setOnAction(e -> toggleSidebar());

        split.getItems().addAll(leftSlot, rightPanel);
        split.setDividerPositions(EXPANDED_DIVIDER);
        split.getStyleClass().add("main-split");
        setCenter(split);
    }

    private void toggleSidebar() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    private void collapse() {
        savedDivider = split.getDividerPositions()[0];
        leftSlot.getChildren().remove(toggle);
        if (!collapsedStrip.getChildren().contains(toggle)) {
            collapsedStrip.getChildren().add(toggle);
        }
        split.getItems().set(0, collapsedStrip);
        split.setDividerPositions(COLLAPSED_DIVIDER);
        toggle.setText("\u203A");
        expanded = false;
    }

    private void expand() {
        collapsedStrip.getChildren().remove(toggle);
        if (!leftSlot.getChildren().contains(toggle)) {
            leftSlot.getChildren().add(toggle);
        }
        if (!leftSlot.getChildren().contains(leftPanel)) {
            leftSlot.getChildren().add(0, leftPanel);
        }
        split.getItems().set(0, leftSlot);
        split.setDividerPositions(savedDivider > COLLAPSED_DIVIDER ? savedDivider : EXPANDED_DIVIDER);
        toggle.setText("\u2039");
        expanded = true;
    }
}
