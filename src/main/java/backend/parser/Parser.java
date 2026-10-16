package backend.parser;

import backend.parser.Tokenizer.Token;
import backend.parser.Tokenizer.TokenType;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser that turns a token stream into an {@link ASTNode} tree.
 *
 * <p>Grammar (highest precedence last), with right-associative exponentiation and
 * support for implicit multiplication ({@code 2x}, {@code 2(x+1)}, {@code 2sin(x)},
 * {@code (x+1)(x-1)}):
 *
 * <pre>
 *   expression := term      (('+' | '-') term)*
 *   term       := unary     (('*' | '/') unary | <implicit> power)*
 *   unary      := ('-' | '+') unary | power
 *   power      := primary    ('^' unary)?
 *   primary    := NUMBER | constant | variable | IDENT '(' expression ')' | '(' expression ')'
 * </pre>
 */
public class Parser {

    /** Thrown for any malformed input; callers (e.g. the engine) turn this into a failed Result. */
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    private static final Set<String> CONSTANTS = Set.of("pi", "e");
    private static final List<String> FUNCTIONS = List.of(
            "sqrt", "sin", "cos", "tan", "cot", "sec", "csc", "log", "ln", "abs");

    private static final Set<String> FUNCTION_SET = Set.copyOf(FUNCTIONS);

    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Convenience: tokenize and fully parse a string into an expression tree. */
    public static ASTNode parse(String input) {
        if (input == null || input.isBlank()) {
            throw new ParseException("Empty expression");
        }
        List<Token> tokens = new Tokenizer().tokenize(input);
        Parser parser = new Parser(tokens);
        ASTNode node = parser.parseExpression();
        parser.expect(TokenType.EOF);
        return node;
    }

    private ASTNode parseExpression() {
        ASTNode left = parseTerm();
        while (true) {
            if (match(TokenType.PLUS)) {
                left = new ASTNode.Add(left, parseTerm());
            } else if (match(TokenType.MINUS)) {
                left = new ASTNode.Sub(left, parseTerm());
            } else {
                return left;
            }
        }
    }

    private ASTNode parseTerm() {
        ASTNode left = parseUnary();
        while (true) {
            if (match(TokenType.STAR)) {
                left = new ASTNode.Mul(left, parseUnary());
            } else if (match(TokenType.SLASH)) {
                left = new ASTNode.Div(left, parseUnary());
            } else if (startsFactor()) {
                left = new ASTNode.Mul(left, parsePower());
            } else {
                return left;
            }
        }
    }

    private ASTNode parseUnary() {
        if (match(TokenType.MINUS)) {
            return new ASTNode.Neg(parseUnary());
        }
        if (match(TokenType.PLUS)) {
            return parseUnary();
        }
        return parsePower();
    }

    private ASTNode parsePower() {
        ASTNode base = parsePrimary();
        if (match(TokenType.CARET)) {
            return new ASTNode.Pow(base, parseUnary());
        }
        return base;
    }

    private ASTNode parsePrimary() {
        Token token = peek();
        switch (token.type()) {
            case NUMBER -> {
                advance();
                return number(token.text());
            }
            case IDENT -> {
                advance();
                String name = token.text();
                if (match(TokenType.LPAREN)) {
                    ASTNode arg = parseExpression();
                    expect(TokenType.RPAREN);
                    return new ASTNode.Func(name.toLowerCase(), arg);
                }
                String combined = splitCombinedFunction(name);
                if (combined != null) {
                    return new ASTNode.Func(name.substring(0, combined.length()).toLowerCase(),
                            Parser.parse(name.substring(combined.length())));
                }
                if (isFunctionName(name) && startsFactor()) {
                    return new ASTNode.Func(name.toLowerCase(), parsePower());
                }
                return identifier(name);
            }
            case LPAREN -> {
                advance();
                ASTNode inner = parseExpression();
                expect(TokenType.RPAREN);
                return inner;
            }
            default -> throw new ParseException(
                    "Expected a number, variable, or '(' but found '" + token.text()
                            + "' at position " + token.position());
        }
    }

    private ASTNode number(String text) {
        int dot = text.indexOf('.');
        if (dot < 0) {
            return new ASTNode.Num(new BigInteger(text), BigInteger.ONE);
        }
        String digits = text.replace(".", "");
        if (digits.isEmpty()) {
            throw new ParseException("Malformed number '" + text + "'");
        }
        int decimals = text.length() - dot - 1;
        BigInteger numerator = new BigInteger(digits);
        BigInteger denominator = BigInteger.TEN.pow(decimals);
        return new ASTNode.Num(numerator, denominator);
    }

    private static boolean isFunctionName(String name) {
        return FUNCTION_SET.contains(name.toLowerCase());
    }

    /**
     * If {@code name} is a glued function call like {@code sinx}, returns the function
     * prefix ({@code "sin"}); otherwise {@code null}.
     */
    private static String splitCombinedFunction(String name) {
        String lower = name.toLowerCase();
        for (String fn : FUNCTIONS) {
            if (lower.startsWith(fn) && name.length() > fn.length()) {
                return fn;
            }
        }
        return null;
    }

    private ASTNode identifier(String name) {
        String lower = name.toLowerCase();
        if (CONSTANTS.contains(lower)) {
            return new ASTNode.Constant(lower.equals("pi") ? "PI" : "E");
        }
        return new ASTNode.Var(name);
    }

    private boolean startsFactor() {
        TokenType type = peek().type();
        return type == TokenType.NUMBER || type == TokenType.IDENT || type == TokenType.LPAREN;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private void advance() {
        if (pos < tokens.size() - 1) {
            pos++;
        }
    }

    private boolean match(TokenType type) {
        if (peek().type() == type) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(TokenType type) {
        Token token = peek();
        if (token.type() != type) {
            throw new ParseException(
                    "Expected " + type + " but found '" + token.text()
                            + "' at position " + token.position());
        }
        advance();
    }
}
