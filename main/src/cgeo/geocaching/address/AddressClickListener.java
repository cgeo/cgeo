package cgeo.geocaching.address;

import android.location.Address;

import androidx.annotation.NonNull;

interface AddressClickListener {
    void onClickAddress(@NonNull Address address);

    void onClickMapIcon(@NonNull Address address);
}
