package cgeo.geocaching.utils.formulas;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import static java.lang.Math.abs;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FormulaParserTest {

    private FormulaParser parser;

    @Before
    public void before() {
        parser = new FormulaParser();
        parser.register(new FormulaParser.FunctionData<>("+", 1, true, PlusOperator::new));
        parser.register(new FormulaParser.FunctionData<>("-", 1, false, MinusOperator::new));
        parser.register(new FormulaParser.FunctionData<>("*", 2, true, TimesOperator::new));
        parser.register(new FormulaParser.FunctionData<>("!", 0, true, FactorialOperator::new));
        parser.register(new FormulaParser.FunctionData<>("length", -1, false, LengthOperator::new));
    }

    @Test
    public void simple() throws ParseException {
        assertThat(calc("2+3")).isEqualTo(5);
        assertThat(calc("8-2")).isEqualTo(6);
        assertThat(calc("8-2+1")).isEqualTo(7);
    }

    @Test
    public void precedence() throws ParseException {
        assertThat(calc("8-2*5+1*3+1+6")).isEqualTo(8);
        assertThat(calc("(8-2)*5+1*(3+1)+6")).isEqualTo(40);
    }

    @Test
    public void simplefunction() throws ParseException {
        assertThat(calc("8-length('abc';'d')")).isEqualTo(4);
    }

    @Test
    public void parenthesis() throws ParseException {
        assertThat(calc("(((5)))*1")).isEqualTo(5);
        assertThat(calc("((8)*(5))")).isEqualTo(40);
    }

    @Test
    public void primitives() throws ParseException {
        assertThat(calc("'7'")).isEqualTo(7);
        assertThat(calc("7")).isEqualTo(7);
        assertThat(calc("7.0")).isEqualTo(7);
    }

    @Test
    public void unary() throws ParseException {
        assertThat(calc("!7")).isEqualTo(5040);
        assertThat(calc("!(3-7)")).isEqualTo(24);
    }

    @Test
    public void complex() throws ParseException {
        assertThat(calc("length(3;'wert')")).isEqualTo(5);
        assertThat(calc("length(3;'wert') - 2 -1 +15")).isEqualTo(17);
        assertThat(calc("length(3;'wert') - 2 -1 +15 -2*4+7")).isEqualTo(16);
        assertThat(calc("(length(3;'wert') - 2 -1 +15 -2*4+7)")).isEqualTo(16);
        assertThat(calc("(length(3;'wert') - 2 -1 +15 -2*4+7) - (length(3;'wert') - 2 -1 +15 -2*4+7)")).isEqualTo(0);
    }


    private int calc(final String formula) throws ParseException {
        final IFormulaNode node = parser.parse(formula);
        if (node instanceof BaseFunctionNode) {
            return ((BaseFunctionNode) node).calculate();
        }
        return ((BaseFormulaNode) node).getValueAsInt();
    }

    public abstract static class BaseFunctionNode extends BaseFormulaNode {

        abstract int calc(List<Integer> childValues);

        public int calculate() {
            final List<Integer> values = new ArrayList<>();
            for (IFormulaNode child : getChildren()) {
                calcForChild(child, values);
            }
            return calc(values);
        }

        protected void calcForChild(final IFormulaNode child, final List<Integer> values) {
            if (child instanceof BaseFunctionNode) {
                values.add(((BaseFunctionNode) child).calculate());
            } else  {
                values.add(((BaseFormulaNode) child).getValueAsInt());
            }
        }
    }

    public static class PlusOperator extends BaseFunctionNode {

        public int calc(final List<Integer> childValues) {
            int sum = 0;
            for (int c : childValues) {
                sum += c;
            }
            return sum;
        }
    }

    public static class TimesOperator extends BaseFunctionNode {

        public int calc(final List<Integer> childValues) {
            int result = 1;
            for (int c : childValues) {
                result *= c;
            }
            return result;
        }
    }

    public static class MinusOperator extends BaseFunctionNode {

        public int calc(final List<Integer> childValues) {
            if (childValues.isEmpty()) {
                return 0;
            }

            int sum = 0;
            boolean first = true;
            for (int c : childValues) {
                if (!first) {
                    sum += c;
                }
                first = false;
            }
            return childValues.get(0) - sum;
        }
    }

    public static class LengthOperator extends PlusOperator {

        @Override
        protected void calcForChild(final IFormulaNode child, final List<Integer> values) {
            values.add(((BaseFormulaNode) child).getValueAsString().length());
        }
    }

    public static class FactorialOperator extends BaseFunctionNode {

        public int calc(final List<Integer> childValues) {
            if (childValues.isEmpty()) {
                return 0;
            }
            int result = 1;
            for (int i = 1 ; i <= abs(childValues.get(0)); i++) {
                result *= i;
            }
            return result;
        }
    }


}
