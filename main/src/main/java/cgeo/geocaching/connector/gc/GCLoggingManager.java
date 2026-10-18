package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

class GCLoggingManager extends AbstractLoggingManager {

    private static final List<ReportProblemType> REPORT_PROBLEM_TYPES = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.LOG_WET, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER);

    GCLoggingManager(final Geocache cache) {
        super(GCConnector.getInstance(), cache);
    }

    @NonNull
    @Override
    public GCConnector getConnector() {
        return (GCConnector) super.getConnector();
    }

    @WorkerThread
    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        final LogContextInfo result = new LogContextInfo(this, serviceLogId);
        if (!Settings.hasGCCredentials()) { // allow offline logging
            result.addError(R.string.err_login);
            return result;
        }

        //New Log: https://www.geocaching.com/live/geocache/gcXZ/log
        //Existing log: https://www.geocaching.com/live/log/GLabc
        final String url = serviceLogId == null ? GCLogAPI.getUrlForNewLog(getCache().getGeocode()) : GCLogAPI.getUrlForEditLog(getCache().getGeocode(), serviceLogId);
        final String page;
        try {
            page = Network.getResponseData(Network.getRequest(url, null));
        } catch (final Exception e) {
            Log.w("HCLoggingManager: failed to retrieve log page data for '" + url + "'", e);
            result.setError();
            return result;
        }

        result.setAvailableLogTypes(GCParser.parseTypes(page));
        final int totalTrackables = GCParser.parseTrackableCount(page);
        // log page HTML contains up to 20 TBs
        if (totalTrackables <= 20) {
            result.setAvailableTrackables(GCParser.parseTrackables(page));
        } else {
            try {
                result.setAvailableTrackables(loadTrackablesPaged(totalTrackables));
            } catch (Exception e) {
                result.setError();
                Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e);
            }
        }

        // TODO: also parse ProblemLogTypes: logSettings.problemLogTypes.push(45);

        /* TODO: the GUID is not available in the new log page
        if (StringUtils.isBlank(cache.getGuid())) {
            // Acquire the cache GUID from the log page. This will not only complete the information in the database,
            // but also allow the user to post a rating using GCVote since it requires the GUID to do so.

            final String guid = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_GUID, null);
            if (StringUtils.isNotBlank(guid)) {
                cache.setGuid(guid);
                DataStore.saveChangedCache(cache);
            } else {
                Log.w("Could not acquire GUID from log page for " + cache.getGeocode());
            }
        }*/

        try {
            final GCLogin.ServerParameters params = GCLogin.getInstance().getServerParameters();
            final Integer premFavcount = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet();
            if (premFavcount != null && premFavcount >= 0) {
                result.setAvailableFavoritePoints(premFavcount);
            } else {
                result.setError();
            }
        } catch (final Exception e) {
            result.setError();
            Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e);
        }

        return result;
    }

    private List<Trackable> loadTrackablesPaged(final int totalTrackables) {
        final List<GCWebAPI.TrackableInventoryEntry> trackableInventoryItems = GCWebAPI.getTrackableInventory(totalTrackables);
        final List<Trackable> trackables = CollectionStream.of(trackableInventoryItems).map(entry -> {
            final Trackable trackable = new Trackable();
            trackable.setGeocode(entry.referenceCode);
            trackable.setTrackingcode(entry.trackingNumber);
            trackable.setName(entry.name);
            trackable.forceSetBrand(TrackableBrand.TRAVELBUG);
            return trackable;
            //new TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, TrackableBrand.TRAVELBUG)
        }).toList();
        return trackables;
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {

        final Geocache cache = getCache();

        try {
            return GCLogAPI.createLog(cache.getGeocode(), logEntry, inventory);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.createLog", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "GCLoggingManager.createLog", e);
        }
    }

    @NonNull
    @Override
    public LogResult editLog(@NonNull final LogEntry newEntry) {
        return GCLogAPI.editLog(getCache().getGeocode(), newEntry);
    }

    @NonNull
    @Override
    public LogResult deleteLog(@NonNull final LogEntry newEntry, @Nullable final String reason) {
        return GCLogAPI.deleteLog(newEntry.serviceLogId, reason);
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        if (!image.isEmpty() && image.getFile() != null) {
            return GCLogAPI.addLogImage(logId, image);
        }
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "No valid image:" + image, null);
    }

    @NonNull
    @Override
    public ImageResult editLogImage(@NonNull final String logId, @NonNull final String serviceImageId, @Nullable final String title, @Nullable final String description) {
        return GCLogAPI.editLogImageData(logId, serviceImageId, title, description);
    }

    @NonNull
    @Override
    public ImageResult deleteLogImage(@NonNull final String logId, @NonNull final String serviceImageId) {
        return GCLogAPI.deleteLogImage(logId, serviceImageId);
    }

    @Override
    public boolean supportsEditLogImages() {
        return true;
    }

    @Override
    public boolean supportsDeleteLogImages() {
        return true;
    }

    @Override
    public String convertLogTextToEditableText(final String logText) {
        if (!TextUtils.containsHtml(logText)) {
            return logText;
        }
        return htmlLogToEditableText(logText);
    }

    /**
     * Convert the HTML a log comes back as from geocaching.com into the editable markdown the user typed.
     * geocaching.com renders logs with CommonMark, where a heading needs a space after the '#'. The HTML
     * is walked as a jsoup DOM (robust against attributes, nesting and adjacency),
     * and each heading is emitted on its own line in GC's documented
     * "# text #" form with a blank line around it, so consecutive headings do not merge and the
     * edit -> send -> edit round-trip stays idempotent. (#16147)
     */
    static String htmlLogToEditableText(final String html) {
        final Document document = Jsoup.parseBodyFragment(html);
        final StringBuilder markdown = new StringBuilder();
        appendChildrenAsMarkdown(document.body(), markdown);
        return markdown.toString()
            .replaceAll("[ \\t]+\n", "\n") // no trailing whitespace on any line
            .replaceAll("\n{3,}", "\n\n")  // collapse runs of blank lines
            .trim();
    }

    private static void appendChildrenAsMarkdown(final Node parent, final StringBuilder markdown) {
        for (final Node child : parent.childNodes()) {
            appendNodeAsMarkdown(child, markdown);
        }
    }

    private static void appendNodeAsMarkdown(final Node node, final StringBuilder markdown) {
        if (node instanceof TextNode) {
            markdown.append(((TextNode) node).text());
            return;
        }
        if (!(node instanceof Element)) {
            return;
        }
        final Element element = (Element) node;
        switch (element.normalName()) {
            case "strong":
            case "b":
                appendInline(element, markdown, "**");
                break;
            case "em":
            case "i":
                appendInline(element, markdown, "*");
                break;
            case "a":
                markdown.append('[');
                appendChildrenAsMarkdown(element, markdown);
                markdown.append("](").append(element.attr("href")).append(')');
                break;
            case "h1":
                appendHeading(element, markdown, "#");
                break;
            case "h2":
                appendHeading(element, markdown, "##");
                break;
            case "h3":
                appendHeading(element, markdown, "###");
                break;
            case "hr":
                markdown.append("\n\n---\n\n");
                break;
            case "br":
                markdown.append('\n');
                break;
            case "blockquote":
                appendBlockquote(element, markdown);
                break;
            case "p":
                markdown.append("\n\n");
                appendChildrenAsMarkdown(element, markdown);
                markdown.append("\n\n");
                break;
            case "ul":
                appendList(element, markdown, false);
                break;
            case "ol":
                appendList(element, markdown, true);
                break;
            case "li":
                markdown.append("\n* ");
                appendChildrenAsMarkdown(element, markdown);
                break;
            default:
                appendChildrenAsMarkdown(element, markdown);
                break;
        }
    }

    private static void appendInline(final Element element, final StringBuilder markdown, final String marker) {
        markdown.append(marker);
        appendChildrenAsMarkdown(element, markdown);
        markdown.append(marker);
    }

    //each heading on its own line as "# text #" with a blank line around it, so GC keeps them separate (#16147)
    private static void appendHeading(final Element element, final StringBuilder markdown, final String hashes) {
        final StringBuilder inner = new StringBuilder();
        appendChildrenAsMarkdown(element, inner);
        markdown.append("\n\n").append(hashes).append(' ').append(inner.toString().trim()).append(' ').append(hashes).append("\n\n");
    }

    //blockquote back to CommonMark "> " lines (GC's editor "Quote" button), so the round-trip keeps the quote
    private static void appendBlockquote(final Element element, final StringBuilder markdown) {
        final StringBuilder inner = new StringBuilder();
        appendChildrenAsMarkdown(element, inner);
        final String quoted = inner.toString().trim()
            .replaceAll("\n{2,}", "\n")   // no blank lines inside the quote
            .replaceAll("(?m)^", "> ");   // prefix each line
        markdown.append("\n\n").append(quoted).append("\n\n");
    }

    //ol keeps its "1." numbering (ul uses "* ") so the ordered-list round-trip stays idempotent
    private static void appendList(final Element element, final StringBuilder markdown, final boolean ordered) {
        markdown.append("\n\n");
        int index = 1;
        for (final Node child : element.childNodes()) {
            if (child instanceof Element && "li".equals(((Element) child).normalName())) {
                markdown.append(ordered ? index + ". " : "* ");
                appendChildrenAsMarkdown(child, markdown);
                markdown.append('\n');
                index++;
            }
        }
        markdown.append('\n');
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }

    @Override
    public boolean supportsLogWithTrackables() {
        return true;
    }

    @Override
    public int getMaxImageDescriptionLength() {
        return 125;
    }

    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        if (geocache.isArchived() || geocache.isOwner()) {
            return Collections.emptyList();
        }
        final List<ReportProblemType> possibleReportProblemTypes = new ArrayList<>();
        for (final ReportProblemType reportProblem : REPORT_PROBLEM_TYPES) {
            if ((!geocache.isEventCache() && !geocache.isDisabled()) || reportProblem == ReportProblemType.ARCHIVE) {
                possibleReportProblemTypes.add(reportProblem);
            }
        }
        return possibleReportProblemTypes;
    }

}
