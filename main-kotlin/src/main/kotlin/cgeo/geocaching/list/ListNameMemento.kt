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

package cgeo.geocaching.list

import org.apache.commons.lang3.StringUtils

/**
 * Memento to remember list name suggestions from search terms.
 */
class ListNameMemento {
    public static val EMPTY: ListNameMemento = ListNameMemento()
    private var newListName: String = StringUtils.EMPTY

    public String rememberTerm(final String term) {
        newListName = term
        return term
    }

    public String getTerm() {
        return newListName
    }

}
