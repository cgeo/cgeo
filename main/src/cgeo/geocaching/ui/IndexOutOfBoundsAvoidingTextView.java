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

    public IndexOutOfBoundsAvoidingTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public IndexOutOfBoundsAvoidingTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IndexOutOfBoundsAvoidingTextView(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		try{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } catch (IndexOutOfBoundsException ignored) {
			setText(getText().toString());
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	public void setGravity(int gravity){
		try{
			super.setGravity(gravity);
        } catch (IndexOutOfBoundsException ignored) {
			setText(getText().toString());
			super.setGravity(gravity);
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		try{
			super.setText(text, type);
        } catch (IndexOutOfBoundsException ignored) {
			setText(text.toString());
		}
	}
}