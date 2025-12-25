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

package cgeo.geocaching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.fragment.app.Fragment


class SwipeToOpenFragment : Fragment() {

    private var onStopCallback: Runnable = null

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
    }

    override     public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_swipe_to_open, container, false)
    }

    public Unit setExpansion(final Float expansion, final View alphaView) {
        val view: View = getView()
        if (view != null && alphaView != null) {
            Float alpha = 1 - (expansion * 2)
            if (alpha > 1) {
                alpha = 1
            }
            if (alpha < 0) {
                alpha = 0
            }
            alphaView.setAlpha(alpha)

            val imageview: ImageView = view.findViewById(R.id.icon)
            imageview.setImageResource(alpha == 0 ? R.drawable.ic_menu_done : R.drawable.expand_less)
        }

    }

    public Unit setOnStopCallback(final Runnable onStopCallback) {
        this.onStopCallback = onStopCallback
    }

    override     public Unit onStop() {
        super.onStop()

        if (onStopCallback != null) {
            onStopCallback.run()
        }
    }
}
