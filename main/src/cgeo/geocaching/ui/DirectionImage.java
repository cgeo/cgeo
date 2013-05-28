package cgeo.geocaching.ui;

import cgeo.geocaching.StoredList;
import cgeo.geocaching.network.HtmlImage;

import org.apache.commons.lang3.StringUtils;

import android.graphics.drawable.BitmapDrawable;

public class DirectionImage {

    static private HtmlImage htmlImage = new HtmlImage(HtmlImage.SHARED, false, StoredList.STANDARD_LIST_ID, false);

    /**
     * Retrieve the direction image corresponding to the direction code.
     *
     * @param directionCode one of the eight cardinal points
     * @return a drawable with the arrow pointing into the right direction
     */
    public static BitmapDrawable getDrawable(final String directionCode) {
        return StringUtils.isNotBlank(directionCode) ? htmlImage.getDrawable("http://www.geocaching.com/images/icons/compass/" + directionCode + ".gif") : null;
    }

}
