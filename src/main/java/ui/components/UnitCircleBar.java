package ui.components;

import backend.math.trig.UnitCircleOptions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive unit-circle controls: degree/radian presets and optional calculation sections.
 */
public class UnitCircleBar extends VBox {

    private static final List<String> DEGREE_PRESETS =
            List.of("0", "30", "45", "60", "90", "120", "135", "180", "210", "225", "270", "315");
    private static final List<String> RADIAN_PRESETS =
            List.of("0", "pi/6", "pi/4", "pi/3", "pi/2", "2*pi/3", "3*pi/4", "pi",
                    "5*pi/4", "3*pi/2", "7*pi/4", "2*pi");

    private final ToggleButton degreesToggle = new ToggleButton("Degrees");
    private final FlowPane presetPane = new FlowPane(6, 6);
    private final CheckBox coordinatesCheck = new CheckBox("Coordinates");
    private final CheckBox referenceCheck = new CheckBox("Reference");
    private final CheckBox reciprocalsCheck = new CheckBox("Reciprocals");
    private final CheckBox coterminalCheck = new CheckBox("Coterminal");
    private final CheckBox quadrantCheck = new CheckBox("Quadrant");

    private Consumer<String> onAngleSelected;
    private Runnable onOptionsChanged;

    public UnitCircleBar() {
        getStyleClass().add("unit-circle-bar");
        setSpacing(8);
        setPadding(new Insets(0, 8, 4, 8));

        degreesToggle.getStyleClass().add("unit-degrees-toggle");
        degreesToggle.setFocusTraversable(false);
        degreesToggle.setOnAction(e -> {
            rebuildPresets();
            if (onOptionsChanged != null) {
                onOptionsChanged.run();
            }
        });

        Label presetLabel = new Label("Quick angles:");
        presetLabel.getStyleClass().add("unit-circle-label");

        presetPane.setAlignment(Pos.CENTER);
        presetPane.getStyleClass().add("unit-preset-pane");

        HBox modeRow = new HBox(10, degreesToggle, presetLabel);
        modeRow.setAlignment(Pos.CENTER);

        coordinatesCheck.setSelected(true);
        referenceCheck.setSelected(true);
        reciprocalsCheck.setSelected(true);
        coterminalCheck.setSelected(true);
        quadrantCheck.setSelected(true);
        for (CheckBox box : List.of(coordinatesCheck, referenceCheck, reciprocalsCheck,
                coterminalCheck, quadrantCheck)) {
            box.getStyleClass().add("unit-option-check");
            box.setFocusTraversable(false);
            box.setOnAction(e -> {
                if (onOptionsChanged != null) {
                    onOptionsChanged.run();
                }
            });
        }

        Label showLabel = new Label("Show:");
        showLabel.getStyleClass().add("unit-circle-label");
        FlowPane optionsPane = new FlowPane(10, 6,
                showLabel, coordinatesCheck, referenceCheck, reciprocalsCheck, coterminalCheck, quadrantCheck);
        optionsPane.setAlignment(Pos.CENTER);
        optionsPane.getStyleClass().add("unit-options-pane");

        getChildren().addAll(modeRow, presetPane, optionsPane);
        rebuildPresets();
        setVisible(false);
        setManaged(false);
    }

    public void setVisibleForQuestion(boolean visible) {
        setVisible(visible);
        setManaged(visible);
    }

    public void setOnAngleSelected(Consumer<String> handler) {
        this.onAngleSelected = handler;
    }

    public void setOnOptionsChanged(Runnable handler) {
        this.onOptionsChanged = handler;
    }

    public UnitCircleOptions options() {
        return new UnitCircleOptions(
                coordinatesCheck.isSelected(),
                referenceCheck.isSelected(),
                reciprocalsCheck.isSelected(),
                coterminalCheck.isSelected(),
                quadrantCheck.isSelected(),
                true);
    }

    public boolean isDegreesMode() {
        return degreesToggle.isSelected();
    }

    private void rebuildPresets() {
        presetPane.getChildren().clear();
        List<String> presets = degreesToggle.isSelected() ? DEGREE_PRESETS : RADIAN_PRESETS;
        for (String preset : presets) {
            ToggleButton button = new ToggleButton(formatPresetLabel(preset));
            button.getStyleClass().add("angle-preset-button");
            button.setFocusTraversable(false);
            button.setOnAction(e -> {
                if (onAngleSelected != null) {
                    onAngleSelected.accept(preset);
                }
            });
            presetPane.getChildren().add(button);
        }
    }

    private String formatPresetLabel(String preset) {
        if (degreesToggle.isSelected()) {
            return preset + "°";
        }
        return preset.replace("*", "");
    }
}
