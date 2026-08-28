package cgeo.geocaching.utils;

import cgeo.geocaching.AttributesGridAdapter;
import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheAttributeCategory;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.wherigo.WherigoActivity;
import cgeo.geocaching.wherigo.WherigoUtils;
import cgeo.geocaching.wherigo.WherigoViewUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.ImmutablePair;


public class CacheInfoBoxes {

    private CacheInfoBoxes() {
        // utility class
    }

    public static void updateOfflineBox(final View view, final Geocache cache,
                                        final View.OnClickListener refreshCacheClickListener,
                                        final View.OnClickListener dropCacheClickListener,
                                        final View.OnClickListener storeCacheClickListener,
                                        final View.OnClickListener showHintClickListener,
                                        final View.OnLongClickListener moveCacheListener,
                                        final View.OnLongClickListener storeCachePreselectedListener) {
        if (view == null) {
            return; // fragment already destroyed?
        }

        // offline use
        final TextView offlineText = view.findViewById(R.id.offline_text);
        final View offlineRefresh = view.findViewById(R.id.offline_refresh);
        final View offlineStore = view.findViewById(R.id.offline_store);
        final View offlineDrop = view.findViewById(R.id.offline_drop);
        final View offlineEdit = view.findViewById(R.id.offline_edit);

        // check if hint is available and set onClickListener and hint button visibility accordingly
        final boolean hintButtonEnabled = setOfflineHintText(showHintClickListener, view.findViewById(R.id.offline_hint_text), cache.getHint(), cache.getPersonalNote());
        final View offlineHint = view.findViewById(R.id.offline_hint);
        if (null != offlineHint) {
            if (hintButtonEnabled) {
                offlineHint.setVisibility(View.VISIBLE);
                offlineHint.setClickable(true);
                offlineHint.setOnClickListener(showHintClickListener);
            } else {
                offlineHint.setVisibility(View.GONE);
                offlineHint.setClickable(false);
                offlineHint.setOnClickListener(null);
            }
        }

        offlineStore.setClickable(true);
        offlineStore.setOnClickListener(storeCacheClickListener);
        offlineStore.setOnLongClickListener(storeCachePreselectedListener);

        offlineDrop.setClickable(true);
        offlineDrop.setOnClickListener(dropCacheClickListener);
        offlineDrop.setOnLongClickListener(null);

        offlineEdit.setOnClickListener(storeCacheClickListener);
        if (moveCacheListener != null) {
            offlineEdit.setOnLongClickListener(moveCacheListener);
        }

        offlineRefresh.setVisibility(cache.supportsRefresh() ? View.VISIBLE : View.GONE);
        offlineRefresh.setClickable(true);
        offlineRefresh.setOnClickListener(refreshCacheClickListener);

        if (cache.isOffline()) {
            offlineText.setText(Formatter.formatStoredAgo(cache.getDetailedUpdate()));

            offlineStore.setVisibility(View.GONE);
            offlineDrop.setVisibility(View.VISIBLE);
            offlineEdit.setVisibility(View.VISIBLE);
        } else {
            offlineText.setText(LocalizationUtils.getString(R.string.cache_offline_not_ready));

            offlineStore.setVisibility(View.VISIBLE);
            offlineDrop.setVisibility(View.GONE);
            offlineEdit.setVisibility(View.GONE);
        }
    }


    public static void updateAttributes(final Geocache cache, final TextView attributesText, final GridView attributesGrid, final Activity activity) {
        final List<String> attributes = cache.getAttributes();
        if (!CacheAttribute.hasRecognizedAttributeIcon(attributes)) {
            attributesGrid.setVisibility(View.GONE);
            return;
        }
        // traverse by category and attribute order
        final ArrayList<String> orderedAttributeNames = new ArrayList<>();
        final StringBuilder attributesTextBuilder = new StringBuilder();
        CacheAttributeCategory lastCategory = null;
        for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
            for (CacheAttribute attr : CacheAttribute.getByCategory(category)) {
                for (Boolean enabled : Arrays.asList(false, true, null)) {
                    final String key = attr.getValue(enabled);
                    if (attributes.contains(key)) {
                        if (lastCategory != category) {
                            if (lastCategory != null) {
                                attributesTextBuilder.append("<br /><br />");
                            }
                            attributesTextBuilder.append("<b><u>").append(category.getName()).append("</u></b><br />");
                            lastCategory = category;
                        } else {
                            attributesTextBuilder.append("<br />");
                        }
                        orderedAttributeNames.add(key);
                        attributesTextBuilder.append(attr.getL10n(enabled == null || enabled));
                    }
                }
            }
        }

        attributesGrid.setAdapter(new AttributesGridAdapter(activity, orderedAttributeNames, () -> toggleAttributesView(attributesText, attributesGrid)));
        attributesGrid.setVisibility(View.VISIBLE);

        attributesText.setText(HtmlCompat.fromHtml(attributesTextBuilder.toString(), 0));
        attributesText.setVisibility(View.GONE);
        attributesText.setOnClickListener(v -> toggleAttributesView(attributesText, attributesGrid));
    }

    private static void toggleAttributesView(final TextView attributesText, final GridView attributesGrid) {
        attributesText.setVisibility(attributesText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        attributesGrid.setVisibility(attributesGrid.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private static boolean setOfflineHintText(final View.OnClickListener showHintClickListener, final TextView offlineHintTextView, final String hint, final String personalNote) {
        if (null != showHintClickListener) {
            final boolean hintGiven = StringUtils.isNotEmpty(hint);
            final boolean personalNoteGiven = StringUtils.isNotEmpty(personalNote);
            if (hintGiven || personalNoteGiven) {
                offlineHintTextView.setText((hintGiven ? hint + (personalNoteGiven ? "\r\n" : "") : "") + (personalNoteGiven ? personalNote : ""));
                return true;
            }
        }
        return false;
    }

    public static void updateCacheListsAndMatchingFilters(final View view, final Geocache cache, @Nullable final CacheDetailActivity cacheDetailActivity) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();

        // only display "Lists: ..." if the cache is actually stored offline (i.e. part of at least one list)
        if (cache.isOffline()) {
            final SpannableStringBuilder listsBuilder = new SpannableStringBuilder();
            for (final Integer listId : cache.getLists()) {
                if (listsBuilder.length() > 0) {
                    listsBuilder.append(", ");
                }
                appendClickableList(listsBuilder, view, listId, cacheDetailActivity);
            }
            listsBuilder.insert(0, LocalizationUtils.getString(R.string.list_list_headline) + " ");
            builder.append(listsBuilder);
        }

        final View marker = view.findViewById(R.id.marker_button);
        if (marker != null) {
            final boolean enableMarkerButton = cacheDetailActivity != null && Settings.getNamedFilterDisplayMode() != Settings.NamedFilterDisplayMode.NONE;
            marker.setVisibility(enableMarkerButton ? View.VISIBLE : View.GONE);
            if (enableMarkerButton) {
                marker.setOnClickListener(v -> FilterUtils.openDialogActivateMarkers(cacheDetailActivity));
            }
        }
        appendMatchingNamedFilters(builder, cache, cacheDetailActivity);

        final TextView offlineLists = view.findViewById(R.id.offline_lists);
        offlineLists.setText(builder);
        offlineLists.setMovementMethod(LinkMovementMethod.getInstance());
        offlineLists.setVisibility(builder.length() > 0 ? View.VISIBLE : View.GONE);
    }

    /**
     * Appends the named filters matching {@code cache} to {@code builder} (preceded by a line break if
     * {@code builder} is not empty), depending on {@link Settings#getNamedFilterDisplayMode()}.
     * Active filters are shown in the standard link color, inactive (passive) ones grayed out.
     * The filter name is clickable (its optional marker prefix is not).
     */
    private static void appendMatchingNamedFilters(final SpannableStringBuilder builder, final Geocache cache, final CacheDetailActivity cacheDetailActivity) {
        final Settings.NamedFilterDisplayMode mode = Settings.getNamedFilterDisplayMode();
        if (mode == Settings.NamedFilterDisplayMode.NONE) {
            return;
        }

        final ImmutablePair<List<NamedFilter>, List<NamedFilter>> matches = NamedFilter.getFiltersMatchingCache(cache, mode == Settings.NamedFilterDisplayMode.ACTIVE_ONLY);
        final List<NamedFilter> activeFilters = matches.left;
        final List<NamedFilter> passiveFilters = matches.right;
        if (activeFilters.isEmpty() && passiveFilters.isEmpty()) {
            return;
        }

        final SpannableStringBuilder filtersBuilder = new SpannableStringBuilder();
        for (final NamedFilter filter : activeFilters) {
            appendNamedFilter(filtersBuilder, filter, false, cacheDetailActivity);
        }
        for (final NamedFilter filter : passiveFilters) {
            appendNamedFilter(filtersBuilder, filter, true, cacheDetailActivity);
        }
        filtersBuilder.insert(0, LocalizationUtils.getString(R.string.filters_list_headline) + " ");

        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(filtersBuilder);
    }

    private static void appendNamedFilter(final SpannableStringBuilder builder, final NamedFilter filter, final boolean inactive, final CacheDetailActivity cacheDetailActivity) {
        if (builder.length() > 0) {
            builder.append(", ");
        }

        if (StringUtils.isNotBlank(filter.getMarkerId())) {
            builder.append(filter.getMarkerId()).append(" ");
        }
        final int start = builder.length();
        builder.append(filter.getName());
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull final View widget) {
                Settings.setLastDisplayedList(PseudoList.ALL_LIST.id);
                if (cacheDetailActivity != null) {
                    cacheDetailActivity.setNeedsRefresh();
                }
                CacheListActivity.startActivityOffline(widget.getContext(), filter);
            }
        }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (inactive) {
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(CgeoApplication.getInstance(), R.color.colorTextHint)), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static void appendClickableList(final SpannableStringBuilder builder, final View view, final Integer listId, @Nullable final CacheDetailActivity cacheDetailActivity) {
        final StoredList list = DataStore.getList(listId);
        if (StringUtils.isNotBlank(list.emojiMarker)) {
            builder.append(list.emojiMarker).append(" ");
        }
        final int start = builder.length();
        builder.append(list.getTitle());
        builder.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull final View widget) {
                Settings.setLastDisplayedList(listId);
                if (cacheDetailActivity != null) {
                    cacheDetailActivity.setNeedsRefresh();
                }
                CacheListActivity.startActivityOffline(view.getContext(), null);
            }
        }, start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void updateWherigoBox(final Geocache cache, final Activity activity, @NonNull final Button wherigoButton, @Nullable final View wherigoView, @Nullable final TextView wherigoText) {
        final List<String> wherigoGuis = WherigoUtils.getWherigoGuids(cache);
        final boolean isEnabled = !wherigoGuis.isEmpty();

        ViewUtils.setVisibility(wherigoView, isEnabled ? View.VISIBLE : View.GONE);

        ViewUtils.setVisibility(wherigoText, isEnabled ? View.VISIBLE : View.GONE);
        ViewUtils.setText(wherigoText, (!isEnabled || Settings.hasGCCredentials()) ? R.string.cache_wherigo_start : R.string.cache_wherigo_credentials);

        ViewUtils.setVisibility(wherigoButton, isEnabled ? View.VISIBLE : View.GONE);
        if (isEnabled) {
            wherigoButton.setOnClickListener(v -> {
                if (Settings.hasGCCredentials()) {
                    WherigoViewUtils.executeForOneCartridge(activity, wherigoGuis, guid ->
                            WherigoActivity.startForGuid(activity, guid, cache.getGeocode(), true));
                } else {
                    SettingsActivity.openForScreen(R.string.preference_screen_gc, activity);
                }
            });
        }
    }

    public static void updateChirpWolfBox(final Geocache cache, final Activity activity, @NonNull final Button chirpButton, @Nullable final View chirpView, @Nullable final TextView chirpText) {
        final Intent chirpWolf = ProcessUtils.getLaunchIntent(LocalizationUtils.getPlainString(R.string.package_chirpwolf));
        final String compare = CacheAttribute.WIRELESSBEACON.getValue(true);
        boolean isEnabled = false;
        for (String current : cache.getAttributes()) {
            if (Strings.CS.equals(current, compare)) {
                isEnabled = true;
                break;
            }
        }

        ViewUtils.setVisibility(chirpView, isEnabled ? View.VISIBLE : View.GONE);

        ViewUtils.setVisibility(chirpText, isEnabled ? View.VISIBLE : View.GONE);
        ViewUtils.setText(chirpText, chirpWolf != null ? R.string.cache_chirpwolf_start : R.string.cache_chirpwolf_install);

        ViewUtils.setVisibility(chirpButton, isEnabled ? View.VISIBLE : View.GONE);
        if (isEnabled) {
            chirpButton.setOnClickListener(v -> {
                // re-check installation state, might have changed since creating the view
                final Intent chirpWolf2 = ProcessUtils.getLaunchIntent(LocalizationUtils.getPlainString(R.string.package_chirpwolf));
                if (chirpWolf2 != null) {
                    chirpWolf2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(chirpWolf2);
                } else {
                    ProcessUtils.openMarket(activity, LocalizationUtils.getPlainString(R.string.package_chirpwolf));
                }
            });
        }
    }

    public static void updateALCBox(final Geocache cache, final Activity activity, @NonNull final Button alcButton, @Nullable final View alcView, @Nullable final TextView alcText) {
        final boolean isLabListing = CacheInfoBoxes.isLabAdventure(cache);
        final boolean isEnabled = isLabListing || (cache.getType() == CacheType.MYSTERY && CacheInfoBoxes.findAdvLabUrl(cache) != null);

        ViewUtils.setVisibility(alcView, isEnabled ? View.VISIBLE : View.GONE);

        ViewUtils.setVisibility(alcText, isEnabled ? View.VISIBLE : View.GONE);
        ViewUtils.setText(alcText, isLabPlayerInstalled() ? (isLabListing ? R.string.cache_alc_start : R.string.cache_alc_related_start) : R.string.cache_alc_install);

        ViewUtils.setVisibility(alcButton, isEnabled ? View.VISIBLE : View.GONE);
        if (isEnabled) {
            alcButton.setOnClickListener(v -> {
                // re-check installation state, might have changed since creating the view
                final String url = isLabListing ? cache.getUrl() : CacheInfoBoxes.findAdvLabUrl(cache);
                if (isLabPlayerInstalled() && StringUtils.isNotBlank(url)) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                } else {
                    ProcessUtils.openMarket(activity, LocalizationUtils.getPlainString(R.string.package_alc));
                }
            });
        }
    }

    private static boolean isLabAdventure(@NonNull final Geocache cache) {
        return cache.getType() == CacheType.ADVLAB && StringUtils.isNotEmpty(cache.getUrl());
    }

    private static boolean isLabPlayerInstalled() {
        return null != ProcessUtils.getLaunchIntent(LocalizationUtils.getPlainString(R.string.package_alc));
    }

    /**
     * Find links to Adventure Labs in Listing of a cache. Returns URL if exactly 1 link target is found, else null.
     * 3 types of URLs possible: https://adventurelab.page.link/Cw3L, https://labs.geocaching.com/goto/Theater, https://labs.geocaching.com/goto/a4b45b7b-fa76-4387-a54f-045875ffee0c
     */
    @Nullable
    private static String findAdvLabUrl(final Geocache cache) {
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
