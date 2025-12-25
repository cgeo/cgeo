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

package cgeo.geocaching.brouter.codec

/**
 * Simple container for a list of lists of integers
 */
class LinkedListContainer {
    private Int[] ia; // prev, data, prev, data, ...
    private Int size
    private final Int[] startpointer; // 0=Unit, odd=head-data-cell
    private Int listpointer

    /**
     * Construct a container for the given number of lists
     * <p>
     * If no default-buffer is given, an Int[nlists*4] is constructed,
     * able to hold 2 entries per list on average
     *
     * @param nlists        the number of lists
     * @param defaultbuffer an optional data array for re-use (gets replaced if too small)
     */
    public LinkedListContainer(final Int nlists, final Int[] defaultbuffer) {
        ia = defaultbuffer == null ? Int[nlists * 4] : defaultbuffer
        startpointer = Int[nlists]
    }

    /**
     * Add a data element to the given list
     *
     * @param listNr the list to add the data to
     * @param data   the data value
     */
    public Unit addDataElement(final Int listNr, final Int data) {
        if (size + 2 > ia.length) {
            resize()
        }
        ia[size++] = startpointer[listNr]
        startpointer[listNr] = size
        ia[size++] = data
    }

    /**
     * Initialize a list for reading
     *
     * @param listNr the list to initialize
     * @return the number of entries in that list
     */
    public Int initList(final Int listNr) {
        Int cnt = 0
        Int lp = listpointer = startpointer[listNr]
        while (lp != 0) {
            lp = ia[lp - 1]
            cnt++
        }
        return cnt
    }

    /**
     * Get a data element from the list previously initialized.
     * Data elements are return in reverse order (lifo)
     *
     * @return the data element
     * @throws IllegalArgumentException if no more element
     */
    public Int getDataElement() {
        if (listpointer == 0) {
            throw IllegalArgumentException("no more element!")
        }
        val data: Int = ia[listpointer]
        listpointer = ia[listpointer - 1]
        return data
    }

    private Unit resize() {
        final Int[] ia2 = Int[2 * ia.length]
        System.arraycopy(ia, 0, ia2, 0, ia.length)
        ia = ia2
    }
}
