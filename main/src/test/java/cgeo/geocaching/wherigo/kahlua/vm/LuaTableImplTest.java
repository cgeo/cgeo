package cgeo.geocaching.wherigo.kahlua.vm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

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

}
