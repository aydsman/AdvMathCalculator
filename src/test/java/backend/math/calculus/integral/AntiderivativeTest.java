package backend.math.calculus.integral;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiderivativeTest {

    private MathEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MathEngine();
        engine.register("indefiniteIntegral", new Antiderivative());
    }

    private Result solve(String input) {
        return engine.solve(new Question("Calculus", "indefiniteIntegral", input));
    }

    @ParameterizedTest(name = "∫ {0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "x^2 | 1/3*x^3 + C",
            "x | 1/2*x^2 + C",
            "5 | 5*x + C",
            "sin(x) | -cos(x) + C",
            "cos(x) | sin(x) + C",
            "6*sin(2*x+1) | -3*cos(2*x + 1) + C",
            "sin(2*x) | -1/2*cos(2*x) + C",
            "4*cos(2*x) | 2*sin(2*x) + C",
            "1/x | ln(abs(x)) + C",
            "1/(2*x+3) | 1/2*ln(abs(2*x + 3)) + C",
            "(2*x+1)^3 | 1/8*(2*x + 1)^4 + C",
            "3*(2*x+1)^2 | 1/2*(2*x + 1)^3 + C",
            "sqrt(x) | 2/3*x^3/2 + C",
            "sec(x)^2 | tan(x) + C",
            "csc(3*x)^2 | -1/3*cot(3*x) + C",
    })
    void integratesAfBxC(String input, String expected) {
        Result result = solve(input.trim());
        assertTrue(result.isSuccess(), () -> input + " -> " + result.getError());
        assertEquals(expected.trim(), result.getExact().toDisplay());
    }

    @ParameterizedTest(name = "∫ {0} = {1} (u-sub)")
    @CsvSource(delimiter = '|', value = {
            "2*x*sin(x^2) | -cos(x^2) + C",
            "x*sin(x^2) | -1/2*cos(x^2) + C",
            "2*x*cos(x^2) | sin(x^2) + C",
            "3*x^2*cos(x^3) | sin(x^3) + C",
            "2*x*(x^2)^3 | 1/4*x^8 + C",
            "x/(x^2+1) | 1/2*ln(abs(x^2 + 1)) + C",
    })
    void integratesSimpleUSub(String input, String expected) {
        Result result = solve(input.trim());
        assertTrue(result.isSuccess(), () -> input + " -> " + result.getError());
        assertEquals(expected.trim(), result.getExact().toDisplay());
    }

    @Test
    void uSubExplainsSubstitutionInSteps() {
        Result result = solve("2*x*sin(x^2)");
        assertTrue(result.isSuccess(), result.getError());
        assertTrue(result.getSteps().stream()
                .anyMatch(s -> s.getDescription().toLowerCase().contains("u-substitution")
                        || s.getDescription().contains("u = ")));
        assertEquals("-cos(x^2) + C", result.getExact().toDisplay());
    }

    @Test
    void stripsIntegralWrappers() {
        Result result = solve("integral 2*x dx");
        assertTrue(result.isSuccess(), result.getError());
        assertEquals("x^2 + C", result.getExact().toDisplay());
    }

    @Test
    void explainsLinearCompositionInSteps() {
        Result result = solve("6*sin(2*x+1)");
        assertTrue(result.isSuccess(), result.getError());
        assertTrue(result.getSteps().stream()
                .anyMatch(s -> s.getDescription().contains("Constant multiple")));
        assertTrue(result.getSteps().stream()
                .anyMatch(s -> s.getDescription().contains("Linear composition")
                        || s.getDescription().contains("a/b")));
        assertTrue(result.getSteps().stream()
                .anyMatch(s -> s.getDescription().contains("constant of integration")));
    }

    @Test
    void rejectsUnsupportedShapes() {
        Result result = solve("sin(x)*cos(x)");
        assertFalse(result.isSuccess());
        assertTrue(result.getError().toLowerCase().contains("supported")
                || result.getError().contains("Could not integrate"));
    }

    @Test
    void rejectsSumOfDifferentForms() {
        Result result = solve("sin(x) + cos(x)");
        assertFalse(result.isSuccess());
    }
}
