package cgeo.geocaching.utils;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TextParserTest {

    @Test
    public void simple() {
        final TextParser tp = new TextParser("te st");
        assertThat(tp.ch()).isEqualTo('t');
        assertThat(tp.pos()).isEqualTo(0);

        assertThat(tp.eat('a')).isFalse();
        assertThat(tp.eat('t')).isTrue();
        assertThat(tp.eat('e')).isTrue();
        assertThat(tp.eat('e')).isFalse();
        assertThat(tp.eat('s')).isTrue();
        assertThat(tp.ch()).isEqualTo('t');
        assertThat(tp.pos()).isEqualTo(4);
        assertThat(tp.eof()).isFalse();
        tp.next();
        assertThat(tp.chInt()).isEqualTo(0);
        assertThat(tp.pos()).isEqualTo(5);
        assertThat(tp.eof()).isTrue();
        tp.next();
        tp.next();
        assertThat(tp.chInt()).isEqualTo(0);
        assertThat(tp.pos()).isEqualTo(5);
        assertThat(tp.eof()).isTrue();
    }

    @Test
    public void stopChecker() {
        final TextParser tp = new TextParser("test", c -> c == 'e');
        assertThat(tp.ch()).isEqualTo('t');
        assertThat(tp.eof()).isFalse();
        tp.next();
        assertThat(tp.chInt()).isEqualTo(0);
        assertThat(tp.eof()).isTrue();
    }

    @Test
    public void markReset() {
        final TextParser tp = new TextParser("teaser");
        tp.next();
        tp.mark();
        tp.next();
        tp.next();
        assertThat(tp.ch()).isEqualTo('s');
        tp.reset();
        assertThat(tp.ch()).isEqualTo('e');
    }

    @Test
    public void parseChars() {
        final TextParser tp = new TextParser("test");
        final Set<Integer> charSet = new HashSet<>();
        charSet.add((int) 't');
        charSet.add((int) 'e');
        assertThat(tp.parseChars(charSet)).isEqualTo("te");
    }

    @Test
    public void parseUntil() {
        final TextParser tp = new TextParser("this|is||a\\|test\\\\\\}}");
        final KeyableCharSet scs = KeyableCharSet.createFor("|}");
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isEqualTo("this");
        tp.mark();
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isEqualTo("is");
        tp.reset();
        assertThat(tp.parseUntil(scs::contains, false, '\\', true)).isEqualTo("is|a|test\\}");
        assertThat(tp.parseUntil(scs::contains, false, '\\', false)).isNull();
        assertThat(tp.parseUntil(scs::contains, true, '\\', false)).isEqualTo("");
        assertThat(tp.parseUntil(scs::contains, true, '\\', false)).isEqualTo("");
    }

    @Test
    public void parseUntilSpecial() {
        //symbol and escape char are the same
        assertThat(new TextParser("soon it is ''christmas'' again' after ").parseUntil(c -> c == '\'', false, null, true))
                .isEqualTo("soon it is 'christmas' again");
        //other chars are escaped
        assertThat(new TextParser("soon it \\is \\'christmas\\' \\again' after ").parseUntil(c -> c == '\'', false, '\\', false))
                .isEqualTo("soon it is 'christmas' again");
    }

    @Test
    public void splitUntil() {
        final TextParser tp = new TextParser("this|is||a\\|test\\\\\\}-}a");

        assertThat(tp.splitUntil(c -> c == '}', c -> c == '|', true, '\\', true))
                .containsExactly("this", "is|a|test\\}-");
        assertThat(tp.getExpression().charAt(tp.pos() - 1)).isEqualTo('}');
        tp.setPos(0);
        assertThat(tp.splitUntil(c -> c == '}', c -> c == '|', true, '\\', false))
                .containsExactly("this", "is", "", "a|test\\}-");
        assertThat(tp.getExpression().charAt(tp.pos() - 1)).isEqualTo('}');
    }

    @Test
    public void escape() {
        assertThat(TextParser.escape("abc", c -> c == 'a', null)).isEqualTo("aabc");
        assertThat(TextParser.escape("abc", c -> c == 'b', 'd')).isEqualTo("adbc");
    }
}
