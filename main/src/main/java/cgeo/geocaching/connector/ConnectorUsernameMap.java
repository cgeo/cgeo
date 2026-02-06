package cgeo.geocaching.connector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

/**
 * A specialized HashMap for mapping connector names to usernames.
 * Keys can be null (representing "any connector") or non-null connector names.
 * Values are non-null usernames.
 * 
 * This class implements Serializable to support passing the map via Android Intent extras.
 */
public class ConnectorUsernameMap extends HashMap<@Nullable String, @NonNull String> {
    
    private static final long serialVersionUID = 1L;
    
    public ConnectorUsernameMap() {
        super();
    }
    
    public ConnectorUsernameMap(final int initialCapacity) {
        super(initialCapacity);
    }
}
