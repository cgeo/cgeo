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
import cgeo.geocaching.files.InvalidXMLCharacterFilterReader
import cgeo.geocaching.models.Download
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.net.Uri
import android.sax.Element
import android.sax.RootElement
import android.util.Xml

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList
import java.util.List
import java.util.Locale

import org.apache.commons.lang3.StringUtils
import org.xml.sax.SAXException

class MapDownloaderFreizeitkarte : AbstractMapDownloader() {

    private static final String[] THEME_FILES = {"freizeitkarte-v5.zip", "fzk-outdoor-contrast-v5.zip", "fzk-outdoor-soft-v5.zip"}
    private static val INSTANCE: MapDownloaderFreizeitkarte = MapDownloaderFreizeitkarte()

    private MapDownloaderFreizeitkarte() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_FREIZEITKARTE, R.string.mapserver_freizeitkarte_downloadurl, R.string.mapserver_freizeitkarte_name, R.string.mapserver_freizeitkarte_info, R.string.mapserver_freizeitkarte_projecturl, R.string.mapserver_freizeitkarte_likeiturl)
        companionType = Download.DownloadType.DOWNLOADTYPE_THEME_FREIZEITKARTE
    }

    private static class FZKParser {
        // temporary data per entry
        private String url
        private Long size
        private String description
        private String dateInfo

        private Unit parse(final String page, final List<Download> result, final Download.DownloadType offlineMapType) {
            val root: RootElement = RootElement("", "Freizeitkarte")
            val map: Element = root.getChild("", "Map")
            map.setStartElementListener(attr -> {
                url = ""
                size = 0
                description = ""
                dateInfo = ""
            })
            map.getChild("", "Url").setEndTextElementListener(body -> url = body.replaceAll("\\/[0-9]{4}\\/", "/latest/"))
            map.getChild("", "Size").setEndTextElementListener(body -> size = Long.parseLong(body))
            map.getChild("", "DescriptionEnglish").setEndTextElementListener(body -> description = body)
            map.getChild("", "MapsforgeDateOfCreation").setEndTextElementListener(body -> dateInfo = body)
            map.setEndElementListener(() -> {
                if (StringUtils.isNotBlank(url) && StringUtils.isNotBlank(dateInfo) && size > 0) {
                    result.add(Download(description, Uri.parse(url), false, dateInfo.substring(0, 10), Formatter.formatBytes(size), offlineMapType, ICONRES_MAP))
                }
            })

            try {
                val reader: BufferedReader = BufferedReader(StringReader(page))
                Xml.parse(InvalidXMLCharacterFilterReader(reader), root.getContentHandler())
            } catch (final SAXException | IOException e) {
                Log.e("Cannot parse freizeitkarte XML: " + e.getMessage())
            }
        }
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        FZKParser().parse(page, list, offlineMapType)
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        val list: List<Download> = ArrayList<>()
        FZKParser().parse(page, list, offlineMapType)
        for (Download map : list) {
            if (map.getUri().getLastPathSegment() == (remoteFilename)) {
                return map
            }
        }
        return null
    }

    // Freizeitkarte uses a repository, need to map here to its fixed address
    override     protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_freizeitkarte_downloadurl)
    }

    override     protected String toVisibleFilename(final String filename) {
        if (Settings.getMapDownloaderAutoRename()) {
            return toInfixedString(CompanionFileUtils.getDisplayName((filename.startsWith("Freizeitkarte_") ? filename.substring(14) : filename).toLowerCase(Locale.getDefault())), " (FZK)")
        }
        return filename
    }

    override     public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity, final Uri mapUri) {
        return getExtrafile(THEME_FILES, activity.getString(R.string.mapserver_freizeitkarte_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_FREIZEITKARTE)
    }

    public static MapDownloaderFreizeitkarte getInstance() {
        return INSTANCE
    }

}
