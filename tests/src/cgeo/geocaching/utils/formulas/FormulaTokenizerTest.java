package cgeo.geocaching.utils.formulas;

import java.text.ParseException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class FormulaTokenizerTest {

    @Test
    public void simple() throws ParseException {
        final FormulaTokenizer ft = new FormulaTokenizer("5 + 5");
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "5"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "+"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "5"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.END, null));
    }

    @Test
    public void numeric() throws ParseException {
        assertThat(new FormulaTokenizer("5").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "5"));
        assertThat(new FormulaTokenizer("534.5").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "534.5"));
        assertThat(new FormulaTokenizer("5.5.4").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "5.5"));
        assertThat(new FormulaTokenizer("-5.5.4").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "-5.5"));
        assertThat(new FormulaTokenizer("-0.531.4").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "-0.531"));
        assertThat(new FormulaTokenizer("-5+3").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "-5"));
    }

    @Test
    public void allowedTransitions() throws ParseException {
        final FormulaTokenizer ft = new FormulaTokenizer("8-3");
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "8"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "-"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "3"));
    }

    @Test
    public void text() throws ParseException {
        assertThat(new FormulaTokenizer("'test'").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.TEXT, "test"));
        assertThat(new FormulaTokenizer("\"test\"").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.TEXT, "test"));
        assertThat(new FormulaTokenizer("'test\\'\\\\'").parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.TEXT, "test'\\"));
        assertThatThrownBy(() ->  new FormulaTokenizer("'test").parseNextToken()).isInstanceOf(FormulaParseException.class);
        assertThatThrownBy(() ->  new FormulaTokenizer("'test\"").parseNextToken()).isInstanceOf(FormulaParseException.class);
    }

    @Test
    public void parenthesis() throws ParseException {
        final FormulaTokenizer ft = new FormulaTokenizer("((a;b)");
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_OPEN, "("));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_OPEN, "("));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.ID, "a"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_SEPARATOR, ";"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.ID, "b"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_CLOSE, ")"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.END, null));
    }

    @Test
    public void whitespace() throws ParseException {
        final FormulaTokenizer ft = new FormulaTokenizer("  aBC1**(5.5+ -6.7   ) \t + 'text\t \n'+\"abc\"   ");
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.ID, "aBC1"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "**"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_OPEN, "("));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "5.5"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "+"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.NUMERIC, "-6.7"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.PAREN_CLOSE, ")"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "+"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.TEXT, "text\t \n"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.SYMBOL, "+"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.TEXT, "abc"));
        assertThat(ft.parseNextToken()).isEqualTo(new FormulaTokenizer.TokenData(FormulaTokenizer.Token.END, null));
    }

}
