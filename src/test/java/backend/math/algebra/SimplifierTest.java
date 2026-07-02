package backend.math.algebra;

import backend.parser.ASTNode;
import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimplifierTest {

    private static String simplify(String input) {
        ASTNode result = Simplifier.simplify(Parser.parse(input));
        return result.toDisplay();
    }

    @Test
    void foldsConstantArithmetic() {
        assertEquals("5", simplify("2 + 3"));
        assertEquals("10", simplify("2*3 + 4"));
        assertEquals("2", simplify("6/3"));
        assertEquals("1", simplify("1/2 + 1/2"));
        assertEquals("1024", simplify("2^10"));
    }

    @Test
    void appliesAdditiveIdentities() {
        assertEquals("x", simplify("x + 0"));
        assertEquals("x", simplify("0 + x"));
        assertEquals("0", simplify("x - x"));
    }

    @Test
    void appliesMultiplicativeIdentities() {
        assertEquals("x", simplify("x*1"));
        assertEquals("x", simplify("1*x"));
        assertEquals("0", simplify("x*0"));
    }

    @Test
    void appliesPowerIdentities() {
        assertEquals("x", simplify("x^1"));
        assertEquals("1", simplify("x^0"));
        assertEquals("x^6", simplify("(x^2)^3"));
    }

    @Test
    void combinesLikeTermsInSums() {
        assertEquals("2*x", simplify("x + x"));
        assertEquals("4*x", simplify("3*x + x"));
        assertEquals("-3*x", simplify("2*x - 5*x"));
        assertEquals("3*x + 3*y", simplify("2*x + 3*y + x"));
    }

    @Test
    void combinesPowersInProducts() {
        assertEquals("x^2", simplify("x*x"));
        assertEquals("x^5", simplify("x^2 * x^3"));
        assertEquals("2*x^2", simplify("2*x*x"));
        assertEquals("-sin(x)^2", simplify("sin(x)*-sin(x)"));
        assertEquals("cos(x)^2 - sin(x)^2", simplify("cos(x)*cos(x) + sin(x)*-sin(x)"));
    }

    @Test
    void cancelsDivisionOfEqualExpressions() {
        assertEquals("1", simplify("x/x"));
        assertEquals("x", simplify("x/1"));
    }

    @Test
    void keepsAndRendersSubtractionNicely() {
        assertEquals("x - 3", simplify("x - 3"));
        assertEquals("x + 3", simplify("3 + x"));
    }

    @Test
    void foldsSelectedFunctions() {
        assertEquals("2", simplify("sqrt(4)"));
        assertEquals("0", simplify("sin(0)"));
        assertEquals("1", simplify("cos(0)"));
        assertEquals("0", simplify("ln(1)"));
    }

    @Test
    void leavesIrreducibleExpressionsUnchanged() {
        assertEquals("x + y", simplify("x + y"));
        assertEquals("sin(x) + 1", simplify("sin(x) + 1"));
    }
}
