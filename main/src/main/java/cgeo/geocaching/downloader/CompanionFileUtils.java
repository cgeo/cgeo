package cgeo.geocaching.downloader;

import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class CompanionFileUtils {

    public static final String INFOFILE_SUFFIX = "-cgeo.txt";

    private static final String PROP_PARSETYPE = "remote.parsetype";
    private static final String PROP_REMOTEPAGE = "remote.page";
    private static final String PROP_REMOTEFILE = "remote.file";
    private static final String PROP_REMOTEDATE = "remote.date";
    private static final String PROP_LOCALFILE = "local.file";
    private static final String PROP_DISPLAYNAME = "displayname";

    public static class DownloadedFileData {
        public int remoteParsetype;         // see OfflineMapType.id
        public String remotePage;           // eg: http://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/europe/germany/
        public String remoteFile;           // eg: hessen.map
        public long remoteDate;             // 2020-12-10 (as long)
        public String localFile;            // eg: map43.map (relative to map dir)
        public String displayName;          // eg: Hessen
    }

    private CompanionFileUtils() {
        // utility class
    }

    public static void writeInfo(@NonNull final String remoteUrl, @NonNull final String localFilename, @NonNull final String displayName, final long date, final int offlineMapTypeId) {
        final AbstractDownloader downloader = Download.DownloadType.getInstance(offlineMapTypeId);
        if (downloader.useCompanionFiles) {
            final Uri infoFile = ContentStorage.get().create(downloader.targetFolder, localFilename + INFOFILE_SUFFIX);
            try (OutputStream output = ContentStorage.get().openForWrite(infoFile)) {
                final int i = remoteUrl.lastIndexOf("/");
                final String remotePage = i != -1 ? remoteUrl.substring(0, i) : remoteUrl;
                final String remoteFile = i != -1 ? remoteUrl.substring(i + 1) : localFilename;

                final Properties prop = new Properties();
                prop.setProperty(PROP_PARSETYPE, String.valueOf(offlineMapTypeId));
                prop.setProperty(PROP_REMOTEPAGE, remotePage);
                prop.setProperty(PROP_REMOTEFILE, remoteFile);
                prop.setProperty(PROP_REMOTEDATE, CalendarUtils.yearMonthDay(date));
                prop.setProperty(PROP_LOCALFILE, localFilename);
                prop.setProperty(PROP_DISPLAYNAME, displayName);

                prop.store(output, null);
            } catch (IOException io) {
                // ignore
            }
        }
    }

    /**
     * returns a list of downloaded offline map related files from all sources which are still available in the local filesystem
     */
    public static ArrayList<DownloadedFileData> availableOfflineMapRelatedFiles() {
        final ArrayList<DownloadedFileData> result = new ArrayList<>();
        for (Download.DownloadTypeDescriptor type : Download.DownloadType.getOfflineAllMapRelatedTypes()) {
            result.addAll(availableOfflineMaps(type.type));
        }
        return result;
    }

    /**
     * returns a list of downloaded offline files from requested source which are still available in the local filesystem
     */
    public static ArrayList<DownloadedFileData> availableOfflineMaps(@NonNull final Download.DownloadType filter) {
        final ArrayList<DownloadedFileData> result = new ArrayList<>();
        final AbstractDownloader downloader = Download.DownloadType.getInstance(filter.id);

        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(downloader.targetFolder);
        final Map<String, Uri> filesMap = new HashMap<>();
        for (ContentStorage.FileInformation fi : files) {
            filesMap.put(fi.name, fi.uri);
        }

        if (downloader.useCompanionFiles) {
            for (ContentStorage.FileInformation fi : files) {
                final String filename = fi.name;
                if (!filename.endsWith(INFOFILE_SUFFIX)) {
                    continue;
                }
                try (InputStream input = ContentStorage.get().openForRead(downloader.targetFolder.getFolder(), filename)) {
                    final DownloadedFileData offlineMapData = new DownloadedFileData();
                    final Properties prop = new Properties();
                    prop.load(input);
                    offlineMapData.remoteParsetype = Integer.parseInt(prop.getProperty(PROP_PARSETYPE));
                    if (offlineMapData.remoteParsetype == filter.id) {
                        offlineMapData.remotePage = prop.getProperty(PROP_REMOTEPAGE);
                        offlineMapData.remoteFile = prop.getProperty(PROP_REMOTEFILE);
                        offlineMapData.remoteDate = CalendarUtils.parseYearMonthDay(prop.getProperty(PROP_REMOTEDATE));
                        offlineMapData.localFile = prop.getProperty(PROP_LOCALFILE);
                        offlineMapData.displayName = prop.getProperty(PROP_DISPLAYNAME);

                        if (StringUtils.isNotBlank(offlineMapData.localFile) && filesMap.containsKey(offlineMapData.localFile)) {
                            result.add(offlineMapData);
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    Log.d("Offline map property file error for " + filename + ": " + e.getMessage());
                }
            }
        } else {
            for (ContentStorage.FileInformation fi : files) {
                if (!fi.name.endsWith(INFOFILE_SUFFIX) && fi.name.endsWith(downloader.forceExtension)) {
                    final DownloadedFileData download = new DownloadedFileData();
                    download.remoteParsetype = filter.id;
                    download.remoteDate = fi.lastModified;
                    download.remoteFile = fi.name;
                    download.localFile = fi.name;
                    download.displayName = fi.name;
                    // some properties remain unset when not using companion files (remotePage), others are guesses only (remoteFile, remoteParseType)
                    result.add(download);
                }
            }
        }
        return result;
    }

    public static Uri companionFileExists(final List<ContentStorage.FileInformation> files, final String filename) {
        final String lookFor = filename + INFOFILE_SUFFIX;
        for (ContentStorage.FileInformation fi : files) {
            if (fi.name.equals(lookFor)) {
                return fi.uri;
            }
        }
        return null;
    }

    public static String getDisplayName(final String name) {
        // capitalize first letter + every first after a "-"
        String tempName = StringUtils.upperCase(name.substring(0, 1)) + name.substring(1);
        int pos = name.indexOf("-");
        while (pos > 0) {
            tempName = tempName.substring(0, pos + 1) + StringUtils.upperCase(tempName.substring(pos + 1, pos + 2)) + tempName.substring(pos + 2);
            pos = name.indexOf("-", pos + 1);
        }
        return tempName;
    }

}
