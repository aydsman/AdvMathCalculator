package backend.math.trig;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrigTest {

    private static Result solve(String op, String input) {
        MathEngine engine = new MathEngine();
        engine.register("unitCircle", new UnitCircle());
        engine.register("identities", new Identities());
        engine.register("trigEquation", new TrigEquationSolver());
        return engine.solve(new Question("Trigonometry", op, input));
    }

    @Test
    void unitCircleDegrees() {
        Result r = solve("unitCircle", "30");
        assertTrue(r.isSuccess());
        assertTrue(r.getSolutions().stream().anyMatch(s -> s.toDisplay().equals("sin = 1/2")));
        assertTrue(r.getSolutions().stream().anyMatch(s -> s.toDisplay().equals("cos = sqrt(3)/2")));
    }

    @Test
    void unitCircleRadians() {
        Result r = solve("unitCircle", "pi/4");
        assertTrue(r.isSuccess());
        assertTrue(r.getSolutions().stream().anyMatch(s -> s.toDisplay().equals("sin = sqrt(2)/2")));
        assertTrue(r.getSolutions().stream().anyMatch(s -> s.toDisplay().equals("cos = sqrt(2)/2")));
    }

    @Test
    void unitCircleExtras() {
        UnitCircle uc = new UnitCircle();
        Result r = uc.analyze("45", UnitCircleOptions.allEnabled());
        assertTrue(r.isSuccess());
        assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Quadrant")));
        assertTrue(r.getSolutions().stream().anyMatch(s -> s.toDisplay().startsWith("csc")));
    }

    @Test
    void pythagoreanIdentity() {
        Result r = solve("identities", "sin(x)^2 + cos(x)^2");
        assertTrue(r.isSuccess());
        assertEquals("1", r.getExact().toDisplay());
    }

    @Test
    void solvesSinEquation() {
        Result r = solve("trigEquation", "sin(x) = 1/2");
        assertTrue(r.isSuccess(), r.getError());
        assertEquals(2, r.getSolutions().size());
        assertTrue(r.getSolutions().get(0).toDisplay().contains("pi/6"));
    }

    @Test
    void solvesScaledCosEquation() {
        Result r = solve("trigEquation", "2*cos(x) - 1 = 0");
        assertTrue(r.isSuccess(), r.getError());
        assertEquals(2, r.getSolutions().size());
    }

    @Test
    void implicitSinNotation() {
        Result r = solve("trigEquation", "sinx = 0");
        assertTrue(r.isSuccess(), r.getError());
        assertEquals(1, r.getSolutions().size());
    }
}
