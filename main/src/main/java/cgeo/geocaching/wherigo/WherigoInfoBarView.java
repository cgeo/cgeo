package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoInfoBarViewBinding;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

/** shows info about currently running wherigo (if any) */
public class WherigoInfoBarView extends LinearLayout {

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
        setOrientation(HORIZONTAL);
        this.context = new ContextThemeWrapper(getContext(), R.style.cgeo);
        this.activity = ViewUtils.toActivity(this.context);
        inflate(this.context, R.layout.wherigo_info_bar_view, this);
        binding = WherigoInfoBarViewBinding.bind(this);
        binding.wherigoBar.setOnClickListener(v -> onBarClick());

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
        if (!isVisible) {
            return;
        }

        if (WherigoGame.get().dialogIsPaused()) {
            TextParam.id(R.string.wherigo_notification_waiting, WherigoGame.get().getCartridgeName()).applyTo(binding.wherigoInfoText);
        } else {
            binding.wherigoInfoText.setText(WherigoGame.get().getCartridgeName());
        }
    }

    private void onBarClick() {
        if (WherigoGame.get().dialogIsPaused()) {
            WherigoDialogManager.get().unpause();
        } else if (this.activity != null) {
            WherigoActivity.start(this.activity, false);
        }
    }

}
