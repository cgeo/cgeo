package cgeo.geocaching.playservices;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitation.IntentBuilder;

public class AppInvite {

    private AppInvite() {
        // utility class
    }

    public static final boolean isAvailable() {
        return ProcessUtils.isIntentAvailable("com.google.android.gms.appinvite.ACTION_APP_INVITE");
    }

    public static void send(@NonNull final Activity activity, @NonNull final String message) {
        sendInvite(activity, message, null);
    }

    private static void sendInvite(@NonNull final Activity activity, @NonNull final String message, @Nullable final String deepLink) {
        final IntentBuilder builder = new AppInviteInvitation.IntentBuilder(activity.getString(R.string.invitation_title)).setMessage(message).setCustomImage(Uri.parse("http://www.cgeo.org/images/cgeo-logo.png"));
        if (deepLink != null) {
            builder.setDeepLink(Uri.parse(deepLink));
        }
        final Intent intent = builder.build();
        activity.startActivityForResult(intent, Intents.APP_INVITE_REQUEST_CODE);
    }

    public static void send(@NonNull final Activity activity, @NonNull final Geocache cache) {
        final StringBuilder message = new StringBuilder();
        final String lineBreak = "\n";
        final String separator = ": ";
        message.append(cache.getName()).append(lineBreak);
        message.append(activity.getString(R.string.cache_type)).append(separator).append(cache.getType().getL10n()).append(lineBreak);
        message.append(activity.getString(R.string.cache_terrain)).append(separator).append(cache.getTerrain()).append(lineBreak);
        message.append(activity.getString(R.string.cache_difficulty)).append(separator).append(cache.getDifficulty()).append(lineBreak);
        message.append(cache.getUrl());

        // there is a limit of 100 characters
        while (message.length() > AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH && message.lastIndexOf(lineBreak) > 0) {
            message.delete(message.lastIndexOf(lineBreak), message.length());
        }

        final Intent searchIntent = new Intent(CgeoApplication.getInstance(), SearchActivity.class);
        searchIntent.setAction(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, cache.getGeocode()).putExtra(Intents.EXTRA_KEYWORD_SEARCH, false);
        final String deepLink = searchIntent.toUri(0);

        sendInvite(activity, message.toString(), deepLink);
    }

}
