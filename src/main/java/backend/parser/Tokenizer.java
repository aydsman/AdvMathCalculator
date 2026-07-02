package backend.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexical scanner: turns a raw expression string into a flat list of {@link Token}s
 * for the {@link Parser} to consume.
 *
 * <p>Recognises numbers (integer or decimal), identifiers (variable, constant, or
 * function names), the operators {@code + - * / ^}, parentheses and commas.
 * Whitespace is ignored. Identifiers are maximal runs of letters/digits, so
 * multi-letter function names like {@code sin} scan as a single token while
 * implicit multiplication (e.g. {@code 2x}) is resolved later by the parser.
 */
public class Tokenizer {

    public enum TokenType {
        NUMBER, IDENT, PLUS, MINUS, STAR, SLASH, CARET, LPAREN, RPAREN, COMMA, EOF
    }

    /** A single lexical token; {@code position} is the index in the source string for error messages. */
    public record Token(TokenType type, String text, int position) {}

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = input.length();

        while (i < n) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (isDigit(c) || (c == '.' && i + 1 < n && isDigit(input.charAt(i + 1)))) {
                int start = i;
                boolean seenDot = false;
                while (i < n && (isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    if (input.charAt(i) == '.') {
                        if (seenDot) {
                            throw new Parser.ParseException(
                                    "Malformed number with two decimal points at position " + i);
                        }
                        seenDot = true;
                    }
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, input.substring(start, i), start));
                continue;
            }

            if (isIdentStart(c)) {
                int start = i;
                while (i < n && isIdentPart(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.IDENT, input.substring(start, i), start));
                continue;
            }

            TokenType type = switch (c) {
                case '+' -> TokenType.PLUS;
                case '-' -> TokenType.MINUS;
                case '*' -> TokenType.STAR;
                case '/' -> TokenType.SLASH;
                case '^' -> TokenType.CARET;
                case '(' -> TokenType.LPAREN;
                case ')' -> TokenType.RPAREN;
                case ',' -> TokenType.COMMA;
                default -> throw new Parser.ParseException(
                        "Unexpected character '" + c + "' at position " + i);
            };
            tokens.add(new Token(type, String.valueOf(c), i));
            i++;
        }

        tokens.add(new Token(TokenType.EOF, "", n));
        return tokens;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
