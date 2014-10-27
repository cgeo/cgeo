package cgeo.geocaching.activity;


public interface IAbstractActivity {

    public void showToast(String text);

    public void showShortToast(String text);

    public void invalidateOptionsMenuCompatible();

    /**
     * Override this method to create a showcase view highlighting the most important UI element.
     *
     */
    public ShowcaseViewBuilder getShowcase();

    /**
     * Call this method to actually present a showcase. The right time to invoke this method depends on the showcase
     * target. I.e. if the showcase target is an action bar item, this method can only be invoked after that item has
     * been created in onCreateOptionsMenu.
     */
    public void presentShowcase();

}
