package backend.math.trig;

import backend.math.algebra.Simplifier;
import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Sub;
import backend.parser.Parser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared helpers for unit-circle values, angle parsing, and trig equations. */
public final class TrigUtils {

    public static final ASTNode PI = new Constant("PI");
    public static final ASTNode TWO_PI = Simplifier.simplify(new Mul(Num.of(2), PI));

    private static final Pattern DEGREES = Pattern.compile(
            "^\\s*(-?\\d+(?:/\\d+)?)\\s*(?:°|deg(?:rees)?)?\\s*$", Pattern.CASE_INSENSITIVE);

    public record ExactValues(ASTNode sin, ASTNode cos, ASTNode tan, String angleLabel) {}

    public record AngleInfo(int degrees, String radianLabel, String degreeLabel) {}

    public static AngleInfo angleInfo(int degrees, String inputLabel) {
        int norm = normalizeDegrees(degrees);
        PiMultiple pi = piMultipleForDegrees(norm);
        String rad = pi == null ? inputLabel : pi.label();
        return new AngleInfo(norm, rad, norm + "°");
    }

    public static PiMultiple piMultipleForDegrees(int degrees) {
        return switch (normalizeDegrees(degrees)) {
            case 0 -> new PiMultiple(0, 1);
            case 30 -> new PiMultiple(1, 6);
            case 45 -> new PiMultiple(1, 4);
            case 60 -> new PiMultiple(1, 3);
            case 90 -> new PiMultiple(1, 2);
            case 120 -> new PiMultiple(2, 3);
            case 135 -> new PiMultiple(3, 4);
            case 150 -> new PiMultiple(5, 6);
            case 180 -> new PiMultiple(1, 1);
            case 210 -> new PiMultiple(7, 6);
            case 225 -> new PiMultiple(5, 4);
            case 240 -> new PiMultiple(4, 3);
            case 270 -> new PiMultiple(3, 2);
            case 300 -> new PiMultiple(5, 3);
            case 315 -> new PiMultiple(7, 4);
            case 330 -> new PiMultiple(11, 6);
            default -> null;
        };
    }

    public static int referenceAngleDegrees(int degrees) {
        int d = normalizeDegrees(degrees);
        if (d <= 90) {
            return d;
        }
        if (d <= 180) {
            return 180 - d;
        }
        if (d <= 270) {
            return d - 180;
        }
        return 360 - d;
    }

    public static String quadrant(int degrees) {
        int d = normalizeDegrees(degrees);
        if (d == 0 || d == 90 || d == 180 || d == 270) {
            return "On an axis";
        }
        if (d < 90) {
            return "Quadrant I";
        }
        if (d < 180) {
            return "Quadrant II";
        }
        if (d < 270) {
            return "Quadrant III";
        }
        return "Quadrant IV";
    }

    public static ASTNode reciprocal(ASTNode value, String name) {
        if (value instanceof Num n && n.isZero()) {
            return new Constant("undefined");
        }
        if (value instanceof Constant c && "undefined".equals(c.name())) {
            return c;
        }
        return Simplifier.simplify(new Div(Num.of(1), value));
    }

    public static String coterminalDegreeText(int degrees) {
        return normalizeDegrees(degrees) + "° + 360°n";
    }

    public static String coterminalRadianText(int degrees) {
        PiMultiple pi = piMultipleForDegrees(degrees);
        if (pi == null) {
            return "θ + 2*pi*n";
        }
        if (pi.numerator() == 0) {
            return "2*pi*n";
        }
        return pi.label() + " + 2*pi*n";
    }

    private TrigUtils() {
    }

    /** Parses {@code 30}, {@code 45°}, {@code pi/6}, or {@code 5*pi/4}. */
    public static ExactValues valuesForInput(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new Parser.ParseException("Enter an angle such as 30, 45°, or pi/6");
        }

        Integer degrees = tryParseDegrees(trimmed);
        if (degrees != null) {
            return exactValuesForDegrees(normalizeDegrees(degrees), trimmed);
        }

        ASTNode angle = Simplifier.simplify(Parser.parse(trimmed));
        PiMultiple pi = asPiMultiple(angle);
        if (pi != null) {
            int deg = pi.toDegrees();
            return exactValuesForDegrees(normalizeDegrees(deg), pi.label());
        }

        throw new Parser.ParseException(
                "Use a special angle: multiples of 15° or pi/12 (e.g. 30, 45°, pi/6, pi/4)");
    }

    public static Integer tryParseDegrees(String text) {
        Matcher m = DEGREES.matcher(text);
        if (!m.matches()) {
            return null;
        }
        String num = m.group(1);
        if (num.contains("/")) {
            String[] parts = num.split("/");
            int n = Integer.parseInt(parts[0]);
            int d = Integer.parseInt(parts[1]);
            return n / d;
        }
        return Integer.parseInt(num);
    }

    public static int normalizeDegrees(int degrees) {
        int mod = degrees % 360;
        return mod < 0 ? mod + 360 : mod;
    }

    public static ExactValues exactValuesForDegrees(int degrees, String label) {
        return switch (degrees) {
            case 0 -> new ExactValues(Num.of(0), Num.of(1), Num.of(0), label);
            case 30 -> new ExactValues(half(), divSqrt(3, 2), divSqrt(3, 3), label);
            case 45 -> new ExactValues(divSqrt(2, 2), divSqrt(2, 2), Num.of(1), label);
            case 60 -> new ExactValues(divSqrt(3, 2), half(), sqrtOf(3), label);
            case 90 -> new ExactValues(Num.of(1), Num.of(0), new Constant("undefined"), label);
            case 120 -> new ExactValues(divSqrt(3, 2), neg(half()), neg(divSqrt(3, 3)), label);
            case 135 -> new ExactValues(divSqrt(2, 2), neg(divSqrt(2, 2)), Num.of(-1), label);
            case 150 -> new ExactValues(half(), neg(divSqrt(3, 2)), neg(divSqrt(3, 3)), label);
            case 180 -> new ExactValues(Num.of(0), Num.of(-1), Num.of(0), label);
            case 210 -> new ExactValues(neg(half()), neg(divSqrt(3, 2)), divSqrt(3, 3), label);
            case 225 -> new ExactValues(neg(divSqrt(2, 2)), neg(divSqrt(2, 2)), Num.of(1), label);
            case 240 -> new ExactValues(neg(divSqrt(3, 2)), neg(half()), sqrtOf(3), label);
            case 270 -> new ExactValues(Num.of(-1), Num.of(0), new Constant("undefined"), label);
            case 300 -> new ExactValues(neg(divSqrt(3, 2)), half(), neg(sqrtOf(3)), label);
            case 315 -> new ExactValues(neg(divSqrt(2, 2)), divSqrt(2, 2), Num.of(-1), label);
            case 330 -> new ExactValues(neg(half()), divSqrt(3, 2), neg(divSqrt(3, 3)), label);
            default -> throw new Parser.ParseException(
                    "No exact unit-circle values for " + label + " — try multiples of 15° or pi/12");
        };
    }

    /** If {@code angle} is an exact multiple of pi, returns the numerator/denominator. */
    public static PiMultiple asPiMultiple(ASTNode angle) {
        angle = Simplifier.simplify(angle);
        if (angle instanceof Constant c && c.name().equals("PI")) {
            return new PiMultiple(1, 1);
        }
        if (angle instanceof Neg n) {
            PiMultiple inner = asPiMultiple(n.operand());
            return inner == null ? null : inner.negate();
        }
        if (angle instanceof Mul m) {
            if (m.left() instanceof Num n && isPi(m.right())) {
                return PiMultiple.fromNum(n);
            }
            if (m.right() instanceof Num n && isPi(m.left())) {
                return PiMultiple.fromNum(n);
            }
        }
        if (angle instanceof Div d && isPi(d.left()) && d.right() instanceof Num n) {
            return PiMultiple.fromNum(n).invert();
        }
        if (angle instanceof Div d && d.left() instanceof Num n && isPi(d.right())) {
            return PiMultiple.fromNum(n);
        }
        return null;
    }

    public static ASTNode piFraction(int numerator, int denominator) {
        if (numerator == 0) {
            return Num.of(0);
        }
        ASTNode piPart = denominator == 1
                ? PI
                : Simplifier.simplify(new Div(PI, Num.of(denominator)));
        if (numerator == 1) {
            return piPart;
        }
        if (numerator == -1) {
            return Simplifier.simplify(new Neg(piPart));
        }
        return Simplifier.simplify(new Mul(Num.of(numerator), piPart));
    }

    public static ASTNode generalSolution(ASTNode principal, ASTNode period) {
        return Simplifier.simplify(new Add(principal, new Mul(period, new Constant("n"))));
    }

    public static boolean isPi(ASTNode node) {
        return node instanceof Constant c && c.name().equals("PI");
    }

    public static boolean isTrigFunction(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sin", "cos", "tan", "cot", "sec", "csc" -> true;
            default -> false;
        };
    }

    private static Num half() {
        return Num.of(1, 2);
    }

    private static ASTNode sqrtOf(int n) {
        return new Func("sqrt", Num.of(n));
    }

    private static ASTNode divSqrt(int radicand, int denom) {
        return Simplifier.simplify(new Div(new Func("sqrt", Num.of(radicand)), Num.of(denom)));
    }

    private static ASTNode neg(ASTNode node) {
        return Simplifier.simplify(new Neg(node));
    }

    public record PiMultiple(int numerator, int denominator) {

        public PiMultiple {
            if (denominator == 0) {
                throw new ArithmeticException("Zero denominator");
            }
        }

        public static PiMultiple fromNum(Num n) {
            return new PiMultiple(n.numerator().intValueExact(), n.denominator().intValueExact());
        }

        public PiMultiple negate() {
            return new PiMultiple(-numerator, denominator);
        }

        public PiMultiple invert() {
            return new PiMultiple(denominator, numerator);
        }

        public int toDegrees() {
            return normalizeDegrees(Math.floorMod(numerator * 180 / denominator, 360));
        }

        public String label() {
            if (numerator == 0) {
                return "0";
            }
            if (denominator == 1) {
                return numerator == 1 ? "pi" : numerator + "*pi";
            }
            if (numerator == 1) {
                return "pi/" + denominator;
            }
            return numerator + "*pi/" + denominator;
        }
    }
}
