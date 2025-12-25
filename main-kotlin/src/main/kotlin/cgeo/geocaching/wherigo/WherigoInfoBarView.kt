// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.wherigo

import cgeo.geocaching.R
import cgeo.geocaching.databinding.WherigoInfoBarViewBinding
import cgeo.geocaching.ui.BadgeManager
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AudioManager

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.RelativeLayout

import androidx.annotation.Nullable

/** shows info about currently running wherigo (if any) */
class WherigoInfoBarView : RelativeLayout() {

    private Context context
    private Activity activity
    private WherigoInfoBarViewBinding binding
    private Int wherigoListenerId
    private Int wherigoAudioListenerId

    public WherigoInfoBarView(final Context context) {
        super(context)
        init(null, 0, 0)
    }

    public WherigoInfoBarView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init(attrs, 0, 0)
    }

    public WherigoInfoBarView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init(attrs, defStyleAttr, 0)
    }

    public WherigoInfoBarView(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init(attrs, defStyleAttr, defStyleRes)
    }

    private Unit init(final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        this.context = ContextThemeWrapper(getContext(), R.style.cgeo)
        this.activity = ViewUtils.toActivity(this.context)
        inflate(this.context, R.layout.wherigo_info_bar_view, this)
        binding = WherigoInfoBarViewBinding.bind(this)
        binding.wherigoInfoBox.setOnClickListener(v -> onBarClick())
        binding.wherigoInfoText.setOnClickListener(v -> onBarClick())
        binding.wherigoResumeDialogIcon.setOnClickListener(v -> onResumeDialogClick())
        binding.wherigoResumeDialogText.setOnClickListener(v -> onResumeDialogClick())
        binding.wherigoSongInfoIcon.setOnClickListener(v -> onSongInfoClick())
        binding.wherigoSongInfoText.setOnClickListener(v -> onSongInfoClick())

        BadgeManager.get().setBadge(binding.wherigoResumeDialogText, false, -1)

        wherigoListenerId = WherigoGame.get().addListener(nt -> refreshBar())
        wherigoAudioListenerId = WherigoGame.get().getAudioManager().addListener(st -> refreshBar())
        refreshBar()
    }

    override     protected Unit onDetachedFromWindow() {
        super.onDetachedFromWindow()
        WherigoGame.get().removeListener(wherigoListenerId)
        WherigoGame.get().getAudioManager().removeListener(wherigoAudioListenerId)
    }

    private Unit refreshBar() {
        val isVisible: Boolean = WherigoGame.get().isPlaying() && !(activity is WherigoActivity)
        val showResume: Boolean = WherigoGame.get().dialogIsPaused()
        val showSong: Boolean = WherigoGame.get().getAudioManager().getState() != AudioManager.State.NO_SONG
        binding.wherigoInfoBox.setVisibility(isVisible ? VISIBLE : GONE)
        binding.wherigoAdditionalInfoBox.setVisibility(isVisible && (showResume || showSong) ? VISIBLE : GONE)
        if (!isVisible) {
            return
        }
        binding.wherigoInfoText.setText(WherigoGame.get().getCartridgeName())

        binding.wherigoResumeDialogIcon.setVisibility(showResume ? VISIBLE : GONE)
        binding.wherigoResumeDialogText.setVisibility(showResume ? VISIBLE : GONE)
        binding.wherigoSongInfoIcon.setVisibility(showSong ? VISIBLE : GONE)
        binding.wherigoSongInfoText.setVisibility(showSong ? VISIBLE : GONE)

        if (showSong) {
            binding.wherigoSongInfoText.setText(WherigoGame.get().getAudioManager().getUserDisplayableShortState())
        }
    }

    private Unit onBarClick() {
        WherigoActivity.start(this.activity, false)
    }

    private Unit onResumeDialogClick() {
        WherigoGame.get().unpauseDialog()
    }

    private Unit onSongInfoClick() {
        val audio: AudioManager = WherigoGame.get().getAudioManager()
        switch (audio.getState()) {
            case PLAYING:
                audio.pause()
                break
            case STOPPED:
                audio.resume()
                break
            case COMPLETED:
                audio.reset()
                audio.resume()
                break
            default:
                //do nothing
                break
        }
    }
}
