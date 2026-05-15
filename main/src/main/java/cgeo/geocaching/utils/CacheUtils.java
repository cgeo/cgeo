package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        return null != ProcessUtils.getLaunchIntent(LocalizationUtils.getPlainString(R.string.package_alc));
    }

    public static void setLabLink(@NonNull final Activity activity, @NonNull final View view, @Nullable final String url) {
        view.setOnClickListener(v -> {
            // re-check installation state, might have changed since creating the view
            if (isLabPlayerInstalled(activity) && StringUtils.isNotBlank(url)) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else {
                ProcessUtils.openMarket(activity, LocalizationUtils.getPlainString(R.string.package_alc));
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

    /**
     * Returns a Pair of (title, message) for the hint/personal-note dialog.
     * title combines "Hint" and/or "Personal note" labels separated by " / ".
     * message combines hint and/or personal note text separated by a divider line.
     * Message may be empty if neither hint nor personal note is set.
     */
    @NonNull
    public static Pair<CharSequence, CharSequence> getHintTitleAndMessage(@NonNull final Geocache cache) {
        final String hint = cache.getHint();
        final boolean hasHint = StringUtils.isNotEmpty(hint);
        final String personalNote = cache.getPersonalNote();
        final boolean hasPersonalNote = StringUtils.isNotEmpty(personalNote);

        final List<String> titleList = new ArrayList<>();
        titleList.add(hasHint ? LocalizationUtils.getString(R.string.cache_hint) : null);
        titleList.add(hasPersonalNote ? LocalizationUtils.getString(R.string.cache_personal_note) : null);
        final CharSequence title = TextUtils.join(titleList, s -> s, " / ");

        final List<String> messageList = new ArrayList<>();
        messageList.add(hasHint ? hint : null);
        messageList.add(hasPersonalNote ? personalNote : null);
        final CharSequence message = TextUtils.join(messageList, s -> s, "\r\n──────────\r\n");

        return Pair.create(title, message);
    }
}
