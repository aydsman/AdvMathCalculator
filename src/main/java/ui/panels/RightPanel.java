package ui.panels;

import backend.math.algebra.ExpressionEvaluator;
import backend.math.calculus.integral.RiemannSum;
import backend.math.functions.FunctionUtils;
import backend.math.trig.UnitCircle;
import backend.models.ButtonNode;
import backend.models.Question;
import backend.models.Result;
import backend.parser.ASTNode;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ui.components.AddBubbleButton;
import ui.components.CategoryButton;
import ui.components.EvaluateAtBar;
import ui.components.ModeToggle;
import ui.components.RiemannSumBar;
import ui.components.StepDisplay;
import ui.components.UnitCircleBar;
import ui.visualizer.CalculateGraphPane;
import ui.visualizer.QuestionGraph;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Hosts the {@link ModeToggle} and switches between Calculate (steps + square mini graph)
 * and Visualize (full graph only) for supported operations.
 */
public class RightPanel extends BorderPane {

    private static final Set<String> EVALUATABLE_OPS = Set.of("simplify", "factor", "evaluate");
    private static final String UNIT_CIRCLE = "unitCircle";
    private static final String RIEMANN_SUM = "riemannSum";

    private final CategoryButton categoryButton;
    private final TextField inputField = new TextField();
    private final AddBubbleButton addButton = new AddBubbleButton();
    private final ModeToggle modeToggle = new ModeToggle();
    private final StepDisplay stepDisplay = new StepDisplay();
    private final EvaluateAtBar evaluateAtBar = new EvaluateAtBar();
    private final UnitCircleBar unitCircleBar = new UnitCircleBar();
    private final RiemannSumBar riemannSumBar = new RiemannSumBar();
    private final ScrollPane calculateScroll = new ScrollPane();
    private final CalculateGraphPane calculateGraph = new CalculateGraphPane();
    private final SplitPane calculateSplit = new SplitPane();
    private final StackPane visualizeHost = new StackPane();
    private final StackPane visualizeUnsupported =
            new StackPane(new Label("No graph available for this question"));

    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final UnitCircle unitCircle = new UnitCircle();
    private final RiemannSum riemannSum = new RiemannSum();

    private Question current;
    private String activeOperation;
    private Result evaluationAt;
    private Result unitCircleResult;
    private Result riemannSumResult;
    private String selectedUnitAngle;

    private Consumer<Question> onSubmit;
    private Consumer<ButtonNode> onOperationChange;

    public RightPanel(ButtonNode menuRoot) {
        getStyleClass().add("right-panel");

        categoryButton = new CategoryButton(menuRoot);
        inputField.setPromptText("Enter an expression, e.g. x^2 + 3x");
        inputField.getStyleClass().add("input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        HBox inputBar = new HBox(8, categoryButton, inputField, addButton);
        inputBar.getStyleClass().add("input-bar");
        inputBar.setAlignment(Pos.CENTER);

        HBox header = new HBox(modeToggle);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("right-header");

        VBox top = new VBox(10, inputBar, header, evaluateAtBar, unitCircleBar, riemannSumBar);
        setTop(top);

        addButton.setOnAction(e -> submit());
        inputField.setOnAction(e -> submit());
        categoryButton.setOnOperationSelected((category, node) -> {
            setActiveOperation(node.getActionKey());
            if (onOperationChange != null) {
                onOperationChange.accept(node);
            }
            focusInput();
        });

        calculateScroll.setContent(stepDisplay);
        calculateScroll.setFitToWidth(true);
        calculateScroll.getStyleClass().add("calc-scroll");

        visualizeUnsupported.getStyleClass().add("visualize-placeholder");
        visualizeHost.getStyleClass().add("visualize-graph-host");

        calculateGraph.setMinWidth(180);
        calculateSplit.getItems().addAll(calculateScroll, calculateGraph);
        calculateSplit.setDividerPositions(0.54);
        calculateSplit.getStyleClass().add("calculate-visualize-split");

        evaluateAtBar.setOnEvaluate(this::runEvaluation);
        evaluateAtBar.setOnDecimalToggle(this::render);
        unitCircleBar.setOnAngleSelected(this::runUnitCircleAngle);
        unitCircleBar.setOnOptionsChanged(this::refreshUnitCircle);
        riemannSumBar.setOnCompute(this::runRiemannSum);
        modeToggle.setOnModeChange(mode -> render());
        setCenter(calculateSplit);
        render();
    }

    /** Tracks the operation chosen in the category menu (even before a question exists). */
    public void setActiveOperation(String operationKey) {
        this.activeOperation = operationKey;
        if (!UNIT_CIRCLE.equals(operationKey) && !RIEMANN_SUM.equals(operationKey)) {
            unitCircleResult = null;
            selectedUnitAngle = null;
            riemannSumResult = null;
        }
        if (!RIEMANN_SUM.equals(operationKey)) {
            riemannSumResult = null;
        }
        render();
    }

    public void setOnSubmit(Consumer<Question> handler) {
        this.onSubmit = handler;
    }

    public void setOnOperationChange(Consumer<ButtonNode> handler) {
        this.onOperationChange = handler;
    }

    public void focusInput() {
        javafx.application.Platform.runLater(inputField::requestFocus);
    }

    private void submit() {
        String text = inputField.getText() == null ? "" : inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        ButtonNode operation = categoryButton.getSelected();
        String actionKey = operation != null ? operation.getActionKey() : "parse";
        String category = categoryButton.getSelectedCategory() != null
                ? categoryButton.getSelectedCategory() : "General";

        Question question = new Question(category, actionKey, text);
        if (onSubmit != null) {
            onSubmit.accept(question);
        }
        inputField.clear();
    }

    /** Switches the panel to reflect the given (already-solved) question. */
    public void show(Question question) {
        this.current = question;
        this.evaluationAt = null;
        this.riemannSumResult = null;
        if (question != null && UNIT_CIRCLE.equals(question.getOperation())) {
            selectedUnitAngle = question.getInput();
            refreshUnitCircleFromSelection();
        } else {
            unitCircleResult = null;
            selectedUnitAngle = null;
        }
        evaluateAtBar.clearError();
        riemannSumBar.clearError();
        render();
    }

    private void render() {
        updateVisualizeAvailability();
        boolean graph = supportsGraph(current);

        if (modeToggle.getMode() == ModeToggle.Mode.VISUALIZE) {
            evaluateAtBar.setVisibleForQuestion(false);
            unitCircleBar.setVisibleForQuestion(false);
            riemannSumBar.setVisibleForQuestion(false);
            if (graph) {
                showFullGraph();
            } else {
                setCenter(visualizeUnsupported);
            }
            return;
        }

        boolean showEvaluate = supportsEvaluation(current);
        boolean showUnitCircle = isUnitCircleActive();
        boolean showRiemann = isRiemannSumActive();
        evaluateAtBar.setVisibleForQuestion(showEvaluate);
        unitCircleBar.setVisibleForQuestion(showUnitCircle);
        riemannSumBar.setVisibleForQuestion(showRiemann);
        updateStepDisplay(showEvaluate, showUnitCircle, showRiemann);
        showCalculateWithOptionalGraph(graph);
    }

    private void updateVisualizeAvailability() {
        boolean graphReady = supportsGraph(current);
        boolean graphOp = QuestionGraph.supports(activeOperation);
        modeToggle.setVisualizeVisible(graphReady || graphOp);
    }

    private void showCalculateWithOptionalGraph(boolean graph) {
        if (getCenter() != calculateSplit) {
            detachGraphFromVisualize();
            setCenter(calculateSplit);
        }

        if (graph) {
            if (!calculateSplit.getItems().contains(calculateGraph)) {
                calculateSplit.getItems().add(calculateGraph);
            }
            calculateGraph.setCompact(true);
            calculateGraph.setVisible(true);
            calculateGraph.setManaged(true);
            calculateGraph.update(current, riemannGraphA(), riemannGraphB(), riemannGraphN());
            calculateSplit.setDividerPositions(0.54);
        } else {
            calculateSplit.getItems().remove(calculateGraph);
        }

        calculateGraph.requestLayout();
    }

    private void showFullGraph() {
        calculateSplit.getItems().remove(calculateGraph);
        calculateGraph.setCompact(false);
        calculateGraph.update(current, riemannGraphA(), riemannGraphB(), riemannGraphN());

        if (calculateGraph.getParent() != visualizeHost) {
            visualizeHost.getChildren().setAll(calculateGraph);
        }

        calculateGraph.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        calculateGraph.prefWidthProperty().bind(visualizeHost.widthProperty());
        calculateGraph.prefHeightProperty().bind(visualizeHost.heightProperty());

        setCenter(visualizeHost);
        calculateGraph.requestLayout();
        calculateGraph.getPlotPane().requestRedraw();
    }

    private void detachGraphFromVisualize() {
        calculateGraph.prefWidthProperty().unbind();
        calculateGraph.prefHeightProperty().unbind();
        visualizeHost.getChildren().clear();
        if (!calculateSplit.getItems().contains(calculateGraph)) {
            calculateSplit.getItems().add(calculateGraph);
        }
    }

    private static boolean supportsGraph(Question question) {
        return QuestionGraph.supports(question);
    }

    private void updateStepDisplay(boolean showEvaluate, boolean showUnitCircle, boolean showRiemann) {
        boolean showDecimal = showEvaluate && evaluateAtBar.isShowDecimal();

        if (showRiemann) {
            if (riemannSumResult != null) {
                stepDisplay.setResult(riemannSumResult, null, showDecimal);
            } else if (current != null
                    && current.getResult() != null
                    && current.getResult().isSuccess()) {
                stepDisplay.setPendingRiemannSum();
            } else {
                stepDisplay.setResult(null);
            }
            return;
        }

        if (showUnitCircle) {
            if (unitCircleResult != null) {
                stepDisplay.setResult(unitCircleResult, null, showDecimal);
            } else {
                stepDisplay.setPendingUnitCircle();
            }
            return;
        }

        if (current == null) {
            stepDisplay.setResult(null);
            return;
        }

        if (isEvaluateOperation(current) && evaluationAt == null) {
            Result inline = current.getResult();
            if (inline != null && inline.isSuccess() && !inline.getSteps().isEmpty()) {
                stepDisplay.setResult(null, inline, showDecimal);
            } else {
                stepDisplay.setPendingEvaluation();
            }
            return;
        }

        if (isEvaluateOperation(current)) {
            stepDisplay.setResult(null, evaluationAt, showDecimal);
            return;
        }

        stepDisplay.setResult(current.getResult(), evaluationAt, showDecimal);
    }

    private void runUnitCircleAngle(String angleInput) {
        selectedUnitAngle = angleInput;
        refreshUnitCircle();
    }

    private void refreshUnitCircleFromSelection() {
        if (selectedUnitAngle != null && !selectedUnitAngle.isBlank()) {
            refreshUnitCircle();
        }
    }

    private void refreshUnitCircle() {
        if (!isUnitCircleActive() || selectedUnitAngle == null || selectedUnitAngle.isBlank()) {
            return;
        }
        try {
            unitCircleResult = unitCircle.analyze(selectedUnitAngle, unitCircleBar.options());
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Could not evaluate angle";
            unitCircleResult = Result.failure(message);
        }
        updateStepDisplay(false, true, false);
    }

    private void runRiemannSum(RiemannSumBar.RiemannSumParams params) {
        if (!isRiemannSumActive() || current == null || current.getResult() == null) {
            return;
        }
        try {
            FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(current.getInput(), "f", "x");
            riemannSumResult = riemannSum.solveWithBounds(
                    current.getResult().getExact(),
                    def.variable(),
                    params.a(),
                    params.b(),
                    params.n());
            riemannSumBar.clearError();
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Could not compute Riemann sum";
            riemannSumResult = Result.failure(message);
            riemannSumBar.showError(message);
        }
        updateStepDisplay(false, false, true);
        if (supportsGraph(current)) {
            calculateGraph.update(current, params.a(), params.b(), params.n());
        }
    }

    private String riemannGraphA() {
        return isRiemannSumActive() ? riemannSumBar.params().a() : null;
    }

    private String riemannGraphB() {
        return isRiemannSumActive() ? riemannSumBar.params().b() : null;
    }

    private String riemannGraphN() {
        return isRiemannSumActive() ? riemannSumBar.params().n() : null;
    }

    private void runEvaluation(String xValueText) {
        if (!supportsEvaluation(current)) {
            return;
        }
        try {
            ASTNode expression = ExpressionEvaluator.expressionFromQuestion(
                    current.getInput(), current.getResult(), current.getOperation());
            evaluationAt = evaluator.evaluateAt(expression, "x", xValueText == null ? "" : xValueText.trim());
            evaluateAtBar.clearError();
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Could not evaluate";
            evaluationAt = Result.failure(message);
            evaluateAtBar.showError(message);
        }
        updateStepDisplay(true, false, false);
    }

    private boolean isRiemannSumActive() {
        if (current != null && RIEMANN_SUM.equals(current.getOperation())) {
            return true;
        }
        return RIEMANN_SUM.equals(activeOperation);
    }

    private boolean isUnitCircleActive() {
        if (current != null && UNIT_CIRCLE.equals(current.getOperation())) {
            return true;
        }
        return UNIT_CIRCLE.equals(activeOperation);
    }

    private static boolean isEvaluateOperation(Question question) {
        return question != null && "evaluate".equals(question.getOperation());
    }

    private static boolean supportsEvaluation(Question question) {
        return question != null
                && "Algebra".equals(question.getCategory())
                && EVALUATABLE_OPS.contains(question.getOperation())
                && question.getResult() != null
                && question.getResult().isSuccess();
    }
}
