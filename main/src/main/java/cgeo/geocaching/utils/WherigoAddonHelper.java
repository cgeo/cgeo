package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Wherigo player ships as a separate installable module (see the {@code :wherigo} Gradle
 * module), so it cannot be referenced from here at compile time. This class launches it purely
 * via an implicit component name, the same way c:geo already talks to other separate apps.
 */
public final class WherigoAddonHelper {

    private static final String WHERIGO_ACTIVITY_CLASS = "cgeo.geocaching.wherigo.WherigoActivity";
    private static final String EXTRA_WHERIGO_GUID = "wherigo_guid";
    private static final String EXTRA_WHERIGO_GEOCODE = "wherigo_geocode";

    private static final Pattern PATTERN_CARTRIDGE_LINK = Pattern.compile("https?" + Pattern.quote("://") + "(?:www\\.)?" + Pattern.quote("wherigo.com/cartridge/") + "(?:details|download)" + Pattern.quote(".aspx?") + "[Cc][Gg][Uu][Ii][Dd]=([-0-9a-zA-Z]*)");

    private WherigoAddonHelper() {
        // utility class
    }

    /** Cache-independent GUID scan, kept in sync with {@code WherigoUtils.scanWherigoGuids} in the addon module. */
    @NonNull
    public static List<String> getWherigoGuids(@Nullable final Geocache cache) {
        if (cache == null) {
            return Collections.emptyList();
        }
        final String textToScan = cache.getShortDescription() + " " + cache.getDescription();
        final Matcher matcher = PATTERN_CARTRIDGE_LINK.matcher(textToScan);
        final List<String> guids = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        while (matcher.find()) {
            final String guid = matcher.group(1);
            if (guid != null && seen.add(guid)) {
                guids.add(guid);
            }
        }
        return guids;
    }

    public static boolean isInstalled(@NonNull final Context context) {
        return buildIntent(context).resolveActivity(context.getPackageManager()) != null;
    }

    public static void start(@NonNull final Activity activity) {
        startOrPrompt(activity, buildIntent(activity));
    }

    public static void startForGuid(@NonNull final Activity activity, @NonNull final String guid, @Nullable final String geocode) {
        final Intent intent = buildIntent(activity);
        intent.putExtra(EXTRA_WHERIGO_GUID, guid);
        intent.putExtra(EXTRA_WHERIGO_GEOCODE, geocode);
        startOrPrompt(activity, intent);
    }

    /** Builds a plain launch intent, e.g. for use as a pinned home-screen shortcut. */
    @NonNull
    public static Intent buildIntent(@NonNull final Context context) {
        final Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), WHERIGO_ACTIVITY_CLASS);
        return intent;
    }

    private static void startOrPrompt(@NonNull final Activity activity, @NonNull final Intent intent) {
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            SimpleDialog.of(activity).setTitle(R.string.wherigo_player).setMessage(R.string.wherigo_addon_not_installed).show();
        }
    }
}
