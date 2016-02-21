package cgeo.geocaching.ui;

import android.content.Context;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Jelly beans can crash when calculating the layout of a textview.
 * 
 * https://code.google.com/p/android/issues/detail?id=35466
 *
 */
public class IndexOutOfBoundsAvoidingTextView extends TextView {

    public IndexOutOfBoundsAvoidingTextView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		shouldWindowFocusWait = false;
	}

	public IndexOutOfBoundsAvoidingTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		shouldWindowFocusWait = false;
	}

	public IndexOutOfBoundsAvoidingTextView(final Context context) {
		super(context);
		shouldWindowFocusWait = false;
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		try{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } catch (final IndexOutOfBoundsException ignored) {
			setText(getText().toString());
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	public void setGravity(final int gravity){
		try{
			super.setGravity(gravity);
        } catch (final IndexOutOfBoundsException ignored) {
			setText(getText().toString());
			super.setGravity(gravity);
		}
	}

	@Override
	public void setText(final CharSequence text, final BufferType type) {
		try{
			super.setText(text, type);
        } catch (final IndexOutOfBoundsException ignored) {
			setText(text.toString());
		}
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		if (selStart == -1 || selEnd == -1) {
			// @hack : https://code.google.com/p/android/issues/detail?id=137509
			CharSequence text = getText();
			if (text instanceof Spannable) {
				Selection.setSelection((Spannable) text, 0, 0);
			}
		} else {
			super.onSelectionChanged(selStart, selEnd);
		}
	}

	// https://code.google.com/p/android/issues/detail?id=23381
	private boolean shouldWindowFocusWait;
	public void setWindowFocusWait(final boolean shouldWindowFocusWait) {
		this.shouldWindowFocusWait = shouldWindowFocusWait;
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!shouldWindowFocusWait) {
			super.onWindowFocusChanged(hasWindowFocus);
		}
	}
}
