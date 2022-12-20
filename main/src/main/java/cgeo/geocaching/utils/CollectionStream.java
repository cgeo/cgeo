package cgeo.geocaching.utils;

import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides Streaming-like functionality for Collection classes
 * <p>
 * Poor-mans replacement for {@link Collection#stream()} functionality for Collections which is only
 * available starting at API Level 24. This class can be completely replaced once c:geo switches to
 * that API level.
 * <p>
 * This class is meant for immediate processing and collecting ONLY!
 * Methods called on this class manipulate its internal state directly. This prevents creation of unnecessary intermediate collection objects.
 * However, IT IS NOT SAFE TO CACHE INSTANCES OF THIS CLASS FOR LATER USAGE!
 * <p>
 * This class will be redundant and shall be deprecated once we use API level >= 24
 */
@SuppressWarnings("unchecked")
public class CollectionStream<T> {

    private final Collection<Object> originalCollection;
    //LinkedList can be used most efficiently for 'map' and 'filter' methods
    private LinkedList<Object> collection;

    /**
     * creates CollectionStream with a Collection as its source
     */
    public static <TT> CollectionStream<TT> of(final Collection<TT> coll) {
        return of(coll, false);
    }

    /**
     * creates CollectionStream with a Collection as its source
     * if forceCopy is true, then given coll is copied even if only read operations are performed on it.
     */
    public static <TT> CollectionStream<TT> of(final Collection<TT> coll, final boolean forceCopy) {
        return new CollectionStream<>((Collection<Object>) coll, forceCopy);
    }

    /**
     * creates CollectionStream with an array as its source
     */
    public static <TT> CollectionStream<TT> of(final TT[] coll) {
        return new CollectionStream<>(coll == null ? Collections.emptyList() : Arrays.asList(coll), false);
    }

    private CollectionStream(final Collection<Object> coll, final boolean forceCopy) {
        if (forceCopy) {
            this.originalCollection = Collections.emptyList(); // do not store link to original collection
            this.collection = new LinkedList<>(coll == null ? Collections.emptyList() : coll);
        } else {
            this.originalCollection = coll == null ? Collections.emptyList() : coll;
        }
    }

    /**
     * mimics {@link java.util.stream.Stream#map(Function)}
     * Note that mapping is immediately executed .
     */
    public <U> CollectionStream<U> map(final Func1<T, U> mapper) {
        if (mapper != null) {
            final LinkedList<Object> coll = getCollectionForWrite();
            final int size = coll.size();
            for (int i = 0; i < size; i++) {
                final Object newElement = mapper.call((T) coll.removeLast());
                coll.addFirst(newElement);
            }
        }
        return (CollectionStream<U>) this;
    }

    /**
     * mimics {@link java.util.stream.Stream#filter(Predicate)}
     * Note that filtering is immediately executed.
     */
    public CollectionStream<T> filter(final Func1<T, Boolean> filter) {
        if (filter != null) {
            final LinkedList<Object> coll = getCollectionForWrite();
            final Iterator<Object> it = coll.iterator();
            while (it.hasNext()) {
                final Object element = it.next();
                if (!filter.call((T) element)) {
                    it.remove();
                }
            }
        }
        return this;
    }

    /**
     * mimics {@link java.util.stream.Stream#limit(long)}
     * Note that limiting is immediately executed.
     */
    public CollectionStream<T> limit(final long maxSize) {
        final LinkedList<Object> coll = getCollectionForWrite();
        while (coll.size() > maxSize) {
            coll.removeLast();
        }
        return this;
    }

    /**
     * mimics {@link java.util.stream.Collectors#joining()}
     */
    public String toJoinedString() {
        return toJoinedString(null);
    }

    /**
     * mimics {@link java.util.stream.Collectors#joining(CharSequence)}
     */
    public String toJoinedString(final String separator) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object element : getCollectionForRead()) {
            if (!first && separator != null) {
                sb.append(separator);
            }
            first = false;
            sb.append(element);
        }
        return sb.toString();
    }

    /**
     * mimics {@link java.util.stream.Stream#count}
     */
    public long count() {
        return getCollectionForRead().size();
    }

    /**
     * mimics {@link Collectors#toList()}
     */
    public List<T> toList() {
        return (List<T>) getCollectionForWrite();
    }

    /**
     * mimics {@link Collectors#toSet()}
     */
    public Set<T> toSet() {
        return new HashSet<>((Collection<T>) getCollectionForRead());
    }

    /**
     * mimics {@link Collectors#toMap(Function, Function)}
     */
    public <K, V> Map<K, V> toMap(final Func1<T, K> keyMapper, final Func1<T, V> valueMapper) {
        final Map<K, V> result = new HashMap<>();
        for (Object element : getCollectionForRead()) {
            result.put(keyMapper.call((T) element), valueMapper.call((T) element));
        }
        return result;
    }

    /**
     * convenient function to convert to array
     *
     * Implementation Note: explicit type parameter 'arrayClass' is necessary,
     * otherwise returned array might be of type Object[] instead ot T[].
     */
    public T[] toArray(final Class<T> arrayClass) {
        //the following seems to be the only way to keep warning messages away...
        final T[] newArray = (T[]) Array.newInstance(arrayClass, getCollectionForRead().size());
        int idx = 0;
        for (final Object e : getCollectionForRead()) {
            newArray[idx++] = (T) e;
        }
        return newArray;
    }

    /**
     * mimics {@link java.util.stream.Stream#forEach(Consumer)}
     */
    public void forEach(final Action1<T> action) {
        for (Object element : getCollectionForRead()) {
            action.call((T) element);
        }
    }

    @NonNull
    private Collection<Object> getCollectionForRead() {
        if (this.collection != null) {
            return this.collection;
        }
        return this.originalCollection == null ? Collections.emptyList() : this.originalCollection;
    }

    @NonNull
    private LinkedList<Object> getCollectionForWrite() {
        if (this.collection == null) {
            this.collection = new LinkedList<>(this.originalCollection);
        }
        return this.collection;
    }
}
