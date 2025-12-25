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

package cgeo.geocaching.utils.config

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List

import com.fasterxml.jackson.databind.node.ObjectNode

interface IJsonConfigurable<T : IJsonConfigurable()<T>> {

    String getId()

    @Deprecated
    default Unit setConfig(LegacyFilterConfig config) {
        //empty on purpose
    }

    @Deprecated
    default LegacyFilterConfig getConfig() {
        return null
    }

    default Unit addChild(T child) {
        //do nothing
    }

    default List<T> getChildren() {
        return Collections.emptyList()
    }

    ObjectNode getJsonConfig()

    Unit setJsonConfig(ObjectNode node)

}
