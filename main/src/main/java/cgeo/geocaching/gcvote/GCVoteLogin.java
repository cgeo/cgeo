package cgeo.geocaching.gcvote;

import cgeo.geocaching.connector.AbstractLogin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class GCVoteLogin extends AbstractLogin {

    private GCVoteLogin() {
        // singleton
    }

    private static class SingletonHolder {
        @NonNull
        private static final GCVoteLogin INSTANCE = new GCVoteLogin();
    }

    @NonNull
    public static GCVoteLogin getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry) {
        return login(retry, Settings.getCredentials(GCVote.getInstance()));
    }

    @Override
    @NonNull
    protected StatusCode login(final boolean retry, @NonNull final Credentials credentials) {

        if (credentials.isInvalid()) {
            Log.w("Credentials can't be retrieved");
            return StatusCode.NO_LOGIN_INFO_STORED;
        }

        final Parameters params = new Parameters("version", "cgeo", "userName", credentials.getUserName(), "password", credentials.getPassword());

        final InputStream response = Network.getResponseStream(Network.getRequest("http://gcvote.com/getVotes.php", params));

        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(response, StandardCharsets.UTF_8.name());
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    final String tagName = xpp.getName();
                    if (StringUtils.equals(tagName, "votes")) {
                        if (StringUtils.equals(xpp.getAttributeValue(null, "loggedIn"), "true")) {
                            Log.i("Successfully logged in gcvote.com as " + credentials.getUserName());
                            return StatusCode.NO_ERROR;
                        }
                        Log.w("Username or password is wrong");
                        return StatusCode.WRONG_LOGIN_DATA;
                    }
                }
                eventType = xpp.next();
            }
        } catch (final Exception e) {
            Log.w("Cannot parse GCVote result", e);
            return StatusCode.UNKNOWN_ERROR;
        } finally {
            IOUtils.closeQuietly(response);
        }

        return StatusCode.UNKNOWN_ERROR;
    }
}
