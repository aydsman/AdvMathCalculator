package backend.math.algebra;

import backend.parser.ASTNode.Num;
import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals("(x + 1)^2", factor("x^2 + 2*x + 1"));
    }

    @Test
    void neverDropsNonMonomialFactors() {
        String input = "x^2*(x + 1) - 4*(x + 1)";
        String factored = factor(input);
        assertEquals(Factoring.coefficients(Simplifier.simplify(Parser.parse(input)), "x"),
                Factoring.coefficients(Simplifier.simplify(Parser.parse(factored)), "x"));
    }

    @Test
    void coefficientsExpandProductsAndPowers() {
        assertEquals(Map.of(3, Num.of(1), 2, Num.of(1), 1, Num.of(-4), 0, Num.of(-4)),
                Factoring.coefficients(Parser.parse("x^2*(x + 1) - 4*(x + 1)"), "x"));
        assertEquals(Map.of(2, Num.of(-3), 0, Num.of(1)),
                Factoring.coefficients(Parser.parse("x^2 - (2*x)^2 + 1"), "x"));
    }

    @Test
    void coefficientsRejectNonPolynomials() {
        assertNull(Factoring.coefficients(Parser.parse("sqrt(x^2 + 1)"), "x"));
        assertNull(Factoring.coefficients(Parser.parse("pi*x"), "x"));
        assertNull(Factoring.coefficients(Parser.parse("1/x"), "x"));
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
