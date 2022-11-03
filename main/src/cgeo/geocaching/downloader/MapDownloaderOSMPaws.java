package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.files.InvalidXMLCharacterFilterReader;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.net.Uri;
import android.sax.Element;
import android.sax.RootElement;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

public class MapDownloaderOSMPaws extends AbstractMapDownloader {

    private static final String[] THEME_FILES = {"paws_4.zip"};
    private static final MapDownloaderOSMPaws INSTANCE = new MapDownloaderOSMPaws();

    private MapDownloaderOSMPaws() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_PAWS, R.string.mapserver_osmpaws_downloadurl, R.string.mapserver_osmpaws_name, R.string.mapserver_osmpaws_info, R.string.mapserver_osmpaws_projecturl, R.string.mapserver_osmpaws_likeiturl);
        companionType = Download.DownloadType.DOWNLOADTYPE_THEME_PAWS;
    }

    private static class OSMPawsParser {
        // temporary data per entry
        private String url;
        private long size;
        private String description;
        private String dateInfo;

        private void parse(@NonNull final String page, final List<Download> result, final Download.DownloadType offlineMapType) {
            final RootElement root = new RootElement("", "channel");
            final Element map = root.getChild("", "map");
            map.setStartElementListener(attr -> {
                url = "";
                size = 0;
                description = "";
                dateInfo = "";
            });
            map.getChild("", "link").setEndTextElementListener(body -> url = body);
            map.getChild("", "size").setEndTextElementListener(body -> size = Long.parseLong(body));
            map.getChild("", "title").setEndTextElementListener(body -> description = body);
            map.getChild("", "date").setEndTextElementListener(body -> dateInfo = body);
            map.setEndElementListener(() -> {
                if (StringUtils.isNotBlank(url) && StringUtils.isNotBlank(dateInfo)) {
                    result.add(new Download(description, Uri.parse(url), false, dateInfo.substring(0, 10), Formatter.formatBytes(size), offlineMapType, ICONRES_MAP));
                }
            });

            try {
                final BufferedReader reader = new BufferedReader(new StringReader(page));
                Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            } catch (final SAXException | IOException e) {
                Log.e("Cannot parse paws XML: " + e.getMessage());
            }
        }
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        new OSMPawsParser().parse(page, list, offlineMapType);
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final List<Download> list = new ArrayList<>();
        new OSMPawsParser().parse(page, list, offlineMapType);
        for (Download map : list) {
            if (map.getUri().getLastPathSegment().equals(remoteFilename)) {
                return map;
            }
        }
        return null;
    }

    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_osmpaws_downloadurl);
    }

    @Override
    protected String toVisibleFilename(final String filename) {
        final int posInfix = filename.indexOf(".map");
        return toInfixedString(posInfix == -1 ? filename : filename.substring(0, posInfix), "");
    }

    @Override
    public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity) {
        return getExtrafile(THEME_FILES, activity.getString(R.string.mapserver_osmpaws_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_PAWS);
    }

    @NonNull
    public static MapDownloaderOSMPaws getInstance() {
        return INSTANCE;
    }

}
