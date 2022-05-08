package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.PersistableFolder;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import java.util.IllegalFormatException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A set of static helper methods supporting localization/internationalization
 * especially in areas where Code has no activity available to get a context from.
 *
 * All methods work also in unit-test environments (where there is no available context
 */
public final class LocalizationUtils {

    private static final Context APPLICATION_CONTEXT =
            CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getApplicationContext();

    private LocalizationUtils() {
        //Util class, no instance
    }

    public static boolean hasContext() {
        return APPLICATION_CONTEXT != null;
    }

    public static String getString(@StringRes final int resId, final Object... params) {
        return getStringWithFallback(resId, null, params);
    }

    public static String getStringWithFallback(@StringRes final int resId, final String fallback, final Object... params) {
        if ((APPLICATION_CONTEXT == null || resId == 0) && fallback == null) {
            return "(NoCtx/NoResId/NoFallback)[" + StringUtils.join(params, ";") + "]";
        }
        try {
            if (resId == 0 || APPLICATION_CONTEXT == null) {
                return params != null && params.length > 0 ? String.format(fallback, params) : fallback;
            }
            return params != null && params.length > 0 ? APPLICATION_CONTEXT.getString(resId, params) : APPLICATION_CONTEXT.getString(resId);
        } catch (IllegalFormatException | Resources.NotFoundException e) {
            Log.w("Problem trying to format '" + resId + "/" + fallback + "' with [" + StringUtils.join(params, ";") + "]", e);
            return (fallback == null ? "" : fallback) + ":" + StringUtils.join(params, ";");
        }
    }

    public static String getPlural(@PluralsRes final int pluralId, final int quantity) {
        return getPlural(pluralId, quantity, "thing(s)");
    }

    public static String getPlural(@PluralsRes final int pluralId, final int quantity, final String fallback) {
        if (APPLICATION_CONTEXT == null) {
            return quantity + " " + fallback;
        }
        return APPLICATION_CONTEXT.getResources().getQuantityString(pluralId, quantity, quantity);
    }

    public static String[] getStringArray(@ArrayRes final int arrayId, final String... fallback) {
        if (APPLICATION_CONTEXT == null) {
            return fallback == null ? new String[0] : fallback;
        }
        return APPLICATION_CONTEXT.getResources().getStringArray(arrayId);
    }

    public static int[] getIntArray(@ArrayRes final int arrayId, final int... fallback) {
        if (APPLICATION_CONTEXT == null) {
            return fallback == null ? new int[0] : fallback;
        }
        return APPLICATION_CONTEXT.getResources().getIntArray(arrayId);
    }

    /**
     * Given a resource id and parameters to fill it, constructs one message fit for user display (left) and one for log file
     * (right). Difference is that the one for the log file will contain more detailled information than that for the end user
     */
    public static ImmutablePair<String, String> getMultiPurposeString(@StringRes final int messageId, final String fallback, final Object... params) {

        //prepare params message
        final Object[] paramsForLog = new Object[params.length];
        final Object[] paramsForUser = new Object[params.length];
        //Note that ContentStorage.get() can actually be null here in case there was an error in initialization of Log!
        for (int i = 0; i < params.length; i++) {
            paramsForUser[i] = null;
            paramsForLog[i] = null;
            try {
                if (params[i] instanceof Folder) {
                    paramsForUser[i] = ((Folder) params[i]).toUserDisplayableString();
                    paramsForLog[i] = params[i] + "(" + (ContentStorage.get() == null ? null : ContentStorage.get().getUriForFolder((Folder) params[i])) + ")";
                } else if (params[i] instanceof PersistableFolder) {
                    paramsForUser[i] = ((PersistableFolder) params[i]).toUserDisplayableValue();
                    paramsForLog[i] = params[i] + "(" + (ContentStorage.get() == null ? null : ContentStorage.get().getUriForFolder(((PersistableFolder) params[i]).getFolder())) + ")";
                } else if (params[i] instanceof Uri) {
                    paramsForUser[i] = UriUtils.toUserDisplayableString((Uri) params[i]);
                    paramsForLog[i] = params[i];
                }
            } catch (Exception ex) {
                //regardless of exceptions, getting multipurpose string must always work!
                Log.v("Exception creating multipurposestring", ex);
            }
            if (paramsForUser[i] == null) {
                paramsForUser[i] = params[i];
            }
            if (paramsForLog[i] == null) {
                paramsForLog[i] = params[i];
            }
        }
        return new ImmutablePair<>(getStringWithFallback(messageId, fallback, paramsForUser), getStringWithFallback(messageId, fallback, paramsForLog));
    }

    @NonNull
    public static String getEnglishString(final Context context, @StringRes final int resId) {
        final Configuration configuration = getEnglishConfiguration(context);
        return context.createConfigurationContext(configuration).getResources().getString(resId);
    }

    @NonNull
    private static Configuration getEnglishConfiguration(final Context context) {
        final Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(new Locale("en"));
        return configuration;
    }

}
