package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class UncertainPropertyTest extends TestCase {

    public static void testHigherCertaintyWins() {
        final UncertainProperty<String> prop1 = new UncertainProperty<>("prop1", 10);
        final UncertainProperty<String> prop2 = new UncertainProperty<>("prop2", 20);
        assertThat(UncertainProperty.getMergedProperty(prop1, prop2)).isEqualTo(prop2);
    }

    public static void testAvoidNull() {
        final UncertainProperty<String> prop1 = new UncertainProperty<>("prop1", 10);
        final UncertainProperty<String> prop2 = new UncertainProperty<>(null, 20);
        assertThat(UncertainProperty.getMergedProperty(prop1, prop2)).isEqualTo(prop1);
        assertThat(UncertainProperty.getMergedProperty(prop2, prop1)).isEqualTo(prop1);
        assertThat(UncertainProperty.getMergedProperty(prop1, null)).isEqualTo(prop1);
        assertThat(UncertainProperty.getMergedProperty(null, prop1)).isEqualTo(prop1);
        assertThat(UncertainProperty.getMergedProperty(prop2, null)).isEqualTo(prop2);
        assertThat(UncertainProperty.getMergedProperty(null, prop2)).isEqualTo(prop2);
        assertThat(UncertainProperty.getMergedProperty(null, null)).isEqualTo(null);
    }

    public static void testEquals() {
        final UncertainProperty<String> prop1 = new UncertainProperty<>("prop1", 10);
        final UncertainProperty<String> prop2 = new UncertainProperty<>(null, 20);
        assertThat(UncertainProperty.equalValues(null, null)).isTrue();
        assertThat(UncertainProperty.equalValues(prop1, null)).isFalse();
        assertThat(UncertainProperty.equalValues(null, prop1)).isFalse();
        assertThat(UncertainProperty.equalValues(prop2, null)).isFalse();
        assertThat(UncertainProperty.equalValues(null, prop2)).isFalse();
        assertThat(UncertainProperty.equalValues(prop1, prop1)).isTrue();
        assertThat(UncertainProperty.equalValues(prop2, prop2)).isTrue();
        assertThat(UncertainProperty.equalValues(prop1, prop2)).isFalse();
        assertThat(UncertainProperty.equalValues(prop2, prop1)).isFalse();
    }
}
