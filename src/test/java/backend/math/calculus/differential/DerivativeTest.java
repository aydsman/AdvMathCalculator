package backend.math.calculus.differential;

import backend.engine.MathEngine;
import backend.models.Question;
import backend.models.Result;
import backend.models.Step;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivativeTest {

    private static Result solve(String input) {
        MathEngine engine = new MathEngine();
        engine.register("derivative", new Derivative());
        return engine.solve(new Question("Calculus", "derivative", input));
    }

    private static String derivative(String input) {
        Result r = solve(input);
        assertTrue(r.isSuccess(), r.getError());
        return r.getExact().toDisplay();
    }

    @Nested
    class ConstantsAndVariables {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @CsvSource({
                "x, 1",
                "x^1, 1",
                "2*x, 2",
                "-x, -1",
                "5, 0",
                "7, 0",
                "x^0, 0",
        })
        void basic(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class PowerRule {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#powerRuleCases")
        void powers(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class SumRule {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#sumRuleCases")
        void sums(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class ProductRule {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#productRuleCases")
        void products(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class QuotientRule {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#quotientRuleCases")
        void quotients(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class TrigBasics {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#trigBasicCases")
        void trig(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class ChainRule {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#chainRuleCases")
        void chained(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class LogExpSqrt {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#logExpSqrtCases")
        void specialFunctions(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class InputNotation {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @MethodSource("backend.math.calculus.differential.DerivativeTest#notationCases")
        void notation(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class OtherVariables {

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @CsvSource({
                "y^2, 0",
                "sin(y), 0",
                "5*y + 3, 0",
        })
        void treatsOtherVariablesAsConstants(String input, String expected) {
            assertEquals(expected, derivative(input));
        }
    }

    @Nested
    class VariableExponents {

        @Test
        void xToTheX() {
            assertEquals("x^x*(ln(x) + 1)", derivative("x^x"));
        }

        @ParameterizedTest(name = "d/dx({0}) = {1}")
        @CsvSource({
                "(x + 1)^x, (x + 1)^x*(ln(x + 1) + x/(x + 1))",
                "x^(2*x), x^(2*x)*(2*ln(x) + 2)",
        })
        void logarithmicForms(String input, String expected) {
            assertEquals(expected, derivative(input));
        }

        @Test
        void usesLogStepsForVariableExponent() {
            Result r = solve("x^x");
            assertTrue(r.isSuccess());
            assertTrue(r.getSteps().stream()
                    .anyMatch(s -> s.getDescription().contains("Logarithmic differentiation")));
        }
    }

    @Nested
    class Failures {

        @Test
        void rejectsEmptyInput() {
            Result r = solve("");
            assertFalse(r.isSuccess());
        }
    }

    @Nested
    class Steps {

        @Test
        void includesDetailedSteps() {
            Result r = solve("x^2 + 3*x");
            assertTrue(r.isSuccess());
            assertTrue(r.getSteps().size() >= 3);
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Sum rule")));
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Power rule")));
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("before simplifying")));
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().startsWith("Simplify")));
        }

        @Test
        void productRuleSteps() {
            Result r = solve("x*(x + 2)");
            assertTrue(r.isSuccess());
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Product rule")));
        }

        @Test
        void quotientRuleSteps() {
            Result r = solve("1/x");
            assertTrue(r.isSuccess());
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Quotient rule")));
        }

        @Test
        void chainRuleSteps() {
            Result r = solve("sin(2*x)");
            assertTrue(r.isSuccess());
            assertTrue(r.getSteps().stream().anyMatch(s -> s.getDescription().contains("Chain rule")));
        }

        @Test
        void finalSimplifyStepCombinesTerms() {
            Result r = solve("x*(x + 2)");
            assertTrue(r.isSuccess());
            assertEquals("2*x + 2", r.getExact().toDisplay());
            Step before = r.getSteps().stream()
                    .filter(s -> s.getDescription().contains("before simplifying"))
                    .findFirst().orElseThrow();
            Step simplify = r.getSteps().stream()
                    .filter(s -> s.getDescription().startsWith("Simplify"))
                    .reduce((a, b) -> b).orElseThrow();
            assertTrue(before.hasExpression());
            assertTrue(simplify.hasExpression());
            assertEquals("2*x + 2", simplify.getExpression().toDisplay());
            assertTrue(before.getExpression().toDisplay().contains("+"));
        }
    }

    static Stream<Arguments> powerRuleCases() {
        return Stream.of(
                Arguments.of("x^2", "2*x"),
                Arguments.of("x^3", "3*x^2"),
                Arguments.of("x^4", "4*x^3"),
                Arguments.of("x^5", "5*x^4"),
                Arguments.of("x^(-2)", "-2*x^(-3)"),
                Arguments.of("3*x^2", "6*x"),
                Arguments.of("-x^2", "-(2*x)"),
                Arguments.of("(-x)^2", "2*x"),
                Arguments.of("x*x", "2*x"),
                Arguments.of("x*x*x", "3*x^2"),
                Arguments.of("x^2*x", "3*x^2"),
                Arguments.of("2*x^3", "6*x^2"),
                Arguments.of("5*x^4", "20*x^3")
        );
    }

    static Stream<Arguments> sumRuleCases() {
        return Stream.of(
                Arguments.of("x^2 + 3*x", "2*x + 3"),
                Arguments.of("x^2 + 3*x + 1", "2*x + 3"),
                Arguments.of("3*x^3 + 2*x + 1", "9*x^2 + 2"),
                Arguments.of("x + x + x", "3"),
                Arguments.of("0*x + 5", "0"),
                Arguments.of("sin(x) + cos(x)", "cos(x) - sin(x)"),
                Arguments.of("sin(x)-cos(x)", "cos(x) - sin(x)"),
                Arguments.of("2*sin(x) + 3*cos(x)", "2*cos(x) - 3*sin(x)"),
                Arguments.of("tan(x) + 1", "sec(x)^2"),
                Arguments.of("sin(2*x) + cos(2*x)", "2*cos(2*x) - 2*sin(2*x)"),
                Arguments.of("-4*x + x^2", "2*x - 4")
        );
    }

    static Stream<Arguments> productRuleCases() {
        return Stream.of(
                Arguments.of("x*(x + 2)", "2*x + 2"),
                Arguments.of("x*(x + 1)", "2*x + 1"),
                Arguments.of("(x + 1)*(x - 1)", "2*x"),
                Arguments.of("x^2*(x + 3)", "2*x*(x + 3) + x^2"),
                Arguments.of("x*sin(x)", "sin(x) + x*cos(x)"),
                Arguments.of("x^2*cos(x)", "2*x*cos(x) - x^2*sin(x)"),
                Arguments.of("x^2*sin(x)", "2*x*sin(x) + x^2*cos(x)"),
                Arguments.of("sin(x)*cos(x)", "cos(x)^2 - sin(x)^2"),
                Arguments.of("sinx*cosx", "cos(x)^2 - sin(x)^2"),
                Arguments.of("sinx", "cos(x)"),
                Arguments.of("cosx", "-sin(x)")
        );
    }

    static Stream<Arguments> quotientRuleCases() {
        return Stream.of(
                Arguments.of("1/x", "-1/x^2"),
                Arguments.of("1/(x^2)", "-2*x/x^4"),
                Arguments.of("x/(x + 1)", "1/(x + 1)^2"),
                Arguments.of("sin(x)/x", "(cos(x)*x - sin(x))/x^2"),
                Arguments.of("(x^2 + 1)/(x + 1)", "(2*x*(x + 1) - x^2 - 1)/(x + 1)^2"),
                Arguments.of("x/(x^2 + 1)", "(-x^2 + 1)/(x^2 + 1)^2"),
                Arguments.of("1/(x + 1)^2", "-2*(x + 1)/(x + 1)^4")
        );
    }

    static Stream<Arguments> trigBasicCases() {
        return Stream.of(
                Arguments.of("sin(x)", "cos(x)"),
                Arguments.of("cos(x)", "-sin(x)"),
                Arguments.of("tan(x)", "sec(x)^2"),
                Arguments.of("sec(x)", "sec(x)*tan(x)"),
                Arguments.of("csc(x)", "-(csc(x)*cot(x))"),
                Arguments.of("cot(x)", "-csc(x)^2"),
                Arguments.of("sec(x)^2", "2*sec(x)^2*tan(x)")
        );
    }

    static Stream<Arguments> chainRuleCases() {
        return Stream.of(
                Arguments.of("sin(2*x)", "2*cos(2*x)"),
                Arguments.of("sin(3*x)", "3*cos(3*x)"),
                Arguments.of("cos(5*x)", "-5*sin(5*x)"),
                Arguments.of("tan(2*x)", "2*sec(2*x)^2"),
                Arguments.of("sin(x^2)", "2*cos(x^2)*x"),
                Arguments.of("cos(x^2 + 1)", "-(2*sin(x^2 + 1)*x)"),
                Arguments.of("(x + 1)^3", "3*(x + 1)^2"),
                Arguments.of("(x + 2)^2", "2*(x + 2)"),
                Arguments.of("(2*x + 3)^4", "8*(2*x + 3)^3"),
                Arguments.of("(x^2 + 1)^3", "6*(x^2 + 1)^2*x"),
                Arguments.of("d/dx(sin(2*x))", "2*cos(2*x)"),
                Arguments.of("d/dx((x + 1)^2)", "2*(x + 1)")
        );
    }

    static Stream<Arguments> logExpSqrtCases() {
        return Stream.of(
                Arguments.of("ln(x)", "1/x"),
                Arguments.of("lnx", "1/x"),
                Arguments.of("ln(x^2)", "2*1/x^2*x"),
                Arguments.of("ln(2*x)", "2*1/(2*x)"),
                Arguments.of("sqrt(x)", "1/(2*sqrt(x))"),
                Arguments.of("sqrt(x^2 + 1)", "2*1/(2*sqrt(x^2 + 1))*x"),
                Arguments.of("sqrt(x^3)", "3*1/(2*sqrt(x^3))*x^2"),
                Arguments.of("E^x", "E^x")
        );
    }

    static Stream<Arguments> notationCases() {
        return Stream.of(
                Arguments.of("d/dx(x^2)", "2*x"),
                Arguments.of("d/dx(x^3)", "3*x^2"),
                Arguments.of("d/dx(sin(x))", "cos(x)"),
                Arguments.of("d/dx(x^2 + 3*x)", "2*x + 3"),
                Arguments.of("f(x)=x^2 + 1", "2*x"),
                Arguments.of("f(x)=sin(x)", "cos(x)"),
                Arguments.of("f(x)=3*x^2", "6*x")
        );
    }
}
