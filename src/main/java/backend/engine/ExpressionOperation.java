package backend.engine;

import backend.math.algebra.Simplifier;
import backend.models.Result;
import backend.parser.ASTNode;
import backend.parser.Parser;

/**
 * An operation whose input is a single expression. The default text entry point
 * parses and simplifies the input before handing it to {@link #solve(ASTNode)}.
 */
@FunctionalInterface
public interface ExpressionOperation extends MathOperation {

    Result solve(ASTNode expression);

    @Override
    default Result solve(String input) {
        return solve(Simplifier.simplify(Parser.parse(input)));
    }
}
