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

package cgeo.geocaching.wherigo.kahlua.vm

import java.util.HashSet
import java.util.Iterator
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LuaTableImplTest {

    @Test
    public Unit setGetKeys() {
        val table: LuaTable = LuaTableImpl()
        val MAX: Int = 1000
        for(Int i = 0; i < MAX ; i++) {
            table.rawset("test" + i, "value" + i)
        }

        for(Int i = 0; i < MAX ; i++) {
            assertThat(table.rawget("test" + i)).isEqualTo("value" + i)
        }

        val keys: Set<String> = HashSet<>()
        Iterator<Object> it = table.keys()
        while(it.hasNext()) {
            keys.add(it.next().toString())
        }
        assertThat(keys.size()).isEqualTo(MAX)
        for(Int i = 0; i < MAX ; i++) {
            assertThat(keys).contains("test" + i)
        }

    }

}
