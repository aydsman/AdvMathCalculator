package backend.math.algebra;

import backend.math.calculus.limits.Limit;
import backend.models.Result;
import backend.parser.ASTNode;
import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RationalizerTest {

    @Test
    void rationalizesSqrtMinusConstant() {
        ASTNode input = Parser.parse("(sqrt(x+1)-2)/(x^2-9)");
        ASTNode result = Rationalizer.rationalize(input).orElseThrow();
        assertEquals("(x-3)/((x^2-9)*(sqrt(x+1)+2))", Simplifier.simplify(result).toDisplay());
    }

    @Test
    void rationalizesForLimit() {
        Limit limit = new Limit();
        Result result = limit.solveFromInput("lim x->3 (sqrt(x+1)-2)/(x^2-9)");
        assertTrue(result.isSuccess());
        assertEquals("1/24", result.getExact().toDisplay());
    }

    @Test
    void rationalizesDifferenceOfSquareRoots() {
        ASTNode input = Parser.parse("(sqrt(x+2)-sqrt(2-x))/x");
        ASTNode result = Rationalizer.rationalize(input).orElseThrow();
        assertEquals("2/(sqrt(x+2)+sqrt(2-x))", Simplifier.simplify(result).toDisplay());
    }

    @Test
    void skipsPolynomialDifference() {
        ASTNode input = Parser.parse("(x^2-4)/(x-2)");
        assertTrue(Rationalizer.rationalize(input).isEmpty());
    }
}
