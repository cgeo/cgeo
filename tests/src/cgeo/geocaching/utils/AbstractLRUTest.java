package cgeo.geocaching.utils;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractLRUTest extends AndroidTestCase {

    protected static String colToStr(Collection<?> col) {
        final ArrayList<String> list = new ArrayList<String>(col.size());
        for (Object o : col) {
            list.add(o.toString());
        }
        return StringUtils.join(list, ", ");
    }

}