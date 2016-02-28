package cgeo.geocaching.activity;


public interface IAbstractActivity {

    void showToast(String text);

    void showShortToast(String text);

    void invalidateOptionsMenuCompatible();

    /**
     * Override this method to create a showcase view highlighting the most important UI element.
     *
     */
    ShowcaseViewBuilder getShowcase();

    /**
     * Call this method to actually present a showcase. The right time to invoke this method depends on the showcase
     * target. I.e. if the showcase target is an action bar item, this method can only be invoked after that item has
     * been created in onCreateOptionsMenu.
     */
    void presentShowcase();

}
