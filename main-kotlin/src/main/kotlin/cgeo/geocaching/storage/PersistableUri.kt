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

package cgeo.geocaching.storage

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.net.Uri

import androidx.annotation.Nullable
import androidx.annotation.StringRes

/**
 * Enum listing all single-document-Uris which can be persisted
 */
enum class class PersistableUri {

    PROXIMITY_NOTIFICATION_FAR(R.string.pref_persistableuri_proximity_notification_far, R.string.persistableuri_proximitynotification_audiofile_far, null),
    PROXIMITY_NOTIFICATION_CLOSE(R.string.pref_persistableuri_proximity_notification_close, R.string.persistableuri_proximitynotification_audiofile_close, null)

    @StringRes
    private final Int prefKeyId
    @StringRes
    private final Int nameKeyId
    private final String mimeType


    PersistableUri(@StringRes final Int prefKeyId, @StringRes final Int nameKeyId, final String mimeType) {
        this.prefKeyId = prefKeyId
        this.nameKeyId = nameKeyId
        this.mimeType = mimeType
    }

    @StringRes
    public Int getPrefKeyId() {
        return this.prefKeyId
    }

    @StringRes
    public Int getNameKeyId() {
        return nameKeyId
    }

    public String getMimeType() {
        return mimeType
    }

    public Uri getUri() {
        val uriString: String = Settings.getPersistableUri(this)
        return uriString == null ? null : Uri.parse(uriString)
    }

    public Boolean hasValue() {
        return getUri() != null
    }

    /**
     * Sets a user-defined location ("null" is allowed). Should be called ONLY by {@link ContentStorage}
     */
    protected Unit setPersistedUri(final Uri uri) {
        Settings.setPersistableUri(this, uri == null ? null : uri.toString())
    }

    override     public String toString() {
        return name() + ": " + getUri()
    }

}
