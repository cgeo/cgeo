package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.KeyableCharSet;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.MISSING_VARIABLE_VALUE;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.OTHER;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.UNEXPECTED_TOKEN;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.assertj.core.data.Offset;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class FormulaTest {

    private static final Offset<Double> DOUBLE_OFFSET = Offset.offset(0.0000001d);

    private static double eval(final String expression, final Object... vars) {
        System.out.println("EVAL: " + Formula.compile(expression).toDebugString(null, true, true));
        return Formula.eval(expression, vars);
    }

    private static Set<String> neededVars(final String expression) {
        return Formula.compile(expression).getNeededVariables();
    }

    @Test
    public void numbers() {
        assertThat(Formula.evaluate("-2.5")).isEqualTo(Value.of(-2.5d));
        assertThat(Formula.evaluate("2,5")).isEqualTo(Value.of(2.5d));
        assertThat(Formula.evaluate("2,5.5")).isEqualTo(Value.of("2,5.5"));
    }

    @Test
    public void numericOverflow() {
        //Factor
        assertCharSequence(Formula.compile("99^99").evaluateToCharSequence(null), "369729637649726772657187905628805440595668764281741102430259972423552570455277523421410650010128232727940978889548326540119429996769494359451621570193644014418071060667659301384999779999159200499899");
        assertCharSequence(Formula.compile("99.2^99").evaluateToCharSequence(null), "451497701011167906703841661163527048570327680456821131912927542131240109815747012332035456522827892501171432658231661888826169986748536541541135792931381804288134021576803170332358962773334502471794.879484008303777090366409951066961034427451419830234801561538473283654036152732093162031995921891328");
        assertCharSequence(Formula.compile("3.2^3.2").evaluateToCharSequence(null), "41.350421");
        assertCharSequence(Formula.compile("9.2^9.2").evaluateToCharSequence(null), "[9.2 ^ 9.2]");

        //factor
        assertCharSequence(Formula.compile("20!").evaluateToCharSequence(null), "2432902008176640000");
        assertCharSequence(Formula.compile("21.000!").evaluateToCharSequence(null), "51090942171709440000");
        assertCharSequence(Formula.compile("100!").evaluateToCharSequence(null), "[100!]");

        //various
        assertCharSequence(Formula.compile("1/0").evaluateToCharSequence(null), "[1 / 0]");
        assertCharSequence(Formula.compile("1/0.00000000000000000000000001").evaluateToCharSequence(null), "100000000000000000000000000");
    }

    @Test
    public void bigIntegers() {
        //Long value
        final String longMax = String.valueOf(Long.MAX_VALUE); // this should be 9223372036854775807
        final int longMaxCs = 88; // manually calculated checksum of Long.MAX_VALUE

        assertThat(Formula.evaluate(longMax).isInteger()).isTrue();
        assertThat(Formula.evaluate(longMax).getAsInteger()).isEqualTo(Long.MAX_VALUE);
        //Beyond Long Value
        final String longMaxBeyond = longMax + "1";
        final Value longMaxBeyondValue = Formula.evaluate(longMaxBeyond);
        assertThat(longMaxBeyondValue.isInteger()).isTrue();
        assertThat(longMaxBeyondValue.getAsInteger()).isEqualTo(new BigInteger(longMaxBeyond)); // if too long for parsing -> return 0

        //Long value checksums
        assertThat(Formula.evaluate("cs(" + longMax + ")").getAsInteger()).isEqualTo(longMaxCs);
        assertThat(FormulaUtils.checksum(longMaxBeyondValue, false)).isEqualTo(longMaxCs + 1);
        assertThat(Formula.evaluate("cs(" + longMaxBeyond + ")").getAsInteger()).isEqualTo(longMaxCs + 1);
    }

    @Test
    public void complex() {
        assertThat(eval("-2.5 + 3 * (4-1) + 3^3")).isEqualTo(33.5d);
    }

    @Test
    public void function() {
        assertThat(eval("sqrt(9)")).isEqualTo(3d);
    }

    @Test
    public void whitespaces() {
        assertThat(eval("3+5")).isEqualTo(8d);
        assertThat(eval(" 3 + 5 ")).isEqualTo(8d);
        assertThat(eval("\t\n 3\t\n +\t\n 5\t\n ")).isEqualTo(8d);
        assertThat(eval("(3 + 5)")).isEqualTo(8d);
        assertThat(eval("(3 + 5 )")).isEqualTo(8d);
        assertThat(eval("(3 + A)", "A", "5")).isEqualTo(8d);
        assertThat(eval("(3 + A )", "A", "5")).isEqualTo(8d);
        assertThat(eval("(3 + A ) * 4", "A", "5")).isEqualTo(32d);
    }

    @Test
    public void singleTestForDebug() {
        assertThat(eval("(3 + 5 )")).isEqualTo(8d);
    }

    @Test
    public void concat() {
        assertThat(eval("3A4.3", "A", 1)).isEqualTo(314.3d);
        assertThat(eval("3A4.3", "A", 1.2)).isEqualTo(0d);
        assertThat(eval("3A4.3", "A", 12)).isEqualTo(3124.3d);

        //parenthesis
        assertThat(eval("(3(5))")).isEqualTo(35d);
        assertThat(eval("(3(5)AA)", "A", 1)).isEqualTo(3511d);
        assertThat(eval("3(5)A(A+1)", "A", 1)).isEqualTo(3512d);

        //whitespaces
        assertThat(eval("3 4")).isEqualTo(34d);
        assertThat(eval("3 A   4.3", "A", 12)).isEqualTo(3124.3d);
        assertThat(eval("A+1 4", "A", 12)).isEqualTo(134d);
        assertThat(eval("3 A+1 4.3", "A", 12)).isEqualTo(3134.3d);
        assertThat(eval("3 A+1+4.3", "A", 12)).isEqualTo(317.3d);
        assertThat(eval("3 A+1 4.3", "A", 1.2)).isEqualTo(0d);

        assertThat(eval("3 A + 1 4.3", "A", 2)).isEqualTo(334.3d);
        assertThat(eval("3 (A+1 A + 2) 4.3", "A", 2)).isEqualTo(3344.3d);

        //issue #12610
        assertThat(eval("2+ 3 4 +5")).isEqualTo(59d);
        assertThat(eval("2+ 3 4+5")).isEqualTo(59d);
        assertThat(eval("1 + 2 3")).isEqualTo(33d);
        assertThat(eval("2+(3 4)+5")).isEqualTo(41d);
    }


    @Test
    public void variableCalculations() {
        assertThat(eval("AA / 2 + B", "A", 4, "B", 5)).isEqualTo(27d);
        assertThat(eval("sqrt(A*B) * C", "A", 4, "B", 4, "C", 10)).isEqualTo(40d);
        assertThat(eval("AB + A + B", "AB", 14, "B", 4, "A", 3)).isEqualTo(41d);
        assertThat(eval("$A$B + A + B", "AB", 14, "B", 4, "A", 3)).isEqualTo(41d);
        assertThat(eval("$AB + A + $B", "AB", 14, "B", 4, "A", 3)).isEqualTo(21d);
        assertThat(eval("$AB(A+$B*2)$B", "AB", 14, "B", 4, "A", 3)).isEqualTo(14114d);

        assertThat(eval("A$A$AB", "A", 1, "AB", 2)).isEqualTo(112);
        assertThat(eval("-A$A$AB", "A", 1, "AB", 2)).isEqualTo(-112);
        assertThat(eval("-A$A$AB", "A", 0, "AB", 2)).isEqualTo(-2);
        assertThatThrownBy(() -> eval("AA$", "A", 1))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());

        assertThat(eval("-A", "A", 1)).isEqualTo(-1);
        assertThat(eval("A", "A", -1)).isEqualTo(-1);
        assertThat(Formula.evaluate("AA", "A", -1).getAsString()).isEqualTo("-1-1");
    }

    @Test
    public void variablesWithDecimals() {

        assertThat(eval("3.14A5", "A", 1)).isEqualTo(3.1415d);
        assertThat(eval("3.A4A5", "A", 1)).isEqualTo(3.1415d);
        assertThat(eval("A.(A+1)(A+10)", "A", 1)).isEqualTo(1.211d);
        assertThat(eval("-A.(A+1)(A+10)", "A", 1)).isEqualTo(-1.211d);

        assertThat(eval("A15B", "A", 3.14, "B", 92)).isEqualTo(3.141592d);
        assertThat(Formula.evaluate("AA", "A", 1.2).getAsString()).isEqualTo("1.21.2");

        assertThat(eval("A.A", "A", 1)).isEqualTo(1.1d);
        assertThat(Formula.evaluate("A.A.A", "A", 1).getAsString()).isEqualTo("1.1.1");
        assertThat(Formula.evaluate("...", "A", 1).getAsString()).isEqualTo("...");
        assertThat(eval(".12")).isEqualTo(.12d);

    }

    @Test
    public void variableParsing() {
        assertThat(Formula.compile("A").getNeededVariables()).containsExactlyInAnyOrder("A");
        assertThat(Formula.compile("AB").getNeededVariables()).containsExactlyInAnyOrder("A", "B");
        assertThat(Formula.compile("AB").getNeededVariables()).containsExactlyInAnyOrder("A", "B");
        assertThat(Formula.compile("ABa").getNeededVariables()).containsExactlyInAnyOrder("A", "B", "a");
        assertThat(Formula.compile("AB1").getNeededVariables()).containsExactlyInAnyOrder("A", "B");
        assertThat(Formula.compile("$AB1").getNeededVariables()).containsExactlyInAnyOrder("AB1");
        assertThat(Formula.compile("$AB(1)").getNeededVariables()).containsExactlyInAnyOrder("AB");
        assertThat(Formula.compile("${AB}A1").getNeededVariables()).containsExactlyInAnyOrder("AB", "A");
        assertThat(Formula.compile("${A}A1").getNeededVariables()).containsExactlyInAnyOrder("A");

        assertThatThrownBy(() -> eval("$"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
        assertThatThrownBy(() -> eval("$1"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
        assertThatThrownBy(() -> eval("${A(B)"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining("}");
    }

    @Test
    public void ranges() {
        //assert handling of "normal" formats
        assertThat(Formula.evaluateWithRanges("[:0-9]", 3)).isEqualTo(Value.of(3));
        assertThat(Formula.compile("[:0-9]").getRangeIndexSize()).isEqualTo(10);

        assertThat(Formula.compile("[:0-3]*[:0-4]").getRangeIndexSize()).isEqualTo(20);
        assertRangeFormula("[:1-2, 8]*3", null, 3, 6, 24);
        assertRangeFormula("[:2-1, 8]*3", null, 3, 6, 24);
        assertRangeFormula("[:1-2]*[:3-4]", null, 3, 6, 4, 8);
        assertRangeFormula("[:1,3]*[:10,20]", null, 10, 30, 20, 60);
        assertRangeFormula("[:3,1]*[:10,20]", null, 30, 10, 60, 20);

        //assert graceful handling of "strange" formats
        assertRangeFormula("[:abc1]", null, 1);
        assertRangeFormula("[:2-]", null, 2);
        assertRangeFormula("[:-3]", null, 3);

        //assert
        assertThatThrownBy(() -> eval("[:0-3"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining("]");
        assertThatThrownBy(() -> eval("[:]"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(OTHER.name());
        assertThatThrownBy(() -> eval("[:  ]"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(OTHER.name());
    }

    private void assertRangeFormula(final String formula, final Function<String, Value> varMap, final Object... expectedResults) {
        final Formula f = Formula.compile(formula);
        assertThat(f.getRangeIndexSize()).isEqualTo(expectedResults.length);
        final List<Value> results = new ArrayList<>();
        for (int i = 0; i < f.getRangeIndexSize(); i++) {
            results.add(f.evaluate(varMap, i));
        }
        final List<Value> expectedResultList = new ArrayList<>();
        for (Object o : expectedResults) {
            expectedResultList.add(o instanceof Value ? (Value) o : Value.of(o));
        }
        assertThat(results).containsExactlyElementsOf(expectedResultList);
    }

    @Test
    public void strings() {
        assertThat(Formula.evaluate("'abc'")).isEqualTo(Value.of("abc"));
        assertThat(Formula.evaluate("'a\"b''c'")).isEqualTo(Value.of("a\"b'c"));
        assertThat(Formula.evaluate("\"abc\"")).isEqualTo(Value.of("abc"));
        assertThat(Formula.evaluate("\"a\"\"b'c\"")).isEqualTo(Value.of("a\"b'c"));
        assertThatThrownBy(() -> eval("\"abc'"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name())
                .hasMessageContaining("\"");

        assertThat(eval("length('abc')")).isEqualTo(3d);
        assertThat(eval("length('abc') + length('def')")).isEqualTo(6d);
        assertThat(eval("length('abc''def')")).isEqualTo(7d);
        assertThat(eval("length(A)", "A", "test")).isEqualTo(4d);
        assertThat(eval("length(AA)", "A", "test")).isEqualTo(8d);
        assertThat(eval("length(A'def'123)", "A", "test")).isEqualTo(10d);
    }

    @Test
    public void stringsAndStartEndTokens() {
        assertThat(Formula.evaluate("' '").getRaw()).isEqualTo(" ");
        assertThat(Formula.evaluate("''").getRaw()).isEqualTo("");
        assertThat(Formula.evaluate("''''").getRaw()).isEqualTo("'");
        assertThat(Formula.evaluate("''''''").getRaw()).isEqualTo("''");
        assertThatThrownBy(() -> eval("'''''"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining("'");
        assertThat(Formula.evaluate("'''test'''").getRaw()).isEqualTo("'test'");

    }

    @Test
    public void functions() {
        assertThat(eval("round(1.2)")).isEqualTo(1);
        assertThat(eval("round(1.2345;3)")).isEqualTo(1.235d);
        assertThat(Formula.evaluate("rot13('abc')").getRaw()).isEqualTo("nop");
        assertThat(Formula.evaluate("rot13(rot('aBc'; -13))").getRaw()).isEqualTo("aBc");
        assertThat(Formula.evaluate("rot('abc'; 1)").getRaw()).isEqualTo("bcd");
        assertThat(eval("rot1(a)", "r", 1d, "o", 2d, "t", 3d, "a", 4d)).isEqualTo(12314d);

        assertCharSequence(Formula.compile("substr('abcde'; 2; 2)").evaluateToCharSequence(null), "bc");
        assertCharSequence(Formula.compile("substr('abcde'; 1; 5)").evaluateToCharSequence(null), "abcde");
        assertCharSequence(Formula.compile("substr('abcde'; 3)").evaluateToCharSequence(null), "cde");
        assertCharSequence(Formula.compile("substr('abcde';2;8)").evaluateToCharSequence(null), "substr('abcde'; 2; [<8>])");
        assertCharSequence(Formula.compile("substr('abcde';'b';2)").evaluateToCharSequence(null), "substr('abcde'; [<'b'>]; 2)");
    }

    @Test
    public void unaryOperators() {
        assertThat(eval("(1+2)!")).isEqualTo(6);
        assertThat(eval("10--2")).isEqualTo(12);
        assertThat(eval("10- -+ +- -+ +-2")).isEqualTo(8);
    }

    @Test
    public void operatorPrecedenceAndEvaluationOrder() {
        assertThat(eval("100-5-5")).isEqualTo(90);
        assertThat(eval("100-5*5")).isEqualTo(75);
        assertThat(eval("100/5")).isEqualTo(20);
        assertThat(eval("100/5/5")).isEqualTo(4);
    }

    @Test
    public void unknownOperator() {
        assertThatThrownBy(() -> eval("2 & 5"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
    }

    @Test
    public void unknownVariables() {
        assertThatThrownBy(() -> eval("unknown(42)"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(MISSING_VARIABLE_VALUE.name()).hasMessageContaining("k, n, o, u, w");
    }

    @Test
    public void parenthesis() {
        assertThat(eval("(1+2)*3")).isEqualTo(9);
        assertThat(eval("[1+2]*3")).isEqualTo(9);
        assertThat(eval("[1+(7-4)]*3")).isEqualTo(12);

        //unclosed parenthesis
        assertThatThrownBy(() -> eval("3 * (2 + 4 * 2"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
        //non-matching parenthesis
        assertThatThrownBy(() -> eval("3 * (2 + 4 * 2]"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
        assertThatThrownBy(() -> eval("3 * [2 + 4 * 2)"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());

    }

    @Test
    public void parseFormulaParts() {
        assertFormulaPart("(1+2)", 0, null, "3", "(1+2)");
        assertFormulaPart("a1+2|", 1, "|", "3", "1+2");
        assertFormulaPart("1 + (10 - 2) + 5 -", 0, "-", "14", "1 + (10 - 2) + 5 ");
        assertFormulaPart("1+(10 - 2) + 5 -", 0, "- ", "9", "1+(10 - 2)");
        assertFormulaPart("111 + (10 - '2') + 5 ''", 2, "'", "14", "1 + (10 - '2') + 5 ");
    }

    private void assertFormulaPart(final String formulaString, final int startPos, final String stopChars, final String expectedValue, final String expectedFormPart) {
        final Formula f = Formula.compile(formulaString, startPos, KeyableCharSet.createFor(stopChars));
        assertThat(f.evaluate().getAsString()).isEqualTo(expectedValue);
        assertThat(f.getExpression()).isEqualTo(expectedFormPart);
    }

    @Test
    public void incorrectFunctionCall() {
        assertThatThrownBy(() -> eval("sin('abc')"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(WRONG_TYPE.name()).hasMessageContaining("sin").hasMessageContaining("abc");
        assertThatThrownBy(() -> eval("sin()"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(WRONG_PARAMETER_COUNT.name()).hasMessageContaining("sin");
    }

    @Test
    public void advancedFunctions() {
        assertThat(eval("checksum(888)")).isEqualTo(24);
        assertThat(eval("ichecksum(888)")).isEqualTo(6);
        assertThat(eval("ichecksum(-888)")).isEqualTo(-6);
        assertThat(eval("ichecksum(-888.235)")).isEqualTo(-7);
        assertThat(eval("lettervalue('Test123')")).isEqualTo(20 + 5 + 19 + 20 + 1 + 2 + 3);
        assertThat(eval("lettervalue(-888.123)")).isEqualTo(30);
    }

    @Test
    public void neededVariables() {
        assertThat(neededVars("abcdef")).containsExactlyInAnyOrder("a", "b", "c", "d", "e", "f");
        assertThat(neededVars("abcdef")).containsExactlyInAnyOrder("a", "b", "c", "d", "e", "f");
        assertThat(neededVars("$abcdef")).containsExactlyInAnyOrder("abcdef");
        assertThat(neededVars("Test123one")).containsExactlyInAnyOrder("T", "e", "s", "t", "o", "n");
        assertThat(neededVars("Test123$one")).containsExactlyInAnyOrder("T", "e", "s", "t", "one");
        assertThat(neededVars("B+C")).containsExactlyInAnyOrder("B", "C");

    }

    @Test
    public void value() {
        assertThat(Value.of(345).getAsInteger()).isEqualTo(345);
        assertThat(Value.of("345").getAsInteger()).isEqualTo(345);
        assertThat(Value.of(345).isInteger()).isTrue();
        assertThat(Value.of("345").isInteger()).isTrue();
        assertThat(Value.of("345b").isInteger()).isFalse();
        assertThat(Value.of("abcd").isInteger()).isFalse();

        assertThat(Formula.evaluate("123").isInteger()).isTrue();
        assertThat(Formula.evaluate("123").getAsInteger()).isEqualTo(123L);
        assertThat(Formula.evaluate("123.0").isInteger()).isTrue();
        assertThat(Formula.evaluate("123.0").getAsInteger()).isEqualTo(123L);
        assertThat(Formula.evaluate("123.00000000000000001").isInteger()).isFalse();
        assertThat(Formula.evaluate("123.00000000000000001").getAsInteger()).isEqualTo(123L);
    }

    @Test
    public void overflow() {
        assertThat(eval("A8.A2", "A", 10)).isEqualTo(108.102d);
        assertThat(eval("_A8._A2", "A", 10)).isEqualTo(108.102d);
        assertThat(eval("_A8._A2", "A", 8)).isEqualTo(88.082d);
        assertThat(eval("8._1_A2", "A", 8)).isEqualTo(8.01082d);
        assertThat(eval("8.A__A2", "A", 14)).isEqualTo(8.140142d);
        assertThat(eval("_A._A", "A", 143)).isEqualTo(143.143d);
        assertThat(eval("_A._A", "A", 14)).isEqualTo(14.14d);
        assertThat(eval("_A._A", "A", 1)).isEqualTo(1.01d);

        //handling of _ on more strange positions
        assertThat(eval("5._1")).isEqualTo(5.01d);
        assertThat(eval("5._12")).as("number should not spill over to _").isEqualTo(5.012d);
        assertThat(eval("5._(8+1)")).as("expression results should spill over").isEqualTo(5.09d);
        assertThat(eval("5._(8+5)")).as("expression results should spill over").isEqualTo(5.13d);
        assertThat(eval("5_.12")).as("_ before . should have no effect").isEqualTo(5.12d);
        assertThat(Formula.evaluate("_5.12").getAsString()).as("_ at start should be removed").isEqualTo("5.12");
        assertThat(Formula.evaluate("5.12_").getAsString()).as("_ at end should be removed").isEqualTo("5.12");
    }

    @Test
    public void evaluateToString() {
        assertThat(Formula.compile("A.B").evaluateToString(Formula.toVarProvider("A", 1, "B", 2))).isEqualTo("1.2");
        assertThat(Formula.compile("A(A+B)B").evaluateToString(Formula.toVarProvider("A", 1, "B", 2))).isEqualTo("132");
        assertThat(Formula.compile("A.B").evaluateToString(Formula.toVarProvider("A", 1))).isEqualTo("1.?B");
        assertThat(Formula.compile("A*5").evaluateToString(Formula.toVarProvider("A", "eddie"))).isEqualTo("'eddie' * 5");
        assertThat(Formula.compile("(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", 2)))
                .isEqualTo("15");
        assertThat(Formula.compile("(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", "e")))
                .isEqualTo("('e' * 4 + 7)");
        assertThat(Formula.compile("123(4*3)B_A(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", "eddie")))
                .isEqualTo("12312?Beddie('eddie' * 4 + 7)");
    }

    @Test
    public void evaluateToCharSequence() {
        assertCharSequence(Formula.compile("3-'a'").evaluateToCharSequence(null), "[3 - 'a']");
        assertCharSequence(Formula.compile("3+4+'a'+5").evaluateToCharSequence(null), "[7 + 'a'] + 5");
        assertCharSequence(Formula.compile("3+4+'a'+AB12").evaluateToCharSequence(null), "[7 + 'a'] + [?A][?B]12");
        assertCharSequence(Formula.compile("(5+'a')").evaluateToCharSequence(null), "([5 + 'a'])");

        //parameter errors
        assertCharSequence(Formula.compile("chars('abc';1;4;2;5;-4;'b')").evaluateToCharSequence(null), "chars('abc'; 1; [<4>]; 2; [<5>]; [<-4>]; [<'b'>])");
        assertCharSequence(Formula.compile("'a'/'b'").evaluateToCharSequence(null), "['a' / 'b']");
    }

    /**
     * checks whether a given error string is correct and has the right places highlighted with an error format
     */
    private void assertCharSequence(final CharSequence cs, final String expectedString) {
        final String annotated = TextUtils.annotateSpans(cs, s -> {
            if (s instanceof ForegroundColorSpan && ((ForegroundColorSpan) s).getForegroundColor() == Color.RED) {
                return new Pair<>("[", "]");
            }
            return new Pair<>("", "");
        });

        assertThat(annotated).isEqualTo(expectedString);
    }

    @Test
    public void efficientFormulaNodeTrees() {
        assertThat(Formula.compile("13.123").toDebugString(null, false, true))
                .as("constant value should be in one node").isEqualTo("13.123{}");
        assertThat(Formula.compile("13.123(4+5)").toDebugString(null, false, true))
                .as("constant value should be in one node").isEqualTo("13.1239{}");
        assertThat(Formula.compile("13.123 + length('abc')").toDebugString(null, false, true))
                .as("constant value should be in one node").isEqualTo("16.123{}");

        assertThat(Formula.compile("A + (length('abc') + 3)").toDebugString(null, false, true))
                .as("constant parts should be in one subnode").isEqualTo("?A + 6{?A{};6{}}");

        assertThat(Formula.compile("12A.456B8901").toDebugString(null, false, true))
                .as("decimals should be stored as efficiently as possible").isEqualTo("12?A.456?B8901{12{};?A{};.456{};?B{};8901{}}");


    }

    @Test
    public void unusualCharacters() {
        assertThat(eval("(3+5) * (4+7)")).isEqualTo(88d);
        assertThat(eval("42•2")).isEqualTo(84d);
        assertThat(eval("42÷2")).isEqualTo(21d);
        assertThat(eval("42:2")).isEqualTo(21d);
        assertThat(eval("42—2")).isEqualTo(40d);
        assertThat(eval("—2")).isEqualTo(-2d);

        assertThat(eval("2²")).isEqualTo(4d);
        assertThat(eval("-4³")).isEqualTo(-64d);
        assertThat(eval("3!²")).isEqualTo(36d);
        assertThat(eval("(3²)!")).isEqualTo(2 * 3 * 4 * 5 * 6 * 7 * 8 * 9);
    }

    @Test
    public void comparison() {
        assertThat(eval("3==3")).isEqualTo(1d);
        assertThat(eval("3<>4")).isEqualTo(1d);
        assertThat(eval("3<4")).isEqualTo(1d);
        assertThat(eval("3<=4")).isEqualTo(1d);
        assertThat(eval("3>=4")).isEqualTo(0d);
        assertThat(eval("4>=4")).isEqualTo(1d);
        assertThat(eval("(3+5) > (4+7)")).isEqualTo(0d);
        assertThat(eval("(3+5) < (4+7)")).isEqualTo(1d);

        assertThatThrownBy(() -> eval("3>>4"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining(">");
        assertThatThrownBy(() -> eval("3<<4"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining("<");
        assertThatThrownBy(() -> eval("3=>4"))
                .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name()).hasMessageContaining(">");

        assertThat(eval("if(3>4;1;2)")).isEqualTo(2d);
        assertThat(eval("if(3<4;1;2)")).isEqualTo(1d);

        //comaring strings and number-string-mixes
        assertThat(eval("'a' > 'd'")).isEqualTo(0d);
        assertThat(eval("'a' <= 'd'")).isEqualTo(1.0d);
        assertThat(eval("'a' > 4")).isEqualTo(1d);
    }

    @Test
    public void checkFormatter() {
        final char c = 'E';
        final double d = 9.1324567934d;
        assertThat(String.format(Locale.US, "%c%09.5f°", c, d)).isEqualTo("E009.13246°");
    }

    @Test
    public void userComments() {
        assertThat(eval("123#4")).isEqualTo(123d);
        assertThat(eval("123# 4")).isEqualTo(123d);
        assertThat(eval("123 #4")).isEqualTo(123d);
        assertThat(eval("123 # 4")).isEqualTo(123d);
        assertThat(eval("123##*4")).isEqualTo(123 * 4d);
        assertThat(eval("123 ## * 4")).isEqualTo(123 * 4d);
        assertThat(eval("123 #4# * 5")).isEqualTo(123 * 5d);
        assertThat(eval("123 ### * 4")).isEqualTo(123d);
        assertThat(eval("123#ABC")).isEqualTo(123d);
        assertThat(eval("123 # -|=$&/( ABC 456 ÄÖÜäöü πΩ👍")).isEqualTo(123d);
        assertThat(eval("3.14 #this is pi# * R # this is the radius used # *2    # 2*pi*r", "R", 10)).isEqualTo(3.14 * 10 * 2, DOUBLE_OFFSET);
        assertThat(eval("4! #comment")).isEqualTo(4 * 3 * 2);
        assertThat(eval("4^2 #comment")).isEqualTo(4 * 4);
        assertThat(eval("2 * (3 + 4) #comment")).isEqualTo(2 * (3 + 4));
    }

}
