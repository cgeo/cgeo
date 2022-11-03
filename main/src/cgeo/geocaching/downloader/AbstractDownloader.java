package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractDownloader {
    public final Download.DownloadType offlineMapType;
    public Download.DownloadType companionType = null;
    public final Uri mapBase;
    public final String mapSourceName;
    public String mapSourceInfo;
    public final String projectUrl;
    public final String likeItUrl;
    public final PersistableFolder targetFolder;
    public static final String oneDirUp = CgeoApplication.getInstance().getString(R.string.downloadmap_onedirup);
    public String forceExtension = "";
    public boolean useCompanionFiles = true; // store source info (uri etc.) in companion files (true) or use date/timestamp and identical uri only (false)?
    @DrawableRes public int iconRes = R.drawable.ic_menu_save;

    public static final int ICONRES_FOLDER = R.drawable.downloader_folder;

    AbstractDownloader(final Download.DownloadType offlineMapType, final @StringRes int mapBase, final @StringRes int mapSourceName, final @StringRes int mapSourceInfo, final @StringRes int projectUrl, final @StringRes int likeItUrl, final PersistableFolder targetFolder) {
        this.offlineMapType = offlineMapType;
        this.mapBase = mapBase == 0 ? Uri.parse("") : Uri.parse(CgeoApplication.getInstance().getString(mapBase));
        this.mapSourceName = mapSourceName == 0 ? "" : CgeoApplication.getInstance().getString(mapSourceName);
        this.mapSourceInfo = mapSourceInfo == 0 ? "" : CgeoApplication.getInstance().getString(mapSourceInfo);
        this.projectUrl = projectUrl == 0 ? "" : CgeoApplication.getInstance().getString(projectUrl);
        if (projectUrl != 0) {
            this.mapSourceInfo += (mapSourceInfo != 0 ? "\n" : "") + "(" + this.projectUrl + ")";
        }
        this.likeItUrl = likeItUrl == 0 ? "" : CgeoApplication.getInstance().getString(likeItUrl);
        this.targetFolder = targetFolder;
    }

    // find available maps, dir-up, subdirs
    protected abstract void analyzePage(Uri uri, List<Download> list, @NonNull String page);

    // find source for single map
    @Nullable
    protected abstract Download checkUpdateFor(@NonNull String page, String remoteUrl, String remoteFilename);

    // create update check page url for download page url
    // default is: identical
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return downloadPageUrl;
    }

    // generic matchers
    protected void basicUpMatcher(final Uri uri, final List<Download> list, final @NonNull String page, final Pattern patternUp) {
        if (!mapBase.equals(uri)) {
            final MatcherWrapper matchUp = new MatcherWrapper(patternUp, page);
            if (matchUp.find()) {
                final String oneUp = uri.toString();
                final int endOfPreviousSegment = oneUp.lastIndexOf("/", oneUp.length() - 2); // skip trailing "/"
                if (endOfPreviousSegment > -1) {
                    final Download offlineMap = new Download(oneDirUp, Uri.parse(oneUp.substring(0, endOfPreviousSegment + 1)), true, "", "", offlineMapType, ICONRES_FOLDER);
                    list.add(offlineMap);
                }
            }
        }
    }

    // do any cleanup on filename?
    protected String toVisibleFilename(final String filename) {
        return filename;
    }

    // infix a certain string into the filename?
    protected String toInfixedString(final String filename, final String infix) {
        if (StringUtils.isNotBlank(infix)) {
            if (StringUtils.isNotBlank(forceExtension)) {
                final int extPos = filename.indexOf(forceExtension);
                return extPos == -1 ? filename + infix : filename.substring(0, extPos) + infix + forceExtension;
            } else {
                return filename + infix;
            }
        } else {
            return filename;
        }
    }

    // extra file to download?
    public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity) {
        return null;
    }

    // default action to be started after having received and copied the downloaded file successfully
    protected void onSuccessfulReceive(final Uri result) {
        // default: nothing to do
    }

}
