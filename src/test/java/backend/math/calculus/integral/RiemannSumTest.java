package backend.math.calculus.integral;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiemannSumTest {

    private final MathEngine engine = new MathEngine();

    RiemannSumTest() {
        engine.register("riemannSum", new RiemannSum());
    }

    @Test
    void parsesIntegrandOnSubmit() {
        Question question = new Question("Calculus", "riemannSum", "x^2");
        Result result = engine.solve(question);

        assertTrue(result.isSuccess());
        assertEquals("x^2", result.getExact().toDisplay());
    }

    @Test
    void symbolicStepsUseSummationIdentities() {
        RiemannSum riemann = new RiemannSum();
        Result result = riemann.solveWithBounds(
                RiemannSum.parseIntegrand("x^2"), "x", "0", "4", "10");

        assertTrue(result.isSuccess());
        assertTrue(result.getSteps().size() >= 8);
        assertEquals("Definite integral", result.getSteps().get(0).getDescription());
        assertTrue(result.getSteps().get(0).hasDefiniteIntegral());
        assertEquals("0", result.getSteps().get(0).getDefiniteIntegral().lower());
        assertEquals("4", result.getSteps().get(0).getDefiniteIntegral().upper());
        assertEquals("x^2", result.getSteps().get(0).getDefiniteIntegral().integrand());

        boolean hasSumIdentity = result.getSteps().stream()
                .anyMatch(step -> step.getDescription().equals("Apply summation identity")
                        && step.getMathLine() != null
                        && step.getMathLine().contains("n(n-1)(2n-1)/6"));
        assertTrue(hasSumIdentity);

        assertNotNull(result.getExact());
        assertEquals(18.24, result.getDecimal(), 1e-9);
    }

    @Test
    void constantIntegrandUsesLinearSummation() {
        RiemannSum riemann = new RiemannSum();
        Result result = riemann.solveWithBounds(
                RiemannSum.parseIntegrand("2"), "x", "0", "4", "4");

        assertTrue(result.isSuccess());
        assertEquals(8, result.getDecimal(), 1e-9);

        boolean hasNIdentity = result.getSteps().stream()
                .anyMatch(step -> step.getMathLine() != null && step.getMathLine().contains("= n"));
        assertTrue(hasNIdentity);
    }

    @Test
    void wrapsSumsInIntegrand() {
        RiemannSum riemann = new RiemannSum();
        Result result = riemann.solveWithBounds(
                RiemannSum.parseIntegrand("x^2+2"), "x", "0", "4", "10");

        assertTrue(result.isSuccess());
        assertEquals("(x^2+2)", result.getSteps().get(0).getDefiniteIntegral().integrand());
    }
}
