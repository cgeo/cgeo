package cgeo.geocaching.activity;

import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import android.app.Activity;

/**
 * TODO: replace by simple utility class embedding a builder instead of inheriting from it
 */
public class ShowcaseViewBuilder extends Builder {

    private final Activity activity;

    public ShowcaseViewBuilder(final Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public ShowcaseViewBuilder setContentTitle(final int resId) {
        setSingleshot(activity.getResources().getString(resId));
        return (ShowcaseViewBuilder) super.setContentTitle(resId);
    }

    /**
     * Use the hash of the title for the single shot remembering
     *
     * @param resId
     */
    private void setSingleshot(final CharSequence title) {
        super.singleShot(title.hashCode());
    }

    @Override
    public ShowcaseViewBuilder setContentText(final int resId) {
        return (ShowcaseViewBuilder) super.setContentText(resId);
    }

    @Override
    public ShowcaseViewBuilder setContentText(final CharSequence text) {
        return (ShowcaseViewBuilder) super.setContentText(text);
    }

    @Override
    public ShowcaseViewBuilder setContentTitle(final CharSequence title) {
        setSingleshot(title);
        return (ShowcaseViewBuilder) super.setContentTitle(title);
    }

    @Override
    public ShowcaseViewBuilder setTarget(final Target target) {
        return (ShowcaseViewBuilder) super.setTarget(target);
    }

    public ShowcaseViewBuilder setContent(final int titleId, final int textId) {
        setContentTitle(titleId);
        return setContentText(textId);
    }

}
