package backend.math.functions;

import backend.engine.MathOperation;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Func;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports key properties of a trigonometric function: domain, range, and period.
 */
public class TrigFunctions implements MathOperation {

    @Override
    public Result solve(ASTNode input) {
        return analyze(input);
    }

    public Result solveFromInput(String raw) {
        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(raw, "f", "x");
        return analyze(Simplifier.simplify(def.body()));
    }

    private static Result analyze(ASTNode body) {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("Function", body));

        if (!(body instanceof Func f)) {
            return Result.failure("Enter a trig function such as sin(x), cos(2*x), or tan(x).");
        }

        String name = f.name().toLowerCase();
        if (!List.of("sin", "cos", "tan", "cot", "sec", "csc").contains(name)) {
            return Result.failure("Unsupported function: " + name);
        }

        Properties props = properties(name);
        steps.add(new Step("Domain", new Constant(props.domain())));
        steps.add(new Step("Range", new Constant(props.range())));
        steps.add(new Step("Period", new Constant(props.period())));

        return Result.ofSolutions(
                List.of(
                        new Constant("Domain: " + props.domain()),
                        new Constant("Range: " + props.range()),
                        new Constant("Period: " + props.period())),
                steps);
    }

    private record Properties(String domain, String range, String period) {}

    private static Properties properties(String name) {
        return switch (name) {
            case "sin", "cos", "sec", "csc" -> new Properties(
                    "All real numbers", "[-1, 1]", "2*pi");
            case "tan", "cot" -> new Properties(
                    "All real numbers except odd multiples of pi/2",
                    "All real numbers", "pi");
            default -> new Properties("Unknown", "Unknown", "Unknown");
        };
    }
}
