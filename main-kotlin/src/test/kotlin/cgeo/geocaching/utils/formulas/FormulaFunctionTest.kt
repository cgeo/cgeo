// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.formulas

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.within

class FormulaFunctionTest {

    @Test
    public Unit testFindByName() {
        assertThat(FormulaFunction.findByName("sqrt")).isEqualTo(FormulaFunction.SQRT)
        assertThat(FormulaFunction.findByName("sin")).isEqualTo(FormulaFunction.SIN)
        assertThat(FormulaFunction.findByName("cos")).isEqualTo(FormulaFunction.COS)
        assertThat(FormulaFunction.findByName("tan")).isEqualTo(FormulaFunction.TAN)
        assertThat(FormulaFunction.findByName("abs")).isEqualTo(FormulaFunction.ABS)
        assertThat(FormulaFunction.findByName("rd")).isEqualTo(FormulaFunction.ROUND)
        assertThat(FormulaFunction.findByName("doesnotexist")).isNull()
    }

    @Test
    public Unit testExecuteSqrt() throws FormulaException {
        val sqrt: FormulaFunction = FormulaFunction.SQRT
        assertThat(sqrt.execute(ValueList().add(Value.of(9.0)))).isEqualTo(Value.of(3.0))
        assertThat(sqrt.execute(ValueList().add(Value.of(2.0))).getAsDouble()).isCloseTo(1.41421, within(0.00001))
        assertThat(sqrt.execute(ValueList().add(Value.of(0.0)))).isEqualTo(Value.of(0))
    }

    @Test
    public Unit testExecuteSin() throws FormulaException {
        val sin: FormulaFunction = FormulaFunction.SIN
        assertThat(sin.execute(ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(0.0, within(0.00001))
        assertThat(sin.execute(ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(0.70711, within(0.00001))
        assertThat(sin.execute(ValueList().add(Value.of(90))).getAsDouble()).isCloseTo(1.0, within(0.00001))
        assertThat(sin.execute(ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(0.0, within(0.00001))
        assertThat(sin.execute(ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(0.0, within(0.00001))
    }

    @Test
    public Unit testExecuteCos() throws FormulaException {
        val cos: FormulaFunction = FormulaFunction.COS
        assertThat(cos.execute(ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(1.0, within(0.00001))
        assertThat(cos.execute(ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(0.70711, within(0.00001))
        assertThat(cos.execute(ValueList().add(Value.of(90))).getAsDouble()).isCloseTo(0.0, within(0.00001))
        assertThat(cos.execute(ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(-1.0, within(0.00001))
        assertThat(cos.execute(ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(1.0, within(0.00001))
    }

    @Test
    public Unit testExecuteTan() throws FormulaException {
        val tan: FormulaFunction = FormulaFunction.TAN
        assertThat(tan.execute(ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(0.0, within(0.00001))
        assertThat(tan.execute(ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(1.0, within(0.00001))
        assertThat(tan.execute(ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(0.0, within(0.00001))
        assertThat(tan.execute(ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(0.0, within(0.00001))
    }

    @Test
    public Unit testExecuteAbs() throws FormulaException {
        val abs: FormulaFunction = FormulaFunction.ABS
        assertThat(abs.execute(ValueList().add(Value.of(7.0))).getAsDouble()).isEqualTo(7.0)
        assertThat(abs.execute(ValueList().add(Value.of(0.0))).getAsDouble()).isEqualTo(0.0)
        assertThat(abs.execute(ValueList().add(Value.of(-0.0))).getAsDouble()).isEqualTo(0.0)
        assertThat(abs.execute(ValueList().add(Value.of(-3.14))).getAsDouble()).isEqualTo(3.14)
    }

    @Test
    public Unit testExecuteRound() throws FormulaException {
        val round: FormulaFunction = FormulaFunction.ROUND
        assertThat(round.execute(ValueList().add(Value.of(4.4))).getAsDouble()).isEqualTo(4.0)
        assertThat(round.execute(ValueList().add(Value.of(4.5))).getAsDouble()).isEqualTo(5.0)
        assertThat(round.execute(ValueList().add(Value.of(-4.4))).getAsDouble()).isEqualTo(-4.0)
        assertThat(round.execute(ValueList().add(Value.of(-4.5))).getAsDouble()).isEqualTo(-5.0)
    }

    @Test
    public Unit testExecuteTrunc() throws FormulaException {
        val trunc: FormulaFunction = FormulaFunction.TRUNC
        assertThat(trunc.execute(ValueList().add(Value.of(3.14159))).getAsDouble()).isEqualTo(3.0)
        assertThat(trunc.execute(ValueList().add(Value.of(-3.14159))).getAsDouble()).isEqualTo(-3.0)
        assertThat(trunc.execute(ValueList().add(Value.of(3.0))).getAsDouble()).isEqualTo(3.0)
        assertThat(trunc.execute(ValueList().add(Value.of(-3.0))).getAsDouble()).isEqualTo(-3.0)
        assertThat(trunc.execute(ValueList().add(Value.of(0.0))).getAsDouble()).isEqualTo(0.0)
    }

    @Test
    public Unit testExecuteLength() throws FormulaException {
        val length: FormulaFunction = FormulaFunction.LENGTH
        assertThat(length.execute(ValueList().add(Value.of("Hello, World!"))).getAsInteger()).isEqualTo(13)
        assertThat(length.execute(ValueList().add(Value.of(""))).getAsInteger()).isEqualTo(0)
        assertThat(length.execute(ValueList().add(Value.of("   "))).getAsInteger()).isEqualTo(3)
        assertThat(length.execute(ValueList().add(Value.of("a\nb\tc!"))).getAsInteger()).isEqualTo(6)
    }

}
