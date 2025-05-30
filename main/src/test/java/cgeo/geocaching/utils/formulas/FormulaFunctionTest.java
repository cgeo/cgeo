package cgeo.geocaching.utils.formulas;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.within;

public class FormulaFunctionTest {

    @Test
    public void testFindByName() {
        assertThat(FormulaFunction.findByName("sqrt")).isEqualTo(FormulaFunction.SQRT);
        assertThat(FormulaFunction.findByName("sin")).isEqualTo(FormulaFunction.SIN);
        assertThat(FormulaFunction.findByName("cos")).isEqualTo(FormulaFunction.COS);
        assertThat(FormulaFunction.findByName("tan")).isEqualTo(FormulaFunction.TAN);
        assertThat(FormulaFunction.findByName("abs")).isEqualTo(FormulaFunction.ABS);
        assertThat(FormulaFunction.findByName("rd")).isEqualTo(FormulaFunction.ROUND);
        assertThat(FormulaFunction.findByName("doesnotexist")).isNull();
    }

    @Test
    public void testExecuteSqrt() throws FormulaException {
        final FormulaFunction sqrt = FormulaFunction.SQRT;
        assertThat(sqrt.execute(new ValueList().add(Value.of(9.0)))).isEqualTo(Value.of(3.0));
        assertThat(sqrt.execute(new ValueList().add(Value.of(2.0))).getAsDouble()).isCloseTo(1.41421, within(0.00001));
        assertThat(sqrt.execute(new ValueList().add(Value.of(0.0)))).isEqualTo(Value.of(0));
    }

    @Test
    public void testExecuteSin() throws FormulaException {
        final FormulaFunction sin = FormulaFunction.SIN;
        assertThat(sin.execute(new ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(0.0, within(0.00001));
        assertThat(sin.execute(new ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(0.70711, within(0.00001));
        assertThat(sin.execute(new ValueList().add(Value.of(90))).getAsDouble()).isCloseTo(1.0, within(0.00001));
        assertThat(sin.execute(new ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(0.0, within(0.00001));
        assertThat(sin.execute(new ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(0.0, within(0.00001));
    }

    @Test
    public void testExecuteCos() throws FormulaException {
        final FormulaFunction cos = FormulaFunction.COS;
        assertThat(cos.execute(new ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(1.0, within(0.00001));
        assertThat(cos.execute(new ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(0.70711, within(0.00001));
        assertThat(cos.execute(new ValueList().add(Value.of(90))).getAsDouble()).isCloseTo(0.0, within(0.00001));
        assertThat(cos.execute(new ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(-1.0, within(0.00001));
        assertThat(cos.execute(new ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(1.0, within(0.00001));
    }

    @Test
    public void testExecuteTan() throws FormulaException {
        final FormulaFunction tan = FormulaFunction.TAN;
        assertThat(tan.execute(new ValueList().add(Value.of(0))).getAsDouble()).isCloseTo(0.0, within(0.00001));
        assertThat(tan.execute(new ValueList().add(Value.of(45))).getAsDouble()).isCloseTo(1.0, within(0.00001));
        assertThat(tan.execute(new ValueList().add(Value.of(180))).getAsDouble()).isCloseTo(0.0, within(0.00001));
        assertThat(tan.execute(new ValueList().add(Value.of(360))).getAsDouble()).isCloseTo(0.0, within(0.00001));
    }

    @Test
    public void testExecuteAbs() throws FormulaException {
        final FormulaFunction abs = FormulaFunction.ABS;
        assertThat(abs.execute(new ValueList().add(Value.of(7.0))).getAsDouble()).isEqualTo(7.0);
        assertThat(abs.execute(new ValueList().add(Value.of(0.0))).getAsDouble()).isEqualTo(0.0);
        assertThat(abs.execute(new ValueList().add(Value.of(-0.0))).getAsDouble()).isEqualTo(0.0);
        assertThat(abs.execute(new ValueList().add(Value.of(-3.14))).getAsDouble()).isEqualTo(3.14);
    }

    @Test
    public void testExecuteRound() throws FormulaException {
        final FormulaFunction round = FormulaFunction.ROUND;
        assertThat(round.execute(new ValueList().add(Value.of(4.4))).getAsDouble()).isEqualTo(4.0);
        assertThat(round.execute(new ValueList().add(Value.of(4.5))).getAsDouble()).isEqualTo(5.0);
        assertThat(round.execute(new ValueList().add(Value.of(-4.4))).getAsDouble()).isEqualTo(-4.0);
        assertThat(round.execute(new ValueList().add(Value.of(-4.5))).getAsDouble()).isEqualTo(-5.0);
    }

    @Test
    public void testExecuteTrunc() throws FormulaException {
        final FormulaFunction trunc = FormulaFunction.TRUNC;
        assertThat(trunc.execute(new ValueList().add(Value.of(3.14159))).getAsDouble()).isEqualTo(3.0);
        assertThat(trunc.execute(new ValueList().add(Value.of(-3.14159))).getAsDouble()).isEqualTo(-3.0);
        assertThat(trunc.execute(new ValueList().add(Value.of(3.0))).getAsDouble()).isEqualTo(3.0);
        assertThat(trunc.execute(new ValueList().add(Value.of(-3.0))).getAsDouble()).isEqualTo(-3.0);
        assertThat(trunc.execute(new ValueList().add(Value.of(0.0))).getAsDouble()).isEqualTo(0.0);
    }

    @Test
    public void testExecuteLength() throws FormulaException {
        final FormulaFunction length = FormulaFunction.LENGTH;
        assertThat(length.execute(new ValueList().add(Value.of("Hello, World!"))).getAsInteger()).isEqualTo(13);
        assertThat(length.execute(new ValueList().add(Value.of(""))).getAsInteger()).isEqualTo(0);
        assertThat(length.execute(new ValueList().add(Value.of("   "))).getAsInteger()).isEqualTo(3);
        assertThat(length.execute(new ValueList().add(Value.of("a\nb\tc!"))).getAsInteger()).isEqualTo(6);
    }

}
