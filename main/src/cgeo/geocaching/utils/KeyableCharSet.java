package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A charset identifiable by a string key. The later can be used e.g. for caching purposes
 */
public class KeyableCharSet {

    public static final KeyableCharSet EMPTY = new KeyableCharSet(null);

    private final Set<Character> charSet = new HashSet<>();
    private final String charSetKey;

    private KeyableCharSet(final Collection<Character> initCharSet) {
        if (initCharSet != null) {
            for (Character c : initCharSet) {
                if (c != null) {
                    this.charSet.add(c);
                }
            }
        }
        final List<Character> cList = new ArrayList<>(this.charSet);
        Collections.sort(cList);
        this.charSetKey = StringUtils.join(cList);
    }

    public static KeyableCharSet createFor(final String chars) {
        if (chars == null) {
            return EMPTY;
        }
        final List<Character> cList = new ArrayList<>();
        for (char c : chars.toCharArray()) {
            cList.add(c);
        }
        return new KeyableCharSet(cList);
    }

    public static KeyableCharSet createFor(final Collection<Character> charSet) {
        return new KeyableCharSet(charSet);
    }

    public String getKey() {
        return charSetKey;
    }

    public boolean contains(final char c) {
        return charSet.contains(c);
    }

}
