package cgeo.geocaching.activity;

/**
 * Interface to implement by activities that want to utilize the NavigationActionProvider
 */
public interface INavigationSource {

    /**
     * Calls the default navigation in the current context
     */
    void startDefaultNavigation();

    /**
     * Calls the second default navigation in the current context
     */
    void startDefaultNavigation2();
}
