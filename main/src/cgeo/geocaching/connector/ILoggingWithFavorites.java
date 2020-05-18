package cgeo.geocaching.connector;

/**
 * This is the counterpart of {@link cgeo.geocaching.connector.capability.IFavoriteCapability} for logging.
 * This Interface  has to be implemented by Logging Managers in order to be able to mark caches
 * as favorites directly on Cache Logging screen.
 * Typically the implementation of the fetching is done inside @code {onLoadFinished} method.
 */
public interface ILoggingWithFavorites extends ILoggingManager {

    /**
     * @return number of available favorite points. This number will be displayed near "add to favorites" checkbox
     */
    int getFavoritePoints();

    /**
     * @return true if there was loading error, false otherwise.
     */
    boolean hasFavPointLoadError();
}
