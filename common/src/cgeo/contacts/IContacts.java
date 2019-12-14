package cgeo.contacts;

import androidx.annotation.NonNull;

public interface IContacts {
    @NonNull String INTENT = "cgeo.contacts.FIND";

    @NonNull String URI_SCHEME = "find";
    @NonNull String URI_HOST = "cgeo.org";

    @NonNull String PARAM_NAME = "name"; // user name
}
