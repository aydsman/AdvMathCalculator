package ui.components;

import backend.models.DefiniteIntegral;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Renders ∫ with upper/lower bounds, integrand, and dx. */
public class IntegralDisplay extends HBox {

    public IntegralDisplay(DefiniteIntegral integral) {
        getStyleClass().add("integral-display");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);

        Label upper = boundLabel(integral.upper());
        Label sign = new Label("\u222B");
        sign.getStyleClass().add("integral-sign");
        Label lower = boundLabel(integral.lower());

        VBox boundsColumn = new VBox(-2, upper, sign, lower);
        boundsColumn.setAlignment(Pos.CENTER_RIGHT);
        boundsColumn.getStyleClass().add("integral-bounds");

        Label integrand = new Label(integral.integrand());
        integrand.getStyleClass().add("integral-integrand");
        integrand.setWrapText(true);

        Label differential = new Label(" d" + integral.variable());
        differential.getStyleClass().add("integral-dx");

        getChildren().addAll(boundsColumn, integrand, differential);
    }

    private static Label boundLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("integral-bound");
        return label;
    }
}
