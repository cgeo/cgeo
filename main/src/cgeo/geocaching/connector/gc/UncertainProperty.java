package cgeo.geocaching.connector.gc;


/**
 * Property with certainty. When merging properties, the one with higher certainty wins.
 * 
 * @param <T>
 */
public class UncertainProperty<T> {

    private final T value;
    private final int certaintyLevel;

    public UncertainProperty(T value) {
        this(value, Tile.ZOOMLEVEL_MAX + 1);
    }

    public UncertainProperty(T value, int certaintyLevel) {
        this.value = value;
        this.certaintyLevel = certaintyLevel;
    }

    public T getValue() {
        return value;
    }

    public int getCertaintyLevel() {
        return certaintyLevel;
    }

    public UncertainProperty<T> getMergedProperty(final UncertainProperty<T> other) {
        if (null == other || null == other.value) {
            return this;
        }
        if (null == this.value) {
            return other;
        }
        if (other.certaintyLevel > certaintyLevel) {
            return other;
        }

        return this;
    }

    public static <T> UncertainProperty<T> getMergedProperty(UncertainProperty<T> property, UncertainProperty<T> otherProperty) {
        if (null == property) {
            return otherProperty;
        }
        return property.getMergedProperty(otherProperty);
    }

}
