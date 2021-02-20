package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.StringRes;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractDownloader {
    public OfflineMap.OfflineMapType offlineMapType;
    public Uri mapBase;
    public String mapSourceName;
    public String mapSourceInfo;
    public String projectUrl;
    public String likeItUrl;
    public PersistableFolder targetFolder;
    public static final String oneDirUp = CgeoApplication.getInstance().getString(R.string.downloadmap_onedirup);
    public String forceExtension = "";

    AbstractDownloader(final OfflineMap.OfflineMapType offlineMapType, final @StringRes int mapBase, final @StringRes int mapSourceName, final @StringRes int mapSourceInfo, final @StringRes int projectUrl, final @StringRes int likeItUrl, final PersistableFolder targetFolder) {
        this.offlineMapType = offlineMapType;
        this.mapBase = Uri.parse(CgeoApplication.getInstance().getString(mapBase));
        this.mapSourceName = CgeoApplication.getInstance().getString(mapSourceName);
        this.mapSourceInfo = mapSourceInfo == 0 ? "" : CgeoApplication.getInstance().getString(mapSourceInfo);
        this.projectUrl = projectUrl == 0 ? "" : CgeoApplication.getInstance().getString(projectUrl);
        if (projectUrl != 0) {
            this.mapSourceInfo += (mapSourceInfo != 0 ? "\n" : "") + "(" + this.projectUrl + ")";
        }
        this.likeItUrl = likeItUrl == 0 ? "" : CgeoApplication.getInstance().getString(likeItUrl);
        this.targetFolder = targetFolder;
    }

    // find available maps, dir-up, subdirs
    protected abstract void analyzePage(Uri uri, List<OfflineMap> list, String page);

    // find source for single map
    protected abstract OfflineMap checkUpdateFor(String page, String remoteUrl, String remoteFilename);

    // create update check page url for download page url
    // default is: identical
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return downloadPageUrl;
    }

    // generic matchers
    protected void basicUpMatcher(final Uri uri, final List<OfflineMap> list, final String page, final Pattern patternUp) {
        if (!mapBase.equals(uri)) {
            final MatcherWrapper matchUp = new MatcherWrapper(patternUp, page);
            if (matchUp.find()) {
                final String oneUp = uri.toString();
                final int endOfPreviousSegment = oneUp.lastIndexOf("/", oneUp.length() - 2); // skip trailing "/"
                if (endOfPreviousSegment > -1) {
                    final OfflineMap offlineMap = new OfflineMap(oneDirUp, Uri.parse(oneUp.substring(0, endOfPreviousSegment + 1)), true, "", "", offlineMapType);
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

    // default action to be started after having received and copied the downloaded file successfully
    protected void onSuccessfulReceive(final Uri result) {
        // default: nothing to do
    }

    // default followup action on UI thread after having received and copied the downloaded file successfully
    protected void onFollowup(final Activity activity, final Runnable callback) {
        // default: just continue with callback
        callback.run();
    }

}
