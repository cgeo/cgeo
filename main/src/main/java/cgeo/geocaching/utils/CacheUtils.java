package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class CacheUtils {

    private CacheUtils() {
        // utility class
    }

    public static boolean isLabAdventure(@NonNull final Geocache cache) {
        return cache.getType() == CacheType.ADVLAB && StringUtils.isNotEmpty(cache.getUrl());
    }

    public static boolean isLabPlayerInstalled(@NonNull final Activity activity) {
        return null != ProcessUtils.getLaunchIntent(activity.getString(R.string.package_alc));
    }

    public static void setLabLink(@NonNull final Activity activity, @NonNull final View view, @Nullable final String url) {
        view.setOnClickListener(v -> {
            // re-check installation state, might have changed since creating the view
            if (isLabPlayerInstalled(activity) && StringUtils.isNotBlank(url)) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else {
                ProcessUtils.openMarket(activity, activity.getString(R.string.package_alc));
            }
        });
    }

    /**
     * Find links to Adventure Labs in Listing of a cache. Returns URL if exactly 1 link target is found, else null.
     * 3 types of URLs possible: https://adventurelab.page.link/Cw3L, https://labs.geocaching.com/goto/Theater, https://labs.geocaching.com/goto/a4b45b7b-fa76-4387-a54f-045875ffee0c
     */
    @Nullable
    public static String findAdvLabUrl(final Geocache cache) {
        final Pattern patternAdvLabUrl = Pattern.compile("(https?://labs.geocaching.com/goto/[a-zA-Z0-9-_]{1,36}|https?://adventurelab.page.link/[a-zA-Z0-9]{4})");
        final Matcher matcher = patternAdvLabUrl.matcher(cache.getShortDescription() + " " + cache.getDescription());
        final Set<String> urls = new HashSet<>();
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        if (urls.size() == 1) {
            return urls.iterator().next();
        }
        return null;
    }
}
