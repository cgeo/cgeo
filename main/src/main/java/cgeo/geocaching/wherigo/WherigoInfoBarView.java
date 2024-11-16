package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.WherigoInfoBarViewBinding;
import cgeo.geocaching.ui.BadgeManager;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AudioManager;

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
    private int wherigoAudioListenerId;

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
        binding.wherigoInfoBox.setOnClickListener(v -> onBarClick());
        binding.wherigoInfoText.setOnClickListener(v -> onBarClick());
        binding.wherigoResumeDialogIcon.setOnClickListener(v -> onResumeDialogClick());
        binding.wherigoResumeDialogText.setOnClickListener(v -> onResumeDialogClick());
        binding.wherigoSongInfoIcon.setOnClickListener(v -> onSongInfoClick());
        binding.wherigoSongInfoText.setOnClickListener(v -> onSongInfoClick());

        BadgeManager.get().setBadge(binding.wherigoResumeDialogText, false, -1);

        wherigoListenerId = WherigoGame.get().addListener(nt -> refreshBar());
        wherigoAudioListenerId = WherigoGame.get().getAudioManager().addListener(st -> refreshBar());
        refreshBar();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        WherigoGame.get().removeListener(wherigoListenerId);
        WherigoGame.get().getAudioManager().removeListener(wherigoAudioListenerId);
    }

    private void refreshBar() {
        final boolean isVisible = WherigoGame.get().isPlaying() && !(activity instanceof WherigoActivity);
        final boolean showResume = WherigoGame.get().dialogIsPaused();
        final boolean showSong = WherigoGame.get().getAudioManager().getState() != AudioManager.State.NO_SONG;
        binding.wherigoInfoBox.setVisibility(isVisible ? VISIBLE : GONE);
        binding.wherigoAdditionalInfoBox.setVisibility(isVisible && (showResume || showSong) ? VISIBLE : GONE);
        if (!isVisible) {
            return;
        }
        binding.wherigoInfoText.setText(WherigoGame.get().getCartridgeName());

        binding.wherigoResumeDialogIcon.setVisibility(showResume ? VISIBLE : GONE);
        binding.wherigoResumeDialogText.setVisibility(showResume ? VISIBLE : GONE);
        binding.wherigoSongInfoIcon.setVisibility(showSong ? VISIBLE : GONE);
        binding.wherigoSongInfoText.setVisibility(showSong ? VISIBLE : GONE);

        if (showSong) {
            binding.wherigoSongInfoText.setText(WherigoGame.get().getAudioManager().getUserDisplayableShortState());
        }
    }

    private void onBarClick() {
        WherigoActivity.start(this.activity, false);
    }

    private void onResumeDialogClick() {
        WherigoGame.get().unpauseDialog();
    }

    private void onSongInfoClick() {
        final AudioManager audio = WherigoGame.get().getAudioManager();
        switch (audio.getState()) {
            case PLAYING:
                audio.pause();
                break;
            case STOPPED:
                audio.resume();
                break;
            case COMPLETED:
                audio.reset();
                audio.resume();
                break;
            default:
                //do nothing
                break;
        }
    }
}
