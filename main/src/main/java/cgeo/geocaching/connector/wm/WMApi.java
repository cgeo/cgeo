package cgeo.geocaching.connector.wm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;
import java.util.regex.Pattern;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.NetworkUtils;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import cgeo.geocaching.utils.TextUtils;

final class WMApi {
    private static final String WAYMARK_URI = "https://www.waymarking.com/waymarks/%s";

    private static final Pattern PATTERN_OWNER_GUID = Pattern.compile("guid=([0-9a-z\\-]+)");

    @Nullable
    public static Geocache searchByGeocode(@NonNull final String geocode) {
        try {
            final String waymarkHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(String.format(WAYMARK_URI, geocode)).blockingGet());
            final Document waymarkDocument = Jsoup.parse(waymarkHtml);

            // TODO: I dont know if any waymarks might not have these details

            final Geocache cache = new Geocache();
            final Element nameElement = waymarkDocument.select("#wm_name").first();
            final String imageLink = waymarkDocument.select(".wm_photo img").first().attr("src");
            final Element ownerElement = waymarkDocument.select("#wm_postedby a").last();

            cache.setGeocode(waymarkDocument.select("#wm_code strong").first().nextSibling().toString().trim());
            cache.setName(nameElement.select("img").first() != null ? nameElement.select("img").first().nextSibling().toString().trim() : nameElement.text());
            cache.setShortDescription(waymarkDocument.select("#wm_quickdesc").html() + "<p><hr><p>");
            cache.setDescription("<img src=\"" + imageLink + "\"></img><p><p>" +
                    waymarkDocument.select("#wm_longdesc").html() + "<p><hr><p>" +
                    waymarkDocument.select("#wm_variables").html() + "<p><hr><p>" +
                    waymarkDocument.select("#wm_loginstructions").html());
            cache.setCoords(new Geopoint(waymarkDocument.select("#wm_coordinates").text()));
            cache.setType(CacheType.WAYMARK);
            cache.setSize(CacheSize.VIRTUAL);
            cache.setDisabled(false);
            cache.setArchived(!waymarkHtml.contains("/logs/add.aspx"));
            cache.setLocation(waymarkDocument.select("#wm_location strong").first().nextSibling().toString().trim());
            cache.setHidden(parseDate(waymarkDocument.select("#wm_datelisted strong").first().nextSibling().toString().trim()));
            cache.setOwnerDisplayName(ownerElement.text());
            cache.setOwnerUserId(ownerElement.text());
            cache.setOwnerGuid(TextUtils.getMatch(ownerElement.attr("href"), PATTERN_OWNER_GUID, false, 1, "", false));
            cache.setPremiumMembersOnly(false);
            cache.setUserModifiedCoords(false);
            cache.setDetailedUpdatedNow();

            //TODO images
            //TODO logs
            //TODO found state

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
            return cache;
        } catch (final Exception ex) {
            Log.w("WMApi: Exception while getting " + geocode, ex);
            return null;
        }
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
