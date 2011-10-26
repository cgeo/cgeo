/**
*
*/
package cgeo.geocaching.utils;


/**
 * Stop watch
 * 
 * @author blafoo
 */
public final class StopWatch {

    private long start;

    public StopWatch() {
        reset();
    }

    public void reset() {
        start = System.currentTimeMillis();
    }

    /**
     * @return difference in ms from the start to "now"
     */
    public String getTimeAsString() {
        return String.format("%d ms", getTime());
    }

    /**
     * @return difference in ms from the start to "now"
     */
    public long getTime() {
        return System.currentTimeMillis() - start;
    }

}