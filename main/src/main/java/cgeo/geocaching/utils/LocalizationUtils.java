package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.PersistableFolder;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A set of static helper methods supporting localization/internationalization
 * especially in areas where Code has no activity available to get a context from.
 * <br>
 * All methods work also in unit-test environments (where there is no available context
 */
public final class LocalizationUtils {

    private static final Map<String, String> LANGUAGE_TO_MAIN_COUNTRY = new HashMap<>();
    private static final Map<String, Set<String>> LANGUAGE_TO_COUNTRIES = new HashMap<>();

    static {
        LANGUAGE_TO_MAIN_COUNTRY.put("en", "US"); //many possible countries
        LANGUAGE_TO_MAIN_COUNTRY.put("af", "ZA"); //Afrikaans (Namibia)[NA],Afrikaans (Südafrika)[ZA],
        LANGUAGE_TO_MAIN_COUNTRY.put("sq", "AL"); // Albanisch (Kosovo)[XK],Albanisch (Albanien)[AL],Albanisch (Nordmazedonien)[MK],
        LANGUAGE_TO_MAIN_COUNTRY.put("ar", "SA"); // Arabisch (Palästinensische Autonomiegebiete)[PS],Arabisch (Jordanien)[JO],Arabisch (Bahrain)[BH],Arabisch (Dschibuti)[DJ],Arabisch (Jemen)[YE],Arabisch (Libyen)[LY],Arabisch (Saudi-Arabien)[SA],Arabisch (Katar)[QA],Arabisch (Sudan)[SD],Arabisch (Marokko)[MA],Arabisch (Algerien)[DZ],Arabisch (Somalia)[SO],Arabisch (Oman)[OM],Arabisch (Südsudan)[SS],Arabisch (Ägypten)[EG],Arabisch (Komoren)[KM],Arabisch (Westsahara)[EH],Arabisch (Israel)[IL],Arabisch (Vereinigte Arabische Emirate)[AE],Arabisch (Mauretanien)[MR],Arabisch (Syrien)[SY],Arabisch (Irak)[IQ],Arabisch (Kuwait)[KW],Arabisch (Eritrea)[ER],Arabisch (Tschad)[TD],Arabisch (Welt)[001],Arabisch (Libanon)[LB],Arabisch (Tunesien)[TN],
        LANGUAGE_TO_MAIN_COUNTRY.put("bn", "BD"); // Bengalisch (Bangladesch)[BD],Bengalisch (Indien)[IN],
        LANGUAGE_TO_MAIN_COUNTRY.put("ca", "ES"); // Katalanisch (Andorra)[AD],Katalanisch (Italien)[IT],Katalanisch (Frankreich)[FR],Katalanisch (Spanien)[ES],
        LANGUAGE_TO_MAIN_COUNTRY.put("zh", "CN"); // Chinesisch (Sonderverwaltungsregion Macau)[MO],Chinesisch (Taiwan)[TW],Chinesisch (Sonderverwaltungsregion Hongkong)[HK],Chinesisch (Singapur)[SG],Chinesisch (China)[CN],
        LANGUAGE_TO_MAIN_COUNTRY.put("da", "DK"); // Dänisch (Grönland)[GL],Dänisch (Dänemark)[DK],
        LANGUAGE_TO_MAIN_COUNTRY.put("el", "GR"); // Griechisch (Zypern)[CY],Griechisch (Griechenland)[GR],
        LANGUAGE_TO_MAIN_COUNTRY.put("ht", "HT"); // Haiti
        LANGUAGE_TO_MAIN_COUNTRY.put("ga", "IE"); // Irisch (Vereinigtes Königreich)[GB],Irisch (Irland)[IE],
        LANGUAGE_TO_MAIN_COUNTRY.put("ko", "KR"); // Koreanisch (Nordkorea)[KP],Koreanisch (Südkorea)[KR],
        LANGUAGE_TO_MAIN_COUNTRY.put("ms", "MY"); // Malaiisch (Singapur)[SG],Malaiisch (Indonesien)[ID],Malaiisch (Malaysia)[MY],Malaiisch (Brunei Darussalam)[BN],
        LANGUAGE_TO_MAIN_COUNTRY.put("fa", "IR"); // Persisch (Afghanistan)[AF],Persisch (Iran)[IR],
        LANGUAGE_TO_MAIN_COUNTRY.put("sv", "SE"); // Schwedisch (Schweden)[SE],Schwedisch (Finnland)[FI],Schwedisch (Ålandinseln)[AX],
        LANGUAGE_TO_MAIN_COUNTRY.put("sw", "TZ"); // Suaheli (Kongo-Kinshasa)[CD],Suaheli (Uganda)[UG],Suaheli (Tansania)[TZ],Suaheli (Kenia)[KE],
        LANGUAGE_TO_MAIN_COUNTRY.put("tl", "PH"); // Tagalog -> Philippinen
        LANGUAGE_TO_MAIN_COUNTRY.put("ta", "IN"); // Tamil (Singapur)[SG],Tamil (Indien)[IN],Tamil (Malaysia)[MY],Tamil (Sri Lanka)[LK],
        LANGUAGE_TO_MAIN_COUNTRY.put("ur", "PK"); // Urdu (Indien)[IN],Urdu (Pakistan)[PK],
        LANGUAGE_TO_MAIN_COUNTRY.put("nb", "NO"); // Norwegian Bokmal
        LANGUAGE_TO_MAIN_COUNTRY.put("zh-rtw", "CN"); // Traditional Chinese

        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.isBlank(locale.getLanguage()) || StringUtils.isBlank(locale.getCountry()) || locale.getCountry().length() != 2) {
                continue;
            }
            Set<String> countries = LANGUAGE_TO_COUNTRIES.get(locale.getLanguage());
            if (countries == null) {
                countries = new HashSet<>();
                LANGUAGE_TO_COUNTRIES.put(locale.getLanguage(), countries);
            }
            countries.add(locale.getCountry());
        }
    }

    private LocalizationUtils() {
        //Util class, no instance
    }

    public static String getString(@StringRes final int resId, final Object... params) {
        return getStringWithFallback(resId, null, params);
    }

    public static String getStringWithFallback(@StringRes final int resId, final String fallback, final Object... params) {
        final Context localizationContext = getLocalizationContext();
        if ((localizationContext == null || resId == 0) && fallback == null) {
            return "(NoCtx/NoResId/NoFallback)[" + StringUtils.join(params, ";") + "]";
        }
        try {
            if (resId == 0 || localizationContext == null) {
                return params != null && params.length > 0 ? String.format(fallback, params) : fallback;
            }
            return params != null && params.length > 0 ? localizationContext.getString(resId, params) : localizationContext.getString(resId);
        } catch (IllegalFormatException | Resources.NotFoundException e) {
            String resStringWoFormat = null;
            try {
                resStringWoFormat = localizationContext == null ? null : localizationContext.getString(resId);
            } catch (Exception ex) {
                //ignore, this is for logging only!
            }

            Log.w("Problem trying to format '" + resId + "/'" + resStringWoFormat + "'/" + fallback + "' with [" + StringUtils.join(params, ";") + "] (appContext valid: " + (localizationContext != null) + ")", e);
            return (fallback == null ? "" : fallback) + ":" + StringUtils.join(params, ";");
        }
    }

    public static String getPlural(@PluralsRes final int pluralId, final int quantity) {
        return getPlural(pluralId, quantity, "thing(s)");
    }

    public static String getPlural(@PluralsRes final int pluralId, final int quantity, final String fallback) {
        final Context localizationContext = getLocalizationContext();
        if (localizationContext == null) {
            return quantity + " " + fallback;
        }
        return localizationContext.getResources().getQuantityString(pluralId, quantity, quantity);
    }

    public static String[] getStringArray(@ArrayRes final int arrayId, final String... fallback) {
        final Context localizationContext = getLocalizationContext();
        if (localizationContext == null) {
            return fallback == null ? new String[0] : fallback;
        }
        return localizationContext.getResources().getStringArray(arrayId);
    }

    public static int[] getIntArray(@ArrayRes final int arrayId, final int... fallback) {
        final Context localizationContext = getLocalizationContext();
        if (localizationContext == null) {
            return fallback == null ? new int[0] : fallback;
        }
        return localizationContext.getResources().getIntArray(arrayId);
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

    @Nullable
    private static Context getLocalizationContext() {
        return CgeoApplication.getInstance() == null ? null : CgeoApplication.getInstance().getApplicationContext();
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

    @NonNull
    private static Set<String> getCountriesForLanguage(final String language) {
        final Set<String> set = LANGUAGE_TO_COUNTRIES.get(language);
        return set == null ? Collections.emptySet() : set;
    }

    @NonNull
    private static String getMainCountryForLanguage(final String language) {
        if (language == null) {
            return "";
        }
        final String candidate = LANGUAGE_TO_MAIN_COUNTRY.get(language.toLowerCase(Locale.ROOT));
        if (candidate != null) {
            return candidate.toUpperCase(Locale.ROOT);
        }
        final Set<String> countries = getCountriesForLanguage(language);
        if (countries.size() == 1) {
            return CommonUtils.first(countries);
        }
        return language.toUpperCase(Locale.ROOT);
    }

    @NonNull
    public static String getLocaleDisplayName(final String languageTag, final boolean doShort, final boolean addFlag) {
        return getLocaleDisplayName(languageTag == null ? null : new Locale(languageTag), doShort, addFlag);
    }

    @NonNull
    public static String getLocaleDisplayName(final Locale locale, final boolean doShort, final boolean addFlag) {
        //flag
        final String flag = addFlag ? getLocaleDisplayFlag(locale) + " " : "";
        if (locale == null) {
            return flag + "--";
        }

        //name
        final String nameInAppLocale = locale.getDisplayName(Settings.getApplicationLocale());
        final String nameInLocaleLocale = locale.getDisplayName(locale);
        final String name = nameInAppLocale + (doShort || nameInAppLocale.equals(nameInLocaleLocale) ? "" : "/" + nameInLocaleLocale);

        return flag + name + "[" + locale + "]";
    }

    @NonNull
    public static String getLocaleDisplayFlag(final String languageTag) {
        return getLocaleDisplayFlag(languageTag == null ? null : Locale.forLanguageTag(languageTag));
    }

    @NonNull
    public static String getLocaleDisplayFlag(final Locale locale) {
        if (locale == null) {
            return EmojiUtils.getFlagEmojiFromCountry(null);
        }
        String country = locale.getCountry();
        if (StringUtils.isBlank(country)) {
            country = getMainCountryForLanguage(locale.getLanguage());
        }
        return EmojiUtils.getFlagEmojiFromCountry(country);
    }
}
