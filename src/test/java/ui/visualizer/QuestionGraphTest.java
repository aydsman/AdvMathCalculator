package ui.visualizer;

import backend.engine.MathEngine;
import backend.math.calculus.differential.Derivative;
import backend.models.Question;
import backend.models.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionGraphTest {

    private final MathEngine engine = new MathEngine();

    QuestionGraphTest() {
        engine.register("derivative", new Derivative());
    }

    @Test
    void supportsDerivative() {
        assertTrue(QuestionGraph.supports("derivative"));
        assertFalse(QuestionGraph.supports("simplify"));
    }

    @Test
    void derivativePlotsOriginalAndAnswer() {
        Question question = new Question("Calculus", "derivative", "d/dx(x^2)");
        engine.solve(question);

        assertTrue(QuestionGraph.supports(question));
        var plots = QuestionGraph.plotsFor(question);
        assertEquals(2, plots.size());
        assertEquals("x^2", plots.get(0).getInput());
        assertEquals("2*x", plots.get(1).getExpression().toDisplay());

        Result result = question.getResult();
        assertTrue(result.isSuccess());
        assertEquals("2*x", result.getExact().toDisplay());
    }
}
