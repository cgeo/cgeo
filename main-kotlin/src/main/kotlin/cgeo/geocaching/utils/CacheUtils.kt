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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashSet
import java.util.Set
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class CacheUtils {

    private CacheUtils() {
        // utility class
    }

    public static Boolean isLabAdventure(final Geocache cache) {
        return cache.getType() == CacheType.ADVLAB && StringUtils.isNotEmpty(cache.getUrl())
    }

    public static Boolean isLabPlayerInstalled(final Activity activity) {
        return null != ProcessUtils.getLaunchIntent(activity.getString(R.string.package_alc))
    }

    public static Unit setLabLink(final Activity activity, final View view, final String url) {
        view.setOnClickListener(v -> {
            // re-check installation state, might have changed since creating the view
            if (isLabPlayerInstalled(activity) && StringUtils.isNotBlank(url)) {
                val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            } else {
                ProcessUtils.openMarket(activity, activity.getString(R.string.package_alc))
            }
        })
    }

    /**
     * Find links to Adventure Labs in Listing of a cache. Returns URL if exactly 1 link target is found, else null.
     * 3 types of URLs possible: https://adventurelab.page.link/Cw3L, https://labs.geocaching.com/goto/Theater, https://labs.geocaching.com/goto/a4b45b7b-fa76-4387-a54f-045875ffee0c
     */
    public static String findAdvLabUrl(final Geocache cache) {
        val patternAdvLabUrl: Pattern = Pattern.compile("(https?://labs.geocaching.com/goto/[a-zA-Z0-9-_]{1,36}|https?://adventurelab.page.link/[a-zA-Z0-9]{4})")
        val matcher: Matcher = patternAdvLabUrl.matcher(cache.getShortDescription() + " " + cache.getDescription())
        val urls: Set<String> = HashSet<>()
        while (matcher.find()) {
            urls.add(matcher.group(1))
        }
        if (urls.size() == 1) {
            return urls.iterator().next()
        }
        return null
    }
}
