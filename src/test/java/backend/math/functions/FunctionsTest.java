package backend.math.functions;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionsTest {

    private static Result solve(String op, String input) {
        MathEngine engine = new MathEngine();
        engine.register("domainRange", new DomainRange());
        engine.register("composition", new Composition());
        engine.register("inverse", new InverseFunction());
        engine.register("transformations", new Transformations());
        return engine.solve(new Question("Functions", op, input));
    }

    @Test
    void domainAndRangeOfQuadratic() {
        Result result = solve("domainRange", "f(x)=x^2+1");
        assertTrue(result.isSuccess());
        assertEquals("Domain: All real numbers", result.getSolutions().get(0).toDisplay());
        assertEquals("Range: [1, inf)", result.getSolutions().get(1).toDisplay());
    }

    @Test
    void composesFunctions() {
        Result result = solve("composition", "f(x)=x+1, g(x)=x^2");
        assertTrue(result.isSuccess());
        assertEquals("x^2 + 1", result.getExact().toDisplay());
    }

    @Test
    void invertsLinearFunction() {
        Result result = solve("inverse", "f(x)=2*x+3");
        assertTrue(result.isSuccess());
        assertEquals("(x - 3)/2", result.getExact().toDisplay());
    }

    @Test
    void describesTransformations() {
        Result result = solve("transformations", "x^2 | 2*(x-1)^2 + 3");
        assertTrue(result.isSuccess());
        assertTrue(result.getExact().toDisplay().contains("Vertical stretch by 2"));
    }
}
