package backend.math.calculus.limits;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitTest {

    private MathEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MathEngine();
        engine.register("limit", new Limit());
    }

    private Result solve(String input) {
        return engine.solve(new Question("Calculus", "limit", input));
    }

    @ParameterizedTest(name = "lim {0} = {1}")
    @CsvSource({
            "lim x->2 x^2 + 3, 7",
            "lim x->3 2*x - 1, 5",
            "lim x->0 5, 5",
            "lim x->1 x^2 + 2*x + 1, 4",
    })
    void directSubstitution(String input, String expected) {
        Result result = solve(input);
        assertTrue(result.isSuccess(), result.getError());
        assertEquals(expected, result.getExact().toDisplay());
    }

    @Test
    void factorsPolynomialZeroOverZero() {
        Result result = solve("lim x->2 (x^2-4)/(x-2)");

        assertTrue(result.isSuccess(), result.getError());
        assertEquals("4", result.getExact().toDisplay());
        assertTrue(result.getSteps().stream()
                .anyMatch(step -> step.getDescription().contains("0/0")));
        assertTrue(result.getSteps().stream()
                .anyMatch(step -> step.getDescription().contains("Factor the numerator")));
    }

    @Test
    void factorsDifferenceOfSquares() {
        Result result = solve("lim x->3 (x^2-9)/(x-3)");

        assertTrue(result.isSuccess(), result.getError());
        assertEquals("6", result.getExact().toDisplay());
    }

    @Test
    void nonzeroOverZeroIsNotFinite() {
        Result result = solve("lim x->1 (x+2)/(x-1)");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not exist") || result.getError().contains("\u221e"));
    }

    @Test
    void sinOverXNeedsLhopital() {
        Result result = solve("lim x->0 sin(x)/x");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().toLowerCase().contains("l'h"));
    }

    @Test
    void nonzeroNumeratorOverZeroIsNotFinite() {
        Result result = solve("lim x->0 (x^2 + 1)/(x^2)");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not exist") || result.getError().contains("\u221e"));
    }

    @Test
    void infinityNotSupportedYet() {
        Result result = solve("lim x->inf 1/x");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("not supported"));
    }

    @Test
    void parsesLimitWithParentheses() {
        Result result = solve("lim(x->2) (x^2-4)/(x-2)");

        assertTrue(result.isSuccess(), result.getError());
        assertEquals("4", result.getExact().toDisplay());
    }

    @Nested
    class Worksheet {

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "lim x->0 (x^2-25)/(x^2-4*x-5), 5",
                "lim x->5 (x^2-25)/(x^2-4*x-5), 5/3",
                "lim x->1 (7*x^2-4*x-3)/(3*x^2-4*x+1), 5",
                "lim x->3 (sqrt(x^2+7)-3)/(x+3), 1/6",
                "lim x->7 (2*x-14)^(1/6), 0",
                "lim x->0 (1/(3+x)-1/(3-x))/x, -2/9",
                "lim x->3 (sqrt(x+1)-2)/(x^2-9), 1/24",
        })
        void passes(String input, String expected) {
            Result result = solve(input);
            assertTrue(result.isSuccess(), result.getError());
            assertEquals(expected, result.getExact().toDisplay());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "lim x->2 2*x/(x^2-4)",
                "lim x->-1 3*x/(x^2+2*x+1)",
                "lim x->-1 (x^2-25)/(x^2-4*x-5)",
                "lim x->3 (sqrt(x^2-5)+2)/(x-3)",
        })
        void verticalAsymptote(String input) {
            Result result = solve(input);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("not exist") || result.getError().contains("\u221e"));
        }

        @Test
        void rationalizesDifferenceOfSquareRoots() {
            Result result = solve("lim x->0 (sqrt(x+2)-sqrt(2-x))/x");
            assertTrue(result.isSuccess(), result.getError());
            assertEquals("1/sqrt(2)", result.getExact().toDisplay());
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "lim x->0 sin(x)/x",
                "lim x->1 (x^(1/3)-1)/(sqrt(x)-1)",
        })
        void needsFutureMethod(String input) {
            Result result = solve(input);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().toLowerCase().contains("l'h")
                    || result.getError().contains("not supported"));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({
                "lim x->inf 1/x",
                "lim x->-inf sqrt(x-2)-sqrt(x)",
                "lim x->inf (x^4-10)/(4*x^3+x)",
        })
        void infinityNotSupported(String input) {
            Result result = solve(input);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("not supported"));
        }

        @Test
        void absoluteValueNotSupported() {
            Result result = solve("lim x->-3 abs(x+1)+3/x");
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("abs"));
        }

        @Test
        void oneSidedNotSupported() {
            Result result = solve("lim x->1- sqrt(3-3*x)");
            assertFalse(result.isSuccess());
        }
    }
}
