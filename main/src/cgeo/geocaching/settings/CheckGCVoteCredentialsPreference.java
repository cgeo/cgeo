package cgeo.geocaching.settings;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import rx.Observable;

public class CheckGCVoteCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckGCVoteCredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGCVoteCredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getGCVoteLogin();
    }

    protected ImmutablePair<StatusCode, Observable<Drawable>> login() {
        return new ImmutablePair<>(GCVote.getInstance().login(), null);
    }
}
