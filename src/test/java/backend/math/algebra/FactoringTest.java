package backend.math.algebra;

import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactoringTest {

    private static String factor(String input) {
        return Factoring.factor(Simplifier.simplify(Parser.parse(input))).toDisplay();
    }

    @Test
    void factorsDifferenceOfSquares() {
        assertEquals("(x + 3)*(x - 3)", factor("x^2 - 9"));
        assertEquals("(x + 1)*(x - 1)", factor("x^2 - 1"));
    }

    @Test
    void factorsMonicQuadratic() {
        assertEquals("(x + 2)*(x + 3)", factor("x^2 + 5*x + 6"));
        assertEquals("(x - 2)*(x - 3)", factor("x^2 - 5*x + 6"));
        assertEquals("(x + 1)*(x + 1)", factor("x^2 + 2*x + 1"));
    }

    @Test
    void factorsGeneralQuadratic() {
        assertEquals("(x + 3)*(2*x + 1)", factor("2*x^2 + 7*x + 3"));
    }

    @Test
    void pullsGcfFirst() {
        assertEquals("6*x*(x + 2)", factor("6*x^2 + 12*x"));
        assertEquals("3*(x + 1)^2", factor("3*x^2 + 6*x + 3"));
    }

    @Test
    void leavesPrimeExpressionsUnchanged() {
        assertEquals("x^2 + 1", factor("x^2 + 1"));
        assertEquals("x + y", factor("x + y"));
    }
}
