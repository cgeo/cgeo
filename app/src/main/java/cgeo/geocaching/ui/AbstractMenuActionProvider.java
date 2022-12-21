package cgeo.geocaching.ui;

import android.content.Context;
import android.view.View;

import androidx.core.view.ActionProvider;

/**
 * abstract super class for all our action providers showing sub menus
 */
public abstract class AbstractMenuActionProvider extends ActionProvider {

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    protected AbstractMenuActionProvider(final Context context) {
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
