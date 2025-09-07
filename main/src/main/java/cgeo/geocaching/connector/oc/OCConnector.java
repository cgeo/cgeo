package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.capability.Smiley;
import cgeo.geocaching.connector.capability.SmileyCapability;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class OCConnector extends OCBaseConnector implements SmileyCapability {

    private static final Pattern GPX_ZIP_FILE_PATTERN = Pattern.compile("oc[a-z]{2,3}\\d{5,}\\.zip", Pattern.CASE_INSENSITIVE);

    private static final List<LogType> STANDARD_LOG_TYPES = Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, LogType.NOTE);
    private static final List<LogType> EVENT_LOG_TYPES = Arrays.asList(LogType.WILL_ATTEND, LogType.ATTENDED, LogType.NOTE);

    public OCConnector(@NonNull final String name, @NonNull final String host, final boolean https, final String prefix, @NonNull final String abbreviation) {
        super(name, host, https, prefix, abbreviation);
    }

    @Override
    @Nullable
    public String getCacheLogUrl(@NonNull final Geocache cache, final @NonNull LogEntry logEntry) {
        final String internalId = getServiceSpecificLogId(logEntry.serviceLogId);
        if (StringUtils.isNotBlank(internalId)) {
            return getCacheUrl(cache) + "&log=A#log" + internalId;
        }
        return null;
    }

    @Override
    public String getServiceSpecificLogId(final String logId) {
        //OC serviceLogId has format: 'log_uuid:internal_id', 'internal_id' may be missing
        //the id usable in other contexts is the internal_id
        if (StringUtils.isBlank(logId)) {
            return null;
        }
        final int idx = logId.lastIndexOf(":");
        if (idx >= 0) {
            return logId.substring(idx + 1);
        }
        return null; //do not display uuid
    }

    @Override
    public boolean isZippedGPXFile(@NonNull final String fileName) {
        return GPX_ZIP_FILE_PATTERN.matcher(fileName).matches();
    }

    @Override
    @NonNull
    public final List<LogType> getPossibleLogTypes(@NonNull final Geocache cache) {
        if (cache.isEventCache()) {
            return EVENT_LOG_TYPES;
        }

        return STANDARD_LOG_TYPES;
    }

    @Override
    @Nullable
    public String getGeocodeFromText(@NonNull final String text) {
        // Text containing a Geocode
        return getGeocodeFromUrl(TextUtils.getMatch(text, Pattern.compile("((https?://|)(www.|)opencach[^\\s/$.?#].[^\\s]*)", Pattern.CASE_INSENSITIVE), false, ""));
    }

    @Override
    @Nullable
    public String getCreateAccountUrl() {
        return getSchemeAndHost() + "/register.php";
    }

    @Override
    public List<Smiley> getSmileys() {
        return OCSmileysProvider.getSmileys();
    }

    @Override
    @Nullable
    public Smiley getSmiley(final int id) {
        return OCSmileysProvider.getSmiley(id);
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        final List<UserAction> actions = super.getUserActions(user);
        // caches stored before parsing the UserId will not have the field set, so we must check for correct existence here
        if (NumberUtils.isDigits(user.userName)) {
            actions.add(new UserAction(R.string.user_menu_open_browser, R.drawable.ic_menu_face, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/viewprofile.php?userid=" + Network.encode(context.userName))));
            actions.add(new UserAction(R.string.user_menu_send_message, R.drawable.ic_menu_message, context -> ShareUtils.openUrl(context.getContext(), getSchemeAndHost() + "/mailto.php?userid=" + Network.encode(context.userName))));
        }
        return actions;
    }
}
