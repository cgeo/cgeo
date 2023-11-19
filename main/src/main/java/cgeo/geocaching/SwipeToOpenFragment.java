package cgeo.geocaching;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;


public class SwipeToOpenFragment extends Fragment {

    public SwipeToOpenFragment() {
        // nothing to de
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        final View view = inflater.inflate(R.layout.fragment_swipe_to_open, container, false);
        view.setAlpha(0);
        return view;
    }

    public void setExpansion(final float expansion) {
        final View view = getView();
        if (view != null) {
            float alpha = expansion * 2;
            if (alpha > 1) {
                alpha = 1;
            }
            if (alpha < 0) {
                alpha = 0;
            }
            view.setAlpha(alpha);

            final ImageView imageview = view.findViewById(R.id.icon);
            imageview.setImageResource(alpha == 1 ? R.drawable.ic_menu_done : R.drawable.expand_less);
        }

    }
}