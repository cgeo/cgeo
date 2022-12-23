package cgeo.geocaching.gcvote;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class GCVoteTest extends AbstractResourceInstrumentationTestCase {

    private InputStream responseStream;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        responseStream = new ByteArrayInputStream(getFileContent(R.raw.gcvote).getBytes());
        responseStream.mark(getFileContent(R.raw.gcvote).getBytes().length + 1);
    }

    private InputStream responseStream() {
        try {
            responseStream.reset();
        } catch (final IOException ignored) {
            // Cannot happen
        }
        return responseStream;
    }

    public void testGetRatingsByGeocode() {
        final Map<String, GCVoteRating> ratings = GCVote.getRatingsFromXMLResponse(responseStream(), false);
        assertThat(ratings).hasSize(10);
        assertThat(ratings).containsKey("GCKF13");
        assertThat(ratings.get("GC1WEVZ")).isEqualToComparingFieldByField(new GCVoteRating(3.75f, 2, 0));
    }

    public void testGetRatingsByGuid() {
        final Map<String, GCVoteRating> ratings = GCVote.getRatingsFromXMLResponse(responseStream(), true);
        assertThat(ratings).hasSize(10);
        assertThat(ratings).containsKey("a02894bb-4a08-4c09-a73c-25939894ba15");
        assertThat(ratings.get("5520c33b-3941-45ca-9056-ea655dbaadf7")).isEqualToComparingFieldByField(new GCVoteRating(3.75f, 2, 0));
    }

}
