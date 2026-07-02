package backend.math.functions;

import backend.engine.MathOperation;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Constant;

import java.util.ArrayList;
import java.util.List;

/** Computes function composition {@code (f ∘ g)(x) = f(g(x))}. */
public class Composition implements MathOperation {

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter two functions: f(x)=..., g(x)=...");
    }

    public Result solveFromInput(String raw) {
        List<Step> steps = new ArrayList<>();
        List<FunctionUtils.FunctionDef> defs = FunctionUtils.parseDefinitions(raw);
        FunctionUtils.FunctionDef f = defs.get(0);
        FunctionUtils.FunctionDef g = defs.get(1);

        ASTNode fBody = Simplifier.simplify(f.body());
        ASTNode gBody = Simplifier.simplify(g.body());

        steps.add(new Step("Outer function f", new Constant("f(" + f.variable() + ") = " + fBody.toDisplay())));
        steps.add(new Step("Inner function g", new Constant("g(" + g.variable() + ") = " + gBody.toDisplay())));

        ASTNode composed = Simplifier.simplify(
                FunctionUtils.substitute(fBody, f.variable(), gBody));
        steps.add(new Step("Substitute g(" + f.variable() + ") into f", composed));
        steps.add(new Step("(f ∘ g)(" + f.variable() + ") = f(g(" + f.variable() + "))", composed));

        return Result.of(composed, null, steps);
    }
}
