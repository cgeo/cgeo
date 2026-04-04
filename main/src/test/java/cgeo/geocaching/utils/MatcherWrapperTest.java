package cgeo.geocaching.utils;

import java.util.regex.Pattern;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class MatcherWrapperTest {

    @Test
    public void testFind() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "abc123def456");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("123");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("456");
        assertThat(matcher.find()).isFalse();
    }

    @Test
    public void testFindWithStart() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "abc123def456ghi789");
        
        // Start at position 9 (just before "456")
        assertThat(matcher.find(9)).isTrue();
        assertThat(matcher.group()).isEqualTo("456");
    }

    @Test
    public void testGroupCount() {
        final Pattern pattern = Pattern.compile("(\\w+)=(\\d+)");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "key=123");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.groupCount()).isEqualTo(2);
    }

    @Test
    public void testGroupWithIndex() {
        final Pattern pattern = Pattern.compile("(\\w+)=(\\d+)");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "key=123");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("key");
        assertThat(matcher.group(2)).isEqualTo("123");
    }

    @Test
    public void testStart() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "abc123def");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.start()).isEqualTo(3);
    }

    @Test
    public void testStartWithGroup() {
        final Pattern pattern = Pattern.compile("(\\w+)=(\\d+)");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "key=123");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.start(1)).isEqualTo(0);
        assertThat(matcher.start(2)).isEqualTo(4);
    }

    @Test
    public void testReplaceAll() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "abc123def456");
        
        final String result = matcher.replaceAll("X");
        assertThat(result).isEqualTo("abcXdefX");
    }

    @Test
    public void testReplaceFirst() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "abc123def456");
        
        final String result = matcher.replaceFirst("X");
        assertThat(result).isEqualTo("abcXdef456");
    }

    @Test
    public void testMatches() {
        final Pattern pattern = Pattern.compile("\\d+");
        final MatcherWrapper matcher1 = new MatcherWrapper(pattern, "123");
        final MatcherWrapper matcher2 = new MatcherWrapper(pattern, "abc123");
        
        assertThat(matcher1.matches()).isTrue();
        assertThat(matcher2.matches()).isFalse();
    }

    @Test
    public void testNullGroup() {
        final Pattern pattern = Pattern.compile("(\\w+)=(\\d*)");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "key=");
        
        assertThat(matcher.find()).isTrue();
        // Group 2 matches empty string, not null
        assertThat(matcher.group(2)).isEqualTo("");
    }

    @Test
    public void testMultipleMatches() {
        final Pattern pattern = Pattern.compile("\\w+");
        final MatcherWrapper matcher = new MatcherWrapper(pattern, "one two three");
        
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("one");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("two");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("three");
        assertThat(matcher.find()).isFalse();
    }
}
