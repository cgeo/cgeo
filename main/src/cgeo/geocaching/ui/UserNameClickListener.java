package cgeo.geocaching.ui;

import android.view.View;

public class UserNameClickListener extends AbstractUserClickListener {

    final private String name;

    public UserNameClickListener(final String name) {
        super(true);
        this.name = name;
    }

    @Override
    protected CharSequence getUserName(final View view) {
        return name;
    }
}
