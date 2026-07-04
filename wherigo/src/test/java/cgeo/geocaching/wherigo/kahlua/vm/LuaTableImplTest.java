package cgeo.geocaching.wherigo.kahlua.vm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LuaTableImplTest {

    @Test
    public void setGetKeys() {
        final LuaTable table = new LuaTableImpl();
        final int MAX = 1000;
        for(int i = 0; i < MAX ; i++) {
            table.rawset("test" + i, "value" + i);
        }

        for(int i = 0; i < MAX ; i++) {
            assertThat(table.rawget("test" + i)).isEqualTo("value" + i);
        }

        final Set<String> keys = new HashSet<>();
        Iterator<Object> it = table.keys();
        while(it.hasNext()) {
            keys.add(it.next().toString());
        }
        assertThat(keys.size()).isEqualTo(MAX);
        for(int i = 0; i < MAX ; i++) {
            assertThat(keys).contains("test" + i);
        }

    }

    @Test
    public void numericKeysAreCanonicalizedRegardlessOfBoxedType() {
        // Lua numbers are always Double internally; rawset(Object, Object) must coerce any other
        // boxed Number (e.g. the Integer a plain "1" literal autoboxes to through that overload)
        // to the same Double a lookup would use, or the entry becomes silently unreachable
        final LuaTable table = new LuaTableImpl();

        table.rawset(1, "set with a plain int literal");

        assertThat(table.rawget(1.0)).isEqualTo("set with a plain int literal");
        assertThat(table.rawget(Integer.valueOf(1))).isEqualTo("set with a plain int literal");
        assertThat(table.rawget(Long.valueOf(1))).isEqualTo("set with a plain int literal");
        assertThat(table.len()).isEqualTo(1);
    }

    @Test
    public void numericKeySetWithLongIsReadableAsDouble() {
        final LuaTable table = new LuaTableImpl();

        table.rawset(Long.valueOf(42), "set with a Long key");

        assertThat(table.rawget(42.0)).isEqualTo("set with a Long key");
    }

}
