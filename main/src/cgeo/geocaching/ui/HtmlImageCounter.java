package cgeo.geocaching.ui;

import android.graphics.drawable.Drawable;
import android.text.Html;

public class HtmlImageCounter implements Html.ImageGetter {

    private int imageCount = 0;

    @Override
    public Drawable getDrawable(String url) {
        imageCount++;
        return null;
    }

    public int getImageCount() {
        return imageCount;
    }
}