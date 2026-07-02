package backend.math.algebra;

import backend.math.calculus.limits.Limit;
import backend.models.Result;
import backend.parser.ASTNode;
import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexFractionsTest {

    @Test
    void combinesDifferenceOfFractions() {
        ASTNode input = Parser.parse("1/(3+x)-1/(3-x)");
        ASTNode result = Simplifier.simplify(ComplexFractions.simplify(input));
        assertEquals("-2*x/((x+3)*(-x+3))", result.toDisplay());
    }

    @Test
    void combinesNestedComplexFractionForLimit() {
        Limit limit = new Limit();
        Result result = limit.solveFromInput("lim x->0 (1/(3+x)-1/(3-x))/x");
        assertTrue(result.isSuccess());
        assertEquals("-2/9", result.getExact().toDisplay());
    }
}
