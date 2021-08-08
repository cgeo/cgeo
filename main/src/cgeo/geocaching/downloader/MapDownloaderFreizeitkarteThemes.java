package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.files.InvalidXMLCharacterFilterReader;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

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

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

public class MapDownloaderFreizeitkarteThemes extends AbstractThemeDownloader {
    private static final MapDownloaderFreizeitkarteThemes INSTANCE = new MapDownloaderFreizeitkarteThemes();

    private MapDownloaderFreizeitkarteThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_FREIZEITKARTE, R.string.mapserver_freizeitkarte_downloadurl, R.string.mapserver_freizeitkarte_themes_name, R.string.mapserver_freizeitkarte_themes_info, R.string.mapserver_freizeitkarte_projecturl, R.string.mapserver_freizeitkarte_likeiturl);
    }

    private static class FZKParser {
        // temporary data per entry
        private String url;
        private long size;
        private String description;

        private void parse(@NonNull final String page, final List<Download> result, final Download.DownloadType offlineMapType) {
            final RootElement root = new RootElement("", "Freizeitkarte");
            final Element theme = root.getChild("", "Theme");
            theme.setStartElementListener(attr -> {
                url = "";
                size = 0;
                description = "";
            });
            theme.getChild("", "Url").setEndTextElementListener(body -> url = body);
            theme.getChild("", "Size").setEndTextElementListener(body -> size = Long.parseLong(body));
            theme.getChild("", "DescriptionEnglish").setEndTextElementListener(body -> description = body);
            theme.setEndElementListener(() -> {
                if (StringUtils.isNotBlank(url)) {
                    result.add(new Download(description, Uri.parse(url), false, "", Formatter.formatBytes(size), offlineMapType, ICONRES_THEME));
                }
            });

            try {
                final BufferedReader reader = new BufferedReader(new StringReader(page));
                Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
            } catch (final SAXException | IOException e) {
                Log.e("Cannot parse freizeitkarte XML: " + e.getMessage());
            }
        }
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        new FZKParser().parse(page, list, offlineMapType);
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final List<Download> list = new ArrayList<>();
        new FZKParser().parse(page, list, offlineMapType);
        for (Download map : list) {
            if (map.getUri().getLastPathSegment().equals(remoteFilename)) {
                return map;
            }
        }
        return null;
    }

    // Freizeitkarte uses a repository, need to map here to its fixed address
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl);
    }

    @NonNull
    public static MapDownloaderFreizeitkarteThemes getInstance() {
        return INSTANCE;
    }

}
