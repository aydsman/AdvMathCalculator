package backend.math.functions;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainRangeTest {

    private static Result solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("domainRange", new DomainRange());
        return engine.solve(new Question("Functions", "domainRange", input));
    }

    private static String domain(String input) {
        Result r = solve(input);
        assertTrue(r.isSuccess(), r.getError());
        return r.getSolutions().get(0).toDisplay();
    }

    private static String range(String input) {
        Result r = solve(input);
        assertTrue(r.isSuccess(), r.getError());
        return r.getSolutions().get(1).toDisplay();
    }

    @Test
    void quadraticWithVerticalShift() {
        assertEquals("Domain: All real numbers", domain("f(x)=x^2+1"));
        assertEquals("Range: [1, inf)", range("f(x)=x^2+1"));
    }

    @Test
    void linearFunction() {
        assertEquals("Domain: All real numbers", domain("f(x)=2*x-3"));
        assertEquals("Range: All real numbers", range("f(x)=2*x-3"));
    }

    @Test
    void constantFunction() {
        assertEquals("Domain: All real numbers", domain("f(x)=5"));
        assertEquals("Range: {5}", range("f(x)=5"));
    }

    @Test
    void rationalFunction() {
        assertEquals("Domain: x != 0", domain("f(x)=1/x"));
        assertEquals("Range: All real numbers except 0", range("f(x)=1/x"));
    }

    @Test
    void squareRoot() {
        assertEquals("Domain: x >= 0", domain("f(x)=sqrt(x)"));
        assertEquals("Range: [0, inf)", range("f(x)=sqrt(x)"));
    }

    @Test
    void downwardOpeningQuadratic() {
        assertEquals("Domain: All real numbers", domain("f(x)=-x^2+4"));
        assertEquals("Range: (-inf, 4]", range("f(x)=-x^2+4"));
    }

    @Test
    void cubic() {
        assertEquals("Domain: All real numbers", domain("f(x)=x^3-x"));
        assertEquals("Range: All real numbers", range("f(x)=x^3-x"));
    }

    @Test
    void sine() {
        assertEquals("Domain: All real numbers", domain("f(x)=sin(x)"));
        assertEquals("Range: [-1, 1]", range("f(x)=sin(x)"));
    }

    @Test
    void shiftedSquare() {
        assertEquals("Domain: All real numbers", domain("(x-4)^2"));
        assertEquals("Range: [0, inf)", range("(x-4)^2"));
    }

    @Test
    void downwardShiftedSquare() {
        assertEquals("Domain: All real numbers", domain("-(x-4)^2"));
        assertEquals("Range: (-inf, 0]", range("-(x-4)^2"));
    }

    @Test
    void naturalLogWithoutParentheses() {
        assertEquals("Domain: x > 0", domain("lnx"));
        assertEquals("Range: All real numbers", range("lnx"));
    }

    @Test
    void sineWithoutParentheses() {
        assertEquals("Domain: All real numbers", domain("f(x)=sinx"));
        assertEquals("Range: [-1, 1]", range("f(x)=sinx"));
    }

    @Test
    void expressionWithoutFunctionNotation() {
        assertEquals("Domain: All real numbers", domain("x^2+1"));
        assertEquals("Range: [1, inf)", range("x^2+1"));
    }

    @Test
    void compositeWithImplicitLog() {
        assertEquals("Domain: x > 0", domain("sinx + (lnx - 1)^3"));
        assertEquals("Range: Unable to determine", range("sinx + (lnx - 1)^3"));
    }

    @Test
    void logPowerAlone() {
        assertEquals("Domain: x > 0", domain("(lnx-1)^3"));
        assertEquals("Range: All real numbers", range("(lnx-1)^3"));
    }

    @Test
    void sumOfTrigFunctions() {
        assertEquals("Domain: All real numbers", domain("sinx+cosx"));
        assertEquals("Range: Unable to determine", range("sinx+cosx"));
    }

    @Test
    void shiftedReciprocal() {
        assertEquals("Domain: x - 1 != 0", domain("1/(x-1)"));
        assertEquals("Range: All real numbers except 0", range("1/(x-1)"));
    }
}
