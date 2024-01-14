package cgeo.geocaching;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;


public class SwipeToOpenFragment extends Fragment {

    private Runnable onStopCallback = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_swipe_to_open, container, false);
    }

    public void setExpansion(final float expansion, final View alphaView) {
        final View view = getView();
        if (view != null && alphaView != null) {
            float alpha = 1 - (expansion * 2);
            if (alpha > 1) {
                alpha = 1;
            }
            if (alpha < 0) {
                alpha = 0;
            }
            alphaView.setAlpha(alpha);

            final ImageView imageview = view.findViewById(R.id.icon);
            imageview.setImageResource(alpha == 0 ? R.drawable.ic_menu_done : R.drawable.expand_less);
        }

    }

    public void setOnStopCallback(final Runnable onStopCallback) {
        this.onStopCallback = onStopCallback;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (onStopCallback != null) {
            onStopCallback.run();
        }
    }
}
