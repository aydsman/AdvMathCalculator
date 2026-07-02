package backend.math.calculus.limits;

import backend.models.Result;

/**
 * Runs worksheet limit problems and prints a status table (manual audit runner).
 * Usage: compile main + this file, then run with main classes on the classpath.
 */
public final class LimitWorksheetAudit {

    private LimitWorksheetAudit() {}

    record Case(String id, String input, String expectedAnswer, String category) {}

    public static void main(String[] args) {
        Limit limit = new Limit();
        Case[] cases = worksheetCases();

        System.out.printf("%-4s %-10s %-30s %s%n", "ID", "STATUS", "CATEGORY", "DETAIL");
        System.out.println("-".repeat(100));

        for (Case c : cases) {
            String status;
            String detail;
            try {
                Result r = limit.solveFromInput(c.input());
                if (r.isSuccess()) {
                    String got = r.getExact() != null ? r.getExact().toDisplay()
                            : (r.getDecimal() != null ? String.valueOf(r.getDecimal()) : "?");
                    if (c.expectedAnswer() == null || c.expectedAnswer().isBlank()) {
                        status = "OK";
                        detail = "answer=" + got;
                    } else if (matchesExpected(got, r, c.expectedAnswer())) {
                        status = "OK";
                        detail = got;
                    } else {
                        status = "WRONG";
                        detail = "got " + got + ", expected " + c.expectedAnswer();
                    }
                } else {
                    String err = r.getError() != null ? r.getError() : "unknown error";
                    if (matchesExpected(null, r, c.expectedAnswer())) {
                        status = "OK";
                        detail = err;
                    } else if (c.category().startsWith("NOT_IMPL") || c.category().startsWith("PARSE")) {
                        status = "EXPECTED";
                        detail = err;
                    } else {
                        status = "FAIL";
                        detail = err;
                    }
                }
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (c.category().startsWith("PARSE") || c.category().startsWith("NOT_IMPL")) {
                    status = "EXPECTED";
                    detail = msg;
                } else {
                    status = "PARSE_ERR";
                    detail = msg;
                }
            }
            System.out.printf("%-4s %-10s %-30s %s%n", c.id(), status, c.category(), truncate(detail, 55));
        }
    }

    private static boolean matchesExpected(String got, Result r, String expected) {
        if (expected.equalsIgnoreCase("DNE") || expected.contains("not exist")) {
            return !r.isSuccess() && r.getError() != null
                    && (r.getError().contains("not exist") || r.getError().contains("\u221e"));
        }
        if (expected.equalsIgnoreCase("inf") || expected.equals("+inf")) {
            return got.contains("inf") || (r.getError() != null && r.getError().contains("inf"));
        }
        return normalize(got).equals(normalize(expected))
                || (r.getDecimal() != null && approxMatch(r.getDecimal(), expected));
    }

    private static boolean approxMatch(double val, String expected) {
        try {
            double exp = Double.parseDouble(expected);
            return Math.abs(val - exp) < 1e-6;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String normalize(String s) {
        return s.replace(" ", "").replace("*", "");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    static Case[] worksheetCases() {
        return new Case[] {
            c("a", "lim x->0 (x^2-25)/(x^2-4*x-5)", "5", "WORKS_DIRECT"),
            c("b", "lim x->5 (x^2-25)/(x^2-4*x-5)", "5/3", "WORKS_FACTOR"),
            c("c", "lim x->1 (7*x^2-4*x-3)/(3*x^2-4*x+1)", "5", "WORKS_FACTOR"),
            c("d", "lim x->-2 (x^4+5*x^3+6*x^2)/(x^2*(x+1)-4*(x+1))", "-1/3", "KNOWN_BUG_FACTOR"),
            c("e", "lim x->-3 abs(x+1)+3/x", null, "NOT_IMPL_ABS"),
            c("f", "lim x->3 (sqrt(x+1)-2)/(x^2-9)", null, "NOT_IMPL_RATIONALIZE"),
            c("g", "lim x->3 (sqrt(x^2+7)-3)/(x+3)", "1/6", "WORKS_DIRECT"),
            c("h", "lim x->2 (x^2+2*x-8)/(sqrt(x^2+5)-(x+1))", null, "NOT_IMPL_RATIONALIZE"),
            c("i", "lim y->5 ((2*y^2+2*y+4)/(6*y-3))^(1/3)", null, "WORKS_DIRECT"),
            c("j", "lim x->0 (2*cos(x)-5)^(1/4)", null, "NOT_IMPL_COMPLEX"),
            c("k", "lim x->0 (1/(3+x)-1/(3-x))/x", "-2/9", "NOT_IMPL_COMPLEX_FRAC"),
            c("l", "lim x->-6 ((2*x+8)/(x^2-12)-1/x)/(x+6)", null, "NOT_IMPL_COMPLEX_FRAC"),
            c("m", "lim x->inf sqrt(x^2-2)-sqrt(x^2+1)", null, "NOT_IMPL_INFINITY"),
            c("n", "lim x->-inf sqrt(x-2)-sqrt(x)", null, "NOT_IMPL_INFINITY"),
            c("o", "lim x->7 (2*x-14)^(1/6)", "0", "WORKS_DIRECT"),
            c("p", "lim x->1- sqrt(3-3*x)", null, "NOT_IMPL_ONE_SIDED"),
            c("q", "lim x->inf (x^4-10)/(4*x^3+x)", null, "NOT_IMPL_INFINITY"),
            c("r", "lim x->-inf ((x-3)/(5-x))^(1/3)", null, "NOT_IMPL_INFINITY"),
            c("s", "lim x->inf (3*x^3+x^2-2)/(x^2+x-2*x^3+1)", null, "NOT_IMPL_INFINITY"),
            c("t", "lim x->inf (x+5)/(2*x^2+1)", null, "NOT_IMPL_INFINITY"),
            c("u", "lim x->-inf cos((x^5+1)/(x^6+x^5+100))", null, "NOT_IMPL_INFINITY"),
            c("v", "lim x->2 2*x/(x^2-4)", "DNE", "WORKS_POLE"),
            c("w", "lim x->-1 3*x/(x^2+2*x+1)", "DNE", "WORKS_POLE"),
            c("x", "lim x->-1 (x^2-25)/(x^2-4*x-5)", "DNE", "WORKS_POLE"),
            c("y", "lim x->3 (sqrt(x^2-5)+2)/(x-3)", "DNE", "WORKS_POLE"),
            c("z", "lim x->0 (2^x+sin(x))/x^4", null, "NOT_IMPL_LHOPITAL"),
            c("A", "lim x->1- 1/(x-1)+exp(x^2)", null, "NOT_IMPL_ONE_SIDED"),
            c("B", "lim x->inf 2*x^2-3*x", null, "NOT_IMPL_INFINITY"),
            c("C", "lim x->0 (sqrt(x+2)-sqrt(2-x))/x", null, "NOT_IMPL_RATIONALIZE"),
            c("D", "lim x->0+ exp(x)/(1+ln(x))", null, "NOT_IMPL_ONE_SIDED"),
            c("E", "lim x->inf sqrt(x^2+1)-2*x", null, "NOT_IMPL_INFINITY"),
            c("F", "lim x->1 (x^(1/3)-1)/(sqrt(x)-1)", null, "NOT_IMPL_RATIONALIZE"),
        };
    }

    private static Case c(String id, String input, String expected, String category) {
        return new Case(id, input, expected, category);
    }
}
