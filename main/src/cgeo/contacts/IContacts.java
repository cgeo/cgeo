package cgeo.contacts;

import org.eclipse.jdt.annotation.NonNull;

public interface IContacts {
    @NonNull static final String INTENT = "cgeo.contacts.FIND";

    @NonNull static final String URI_SCHEME = "find";
    @NonNull static final String URI_HOST = "cgeo.org";

    @NonNull static final String PARAM_NAME = "name"; // user name
}
