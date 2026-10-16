package ui;

import backend.engine.MathEngine;
import backend.math.algebra.EquationSolver;
import backend.math.algebra.ExpressionEvaluator;
import backend.math.algebra.Factoring;
import backend.math.algebra.Inequalities;
import backend.math.algebra.Simplifier;
import backend.math.calculus.differential.Derivative;
import backend.math.calculus.differential.ImplicitDerivative;
import backend.math.calculus.differential.TangentLine;
import backend.math.calculus.integral.Antiderivative;
import backend.math.calculus.integral.RiemannSum;
import backend.math.calculus.limits.Limit;
import backend.math.functions.Composition;
import backend.math.functions.DomainRange;
import backend.math.functions.InverseFunction;
import backend.math.functions.Transformations;
import backend.math.trig.Identities;
import backend.math.trig.TrigEquationSolver;
import backend.math.trig.UnitCircle;
import backend.models.ButtonNode;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.panels.CalculatorShell;
import ui.panels.LeftPanel;
import ui.panels.RightPanel;
import ui.visualizer.FreeGraph;

import java.net.URL;

/**
 * JavaFX entry point. Builds the operation menu, wires the {@link MathEngine} to the
 * two-panel layout, and applies the dark theme.
 */
public class MainApp extends Application {

    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 600;

    private final MathEngine engine = new MathEngine();

    @Override
    public void start(Stage stage) {
        registerOperations();
        ButtonNode menu = buildMenu();

        LeftPanel leftPanel = new LeftPanel();
        RightPanel rightPanel = new RightPanel(menu);

        rightPanel.setOnSubmit(question -> {
            engine.solve(question);
            leftPanel.addBubble(question);
        });
        leftPanel.setOnSelect(rightPanel::show);

        CalculatorShell calculatorShell = new CalculatorShell(leftPanel, rightPanel);
        StackPane calculatorRoot = new StackPane(calculatorShell);
        FreeGraph freeGraph = new FreeGraph();
        freeGraph.setVisible(false);
        freeGraph.setOnBack(() -> showCalculator(freeGraph, calculatorRoot));

        rightPanel.setOnOperationChange(node -> {
            if (node != null && node.isAction()) {
                if ("freeGraph".equals(node.getActionKey())) {
                    showFreeGraph(freeGraph, calculatorRoot);
                } else {
                    showCalculator(freeGraph, calculatorRoot);
                    rightPanel.setActiveOperation(node.getActionKey());
                }
            }
        });

        StackPane root = new StackPane(calculatorRoot, freeGraph);
        bindFullWindow(freeGraph, root);
        bindFullWindow(calculatorRoot, root);

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        URL css = MainApp.class.getResource("/ui/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Advanced Math Calculator");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }

    private void registerOperations() {
        engine.register("simplify", new Simplifier());
        engine.register("factor", new Factoring());
        engine.register("evaluate", new ExpressionEvaluator());
        engine.register("solveEquation", new EquationSolver());
        engine.register("inequalities", new Inequalities());
        engine.register("domainRange", new DomainRange());
        engine.register("composition", new Composition());
        engine.register("inverse", new InverseFunction());
        engine.register("transformations", new Transformations());

        engine.register("unitCircle", new UnitCircle());
        engine.register("identities", new Identities());
        engine.register("trigEquation", new TrigEquationSolver());

        engine.register("derivative", new Derivative());
        engine.register("implicitDerivative", new ImplicitDerivative());
        engine.register("tangentLine", new TangentLine());
        engine.register("limit", new Limit());
        engine.register("riemannSum", new RiemannSum());
        engine.register("indefiniteIntegral", new Antiderivative());
    }

    private static ButtonNode buildMenu() {
        return ButtonNode.dropdown("Categories",
                ButtonNode.dropdown("Algebra",
                        ButtonNode.action("Factor", "factor"),
                        ButtonNode.action("Solve equation", "solveEquation"),
                        ButtonNode.action("Simplify", "simplify"),
                        ButtonNode.action("Evaluate", "evaluate"),
                        ButtonNode.action("Inequalities", "inequalities")),
                ButtonNode.dropdown("Functions",
                        ButtonNode.action("Domain & Range", "domainRange"),
                        ButtonNode.action("Composition", "composition"),
                        ButtonNode.action("Inverse", "inverse"),
                        ButtonNode.action("Transformations", "transformations")),
                ButtonNode.dropdown("Trigonometry",
                        ButtonNode.action("Unit circle", "unitCircle"),
                        ButtonNode.action("Identities", "identities"),
                        ButtonNode.action("Solve trig equation", "trigEquation")),
                ButtonNode.dropdown("Calculus",
                        ButtonNode.dropdown("Differential",
                                ButtonNode.action("Limit", "limit"),
                                ButtonNode.action("Derivative", "derivative"),
                                ButtonNode.action("Implicit derivative", "implicitDerivative"),
                                ButtonNode.action("Tangent line", "tangentLine")),
                        ButtonNode.dropdown("Integral",
                                ButtonNode.action("Riemann sum", "riemannSum"),
                                ButtonNode.action("Indefinite", "indefiniteIntegral"))),
                ButtonNode.action("Free graph", "freeGraph"));
    }

    private static void bindFullWindow(Region child, StackPane root) {
        child.prefWidthProperty().bind(root.widthProperty());
        child.prefHeightProperty().bind(root.heightProperty());
        child.maxWidthProperty().bind(root.widthProperty());
        child.maxHeightProperty().bind(root.heightProperty());
    }

    private static void showFreeGraph(FreeGraph freeGraph, StackPane calculatorShell) {
        calculatorShell.setVisible(false);
        freeGraph.setVisible(true);
        freeGraph.toFront();
        freeGraph.refreshAfterShow();
    }

    private static void showCalculator(FreeGraph freeGraph, StackPane calculatorShell) {
        freeGraph.setVisible(false);
        calculatorShell.setVisible(true);
        calculatorShell.toFront();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
