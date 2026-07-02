package backend.math.functions;

import backend.engine.MathOperation;
import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the transformations applied to a parent function to produce a given graph.
 *
 * <p>Input format: {@code parent | transformed}, e.g. {@code x^2 | 2*(x-1)^2 + 3}.
 */
public class Transformations implements MathOperation {

    private static final String VAR = "x";

    @Override
    public Result solve(ASTNode input) {
        return Result.failure("Enter parent | transformed, e.g. x^2 | 2*(x-1)^2 + 3");
    }

    public Result solveFromInput(String raw) {
        List<Step> steps = new ArrayList<>();
        String[] parts = raw.split("\\|");
        if (parts.length != 2) {
            throw new Parser.ParseException("Use the format: parent | transformed  (e.g. x^2 | 2*(x-1)^2 + 3)");
        }

        ASTNode parent = Simplifier.simplify(Parser.parse(parts[0].trim()));
        ASTNode transformed = Simplifier.simplify(Parser.parse(parts[1].trim()));

        steps.add(new Step("Parent function", parent));
        steps.add(new Step("Transformed function", transformed));

        Analysis analysis = analyze(transformed);
        List<String> descriptions = describe(parent, analysis);

        for (String desc : descriptions) {
            steps.add(new Step(desc));
        }

        String summary = String.join("; ", descriptions);
        return Result.of(new Constant(summary), null, steps);
    }

    private record Analysis(Num verticalScale, Num horizontalScale, ASTNode horizontalShift,
                          Num verticalShift, ASTNode innerPower) {}

    private static Analysis analyze(ASTNode expr) {
        Num verticalScale = Num.of(1);
        Num horizontalScale = Num.of(1);
        Num verticalShift = Num.of(0);
        ASTNode horizontalShift = Num.of(0);
        ASTNode core = expr;

        if (core instanceof Add a) {
            verticalShift = extractConstant(a.right());
            core = a.left();
        } else if (core instanceof Sub s && s.right() instanceof Num n) {
            verticalShift = new Num(n.numerator().negate(), n.denominator());
            core = s.left();
        }

        if (core instanceof Mul m && m.left() instanceof Num n) {
            verticalScale = n;
            core = m.right();
        }

        if (core instanceof Pow p) {
            ASTNode base = p.base();
            if (base instanceof Add a && a.left() instanceof Var v && v.name().equals(VAR)
                    && a.right() instanceof Num h) {
                horizontalShift = new Num(h.numerator().negate(), h.denominator());
            } else if (base instanceof Sub s && s.left() instanceof Var v && v.name().equals(VAR)
                    && s.right() instanceof Num h) {
                horizontalShift = h;
            } else if (base instanceof Mul m && m.left() instanceof Num b && m.right() instanceof Var v
                    && v.name().equals(VAR)) {
                horizontalScale = b;
            }
            return new Analysis(verticalScale, horizontalScale, horizontalShift, verticalShift, p);
        }

        if (core instanceof Add a && a.left() instanceof Var v && v.name().equals(VAR)) {
            horizontalShift = extractConstant(a.right());
            if (horizontalShift instanceof Num n) {
                horizontalShift = new Num(n.numerator().negate(), n.denominator());
            }
        }

        return new Analysis(verticalScale, horizontalScale, horizontalShift, verticalShift, core);
    }

    private static Num extractConstant(ASTNode node) {
        if (node instanceof Num n) {
            return n;
        }
        if (node instanceof Neg n && n.operand() instanceof Num num) {
            return new Num(num.numerator().negate(), num.denominator());
        }
        return Num.of(0);
    }

    private static List<String> describe(ASTNode parent, Analysis a) {
        List<String> out = new ArrayList<>();
        out.add("Parent: " + parent.toDisplay());

        if (!a.verticalScale().equals(Num.of(1))) {
            out.add("Vertical stretch by " + a.verticalScale().toDisplay());
        }
        if (!a.horizontalScale().equals(Num.of(1))) {
            out.add("Horizontal scale by " + a.horizontalScale().toDisplay());
        }
        if (a.horizontalShift() instanceof Num h && !h.isZero()) {
            if (h.numerator().signum() > 0) {
                out.add("Shift right by " + h.toDisplay());
            } else {
                out.add("Shift left by " + new Num(h.numerator().negate(), h.denominator()).toDisplay());
            }
        }
        if (!a.verticalShift().isZero()) {
            if (a.verticalShift().numerator().signum() > 0) {
                out.add("Shift up by " + a.verticalShift().toDisplay());
            } else {
                out.add("Shift down by " + new Num(a.verticalShift().numerator().negate(),
                        a.verticalShift().denominator()).toDisplay());
            }
        }
        if (a.innerPower() instanceof Pow p) {
            out.add("Apply parent power to the inner expression: (" + p.base().toDisplay() + ")^"
                    + p.exponent().toDisplay());
        }
        if (out.size() == 1) {
            out.add("No standard transformations detected beyond the parent form");
        }
        return out;
    }
}
