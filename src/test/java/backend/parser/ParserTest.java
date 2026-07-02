package backend.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    @Test
    void parsesIntegerLiteral() {
        ASTNode node = Parser.parse("42");
        assertInstanceOf(ASTNode.Num.class, node);
        assertEquals("42", node.toDisplay());
    }

    @Test
    void parsesDecimalAsExactRational() {
        ASTNode node = Parser.parse("3.14");
        ASTNode.Num num = assertInstanceOf(ASTNode.Num.class, node);
        assertEquals("157", num.numerator().toString());
        assertEquals("50", num.denominator().toString());
    }

    @Test
    void respectsAdditiveAndMultiplicativePrecedence() {
        ASTNode node = Parser.parse("2 + 3 * 4");
        assertInstanceOf(ASTNode.Add.class, node);
        assertEquals("2 + 3*4", node.toDisplay());
    }

    @Test
    void parenthesesOverridePrecedence() {
        ASTNode node = Parser.parse("(2 + 3) * 4");
        assertInstanceOf(ASTNode.Mul.class, node);
        assertEquals("(2 + 3)*4", node.toDisplay());
    }

    @Test
    void unaryMinusBindsLooserThanPower() {
        ASTNode node = Parser.parse("-x^2");
        assertInstanceOf(ASTNode.Neg.class, node);
        assertEquals("-x^2", node.toDisplay());
    }

    @Test
    void exponentiationIsRightAssociative() {
        ASTNode node = Parser.parse("2^3^2");
        ASTNode.Pow outer = assertInstanceOf(ASTNode.Pow.class, node);
        assertInstanceOf(ASTNode.Pow.class, outer.exponent());
        assertEquals("2^3^2", node.toDisplay());
    }

    @Test
    void implicitMultiplicationNumberAndVariable() {
        ASTNode node = Parser.parse("2x");
        assertInstanceOf(ASTNode.Mul.class, node);
        assertEquals("2*x", node.toDisplay());
    }

    @Test
    void implicitMultiplicationBetweenParentheses() {
        ASTNode node = Parser.parse("(x+1)(x-1)");
        assertInstanceOf(ASTNode.Mul.class, node);
        assertEquals("(x + 1)*(x - 1)", node.toDisplay());
    }

    @Test
    void parsesFunctionCall() {
        ASTNode node = Parser.parse("3*sin(x)");
        assertEquals("3*sin(x)", node.toDisplay());
    }

    @Test
    void recognisesConstants() {
        assertInstanceOf(ASTNode.Constant.class, Parser.parse("pi"));
        assertEquals("PI", Parser.parse("pi").toDisplay());
        assertEquals("E", Parser.parse("e").toDisplay());
    }

    @Test
    void parsesImplicitTrigCallWithoutParentheses() {
        assertEquals("sin(x)", Parser.parse("sinx").toDisplay());
        assertEquals("sin(x)", Parser.parse("sin x").toDisplay());
        assertEquals("cos(x)", Parser.parse("cosx").toDisplay());
        assertEquals("sqrt(x)", Parser.parse("sqrtx").toDisplay());
    }

    @Test
    void roundTripsACompoundExpression() {
        String input = "x^2 + 3*sin(x) - 1/2";
        assertEquals("x^2 + 3*sin(x) - 1/2", Parser.parse(input).toDisplay());
    }

    @Test
    void rejectsTrailingOperator() {
        assertThrows(Parser.ParseException.class, () -> Parser.parse("x +"));
    }

    @Test
    void rejectsUnbalancedParentheses() {
        assertThrows(Parser.ParseException.class, () -> Parser.parse("(x + 1"));
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(Parser.ParseException.class, () -> Parser.parse("   "));
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThrows(Parser.ParseException.class, () -> Parser.parse("x @ 2"));
    }

    @Test
    void numberReducesToLowestTerms() {
        ASTNode.Num num = assertInstanceOf(ASTNode.Num.class, Parser.parse("0.50"));
        assertEquals("1", num.numerator().toString());
        assertEquals("2", num.denominator().toString());
        assertTrue(num.toDouble() > 0.49 && num.toDouble() < 0.51);
    }
}
