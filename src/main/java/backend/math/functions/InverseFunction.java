package backend.math.functions;

import backend.engine.MathOperation;
import backend.math.algebra.Factoring;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Finds the inverse of simple functions symbolically. */
public class InverseFunction implements MathOperation {

    private static final String VAR = "x";

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter a function such as f(x) = 2*x + 3.");
    }

    public Result solveFromInput(String raw) {
        List<Step> steps = new ArrayList<>();
        FunctionUtils.FunctionDef def = FunctionUtils.parseDefinition(raw, "f", VAR);
        ASTNode body = Simplifier.simplify(def.body());

        steps.add(new Step("Function", new Constant("f(" + VAR + ") = " + body.toDisplay())));

        ASTNode inverse = tryInverse(body, steps);
        if (inverse == null) {
            return Result.failure("Inverse not supported for this function form yet.");
        }

        inverse = Simplifier.simplify(inverse);
        steps.add(new Step("Solve y = f(x) for x to get f\u207B\u00B9(y)", inverse));
        return Result.of(inverse, null, steps);
    }

    private static ASTNode tryInverse(ASTNode body, List<Step> steps) {
        Map<Integer, Num> poly = Factoring.coefficients(body, VAR);
        if (poly != null && poly.size() <= 2 && poly.containsKey(1)) {
            Num a = poly.getOrDefault(1, Num.of(0));
            Num b = poly.getOrDefault(0, Num.of(0));
            if (!a.isZero()) {
                steps.add(new Step("Linear function: f\u207B\u00B9(x) = (x \u2212 b) / a"));
                return new Div(new Add(new Var(VAR), new Neg(b)), a);
            }
        }

        if (body instanceof Pow p && p.base() instanceof Var v && v.name().equals(VAR)
                && p.exponent() instanceof Num n && n.isInteger()) {
            int exp = n.numerator().intValueExact();
            if (exp == 2) {
                steps.add(new Step("Square function (x >= 0): f\u207B\u00B9(x) = sqrt(x)"));
                return new Func("sqrt", new Var(VAR));
            }
            if (exp == 3) {
                steps.add(new Step("Cube function: f\u207B\u00B9(x) = x^(1/3)"));
                return new Pow(new Var(VAR), new Div(Num.of(1), Num.of(3)));
            }
        }

        if (body instanceof Func f && "sqrt".equals(f.name()) && f.argument() instanceof Var v
                && v.name().equals(VAR)) {
            steps.add(new Step("Square root (x >= 0): f\u207B\u00B9(x) = x^2"));
            return new Pow(new Var(VAR), Num.of(2));
        }

        if (body instanceof Div d && d.left() instanceof Num one && one.equals(Num.of(1))
                && d.right() instanceof Var v && v.name().equals(VAR)) {
            steps.add(new Step("Reciprocal: f\u207B\u00B9(x) = 1/x"));
            return new Div(Num.of(1), new Var(VAR));
        }

        if (body instanceof Add a && a.left() instanceof Pow p && isShiftedLinear(a.right())) {
            return inverseShiftedPower(p, a.right(), steps);
        }
        if (body instanceof Sub s && s.left() instanceof Pow p && isShiftedLinear(new Neg(s.right()))) {
            return inverseShiftedPower(p, s.right(), steps);
        }

        return null;
    }

    private static boolean isShiftedLinear(ASTNode node) {
        Map<Integer, Num> poly = Factoring.coefficients(node, VAR);
        return poly != null && poly.size() <= 2 && poly.containsKey(0);
    }

    private static ASTNode inverseShiftedPower(Pow power, ASTNode shift, List<Step> steps) {
        steps.add(new Step("Shifted power function — solve for the inner variable"));
        ASTNode inner = new Pow(new Var(VAR), new Div(Num.of(1), power.exponent()));
        Map<Integer, Num> poly = Factoring.coefficients(shift, VAR);
        if (poly == null) {
            return null;
        }
        Num a = poly.getOrDefault(1, Num.of(0));
        Num b = poly.getOrDefault(0, Num.of(0));
        if (a.isZero()) {
            return null;
        }
        ASTNode unshift = new Div(new Add(new Var(VAR), new Neg(b)), a);
        return Simplifier.simplify(FunctionUtils.substitute(inner, VAR, unshift));
    }
}
