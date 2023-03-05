package cgeo.geocaching.models;

import cgeo.geocaching.Intents;
import cgeo.geocaching.ui.CheckableItemSelectActivity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.StringRes;

import java.util.ArrayList;

public abstract class InfoItem {

    protected final int id;
    @StringRes protected final int titleResId;

    public InfoItem(final int id, final @StringRes int titleResId) {
        this.id = id;
        this.titleResId = titleResId;
    }

    public int getId() {
        return id;
    };

    public @StringRes int getTitleResId() {
        return titleResId;
    };

    public static InfoItem getById(final int id, final ArrayList<InfoItem> items) {
        for (InfoItem item : items) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    public static void startActivity(final Activity caller, final String className, final String fieldName, final @StringRes int title, @StringRes final int prefKey) {
        final Intent intent = new Intent(caller, CheckableItemSelectActivity.class);
        intent.putExtra(Intents.EXTRA_TITLE, title);
        intent.putExtra(Intents.EXTRA_ID, prefKey);
        intent.putExtra(Intents.EXTRA_CLASS, className);
        intent.putExtra(Intents.EXTRA_FIELD, fieldName);
        caller.startActivity(intent);
    }

}
