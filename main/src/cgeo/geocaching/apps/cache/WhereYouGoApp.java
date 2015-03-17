package cgeo.geocaching.apps.cache;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.util.regex.Pattern;

public class WhereYouGoApp extends AbstractGeneralApp {
    private static final Pattern PATTERN_CARTRIDGE = Pattern.compile("(" + Pattern.quote("http://www.wherigo.com/cartridge/details.aspx?") + ".*?)" + Pattern.quote("\""));

    public WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), R.id.cache_app_whereyougo, "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(final Geocache cache) {
        return cache.getType() == CacheType.WHERIGO && StringUtils.isNotEmpty(getWhereIGoUrl(cache));
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getWhereIGoUrl(cache))));
    }

    protected static String getWhereIGoUrl(final Geocache cache) {
        return TextUtils.getMatch(cache.getDescription(), PATTERN_CARTRIDGE, null);
    }
}
