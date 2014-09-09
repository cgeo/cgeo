package cgeo.geocaching.connector.gc;

import cgeo.geocaching.utils.MatcherWrapper;

import java.util.regex.Pattern;


/**
 * Representation of a position inside an UTFGrid
 */
public final class UTFGridPosition {

    public final int x;
    public final int y;
    private final static Pattern PATTERN_JSON_KEY = Pattern.compile("[^\\d]*" + "(\\d+),\\s*(\\d+)" + "[^\\d]*"); // (12, 34)

    public UTFGridPosition(final int x, final int y) {
        if (x < 0 || x > UTFGrid.GRID_MAXX) {
            throw new IllegalArgumentException("x outside bounds");
        }
        if (y < 0 || y > UTFGrid.GRID_MAXY) {
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
     * @return
     */
    static UTFGridPosition fromString(String key) {
        final MatcherWrapper matcher = new MatcherWrapper(UTFGridPosition.PATTERN_JSON_KEY, key);
        try {
            if (matcher.matches()) {
                final int x = Integer.parseInt(matcher.group(1));
                final int y = Integer.parseInt(matcher.group(2));
                return new UTFGridPosition(x, y);
            }
        } catch (NumberFormatException ignored) {
        }
        return new UTFGridPosition(0, 0);
    }

}
