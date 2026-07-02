package backend.math.algebra;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import backend.parser.ASTNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquationSolverTest {

    private static Result solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("solveEquation", new EquationSolver());
        return engine.solve(new Question("Algebra", "solveEquation", input));
    }

    private static String solutions(String input) {
        Result result = solve(input);
        assertTrue(result.isSuccess(), result.getError());
        return String.join(", ", result.getSolutions().stream().map(r -> "x = " + r.toDisplay()).toList());
    }

    @Test
    void solvesLinearEquation() {
        assertEquals("x = -3", solutions("x + 3 = 0"));
        assertEquals("x = 2", solutions("2*x - 4 = 0"));
    }

    @Test
    void factorsQuadraticWhenPossible() {
        assertEquals("x = 2, x = 3", solutions("x^2 - 5*x + 6 = 0"));
        assertEquals("x = -2, x = 2", solutions("x^2 - 4 = 0"));
    }

    @Test
    void usesQuadraticFormulaWithRadicals() {
        assertEquals("x = sqrt(2), x = -sqrt(2)", solutions("x^2 - 2 = 0"));
    }

    @Test
    void assumesExpressionEqualsZeroWithoutEqualsSign() {
        assertEquals("x = -2, x = -3", solutions("x^2 + 5*x + 6"));
    }

    @Test
    void reportsNoRealSolutions() {
        Result result = solve("x^2 + x + 1 = 0");
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("No real solutions"));
    }

    @Test
    void toZeroFormRewritesEquation() {
        ASTNode zero = EquationSolver.toZeroForm("x^2 - 4 = 0");
        assertEquals("x^2 - 4", zero.toDisplay());
    }
}
