package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Enum listing all single-document-Uris which can be persisted
 */
public enum PersistableUri {

    UNUSED_TRACK(R.string.pref_persistableuri_track, R.string.persistableuri_track, null);

    @StringRes
    private final int prefKeyId;
    @StringRes
    private final int nameKeyId;
    private final String mimeType;


    PersistableUri(@StringRes final int prefKeyId, @StringRes final int nameKeyId, final String mimeType) {
        this.prefKeyId = prefKeyId;
        this.nameKeyId = nameKeyId;
        this.mimeType = mimeType;
    }

    @StringRes
    public int getPrefKeyId() {
        return this.prefKeyId;
    }

    @StringRes
    public int getNameKeyId() {
        return nameKeyId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Uri getUri() {
        final String uriString = Settings.getPersistableUri(this);
        return uriString == null ? null : Uri.parse(uriString);
    }

    public boolean hasValue() {
        return getUri() != null;
    }

    /**
     * Sets a new user-defined location ("null" is allowed). Should be called ONLY by {@link ContentStorage}
     */
    protected void setPersistedUri(@Nullable final Uri uri) {
        Settings.setPersistableUri(this, uri == null ? null : uri.toString());
    }

    @Override
    public String toString() {
        return name() + ": " + getUri();
    }

}
