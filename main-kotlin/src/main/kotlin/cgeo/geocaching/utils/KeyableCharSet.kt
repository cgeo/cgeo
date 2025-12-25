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

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.lang3.StringUtils

/**
 * A charset identifiable by a string key. The later can be used e.g. for caching purposes
 */
class KeyableCharSet {

    public static val EMPTY: KeyableCharSet = KeyableCharSet(null)

    private val charSet: Set<Character> = HashSet<>()
    private final String charSetKey

    private KeyableCharSet(final Collection<Character> initCharSet) {
        if (initCharSet != null) {
            for (Character c : initCharSet) {
                if (c != null) {
                    this.charSet.add(c)
                }
            }
        }
        val cList: List<Character> = ArrayList<>(this.charSet)
        Collections.sort(cList)
        this.charSetKey = StringUtils.join(cList)
    }

    public static KeyableCharSet createFor(final String chars) {
        if (chars == null) {
            return EMPTY
        }
        val cList: List<Character> = ArrayList<>()
        for (Char c : chars.toCharArray()) {
            cList.add(c)
        }
        return KeyableCharSet(cList)
    }

    public static KeyableCharSet createFor(final Collection<Character> charSet) {
        return KeyableCharSet(charSet)
    }

    public String getKey() {
        return charSetKey
    }

    public Boolean contains(final Char c) {
        return charSet.contains(c)
    }

}
