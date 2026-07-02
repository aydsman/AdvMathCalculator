package ui.visualizer;

import backend.parser.ASTNode;
import javafx.scene.paint.Color;

/** One expression plotted on the free graph. */
public class PlottedFunction {

    private final String input;
    private final ASTNode expression;
    private final String variable;
    private Color color;

    public PlottedFunction(String input, ASTNode expression, String variable, Color color) {
        this.input = input;
        this.expression = expression;
        this.variable = variable;
        this.color = color;
    }

    public String getInput() {
        return input;
    }

    public ASTNode getExpression() {
        return expression;
    }

    public String getVariable() {
        return variable;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
