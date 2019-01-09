package cgeo.geocaching.address;

import android.location.Address;
import android.support.annotation.NonNull;

interface AddressClickListener {
    void onClickAddress(@NonNull Address address);
}
