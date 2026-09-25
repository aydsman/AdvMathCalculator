package backend.math.calculus.differential;

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

class TangentLineTest {

    private MathEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MathEngine();
        engine.register("tangentLine", new TangentLine());
    }

    private Result solve(String input) {
        return engine.solve(new Question("Calculus", "tangentLine", input));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "x^2 at x=2 | y = 4*x - 4",
            "x^2 @ x=3 | y = 6*x - 9",
            "f(x)=x^2, x=1 | y = 2*x - 1",
            "y=x^2 + 1 at x=0 | y = 1",
            "2*x + 1 at x=4 | y = 2*x + 1",
    })
    void findsTangentLine(String input, String expected) {
        Result result = solve(input.trim());
        assertTrue(result.isSuccess(), result.getError());
        assertEquals(expected.trim(), result.getExact().toDisplay());
        assertTrue(result.getSteps().stream()
                .anyMatch(s -> s.getDescription().contains("Point-slope")));
    }

    @Test
    void requiresPointOfTangency() {
        Result result = solve("x^2");
        assertFalse(result.isSuccess());
        assertTrue(result.getError().toLowerCase().contains("point"));
    }
}
