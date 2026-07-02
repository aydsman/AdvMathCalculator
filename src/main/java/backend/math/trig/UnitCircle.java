package backend.math.trig;

import backend.engine.MathOperation;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact sine, cosine, and tangent values for special unit-circle angles.
 *
 * <p>Input examples: {@code 30}, {@code 45°}, {@code pi/6}, {@code 5*pi/4}.
 */
public class UnitCircle implements MathOperation {

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter an angle such as 30, 45°, or pi/6.");
    }

    public Result solveFromInput(String raw) {
        return analyze(raw, UnitCircleOptions.allEnabled());
    }

    public Result analyze(String raw, UnitCircleOptions options) {
        List<Step> steps = new ArrayList<>();
        List<ASTNode> solutions = new ArrayList<>();

        TrigUtils.ExactValues values = TrigUtils.valuesForInput(raw);
        TrigUtils.AngleInfo info = angleInfoFor(values);

        steps.add(new Step("Angle", new Constant(info.degreeLabel() + "  (" + info.radianLabel() + ")")));
        solutions.add(new Constant("Angle: " + info.degreeLabel() + " = " + info.radianLabel()));

        if (options.convertAngle()) {
            steps.add(new Step("Degree ↔ radian",
                    new Constant(info.degreeLabel() + " = " + info.radianLabel())));
        }

        if (options.quadrant()) {
            String q = TrigUtils.quadrant(info.degrees());
            steps.add(new Step("Quadrant", new Constant(q)));
            solutions.add(new Constant(q));
        }

        if (options.referenceAngle()) {
            int ref = TrigUtils.referenceAngleDegrees(info.degrees());
            TrigUtils.PiMultiple refPi = TrigUtils.piMultipleForDegrees(ref);
            String refRad = refPi == null ? String.valueOf(ref) + "°" : refPi.label();
            steps.add(new Step("Reference angle", new Constant(ref + "°  (" + refRad + ")")));
            solutions.add(new Constant("Reference: " + ref + "°"));
        }

        if (options.coordinates()) {
            steps.add(new Step("Point on the unit circle (cos θ, sin θ)",
                    new Constant("(" + values.cos().toDisplay() + ", " + values.sin().toDisplay() + ")")));
            solutions.add(new Constant("(" + values.cos().toDisplay() + ", " + values.sin().toDisplay() + ")"));
        }

        steps.add(new Step("sin(" + info.radianLabel() + ")", values.sin()));
        steps.add(new Step("cos(" + info.radianLabel() + ")", values.cos()));
        steps.add(new Step("tan(" + info.radianLabel() + ")", values.tan()));
        solutions.add(new Constant("sin = " + values.sin().toDisplay()));
        solutions.add(new Constant("cos = " + values.cos().toDisplay()));
        solutions.add(new Constant("tan = " + values.tan().toDisplay()));

        if (options.reciprocals()) {
            ASTNode csc = TrigUtils.reciprocal(values.sin(), "csc");
            ASTNode sec = TrigUtils.reciprocal(values.cos(), "sec");
            ASTNode cot = TrigUtils.reciprocal(values.tan(), "cot");
            steps.add(new Step("csc(" + info.radianLabel() + ")", csc));
            steps.add(new Step("sec(" + info.radianLabel() + ")", sec));
            steps.add(new Step("cot(" + info.radianLabel() + ")", cot));
            solutions.add(new Constant("csc = " + csc.toDisplay()));
            solutions.add(new Constant("sec = " + sec.toDisplay()));
            solutions.add(new Constant("cot = " + cot.toDisplay()));
        }

        if (options.coterminal()) {
            String degText = TrigUtils.coterminalDegreeText(info.degrees());
            String radText = TrigUtils.coterminalRadianText(info.degrees());
            steps.add(new Step("Coterminal angles",
                    new Constant(degText + ",   " + radText)));
            solutions.add(new Constant("Coterminal: " + degText));
        }

        return Result.ofSolutions(solutions, steps);
    }

    private static TrigUtils.AngleInfo angleInfoFor(TrigUtils.ExactValues values) {
        Integer deg = TrigUtils.tryParseDegrees(values.angleLabel());
        if (deg != null) {
            return TrigUtils.angleInfo(deg, values.angleLabel());
        }
        TrigUtils.PiMultiple pi = TrigUtils.asPiMultiple(
                backend.math.algebra.Simplifier.simplify(
                        backend.parser.Parser.parse(values.angleLabel())));
        if (pi != null) {
            return TrigUtils.angleInfo(pi.toDegrees(), pi.label());
        }
        return new TrigUtils.AngleInfo(0, values.angleLabel(), values.angleLabel());
    }
}
