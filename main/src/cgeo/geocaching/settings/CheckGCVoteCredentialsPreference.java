package cgeo.geocaching.settings;

import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;

import rx.Observable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CheckGCVoteCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckGCVoteCredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGCVoteCredentialsPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    @NonNull
    protected Credentials getCredentials() {
        return Settings.getGCVoteLogin();
    }

    @Override
    protected ImmutablePair<StatusCode, Observable<Drawable>> login() {
        return new ImmutablePair<>(GCVote.login(), null);
    }
}
