package backend.math.calculus.limits;

import backend.models.Result;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Problems from a calculus limits worksheet, checked against their textbook answers. */
class LimitWorksheetTest {

    private static Result solve(String input) {
        return new Limit().solve(input);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "lim x->0 (x^2-25)/(x^2-4*x-5)                    | 5",
            "lim x->5 (x^2-25)/(x^2-4*x-5)                    | 5/3",
            "lim x->1 (7*x^2-4*x-3)/(3*x^2-4*x+1)             | 5",
            "lim x->-2 (x^4+5*x^3+6*x^2)/(x^2*(x+1)-4*(x+1))  | 1",
            "lim x->-3 abs(x+1)+3/x                           | 1",
            "lim x->3 (sqrt(x+1)-2)/(x^2-9)                   | 1/24",
            "lim x->3 (sqrt(x^2+7)-3)/(x+3)                   | 1/6",
            "lim x->2 (x^2+2*x-8)/(sqrt(x^2+5)-(x+1))         | -18",
            "lim y->5 ((2*y^2+2*y+4)/(6*y-3))^(1/3)           | 4/3",
            "lim x->0 (1/(3+x)-1/(3-x))/x                     | -2/9",
            "lim x->7 (2*x-14)^(1/6)                          | 0",
            "lim x->1- sqrt(3-3*x)                            | 0",
            "lim x->inf (x^4-10)/(4*x^3+x)                    | ∞",
            "lim x->inf (3*x^3+x^2-2)/(x^2+x-2*x^3+1)         | -3/2",
            "lim x->inf (x+5)/(2*x^2+1)                       | 0",
            "lim x->inf 2*x^2-3*x                             | ∞",
            "lim x->0 (sqrt(x+2)-sqrt(2-x))/x                 | 1/sqrt(2)",
            "lim x->1 (x^(1/3)-1)/(sqrt(x)-1)                 | 2/3",
    })
    void solves(String input, String expected) {
        Result result = solve(input);
        assertTrue(result.isSuccess(), result::getError);
        assertEquals(expected, result.getExact().toDisplay());
    }

    @ParameterizedTest(name = "{0} does not exist")
    @CsvSource({
            "lim x->2 2*x/(x^2-4)",
            "lim x->-1 3*x/(x^2+2*x+1)",
            "lim x->-1 (x^2-25)/(x^2-4*x-5)",
            "lim x->3 (sqrt(x^2-5)+2)/(x-3)",
    })
    void reportsNonexistentLimits(String input) {
        Result result = solve(input);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("does not exist"), result.getError());
    }

    @Disabled("Limits at infinity of radicals and compositions are not supported yet")
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "lim x->inf sqrt(x^2-2)-sqrt(x^2+1)        | 0",
            "lim x->inf sqrt(x^2+1)-2*x                | -∞",
            "lim x->-inf ((x-3)/(5-x))^(1/3)           | -1",
            "lim x->-inf cos((x^5+1)/(x^6+x^5+100))    | 1",
            "lim x->0 (2^x+sin(x))/x^4                 | ∞",
            "lim x->1- 1/(x-1)+exp(x^2)                | -∞",
            "lim x->0+ exp(x)/(1+ln(x))                | 0",
    })
    void unsupported(String input, String expected) {
        solves(input, expected);
    }
}
