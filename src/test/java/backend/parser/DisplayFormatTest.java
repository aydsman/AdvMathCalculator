package backend.parser;

import backend.math.algebra.ExpressionEvaluator;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayFormatTest {

    private static final Var X = new Var("x");

    @Test
    void rationalCoefficientsBecomeDenominators() {
        assertEquals("sqrt(3)/2", new Mul(Num.of(1, 2), new Func("sqrt", Num.of(3))).toDisplay());
        assertEquals("5*pi/6", new Mul(Num.of(5, 6), new ASTNode.Constant("PI")).toDisplay());
        assertEquals("-x^2/3", new Mul(Num.of(-1, 3), new Pow(X, Num.of(2))).toDisplay());
    }

    @Test
    void reciprocalFactorsBecomeDenominators() {
        assertEquals("x/(x + 1)", new Mul(X, new Div(Num.of(1), new Add(X, Num.of(1)))).toDisplay());
        assertEquals("2/sqrt(3)", new Mul(Num.of(2), new Div(Num.of(1), new Func("sqrt", Num.of(3)))).toDisplay());
    }

    @Test
    void fractionalExponentsAreParenthesised() {
        assertEquals("x^(3/2)", new Pow(X, Num.of(3, 2)).toDisplay());
    }

    @Test
    void leadingMinusFoldsIntoOperator() {
        assertEquals("x - 3", new Add(X, Num.of(-3)).toDisplay());
        assertEquals("x - 2*x", new Add(X, new Mul(Num.of(-2), X)).toDisplay());
        assertEquals("x + 3", new Sub(X, new Neg(Num.of(3))).toDisplay());
        assertEquals("x - (x + 1)", new Sub(X, new Add(X, Num.of(1))).toDisplay());
    }

    @Test
    void renderedOutputParsesBackToSameValue() {
        String[] inputs = {"x^(3/2)/3", "x - 2*x/5", "1/2*sqrt(x) - -x", "x*1/(x + 1)", "3 - -2*x^2"};
        for (String input : inputs) {
            ASTNode original = Parser.parse(input);
            ASTNode reparsed = Parser.parse(original.toDisplay());
            for (double x : new double[] {0.7, 2.3}) {
                assertEquals(ExpressionEvaluator.evaluateAtDouble(original, "x", x),
                        ExpressionEvaluator.evaluateAtDouble(reparsed, "x", x), 1e-12, input);
            }
        }
    }
}
