package backend.engine;

import backend.models.Question;
import backend.models.Result;
import backend.parser.ASTNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathEngineTest {

    /** A stub operation that just echoes the parsed input back as the exact answer. */
    private static MathEngine engineWithEcho() {
        MathEngine engine = new MathEngine();
        engine.register("echo", input -> Result.of(input, null, List.of()));
        return engine;
    }

    @Test
    void routesToRegisteredOperationAndParsesInput() {
        MathEngine engine = engineWithEcho();
        Question question = new Question("Test", "echo", "x^2 + 1");

        Result result = engine.solve(question);

        assertTrue(result.isSuccess());
        assertNotNull(result.getExact());
        assertEquals("x^2 + 1", result.getExact().toDisplay());
    }

    @Test
    void attachesResultBackOntoQuestion() {
        MathEngine engine = engineWithEcho();
        Question question = new Question("Test", "echo", "x");

        Result result = engine.solve(question);

        assertTrue(question.isSolved());
        assertSame(result, question.getResult());
    }

    @Test
    void unknownOperationFailsGracefully() {
        MathEngine engine = engineWithEcho();
        Question question = new Question("Test", "does-not-exist", "x");

        Result result = engine.solve(question);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unknown operation"));
    }

    @Test
    void parseErrorBecomesFailedResult() {
        MathEngine engine = engineWithEcho();
        Question question = new Question("Test", "echo", "x +");

        Result result = engine.solve(question);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Could not parse input"));
    }

    @Test
    void operationExceptionIsCaught() {
        MathEngine engine = new MathEngine();
        engine.register("boom", input -> {
            throw new IllegalStateException("kaboom");
        });
        Question question = new Question("Test", "boom", "x");

        Result result = engine.solve(question);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("kaboom"));
    }

    @Test
    void operationReturningNullBecomesFailure() {
        MathEngine engine = new MathEngine();
        engine.register("nullop", input -> null);
        Question question = new Question("Test", "nullop", "x");

        Result result = engine.solve(question);

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("no result"));
    }

    @Test
    void supportsAndRegisteredOperationsReflectRegistry() {
        MathEngine engine = engineWithEcho();

        assertTrue(engine.supports("echo"));
        assertFalse(engine.supports("missing"));
        assertTrue(engine.registeredOperations().contains("echo"));
    }

    @Test
    void preSimplifiesInputBeforeRouting() {
        MathEngine engine = engineWithEcho();
        Question question = new Question("Test", "echo", "x + x");

        Result result = engine.solve(question);

        assertTrue(result.isSuccess());
        assertEquals("2*x", result.getExact().toDisplay());
    }

    @Test
    void doesNotPreSimplifyForSimplifyOperation() {
        MathEngine engine = new MathEngine();
        engine.register("simplify", new backend.math.algebra.Simplifier());
        Question question = new Question("Algebra", "simplify", "x + x");

        Result result = engine.solve(question);

        assertTrue(result.isSuccess());
        assertEquals("2*x", result.getExact().toDisplay());
    }
}
