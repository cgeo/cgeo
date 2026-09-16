package cgeo.geocaching.utils;

/**
 * Allocation-free polynomial hash builder (seed 17, multiplier 31).
 *
 * Usage:
 *   final int h = HashCode.start()
 *       .append(someInt)
 *       .append(someBoolean)
 *       .append(someObject)   // null-safe
 *       .toHashCode();
 *
 * The builder is ThreadLocal, so no object is allocated per call. reset() is
 * called automatically by start(). Do not cache the Builder reference across
 * threads or between start() and toHashCode() calls on the same thread.
 *
 * WARNING — reentrancy: append(Object) calls v.hashCode() on the appended
 * value. If that hashCode() itself calls HashCode.start(), the ThreadLocal
 * builder is silently reset mid-computation, corrupting the outer hash.
 * Only pass objects whose hashCode() is known NOT to use HashCode (e.g.
 * String, enums, standard JDK collections of strings/primitives). For all
 * other types, extract the hash first: append(myObj.hashCode()).
 */
public final class HashCode {

    private static final ThreadLocal<Builder> INSTANCE = ThreadLocal.withInitial(Builder::new);

    private HashCode() {}

    public static Builder start() {
        return INSTANCE.get().reset();
    }

    public static final class Builder {

        private int value;

        Builder() {}

        Builder reset() {
            value = 17;
            return this;
        }

        public Builder append(final boolean v) {
            value = 31 * value + (v ? 1 : 0);
            return this;
        }

        public Builder append(final int v) {
            value = 31 * value + v;
            return this;
        }

        public Builder append(final float v) {
            value = 31 * value + Float.floatToIntBits(v);
            return this;
        }

        // Safe only for types whose hashCode() does not call HashCode.start() — see class javadoc.
        public Builder append(final Object v) {
            value = 31 * value + (v == null ? 0 : v.hashCode());
            return this;
        }

        public int toHashCode() {
            return value;
        }
    }
}
