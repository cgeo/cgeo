package cgeo.geocaching.utils.formulas;

import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.MISSING_VARIABLE_VALUE;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.UNEXPECTED_TOKEN;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class FormulaTest {


    private static double eval(final String expression, final Object ... vars) {
        return Formula.eval(expression, vars);
    }

    private static Set<String> neededVars(final String expression) {
        return Formula.compile(expression).getNeededVariables();
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
    }

    @Test
    public void concatWithParenthesis() {
        assertThat(eval("(3(5))")).isEqualTo(35d);
        assertThat(eval("(3(5)AA)", "A", 1)).isEqualTo(3511d);
        assertThat(eval("3(5)A(A+1)", "A", 1)).isEqualTo(3512d);
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
        assertThatThrownBy(() -> eval("$"))
            .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
        assertThatThrownBy(() -> eval("$1"))
            .isInstanceOf(FormulaException.class).hasMessageContaining(UNEXPECTED_TOKEN.name());
    }

    @Test
    public void strings() {
        assertThat(eval("'abc'")).isEqualTo(0d);
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
        assertThat(eval("random(1)")).isEqualTo(0);
        assertThat(eval("random()")).isBetween(0d, 9d);
        assertThat(eval("random(1;0)")).isEqualTo(0d);
        assertThat(Formula.evaluate("rot13('abc')").getRaw()).isEqualTo("nop");
        assertThat(Formula.evaluate("rot13(rot('aBc'; -13))").getRaw()).isEqualTo("aBc");
        assertThat(Formula.evaluate("rot('abc'; 1)").getRaw()).isEqualTo("bcd");
        assertThat(eval("rot1(a)", "r", 1d, "o", 2d, "t", 3d, "a", 4d)).isEqualTo(12314d);
    }

    @Test
    public void  unaryOperators() {
        assertThat(eval("(1+2)!")).isEqualTo(6);
        assertThat(eval("10--2")).isEqualTo(12);
        assertThat(eval("10- -+ +- -+ +-2")).isEqualTo(8);
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
    public void unclosedParentheses() {
        assertThatThrownBy(() -> eval("3 * (2 + 4 * 2"))
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
        final Formula f = Formula.compile(formulaString, startPos, Formula.StopCharSet.createFor(stopChars));
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
        assertThat(eval("ichecksum(-888)")).isEqualTo(6);
        assertThat(eval("ichecksum(-888.234)")).isEqualTo(6);
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
        assertThat(Value.of(345).getAsInt()).isEqualTo(345);
        assertThat(Value.of("345").getAsInt()).isEqualTo(345);
        assertThat(Value.of(345).isInteger()).isTrue();
        assertThat(Value.of("345").isInteger()).isTrue();
        assertThat(Value.of("345b").isInteger()).isFalse();
        assertThat(Value.of("abcd").isInteger()).isFalse();
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
        assertThat(Formula.compile("A.B").evaluateToString(Formula.toVarProvider("A", 1))).isEqualTo("1._");
        assertThat(Formula.compile("A*5").evaluateToString(Formula.toVarProvider("A", "eddie"))).isEqualTo("eddie * 5");
        assertThat(Formula.compile("(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", 2)))
            .isEqualTo("15");
        assertThat(Formula.compile("(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", "e")))
            .isEqualTo("(e * 4 + 7)");
        assertThat(Formula.compile("123(4*3)B_A(A*4+(3+4))").evaluateToString(Formula.toVarProvider("A", "eddie")))
            .isEqualTo("12312_eddie(eddie * 4 + 7)");
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
            .as("constant parts should be in one subnode").isEqualTo("_ + 6{_{}6{}}");

        assertThat(Formula.compile("12A.456B8901").toDebugString(null, false, true))
            .as("decimals should be stored as efficiently as possible").isEqualTo("12_.456_8901{12{}_{}.456{}_{}8901{}}");


    }

}
