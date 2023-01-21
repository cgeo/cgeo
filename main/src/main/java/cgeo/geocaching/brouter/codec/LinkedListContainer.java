package cgeo.geocaching.brouter.codec;

/**
 * Simple container for a list of lists of integers
 */
public class LinkedListContainer {
    private int[] ia; // prev, data, prev, data, ...
    private int size;
    private final int[] startpointer; // 0=void, odd=head-data-cell
    private int listpointer;

    /**
     * Construct a container for the given number of lists
     * <p>
     * If no default-buffer is given, an int[nlists*4] is constructed,
     * able to hold 2 entries per list on average
     *
     * @param nlists        the number of lists
     * @param defaultbuffer an optional data array for re-use (gets replaced if too small)
     */
    public LinkedListContainer(final int nlists, final int[] defaultbuffer) {
        ia = defaultbuffer == null ? new int[nlists * 4] : defaultbuffer;
        startpointer = new int[nlists];
    }

    /**
     * Add a data element to the given list
     *
     * @param listNr the list to add the data to
     * @param data   the data value
     */
    public void addDataElement(final int listNr, final int data) {
        if (size + 2 > ia.length) {
            resize();
        }
        ia[size++] = startpointer[listNr];
        startpointer[listNr] = size;
        ia[size++] = data;
    }

    /**
     * Initialize a list for reading
     *
     * @param listNr the list to initialize
     * @return the number of entries in that list
     */
    public int initList(final int listNr) {
        int cnt = 0;
        int lp = listpointer = startpointer[listNr];
        while (lp != 0) {
            lp = ia[lp - 1];
            cnt++;
        }
        return cnt;
    }

    /**
     * Get a data element from the list previously initialized.
     * Data elements are return in reverse order (lifo)
     *
     * @return the data element
     * @throws IllegalArgumentException if no more element
     */
    public int getDataElement() {
        if (listpointer == 0) {
            throw new IllegalArgumentException("no more element!");
        }
        final int data = ia[listpointer];
        listpointer = ia[listpointer - 1];
        return data;
    }

    private void resize() {
        final int[] ia2 = new int[2 * ia.length];
        System.arraycopy(ia, 0, ia2, 0, ia.length);
        ia = ia2;
    }
}
