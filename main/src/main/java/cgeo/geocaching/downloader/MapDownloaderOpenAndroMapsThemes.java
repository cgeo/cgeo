package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MapDownloaderOpenAndroMapsThemes extends AbstractThemeDownloader {

    protected static final String FILENAME_VOLUNTARY = "Voluntary MF5.zip";
    private static final Pattern PATTERN_LAST_UPDATED_DATE_ELEVATE = Pattern.compile("<a href=\"https:\\/\\/www\\.openandromaps\\.org\\/wp-content\\/users\\/tobias\\/version\\.txt\">[0-9]\\.[0-9](\\.[0-9])?<\\/a><\\/strong>, ([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{2}) ");
    private static final Pattern PATTERN_LAST_UPDATED_DATE_VOLUNTARY = Pattern.compile(FILENAME_VOLUNTARY + "<\\/a>\\s*([0-9]{1,2})-([A-Za-z]{3})-([0-9]{4})");
    private static final MapDownloaderOpenAndroMapsThemes INSTANCE = new MapDownloaderOpenAndroMapsThemes();

    private MapDownloaderOpenAndroMapsThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS, R.string.mapserver_openandromaps_themes_updatecheckurl, R.string.mapserver_openandromaps_themes_name, R.string.mapserver_openandromaps_themes_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        final Download file = checkUpdateFor(page, CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl), "Elevate.zip");
        if (file != null) {
            list.add(file);
        }

        // small hack to support a second theme from a different location
        final String urlVoluntary = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_voluntary_downloadurl);
        String pageVoluntary = null;
        try {
            final Response response = Network.getRequest(urlVoluntary, new Parameters()).blockingGet();
            pageVoluntary = Network.getResponseData(response, true);
        } catch (final Exception ignore) {
            // ignore
        }
        if (StringUtils.isNotBlank(pageVoluntary)) {
            final Download fileVoluntary = checkUpdateFor(pageVoluntary, urlVoluntary, FILENAME_VOLUNTARY);
            if (fileVoluntary != null) {
                list.add(fileVoluntary);
            }
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        // check for elevate
        final MatcherWrapper matchDate = new MatcherWrapper(PATTERN_LAST_UPDATED_DATE_ELEVATE, page);
        if (matchDate.find()) {
            final String date = "20" + matchDate.group(4) + "-" + matchDate.group(3) + "-" + matchDate.group(2);
            return new Download("Elevate", Uri.parse(CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl) + "Elevate.zip"), false, date, "", offlineMapType, iconRes);
        }
        // check for voluntary
        final MatcherWrapper matchDateVoluntary = new MatcherWrapper(PATTERN_LAST_UPDATED_DATE_VOLUNTARY, page);
        if (matchDateVoluntary.find()) {
            String date = matchDateVoluntary.group(3) + "-" + matchDateVoluntary.group(2) + "-" + matchDateVoluntary.group(1);
            try {
                // convert date returned by server into ISO format
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd", Locale.ENGLISH);
                date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Objects.requireNonNull(formatter.parse(date)));
            } catch (Exception ignore) {
                // ignore
            }
            return new Download("Voluntary", Uri.parse(CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_voluntary_downloadurl) + FILENAME_VOLUNTARY), false, date, "", offlineMapType, iconRes);
        }
        // neither found
        return null;
    }

    // elevate uses different servers for update check and download, need to map here
    // no mapping needed for voluntary theme
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        final String compare = downloadPageUrl.endsWith("/") ? downloadPageUrl : downloadPageUrl + "/";
        final String downloadUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl);
        final String updateUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_updatecheckurl);
        if (compare.startsWith(downloadUrl)) {
            final String result = updateUrl + compare.substring(downloadUrl.length());
            return result.endsWith("/") ? result : result + "/";
        }
        return downloadPageUrl;
    }

    @NonNull
    public static MapDownloaderOpenAndroMapsThemes getInstance() {
        return INSTANCE;
    }
}
