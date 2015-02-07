package cgeo.geocaching.ui;

import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.View;

public abstract class AbstractMenuActionProvider extends ActionProvider {

    public AbstractMenuActionProvider(final Context context) {
        super(context);
    }

    @Override
    public boolean hasSubMenu() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        // must return null, otherwise the menu will not work
        return null;
    }

}
