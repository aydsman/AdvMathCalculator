package backend.math.functions;

import backend.parser.ASTNode;
import backend.parser.ASTNode.Add;
import backend.parser.ASTNode.Constant;
import backend.parser.ASTNode.Div;
import backend.parser.ASTNode.Func;
import backend.parser.ASTNode.Mul;
import backend.parser.ASTNode.Neg;
import backend.parser.ASTNode.Num;
import backend.parser.ASTNode.Pow;
import backend.parser.ASTNode.Sub;
import backend.parser.ASTNode.Var;
import backend.parser.Parser;

import java.util.ArrayList;
import java.util.List;

/** Shared helpers for parsing and manipulating function expressions. */
public final class FunctionUtils {

    public record FunctionDef(String name, String variable, ASTNode body) {}

    private FunctionUtils() {
    }

    /** Parses {@code f(x) = expr} or just {@code expr}. */
    public static FunctionDef parseDefinition(String input, String defaultName, String defaultVar) {
        String trimmed = input.trim();
        String name = defaultName;
        String variable = defaultVar;
        String exprPart = trimmed;

        int eq = trimmed.indexOf('=');
        if (eq >= 0) {
            String lhs = trimmed.substring(0, eq).trim();
            exprPart = trimmed.substring(eq + 1).trim();
            int open = lhs.indexOf('(');
            int close = lhs.indexOf(')');
            if (open > 0 && close > open) {
                name = lhs.substring(0, open).trim();
                variable = lhs.substring(open + 1, close).trim();
            }
        }

        ASTNode body = Parser.parse(exprPart);
        return new FunctionDef(name, variable, body);
    }

    /** Parses {@code f(x)=..., g(x)=...} for composition. */
    public static List<FunctionDef> parseDefinitions(String input) {
        List<FunctionDef> defs = new ArrayList<>();
        for (String part : input.split(",")) {
            if (!part.isBlank()) {
                defs.add(parseDefinition(part.trim(), defs.isEmpty() ? "f" : "g", "x"));
            }
        }
        if (defs.size() != 2) {
            throw new Parser.ParseException("Composition expects two functions: f(x)=..., g(x)=...");
        }
        return defs;
    }

    /** Replaces every occurrence of {@code variable} in {@code expr} with {@code replacement}. */
    public static ASTNode substitute(ASTNode expr, String variable, ASTNode replacement) {
        return switch (expr) {
            case Var v -> v.name().equals(variable) ? replacement : v;
            case Num n -> n;
            case Constant c -> c;
            case Neg n -> new Neg(substitute(n.operand(), variable, replacement));
            case Add a -> new Add(
                    substitute(a.left(), variable, replacement),
                    substitute(a.right(), variable, replacement));
            case Sub s -> new Sub(
                    substitute(s.left(), variable, replacement),
                    substitute(s.right(), variable, replacement));
            case Mul m -> new Mul(
                    substitute(m.left(), variable, replacement),
                    substitute(m.right(), variable, replacement));
            case Div d -> new Div(
                    substitute(d.left(), variable, replacement),
                    substitute(d.right(), variable, replacement));
            case Pow p -> new Pow(
                    substitute(p.base(), variable, replacement),
                    substitute(p.exponent(), variable, replacement));
            case Func f -> new Func(f.name(), substitute(f.argument(), variable, replacement));
        };
    }

    /** Collects domain restrictions while walking the expression tree. */
    public static void collectDomainRestrictions(ASTNode node, List<String> restrictions) {
        switch (node) {
            case Div d -> {
                restrictions.add(d.right().toDisplay() + " != 0");
                collectDomainRestrictions(d.left(), restrictions);
                collectDomainRestrictions(d.right(), restrictions);
            }
            case Func f -> {
                switch (f.name()) {
                    case "sqrt" -> restrictions.add(f.argument().toDisplay() + " >= 0");
                    case "ln", "log" -> restrictions.add(f.argument().toDisplay() + " > 0");
                    default -> { }
                }
                collectDomainRestrictions(f.argument(), restrictions);
            }
            case Neg n -> collectDomainRestrictions(n.operand(), restrictions);
            case Add a -> {
                collectDomainRestrictions(a.left(), restrictions);
                collectDomainRestrictions(a.right(), restrictions);
            }
            case Sub s -> {
                collectDomainRestrictions(s.left(), restrictions);
                collectDomainRestrictions(s.right(), restrictions);
            }
            case Mul m -> {
                collectDomainRestrictions(m.left(), restrictions);
                collectDomainRestrictions(m.right(), restrictions);
            }
            case Pow p -> {
                collectDomainRestrictions(p.base(), restrictions);
                collectDomainRestrictions(p.exponent(), restrictions);
            }
            default -> { }
        }
    }
}
