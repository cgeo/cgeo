package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

/**
 * sorts caches by name
 *
 */
public class NameComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return StringUtils.isNotBlank(cache1.name) && StringUtils.isNotBlank(cache2.name);
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        // if the caches have a common prefix followed by a number, sort by the numerical value
        // so 2 is before 11, although "11" comes before "2"
        final String prefix = StringUtils.getCommonPrefix(cache1.name, cache2.name);
        if (StringUtils.length(prefix) > 0) {
            final String remaining1 = cache1.name.substring(prefix.length()).trim();
            if (remaining1.length() > 0 && Character.isDigit(remaining1.charAt(0))) {
                final String remaining2 = cache2.name.substring(prefix.length()).trim();
                if (remaining2.length() > 0 && Character.isDigit(remaining2.charAt(0))) {
                    final Integer number1 = (new Scanner(remaining1)).nextInt();
                    final Integer number2 = (new Scanner(remaining2)).nextInt();
                    return number1.compareTo(number2);
                }
            }
        }
        return cache1.name.compareToIgnoreCase(cache2.name);
    }
}
