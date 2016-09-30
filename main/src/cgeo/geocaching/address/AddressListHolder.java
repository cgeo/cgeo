package cgeo.geocaching.address;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.view.View;
import android.widget.TextView;

import butterknife.BindView;

public class AddressListHolder extends AbstractRecyclerViewHolder {

    @BindView(R.id.label) TextView label;
    @BindView(R.id.distance) TextView distance;

    public AddressListHolder(final View itemView) {
        super(itemView);
    }


}
