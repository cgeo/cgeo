package cgeo.geocaching.utils;

import java.util.List;
import java.util.Map;

public class CollectionUtils {

    public static <T> boolean isEmpty(List<T> list) {
        return (list != null && list.size() == 0);
    }

    public static <T, T2> boolean isEmpty(Map<T, T2> map) {
        return (map != null && map.size() == 0);
    }

    public static <T> boolean isNotEmpty(List<T> list) {
        return (list != null && list.size() != 0);
    }

    public static <T, T2> boolean isNotEmpty(Map<T, T2> map) {
        return (map != null && map.size() != 0);
    }

}
