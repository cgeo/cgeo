package cgeo.geocaching.ui.renderer;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.handlers.TableHandler;

public class HtmlSpannableRenderer extends HtmlSpanner {

    public HtmlSpannableRenderer(final String geocode, final View view) {
        super();

        // use c:geo's image cache instead of the default implementation
        registerHandler("img", new OfflineImageHandler(geocode));

        // table rendering is not enabled by default
        final TableHandler tableHandler = new TableHandler();
        tableHandler.setTableWidth(view.getResources().getDisplayMetrics().widthPixels - ViewUtils.dpToPixel(20));
        tableHandler.setTextSize(view.getResources().getDimensionPixelSize(R.dimen.textSize_detailsPrimary));
        tableHandler.setTextColor(view.getResources().getColor(R.color.colorText));
        registerHandler("table", tableHandler);

        // apply contrast fix
        setContrastPatcher(new ColorNormalizer(view.getResources()));

        // disable font size styling as this can result in bad formatting (e.g. GC9Q1HQ)
        setUseFontSizeFromStyle(false);

    }
}
