package cgeo.geocaching.calculator;

/**
 * This simple evaluation algorithm was derived from the work of user 'Boann' and released to the public domain on Stack Overflow:
 * https://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form
 *
 * The '%' operator was added.
 * The 'sqrt', 'cos', 'sin' and 'tan' functions are implementation but not used at the moment.
 * Because in {#cgeo.geocaching.ui.CalculatorVariable} individual letters are automatically replaced with their corresponding
 * expressions as they are interpreted as being individual variables.
 */
public final class CalculationUtils {

    private int pos = -1;
    private int ch;
    private final String expression;

    public CalculationUtils(final String expression) {
        this.expression = expression;
    }

    public double eval() {
        nextChar();
        final double x = parseExpression();
        if (pos < expression.length()) {
            throw new IllegalArgumentException("Unexpected: " + (char) ch);
        }
        return x;
    }

    private void nextChar() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    private boolean eat(final int charToEat) {
        while (ch == ' ') {
            nextChar();
        }
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    /**
     * Grammar:
     *
     * expression = term | expression `+` term | expression `-` term
     * term = factor | term `*` factor | term `/` factor
     * factor = `+` factor | `-` factor | `(` expression `)`
     *        | number | functionName factor | factor `^` factor
     */
    private double parseExpression() {
        double x = parseTerm();
        for (;;) {
            if (eat('+')) {
                x += parseTerm(); // addition
            } else if (eat('-')) {
                x -= parseTerm(); // subtraction
            } else {
                return x;
            }
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        for (;;) {
            if (eat('*')) {
                x *= parseFactor(); // multiplication
            } else if (eat('/')) {
                x /= parseFactor(); // division
            } else if (eat('%')) {
                x %= parseFactor(); // modulus (remainder)
            } else {
                return x;
            }
        }
    }

    private double parseFactor() {
        if (eat('+')) {
            return parseFactor(); // unary plus
        }
        if (eat('-')) {
            return -parseFactor(); // unary minus
        }

        double x;
        final int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            if (!eat(')')) {
                throw new IllegalArgumentException("Expected ')'");
            }
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                nextChar();
            }
            x = Double.parseDouble(expression.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') { // functions
            while (ch >= 'a' && ch <= 'z') {
                nextChar();
            }
            final String func = expression.substring(startPos, this.pos);
            x = parseFactor();

            switch (func) {
                case "sqrt": x = Math.sqrt(x);                 break;
                case "sin":  x = Math.sin(Math.toRadians(x));  break;
                case "cos":  x = Math.cos(Math.toRadians(x));  break;
                case "tan":  x = Math.tan(Math.toRadians(x));  break;
                default:
                    throw new IllegalArgumentException("Unknown function: " + func);
            }

        } else {
            throw new IllegalArgumentException("Unexpected: " + (char) ch);
        }

        if (eat('^')) {
            x = Math.pow(x, parseFactor()); // exponentiation
        }

        return x;
    }

}
