package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.brouter.BRouterConstants;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;

public class BRouterLookupsDownloader extends AbstractDownloader {

    private static final BRouterLookupsDownloader INSTANCE = new BRouterLookupsDownloader();


    private static final String GITHUB_API_BASE = "https://api.github.com/repos/";
    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/";
    private static final String BROUTER_REPO = "abrensch/brouter/";

    private static final String GITHUB_VERB_GET_LATEST_TAG = "releases/latest";
    private static final String GITHUB_RESPONSE_TAGNAME = "tag_name";
    private static final String GITHUB_VERB_GET_LATEST_COMMIT = "commits";
    private static final String GITHUB_PARAM_PATH = "path";
    private static final String GITHUB_VALUE_PATH = "/misc/profiles2/" + BRouterConstants.BROUTER_LOOKUPS_FILENAME;
    private static final String GITHUB_PARAM_RESULTSIZE = "per_page";
    private static final String GITHUB_PARAM_FILTER_FOR_TAG = "sha";
    private static final String GITHUB_RESPONSE_COMMIT_DATE = "/0/commit/author/date";

    private BRouterLookupsDownloader() {
        super(Download.DownloadType.DOWNLOADTYPE_BROUTER_LOOKUPS, Uri.parse(GITHUB_API_BASE + BROUTER_REPO + GITHUB_VERB_GET_LATEST_TAG), R.string.brouter_name, R.string.brouter_info, R.string.brouter_projecturl, 0, PersistableFolder.ROUTING_BASE);
        useCompanionFiles = false; // use single uri, and no companion files
        forceExtension = BRouterConstants.BROUTER_LOOKUPS_FILEEXTENSION;
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        Log.e("BRouterLookupsDownloader.analyzePage should never get called");
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        // retrieve timestamp of local file
        final ContentStorage.FileInformation fi = ContentStorage.get().getFileInfo(PersistableFolder.ROUTING_BASE.getFolder(), BRouterConstants.BROUTER_LOOKUPS_FILENAME);
        if (fi == null) {
            // should never happen (if routing got started at least once), as routing init ensure this file's existance
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();

        // analyze GitHub tag for latest release
        final String tag;
        try {
            final JsonNode node = mapper.readTree(page);
            tag = node.get(GITHUB_RESPONSE_TAGNAME).asText();
            Log.d("found tag: " + tag);
        } catch (JsonProcessingException e) {
            Log.e("BRouterLookupsDownloader.checkUpdateFor: parsing error (1): " + e.getMessage());
            return null;
        }

        // retrieve date of last commit for lookups.dat in that release
        final Parameters params = new Parameters();
        params.add(GITHUB_PARAM_PATH, GITHUB_VALUE_PATH);
        params.add(GITHUB_PARAM_RESULTSIZE, "1");
        params.add(GITHUB_PARAM_FILTER_FOR_TAG, tag);
        final Response response = Network.getRequest(GITHUB_API_BASE + BROUTER_REPO + GITHUB_VERB_GET_LATEST_COMMIT, params).blockingGet();
        final String page2 = Network.getResponseData(response, true);

        final String date;
        try {
            final JsonNode node = mapper.readTree(page2);
            date = node.at(GITHUB_RESPONSE_COMMIT_DATE).asText();
            Log.d("found date: " + date);
        } catch (JsonProcessingException e) {
            Log.e("BRouterLookupsDownloader.checkUpdateFor: parsing error (2): " + e.getMessage());
            return null;
        }

        // compare time stamps
        long onlineDateTime = 0;
        try {
            onlineDateTime = Instant.parse(date).getEpochSecond() * 1000;
        } catch (DateTimeParseException e) {
            Log.e("BRouterLookupsDownloader.checkUpdateFor: parsing error (3): " + e.getMessage());
        }
        if (onlineDateTime <= fi.lastModified) {
            return null;
        }
        return new Download(BRouterConstants.BROUTER_LOOKUPS_FILENAME, Uri.parse(GITHUB_RAW_BASE + BROUTER_REPO + "master" + GITHUB_VALUE_PATH), false, CalendarUtils.yearMonthDay(onlineDateTime), "", Download.DownloadType.DOWNLOADTYPE_BROUTER_LOOKUPS, R.drawable.ic_menu_route);
    }

    // BRouter uses a single download page, need to map here to its fixed address
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return mapBase.toString();
    }

    @NonNull
    public static BRouterLookupsDownloader getInstance() {
        return INSTANCE;
    }

}
