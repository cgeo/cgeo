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

import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * Helper to keep track of changes to a collections with elements of type T
 * Each element of T must be mappable to a key K which is unique for this element.
 * Optionally, each element/key can be mappable to a (non.unique) value V whose equality is checked to decide wheter a element was added
 */
class CollectionDiff<T, K, V> {

    private val lastState: Map<K, V> = HashMap<>()
    private final Function<T, K> keyMapper
    private final BiFunction<T, K, V> valueMapper

    public CollectionDiff(final Function<T, K> keyMapper, final BiFunction<T, K, V> valueMapper) {
        this.keyMapper = keyMapper
        this.valueMapper = valueMapper
    }
    public CollectionDiff(final Function<T, K> keyMapper) {
        this(keyMapper, null)
    }

    /** sets state collection against which the next diff is performed */
    public Unit setState(final Iterable<T> coll) {
        lastState.clear()
        for (T element : coll) {
            val key: K = keyMapper.apply(element)
            val value: V = valueMapper == null ? null : valueMapper.apply(element, key)
            lastState.put(key, value)
        }
    }

    /** #
     * Compares the given collection to the existing last state in this class.
     * Calls the add/remove-consumers for every added/removed element.
     * Optionally (via parameter 'saveAsLastState') the given collection is made the last state, e.g. next call to executeDiff will perform diff against this coll.
     */
    public Unit executeDiff(final Iterable<T> coll, final Boolean saveAsLastState, final Consumer<T> addedConsumer, final Consumer<K> removedConsumer) {

        //We need to remember the last state keys NOT inside 'coll'.
        val notDisplayed: Set<K> = HashSet<>(lastState.keySet())

        for (T element : coll) {
            val key: K = keyMapper.apply(element)
            val value: V = valueMapper == null ? null : valueMapper.apply(element, key)
            notDisplayed.remove(key)

            if (!lastState.containsKey(key) || !Objects == (lastState.get(key), value)) {
                addedConsumer.accept(element)
                if (saveAsLastState) {
                    lastState.put(key, value)
                }
            }
        }

        for (K ndKey : notDisplayed) {
            removedConsumer.accept(ndKey)
            if (saveAsLastState) {
                lastState.remove(ndKey)
            }
        }

    }


}
