package cgeo.geocaching.settings.fragments;

import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    // callback data
    protected Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback = null;
    protected Action2<String, String> scrolltoCallback = null;
    protected String scrolltoBaseKey = null;
    protected String scrolltoPrefKey = null;
    protected int icon = 0;

    // automatic key generator
    private int nextKey = 0;

    public static class PrefSearchDescriptor {
        public String baseKey;
        public String prefKey;
        public CharSequence prefTitle;
        public CharSequence prefSummary;
        public int prefCategoryIconRes;

        PrefSearchDescriptor(final String baseKey, final String prefKey, final CharSequence prefTitle, final CharSequence prefSummary, @DrawableRes final int prefCategoryIconRes) {
            this.baseKey = baseKey;
            this.prefKey = prefKey;
            this.prefTitle = prefTitle;
            this.prefSummary = prefSummary;
            this.prefCategoryIconRes = prefCategoryIconRes;
        }
    }

    // sets icon resource for search info
    public BasePreferenceFragment setIcon(@DrawableRes final int icon) {
        this.icon = icon;
        return this;
    }

    // sets callback to deliver searchable info about prefs to SettingsActivity
    public void setSearchdataCallback(final Action1<ArrayList<PrefSearchDescriptor>> searchdataCallback) {
        this.searchdataCallback = searchdataCallback;
    }

    // sets a callback to scroll to a specific pref after view has been created
    public void setScrollToPrefCallback(final Action2<String, String> scrolltoCallback, final String scrolltoBaseKey, final String scrolltoPrefKey) {
        this.scrolltoCallback = scrolltoCallback;
        this.scrolltoBaseKey = scrolltoBaseKey;
        this.scrolltoPrefKey = scrolltoPrefKey;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (searchdataCallback != null) {
            final PreferenceScreen prefScreen = getPreferenceScreen();
            if (prefScreen != null) {
                final String baseKey = prefScreen.getKey();
                final ArrayList<PrefSearchDescriptor> data = new ArrayList<>();
                doSearch(baseKey, data, prefScreen);
                searchdataCallback.call(data);
                searchdataCallback = null;
            }
        }
        if (scrolltoCallback != null) {
            scrolltoCallback.call(scrolltoBaseKey, scrolltoPrefKey);
            scrolltoCallback = null;
        }
    }

    /**
     * searches recursively in all elements of given prefGroup for first occurrence of s
     * returns found preference on success, null else
     * (prefers preference entries over preference groups)
     */
    private void doSearch(final String baseKey, final ArrayList<PrefSearchDescriptor> data, final PreferenceGroup start) {
        final int prefCount = start.getPreferenceCount();
        for (int i = 0; i < prefCount; i++) {
            final Preference pref = start.getPreference(i);
            // we can only address prefs that have a key, so create a generic one
            if (StringUtils.isBlank(pref.getKey())) {
                synchronized (this) {
                    pref.setKey(baseKey + "-" + (nextKey++));
                }
            }
            data.add(new PrefSearchDescriptor(baseKey, pref.getKey(), pref.getTitle(), pref.getSummary(), icon));
            if (pref instanceof PreferenceGroup) {
                doSearch(baseKey, data, (PreferenceGroup) pref);
            }
        }
    }
}
