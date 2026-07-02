package backend.math.algebra;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import backend.models.Step;
import backend.parser.ASTNode;
import backend.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionEvaluatorTest {

    private static Result evaluate(String input, String xValue) {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        return evaluator.evaluateAt(
                ExpressionEvaluator.expressionFromQuestion(input, null, "evaluate"),
                "x",
                xValue);
    }

    private static Result solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("evaluate", new ExpressionEvaluator());
        return engine.solve(new Question("Algebra", "evaluate", input));
    }

    @Test
    void quadraticAtInteger() {
        Result r = evaluate("x^2 + 3*x", "2");
        assertTrue(r.isSuccess());
        assertEquals("10", r.getExact().toDisplay());
    }

    @Test
    void factoredForm() {
        Result r = evaluate("(x-4)^2", "7");
        assertTrue(r.isSuccess());
        assertEquals("9", r.getExact().toDisplay());
    }

    @Test
    void fractionValue() {
        Result r = evaluate("2*x", "3/2");
        assertTrue(r.isSuccess());
        assertEquals("3", r.getExact().toDisplay());
    }

    @Test
    void inlineXSyntax() {
        Result r = solve("x^2 + 1 @ x=3");
        assertTrue(r.isSuccess());
        assertEquals("10", r.getExact().toDisplay());
    }

    @Test
    void functionNotation() {
        Result r = evaluate("f(x)=2*x+1", "5");
        assertTrue(r.isSuccess());
        assertEquals("11", r.getExact().toDisplay());
    }

    @Test
    void evaluateAtDoubleForGraphing() {
        ASTNode expr = Parser.parse("x^2 + sin(x)");
        assertEquals(9, ExpressionEvaluator.evaluateAtDouble(expr, "x", 3), 1e-9);
        assertEquals(0, ExpressionEvaluator.evaluateAtDouble(Parser.parse("sin(x)"), "x", 0), 1e-9);
    }
}
