package backend.math.algebra;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InequalitiesTest {

    private static String solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("inequalities", new Inequalities());
        Result result = engine.solve(new Question("Algebra", "inequalities", input));
        assertTrue(result.isSuccess(), result.getError());
        return result.getExact().toDisplay();
    }

    @Test
    void solvesLinearInequality() {
        assertEquals("x > 2", solve("2*x + 3 > 7"));
        assertEquals("x < -2", solve("-2*x + 3 > 7"));
    }

    @Test
    void solvesQuadraticInequality() {
        assertEquals("(-2, 2)", solve("x^2 - 4 < 0"));
        assertEquals("(-inf, -2) U (2, inf)", solve("x^2 - 4 > 0"));
    }

    @Test
    void handlesAlwaysTrueConstant() {
        assertEquals("All real numbers", solve("5 > 3"));
    }
}
