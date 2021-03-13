package cgeo.geocaching.utils.expressions;

import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class ExpressionParserTest {

    private final ExpressionParser<LambdaExpression<Integer, Integer>> calculator = new ExpressionParser<>();

    @Before
    public void before() {
        calculator
            .register(() -> LambdaExpression.createValueSingleConfig("", (s, i) -> Integer.parseInt(s.trim())))
            .register(() -> LambdaExpression.createValueSingleConfig("x", (s, i) -> i))
            .register(() -> LambdaExpression.createValue("length", (sa, i) -> {
                int res = 0;
                for (String s : sa) {
                    res = res * 10 + s.length();
                }
                return res;
            }))
            .register(ExpressionParserTest::getPlusExpression)
            .register(ExpressionParserTest::getMinusExpression)
            .register(ExpressionParserTest::getTimesExpression)
            .register(ExpressionParserTest::getDivideExpression)
            .register(ExpressionParserTest::getPowExpression);
    }

    private static LambdaExpression<Integer, Integer> getPlusExpression() {
        return LambdaExpression.createGroupSingleConfig("+", (s, i, list) -> {
            int res = 0;
            for (Integer in : list) {
                res += in;
            }
            return res;
        });
    }

    private static LambdaExpression<Integer, Integer> getMinusExpression() {
        return LambdaExpression.createGroupSingleConfig("-", (s, i, list) -> {
            int res = 0;
            boolean first = true;
            for (Integer in : list) {
                if (first) {
                    res = in;
                } else {
                    res -= in;
                }
                first = false;
            }
            return res;
        });
    }

    private static LambdaExpression<Integer, Integer> getTimesExpression() {
        return LambdaExpression.createGroupSingleConfig("*", (s, i, list) -> {
            int res = list.isEmpty() ? 0 : 1;
            for (Integer in : list) {
                res *= in;
            }
            return res;
        });
    }

    private static LambdaExpression<Integer, Integer> getDivideExpression() {
        return LambdaExpression.createGroupSingleConfig(":", (s, i, list) -> {
            int res = 0;
            boolean first = true;
            for (Integer in : list) {
                if (first) {
                    res = in;
                } else {
                    res /= in;
                }
                first = false;
            }
            return res;
        });
    }

    private static LambdaExpression<Integer, Integer> getPowExpression() {
        return LambdaExpression.createGroupSingleConfig("^", (s, i, list) -> {
            int res = 0;
            final int factor = s == null ? 1 : Integer.parseInt(s);
            for (Integer in : list) {
                res += Math.pow(in, factor);
            }
            return res;
        });
    }

    @Test
    public void simple() throws ParseException {
        assertLambdaExpression("2", true, 0, 2);
        assertLambdaExpression(":2", false, 0, 2);
        assertLambdaExpression("x", true, 3, 3);
        assertLambdaExpression("length:abcde", true, 0, 5);
    }

    @Test
    public void simpleGroup() throws ParseException {
        assertLambdaExpression("+(2;3;x)", true, 4, 9);
    }

    @Test
    public void complexGroup() throws ParseException {
        assertLambdaExpression("+(2;-(x;3);^(x;2);*(x;5);-(-(6;3);1))", true, 4, 31);
    }

    @Test
    public void escapedCharacters() throws ParseException {
        assertLambdaExpression("\\::notused(*(length:\\)\\:\\\\\\(b\\;;4);x)", true, 8, 3); //length should be 6
    }

    @Test
    public void whitespaces() throws ParseException {
        //whitespaces in config ARE COUNTED!
        assertLambdaExpression("  length  :  1234567\n\t  ", false, 0, 13);
        assertLambdaExpression("  length  :  1234567\n\t  :abc", false, 0, 133);
        assertLambdaExpression("   +   (  length  :  1234567  ;   :  1   ;   x ; -  (  3  ;  1  )  )  ", false, 8, 22);
    }

    @Test
    public void config() throws ParseException {
        assertLambdaExpression("length", false, 0, 0);
        assertLambdaExpression("length:123", false, 0, 3);
        assertLambdaExpression("length:123:1234", false, 0, 34);
        assertLambdaExpression("length:123:1 34:", false, 0, 340);
    }

    @Test
    public void parameterizedGroupExpression() throws ParseException {
        assertLambdaExpression("^:2(x;2)", true, 5, 29);
    }

    @Test
    public void parsingErrors() {
        assertParseException("", "Unexpected end of expression");
        assertParseException("()", "Expression expected, but none was found");
        assertParseException("((5))", "Expression expected, but none was found");
        assertParseException("+(5;3", "Unexpected end of expression");
        assertParseException("+(5;3;", "Unexpected end of expression");
        assertParseException("+(nonexisting:x)", "No expression type found for id");
        assertParseException("+(3;x)a", "Unexpected leftover in expression");
    }

    @Test
    public void caseinsensitiveIds() throws ParseException {
        assertLambdaExpression("length:123", false, 0, 3);
        assertLambdaExpression("LENGTH:123", false, 0, 3);
        assertLambdaExpression("lenGth:123", false, 0, 3);
    }

    private void assertParseException(final String expressionString, final String expectedMessageContains) {
        try {
            final LambdaExpression<Integer, Integer> exp = calculator.create(expressionString);
            fail("Expected ParseException for '" + expressionString + "', but got: '" + calculator.getConfig(exp) + "'");
        } catch (ParseException pe) {
            assertThat(pe.getMessage()).as("ParsingException message").contains(expectedMessageContains);
        }
    }

    private void assertLambdaExpression(final String expressionString, final boolean testConfigEquality, final Integer ... paramExpextedResultPairs) throws ParseException {
        final LambdaExpression<Integer, Integer> exp = calculator.create(expressionString);
        for (int i = 0; i < paramExpextedResultPairs.length; i += 2) {
            assertThat(exp.call(paramExpextedResultPairs[i])).isEqualTo(paramExpextedResultPairs[i + 1]);
        }
        final String config = calculator.getConfig(exp);
        if (testConfigEquality) {
            assertThat(calculator.getConfig(exp)).isEqualTo(expressionString);
        }
        final LambdaExpression<Integer, Integer> exp2 = calculator.create(config);
        for (int i = 0; i < paramExpextedResultPairs.length; i += 2) {
            assertThat(exp2.call(paramExpextedResultPairs[i])).isEqualTo(paramExpextedResultPairs[i + 1]);
        }
    }
}
