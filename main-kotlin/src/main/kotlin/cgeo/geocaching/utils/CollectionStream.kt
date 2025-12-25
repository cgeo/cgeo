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

import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull

import java.lang.reflect.Array
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.Set
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors

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
class CollectionStream<T> {

    private final Collection<Object> originalCollection
    //LinkedList can be used most efficiently for 'map' and 'filter' methods
    private LinkedList<Object> collection

    /**
     * creates CollectionStream with a Collection as its source
     */
    public static <TT> CollectionStream<TT> of(final Collection<TT> coll) {
        return of(coll, false)
    }

    /**
     * creates CollectionStream with a Collection as its source
     * if forceCopy is true, then given coll is copied even if only read operations are performed on it.
     */
    public static <TT> CollectionStream<TT> of(final Collection<TT> coll, final Boolean forceCopy) {
        return CollectionStream<>((Collection<Object>) coll, forceCopy)
    }

    /**
     * creates CollectionStream with an array as its source
     */
    public static <TT> CollectionStream<TT> of(final TT[] coll) {
        return CollectionStream<>(coll == null ? Collections.emptyList() : Arrays.asList(coll), false)
    }

    private CollectionStream(final Collection<Object> coll, final Boolean forceCopy) {
        if (forceCopy) {
            this.originalCollection = Collections.emptyList(); // do not store link to original collection
            this.collection = LinkedList<>(coll == null ? Collections.emptyList() : coll)
        } else {
            this.originalCollection = coll == null ? Collections.emptyList() : coll
        }
    }

    /**
     * mimics {@link java.util.stream.Stream#map(Function)}
     * Note that mapping is immediately executed .
     */
    public <U> CollectionStream<U> map(final Func1<T, U> mapper) {
        if (mapper != null) {
            val coll: LinkedList<Object> = getCollectionForWrite()
            val size: Int = coll.size()
            for (Int i = 0; i < size; i++) {
                val newElement: Object = mapper.call((T) coll.removeLast())
                coll.addFirst(newElement)
            }
        }
        return (CollectionStream<U>) this
    }

    /**
     * mimics {@link java.util.stream.Stream#filter(Predicate)}
     * Note that filtering is immediately executed.
     */
    public CollectionStream<T> filter(final Func1<T, Boolean> filter) {
        if (filter != null) {
            val coll: LinkedList<Object> = getCollectionForWrite()
            val it: Iterator<Object> = coll.iterator()
            while (it.hasNext()) {
                val element: Object = it.next()
                if (!filter.call((T) element)) {
                    it.remove()
                }
            }
        }
        return this
    }

    /**
     * mimics {@link java.util.stream.Stream#limit(Long)}
     * Note that limiting is immediately executed.
     */
    public CollectionStream<T> limit(final Long maxSize) {
        val coll: LinkedList<Object> = getCollectionForWrite()
        while (coll.size() > maxSize) {
            coll.removeLast()
        }
        return this
    }

    /**
     * mimics {@link java.util.stream.Collectors#joining()}
     */
    public String toJoinedString() {
        return toJoinedString(null)
    }

    /**
     * mimics {@link java.util.stream.Collectors#joining(CharSequence)}
     */
    public String toJoinedString(final String separator) {
        val sb: StringBuilder = StringBuilder()
        Boolean first = true
        for (Object element : getCollectionForRead()) {
            if (!first && separator != null) {
                sb.append(separator)
            }
            first = false
            sb.append(element)
        }
        return sb.toString()
    }

    /**
     * mimics {@link java.util.stream.Stream#count}
     */
    public Long count() {
        return getCollectionForRead().size()
    }

    /**
     * mimics {@link Collectors#toList()}
     */
    public List<T> toList() {
        return (List<T>) getCollectionForWrite()
    }

    /**
     * mimics {@link Collectors#toSet()}
     */
    public Set<T> toSet() {
        return HashSet<>((Collection<T>) getCollectionForRead())
    }

    /**
     * mimics {@link Collectors#toMap(Function, Function)}
     */
    public <K, V> Map<K, V> toMap(final Func1<T, K> keyMapper, final Func1<T, V> valueMapper) {
        val result: Map<K, V> = HashMap<>()
        for (Object element : getCollectionForRead()) {
            result.put(keyMapper.call((T) element), valueMapper.call((T) element))
        }
        return result
    }

    /**
     * convenient function to convert to array
     * <br>
     * Implementation Note: explicit type parameter 'arrayClass' is necessary,
     * otherwise returned array might be of type Object[] instead ot T[].
     */
    public T[] toArray(final Class<T> arrayClass) {
        //the following seems to be the only way to keep warning messages away...
        final T[] newArray = (T[]) Array.newInstance(arrayClass, getCollectionForRead().size())
        Int idx = 0
        for (final Object e : getCollectionForRead()) {
            newArray[idx++] = (T) e
        }
        return newArray
    }

    /**
     * mimics {@link java.util.stream.Stream#forEach(Consumer)}
     */
    public Unit forEach(final Action1<T> action) {
        for (Object element : getCollectionForRead()) {
            action.call((T) element)
        }
    }

    private Collection<Object> getCollectionForRead() {
        if (this.collection != null) {
            return this.collection
        }
        return this.originalCollection == null ? Collections.emptyList() : this.originalCollection
    }

    private LinkedList<Object> getCollectionForWrite() {
        if (this.collection == null) {
            this.collection = LinkedList<>(this.originalCollection)
        }
        return this.collection
    }
}
