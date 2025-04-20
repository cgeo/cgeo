package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.wherigo.WherigoUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;

public class WhereYouGoApp extends AbstractGeneralApp {

    public WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(@NonNull final Geocache cache) {
        return cache.getType() == CacheType.WHERIGO && !WherigoUtils.getWherigoGuids(cache).isEmpty();
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final List<String> guids = WherigoUtils.getWherigoGuids(cache);
        if (!guids.isEmpty()) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WherigoUtils.getWherigoUrl(guids.get(0)))));
        }
    }

    public static boolean isWhereYouGoInstalled() {
        return null != ProcessUtils.getLaunchIntent(getString(R.string.package_whereyougo));
    }

    public static void openWherigo(@NonNull final Activity activity, @NonNull final String guid) {
        // re-check installation state, might have changed since creating the view
        if (isWhereYouGoInstalled()) {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WherigoUtils.getWherigoUrl(guid)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } else {
            ProcessUtils.openMarket(activity, activity.getString(R.string.package_whereyougo));
        }
    }

}
