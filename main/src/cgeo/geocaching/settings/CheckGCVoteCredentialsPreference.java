package cgeo.geocaching.settings;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import rx.Observable;

public class CheckGCVoteCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckGCVoteCredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGCVoteCredentialsPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getGCVoteLogin();
    }

    @Override
    protected ImmutablePair<StatusCode, Observable<Drawable>> login() {
        return new ImmutablePair<>(GCVote.login(), null);
    }
}
