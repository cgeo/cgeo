package cgeo.geocaching.connector.wm;

import android.util.Pair;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;

public final class WMParser {


    private static final Pattern PATTERN_FINDS_DISTINCT = Pattern.compile("\\(\\D*(\\d+)\\D*\\)");
    private static final Pattern PATTERN_FINDS_TOTAL = Pattern.compile("<strong>(\\d+)</strong>");
    private static final Pattern PATTERN_GUID = Pattern.compile("guid=([0-9a-z\\-]+)");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

    public static String getUsername(final String loginHtml) {
        if (StringUtils.isBlank(loginHtml)) {
            Log.w("WMParser: could not retrieve username (blank)");
            return "";
        }

        if (!loginHtml.contains("ctl00_ContentBody_lbMessageText")) {
            Log.w("WMParser: could not retrieve username (element not found)");
            Log.w(loginHtml);
            return "";
        }

        final Document document = Jsoup.parse(loginHtml);
        return document.select("#ctl00_ContentBody_lbMessageText strong").text();
    }

    public static int getFindsCount(final String statsHtml) {
        if (StringUtils.isBlank(statsHtml)) {
            Log.w("WMParser: could not retrieve waymarking visits data (blank)");
            return -1;
        }

        if (!statsHtml.contains("<div id=\"BasicFinds\"")) {
            if (statsHtml.contains("<span id=\"ctl00_ContentBody_uxChronologyMessage\"")) {
                return 0; // no finds
            }

            Log.w("WMParser: could not retrieve waymarking visits data (parsing error)");
            return -1;
        }

        final Document document = Jsoup.parse(statsHtml);
        final String value = document.select("div#BasicFinds").html();

        final Matcher distinctMatcher = PATTERN_FINDS_DISTINCT.matcher(value);
        if (distinctMatcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(distinctMatcher.group(1)));
        }

        final Matcher strongMatcher = PATTERN_FINDS_TOTAL.matcher(value);
        if (strongMatcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(strongMatcher.group(1)));
        }

        Log.w("WMParser: could not retrieve waymarking visits data (no match) in " + value);
        return -1;
    }

    public static Pair<Geocache, Integer> parseCoreWaymark(final String listingHtml) {
        if (StringUtils.isBlank(listingHtml)) {
            Log.w("WMParser: could not retrieve waymark (blank)");
            return null;
        }

        final Document waymarkDocument = Jsoup.parse(listingHtml);

        // TODO: I dont know if any waymarks might not have these details

        final Geocache cache = new Geocache();
        final Element nameElement = waymarkDocument.select("#wm_name").first();
        final String imageLink = waymarkDocument.select(".wm_photo img").first().attr("src");
        final Element ownerElement = waymarkDocument.select("#wm_postedby a").last();
        final Elements linkOptionElements = waymarkDocument.select("li.category_linkoption");
        Element galleryElement = null;
        for (final Element linkOptionElement : linkOptionElements) {
            final Element a = linkOptionElement.select("a").first();
            if (a.attr("href").contains("gallery")) {
                galleryElement = a;
                break;
            }
        }
        final int imagesCount = Integer.parseInt(TextUtils.getMatch(galleryElement.text(), PATTERN_NUMBER, false, 0, "", false));

        cache.setGeocode(waymarkDocument.select("#wm_code strong").first().nextSibling().toString().trim());
        cache.setCacheId(TextUtils.getMatch(galleryElement.attr("href"), PATTERN_GUID, false, 1, "", false));
        cache.setName(nameElement.select("img").first() != null ? nameElement.select("img").first().nextSibling().toString().trim() : nameElement.text());
        cache.setShortDescription(waymarkDocument.select("#wm_quickdesc").html() + "<p>");
        cache.setDescription("<img src=\"" + imageLink + "\"></img><p><p>" +
                waymarkDocument.select("#wm_longdesc").html() + "<p><hr><p>" +
                waymarkDocument.select("#wm_variables").html().replaceAll("<img[^>]*?images/spacer.gif[^>]*?>", "<p>") + "<p><hr><p>" +
                waymarkDocument.select("#wm_loginstructions").html());
        cache.setCoords(new Geopoint(waymarkDocument.select("#wm_coordinates").text()));
        cache.setType(CacheType.WAYMARK);
        cache.setSize(CacheSize.VIRTUAL);
        cache.setDisabled(false);
        cache.setArchived(!listingHtml.contains("/logs/add.aspx"));
        cache.setLocation(waymarkDocument.select("#wm_location strong").first().nextSibling().toString().trim());
        cache.setHidden(parseDate(waymarkDocument.select("#wm_datelisted strong").first().nextSibling().toString().trim()));
        cache.setOwnerDisplayName(ownerElement.text());
        cache.setOwnerUserId(ownerElement.text());
        cache.setOwnerGuid(TextUtils.getMatch(ownerElement.attr("href"), PATTERN_GUID, false, 1, "", false));
        cache.setPremiumMembersOnly(false);
        cache.setUserModifiedCoords(false);
        cache.setDetailedUpdatedNow();

        return Pair.create(cache, imagesCount);
    }

    public static List<String> getImagePageUris(final String galleryHtml) {
        if (StringUtils.isBlank(galleryHtml)) {
            Log.w("WMParser: could not retrieve waymarking images (blank)");
            return List.of();
        }

        final Document galleryDocument = Jsoup.parse(galleryHtml);
        final Elements images = galleryDocument.select("#gallerycontrol li");
        if (images.isEmpty()) {
            Log.w("WMParser: waymarking images list is empty");
            return List.of();
        }

        final List<String> result = new ArrayList<>();
        for (final Element image : images) {
            final String imageUrl = image.select("a").first().attr("href");
            result.add(WMConnector.getInstance().getHostUrl() + imageUrl);
        }

        return result;
    }

    public static Image parseImage(final String imagePageHtml) {
        if (StringUtils.isBlank(imagePageHtml)) {
            Log.w("WMParser: could not retrieve gallery image (blank)");
            return null;
        }

        final Document imagePageDocument = Jsoup.parse(imagePageHtml);
        final String imageUri = imagePageDocument.select("#largephoto img").attr("src").toString();

        return new Image.Builder().setUrl(imageUri).setCategory(Image.ImageCategory.LISTING).build();
    }

    @NonNull
    private static Date parseDate(final String date) {
        final SynchronizedDateFormat dateFormat = new SynchronizedDateFormat("dd.MM.yyyy", Locale.getDefault());
        try {
            return dateFormat.parse(date);
        } catch (final ParseException e) {
            return new Date(0);
        }
    }
}
