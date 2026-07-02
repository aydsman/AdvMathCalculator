package backend.math.calculus.differential;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImplicitDerivativeTest {

    private static Result solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("implicitDerivative", new ImplicitDerivative());
        return engine.solve(new Question("Calculus", "implicitDerivative", input));
    }

    private static String dyDx(String input) {
        Result r = solve(input);
        assertTrue(r.isSuccess(), r.getError());
        return r.getExact().toDisplay();
    }

    @Test
    void circle() {
        assertEquals("-x/y", dyDx("x^2 + y^2 = 1"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "x*y = 1, -y/x",
            "x^2 + y^2 - 1, -x/y",
            "y = x^2, 2*x",
    })
    void equations(String input, String expected) {
        assertEquals(expected, dyDx(input));
    }
}
