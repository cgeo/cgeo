package cgeo.geocaching.utils;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Iterator;
import java.util.List;

final public class MiscUtils {

    private MiscUtils() {}  // Do not instantiate

    public static <T> Iterable<List<T>> buffer(final List<T> original, final int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("buffer size must be positive");
        }
        return new IteratorIterable<List<T>>(new Iterator<List<T>>() {
            final int size = original.size();
            int next = 0;

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            public List<T> next() {
                final List<T> result = original.subList(next, Math.min(next + n, size));
                next += n;
                return result;
            }

            @Override
            public void remove() {
                throw new NotImplementedException("remove");
            }
        });
    }

}
