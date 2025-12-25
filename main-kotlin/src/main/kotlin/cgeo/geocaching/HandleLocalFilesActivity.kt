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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.downloader.ReceiveDownloadService
import cgeo.geocaching.files.FileType
import cgeo.geocaching.files.FileTypeDetector
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.FileNameCreator
import cgeo.geocaching.utils.Log
import cgeo.geocaching.wherigo.WherigoActivity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.annotation.Nullable
import androidx.core.content.ContextCompat

import org.apache.commons.lang3.StringUtils

class HandleLocalFilesActivity : AbstractActivity() {

    private static val LOGPRAEFIX: String = "HandleLocalFilesActivity: "

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()

        val intent: Intent = getIntent()
        val uri: Uri = intent.getData()
        Boolean finished = false

        Log.iForce(LOGPRAEFIX + "called for URI:" + uri)

        val contentResolver: ContentResolver = getContentResolver()
        val fileType: FileType = FileTypeDetector(uri, contentResolver).getFileType()
        switch (fileType) {
            case GPX:
            case ZIP:
            case LOC:
                continueWith(CacheListActivity.class, intent)
                finished = true
                break
            case MAP:
                continueWithForegroundService(ReceiveDownloadService.class, intent)
                finished = true
                break
            case WHERIGO:
                val guid: String = copyToWherigoFolder(uri)
                if (guid != null) {
                    WherigoActivity.startForGuid(this, guid, null, false)
                    finish()
                    finished = true
                }
                break
            default:
                break
        }
        if (!finished) {
            SimpleDialog.of(this).setTitle(R.string.localfile_title).setMessage(R.string.localfile_cannot_handle).show(this::finish)
        }
    }

    private Unit continueWith(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        Log.iForce(LOGPRAEFIX + "continueWith:" + clazz)
        val forwarder: Intent = Intent(intent)
        forwarder.setClass(this, clazz)
        startActivity(forwarder)
        finish()
    }

    private Unit continueWithForegroundService(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        Log.iForce(LOGPRAEFIX + "continueWithForegroundService:" + clazz)
        val forwarder: Intent = Intent(intent)
        forwarder.setClass(this, clazz)
        ContextCompat.startForegroundService(this, forwarder)
        finish()
    }

    /**
     * copy uri to wherigo folder
     * <br />
     * For "guid calculation" of WherigoActivity,
     * - capitalization of suffix ".gwc" will be normalized to ".gwc"
     * - all "_" will be replaced by "-"
     * <br />
     * returns "guid" (= filename without suffix ".gwc") on success, null otherwise
     */
    private String copyToWherigoFolder(final Uri uri) {
        String filename = ContentStorage.get().getName(uri)
        Log.iForce(LOGPRAEFIX + "expect Wherigo, raw filename:" + filename)
        if (StringUtils.isBlank(filename)) {
            filename = FileNameCreator.WHERIGO.createName()
        }
        filename = filename.replace("_", "-")
        if (StringUtils.equalsAnyIgnoreCase(filename.substring(filename.length() - 4), ".gwc")) {
            filename = filename.substring(0, filename.length() - 4)
        }
        Log.iForce(LOGPRAEFIX + "Wherigo final filename:" + filename)
        ContentStorage.get().copy(uri, PersistableFolder.WHERIGO.getFolder(), FileNameCreator.forName(filename + ".gwc"), false)
        return filename
    }
}
