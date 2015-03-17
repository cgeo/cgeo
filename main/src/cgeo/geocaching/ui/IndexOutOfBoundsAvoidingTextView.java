package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
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
	}

	public IndexOutOfBoundsAvoidingTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public IndexOutOfBoundsAvoidingTextView(final Context context) {
		super(context);
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
}