package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.content.Context;
import android.util.AttributeSet;

public class ToggleButton extends androidx.appcompat.widget.AppCompatButton {

    private boolean checked = false;

    public ToggleButton(final Context context) {
        super(context, null, R.style.button_small);
        init();
    }

    public ToggleButton(final Context context, final AttributeSet attrs) {
        super(context, attrs, R.style.button_small);
        init();
    }

    public ToggleButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setPadding(dpToPixel(10), dpToPixel(10), dpToPixel(10), dpToPixel(10));
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(final boolean checked) {
        this.checked = checked;
        setButtonVisual(this.checked);
    }

    public void toggle() {
        setChecked(!isChecked());
    }

    private void setButtonVisual(final boolean checked) {
        if (checked) {
            setBackgroundColor(getResources().getColor(R.color.colorAccent));
        } else {
            setBackgroundColor(0x00000000);
            setBackgroundResource(Settings.isLightSkin() ? R.drawable.action_button_light : R.drawable.action_button_dark);
        }
    }

}
