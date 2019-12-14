package cgeo.geocaching.connector.gc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Property with certainty. When merging properties, the one with higher certainty wins.
 *
 */
public class UncertainProperty<T> {

    private final T value;
    private final int certaintyLevel;

    public UncertainProperty(final T value) {
        this(value, Tile.ZOOMLEVEL_MAX + 1);
    }

    public UncertainProperty(final T value, final int certaintyLevel) {
        this.value = value;
        this.certaintyLevel = certaintyLevel;
    }

    public T getValue() {
        return value;
    }

    public int getCertaintyLevel() {
        return certaintyLevel;
    }

    @NonNull
    private UncertainProperty<T> getMergedProperty(@Nullable final UncertainProperty<T> other) {
        if (other == null || other.value == null) {
            return this;
        }
        if (this.value == null || other.certaintyLevel > certaintyLevel) {
            return other;
        }

        return this;
    }

    @Nullable
    public static <T> UncertainProperty<T> getMergedProperty(@Nullable final UncertainProperty<T> property, @Nullable final UncertainProperty<T> otherProperty) {
        return property == null ? otherProperty : property.getMergedProperty(otherProperty);
    }

    public static <T> boolean equalValues(@Nullable final UncertainProperty<T> property, @Nullable final UncertainProperty<T> otherProperty) {
        if (property == null || otherProperty == null) {
            return property == null && otherProperty == null;
        }
        if (property.value == null || otherProperty.value == null) {
            return property.value == null && otherProperty.value == null;
        }
        return property.value.equals(otherProperty.value);
    }

}
