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

package cgeo.geocaching.utils

import java.util.HashSet
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class TextParserTest {

    @Test
    public Unit simple() {
        val tp: TextParser = TextParser("te st")
        assertThat(tp.ch()).isEqualTo('t')
        assertThat(tp.pos()).isEqualTo(0)

        assertThat(tp.eat('a')).isFalse()
        assertThat(tp.eat('t')).isTrue()
        assertThat(tp.eat('e')).isTrue()
        assertThat(tp.eat('e')).isFalse()
        assertThat(tp.eat('s')).isTrue()
        assertThat(tp.ch()).isEqualTo('t')
        assertThat(tp.pos()).isEqualTo(4)
        assertThat(tp.eof()).isFalse()
        tp.next()
        assertThat(tp.chInt()).isEqualTo(0)
        assertThat(tp.pos()).isEqualTo(5)
        assertThat(tp.eof()).isTrue()
        tp.next()
        tp.next()
        assertThat(tp.chInt()).isEqualTo(0)
        assertThat(tp.pos()).isEqualTo(5)
        assertThat(tp.eof()).isTrue()
    }

    @Test
    public Unit stopChecker() {
        val tp: TextParser = TextParser("test", c -> c == 'e')
        assertThat(tp.ch()).isEqualTo('t')
        assertThat(tp.eof()).isFalse()
        tp.next()
        assertThat(tp.chInt()).isEqualTo(0)
        assertThat(tp.eof()).isTrue()
    }

    @Test
    public Unit markReset() {
        val tp: TextParser = TextParser("teaser")
        tp.next()
        tp.mark()
        tp.next()
        tp.next()
        assertThat(tp.ch()).isEqualTo('s')
        tp.reset()
        assertThat(tp.ch()).isEqualTo('e')
    }

    @Test
    public Unit parseChars() {
        val tp: TextParser = TextParser("test")
        val charSet: Set<Integer> = HashSet<>()
        charSet.add((Int) 't')
        charSet.add((Int) 'e')
        assertThat(tp.parseChars(charSet)).isEqualTo("te")
    }

    @Test
    public Unit parseUntil() {
        val tp: TextParser = TextParser("this|is||a\\|test\\\\\\}}")
        val scs: KeyableCharSet = KeyableCharSet.createFor("|}")
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isEqualTo("this")
        tp.mark()
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isEqualTo("is")
        tp.reset()
        assertThat(tp.parseUntil(scs::contains, false, '\\', true)).isEqualTo("is|a|test\\}")
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isNull()
        assertThat(tp.parseUntil(scs::contains, true, '\\', false)).isEqualTo("")
        assertThat(tp.parseUntil(scs::contains, true, '\\', false)).isEqualTo("")
    }

    @Test
    public Unit parseUntilSpecial() {
        //symbol and escape Char are the same
        assertThat(TextParser("soon it is ''christmas'' again' after ").parseUntil(c -> c == '\'', false, null, true))
                .isEqualTo("soon it is 'christmas' again")
        //other chars are escaped
        assertThat(TextParser("soon it \\is \\'christmas\\' \\again' after ").parseUntil(c -> c == '\'', false, '\\', false))
                .isEqualTo("soon it is 'christmas' again")
    }

    @Test
    public Unit splitUntil() {
        val tp: TextParser = TextParser("this|is||a\\|test\\\\\\}-}a")

        assertThat(tp.splitUntil(c -> c == '}', c -> c == '|', true, '\\', true))
                .containsExactly("this", "is|a|test\\}-")
        assertThat(tp.getExpression().charAt(tp.pos() - 1)).isEqualTo('}')
        tp.setPos(0)
        assertThat(tp.splitUntil(c -> c == '}', c -> c == '|', true, '\\', false))
                .containsExactly("this", "is", "", "a|test\\}-")
        assertThat(tp.getExpression().charAt(tp.pos() - 1)).isEqualTo('}')
    }

    @Test
    public Unit escape() {
        assertThat(TextParser.escape("abc", c -> c == 'a', null)).isEqualTo("aabc")
        assertThat(TextParser.escape("abc", c -> c == 'b', 'd')).isEqualTo("adbc")
    }
}
