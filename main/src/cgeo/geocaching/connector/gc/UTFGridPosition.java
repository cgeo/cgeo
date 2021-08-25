package cgeo.geocaching.connector.gc;

import cgeo.geocaching.utils.MatcherWrapper;

import java.util.regex.Pattern;


/**
 * Representation of a position inside an UTFGrid
 */
final class UTFGridPosition {

    static final int GRID_MAXX = 63;
    static final int GRID_MAXY = 63;

    final int x;
    final int y;
    private static final Pattern PATTERN_JSON_KEY = Pattern.compile("[^\\d]*" + "(\\d+),\\s*(\\d+)" + "[^\\d]*"); // (12, 34)

    UTFGridPosition(final int x, final int y) {
        if (x < 0 || x > GRID_MAXX) {
            throw new IllegalArgumentException("x outside bounds");
        }
        if (y < 0 || y > GRID_MAXY) {
            throw new IllegalArgumentException("y outside bounds");
        }
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * @param key
     *            Key in the format (xx, xx)
     */
    static UTFGridPosition fromString(final String key) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_JSON_KEY, key);
        try {
            if (matcher.matches()) {
                final int x = Integer.parseInt(matcher.group(1));
                final int y = Integer.parseInt(matcher.group(2));
                return new UTFGridPosition(x, y);
            }
        } catch (final NumberFormatException ignored) {
        }
        return new UTFGridPosition(0, 0);
    }

}
