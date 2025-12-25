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

package cgeo.geocaching.downloader

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.models.Download
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.MatcherWrapper

import android.app.Activity
import android.net.Uri

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.List
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

abstract class AbstractDownloader {
    public final Download.DownloadType offlineMapType
    public Download.DownloadType companionType = null
    public final Uri mapBase
    public final String mapSourceName
    public String mapSourceInfo
    public final String projectUrl
    public final String likeItUrl
    public final PersistableFolder targetFolder
    var forceExtension: String = ""
    var useCompanionFiles: Boolean = true; // store source info (uri etc.) in companion files (true) or use date/timestamp and identical uri only (false)?
    var downloadHasExtraContents: Boolean = false; // some sources download a zip file containing additional files worthwhile keeping (though not used by c:geo); those zips can be kept using a setting under map => sources
    @DrawableRes var iconRes: Int = R.drawable.ic_menu_save

    public static val ICONRES_FOLDER: Int = R.drawable.downloader_folder

    AbstractDownloader(final Download.DownloadType offlineMapType, final @StringRes Int mapBase, final @StringRes Int mapSourceName, final @StringRes Int mapSourceInfo, final @StringRes Int projectUrl, final @StringRes Int likeItUrl, final PersistableFolder targetFolder) {
        this.offlineMapType = offlineMapType
        this.mapBase = mapBase == 0 ? Uri.parse("") : Uri.parse(CgeoApplication.getInstance().getString(mapBase))
        this.mapSourceName = mapSourceName == 0 ? "" : CgeoApplication.getInstance().getString(mapSourceName)
        this.mapSourceInfo = mapSourceInfo == 0 ? "" : CgeoApplication.getInstance().getString(mapSourceInfo)
        this.projectUrl = projectUrl == 0 ? "" : CgeoApplication.getInstance().getString(projectUrl)
        if (projectUrl != 0) {
            this.mapSourceInfo += (mapSourceInfo != 0 ? "\n" : "") + "(" + this.projectUrl + ")"
        }
        this.likeItUrl = likeItUrl == 0 ? "" : CgeoApplication.getInstance().getString(likeItUrl)
        this.targetFolder = targetFolder
    }

    // find available maps, dir-up, subdirs
    protected abstract Unit analyzePage(Uri uri, List<Download> list, String page)

    // find source for single map
    protected abstract Download checkUpdateFor(String page, String remoteUrl, String remoteFilename)

    // create update check page url for download page url
    // default is: identical
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return downloadPageUrl
    }

    // generic matchers
    protected Unit basicUpMatcher(final Uri uri, final List<Download> list, final String page, final Pattern patternUp) {
        if (!mapBase == (uri)) {
            val matchUp: MatcherWrapper = MatcherWrapper(patternUp, page)
            if (matchUp.find()) {
                val oneUp: String = uri.toString()
                val endOfPreviousSegment: Int = oneUp.lastIndexOf("/", oneUp.length() - 2); // skip trailing "/"
                if (endOfPreviousSegment > -1) {
                    val offlineMap: Download = Download(Uri.parse(oneUp.substring(0, endOfPreviousSegment + 1)), offlineMapType)
                    list.add(offlineMap)
                }
            }
        }
    }

    // do any cleanup on filename?
    protected String toVisibleFilename(final String filename) {
        return filename
    }

    // infix a certain string into the filename?
    protected String toInfixedString(final String filename, final String infix) {
        if (StringUtils.isNotBlank(infix)) {
            if (StringUtils.isNotBlank(forceExtension)) {
                val extPos: Int = filename.indexOf(forceExtension)
                return extPos == -1 ? filename + infix : filename.substring(0, extPos) + infix + forceExtension
            } else {
                return filename + infix
            }
        } else {
            return filename
        }
    }

    // extra file to download?
    public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity, final Uri mapUri) {
        return null
    }

    // do some integrity check after file download (and before copying it to final location)
    // true = integrity ok (or untested), false = error (file will be deleted automatically)
    protected Boolean verifiedBeforeCopying(final String filename, final Uri result) {
        return true
    }

    // similar to verifiedBeforeCopying, but after having copied (and probably ZIP-extracted) the file
    protected Boolean verifiedAfterCopying(final String filename, final Uri result) {
        return true
    }

    // default action to be started after having received and copied the downloaded file successfully
    protected Unit onSuccessfulReceive(final Uri result) {
        // default: nothing to do
    }

}
