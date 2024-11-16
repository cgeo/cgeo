package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoInfoBarViewBinding;
import cgeo.geocaching.ui.BadgeManager;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

/** shows info about currently running wherigo (if any) */
public class WherigoInfoBarView extends RelativeLayout {

    private Context context;
    private Activity activity;
    private WherigoInfoBarViewBinding binding;
    private int wherigoListenerId;

    public WherigoInfoBarView(final Context context) {
        super(context);
        init(null, 0, 0);
    }

    public WherigoInfoBarView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public WherigoInfoBarView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    public WherigoInfoBarView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        this.context = new ContextThemeWrapper(getContext(), R.style.cgeo);
        this.activity = ViewUtils.toActivity(this.context);
        inflate(this.context, R.layout.wherigo_info_bar_view, this);
        binding = WherigoInfoBarViewBinding.bind(this);
        binding.wherigoBar.setOnClickListener(v -> onBarClick());
        binding.wherigoInfoText.setOnClickListener(v -> onBarClick());
        binding.wherigoResumeDialog.setOnClickListener(v -> onResumeDialogClick());
        binding.wherigoResumeDialogText.setOnClickListener(v -> onResumeDialogClick());

        BadgeManager.get().setBadge(binding.wherigoResumeDialog, false, -1);

        wherigoListenerId = WherigoGame.get().addListener(nt -> refreshBar());
        refreshBar();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        WherigoGame.get().removeListener(wherigoListenerId);
    }

    private void refreshBar() {
        final boolean isVisible = WherigoGame.get().isPlaying() && !(activity instanceof WherigoActivity);
        binding.wherigoBar.setVisibility(isVisible ? VISIBLE : GONE);
        binding.wherigoResumeDialog.setVisibility(isVisible && WherigoGame.get().dialogIsPaused() ? VISIBLE : GONE);
        if (!isVisible) {
            return;
        }
        binding.wherigoInfoText.setText(WherigoGame.get().getCartridgeName());

    }

    private void onBarClick() {
        WherigoActivity.start(this.activity, false);
    }

    private void onResumeDialogClick() {
        WherigoDialogManager.get().unpause();
    }

}
