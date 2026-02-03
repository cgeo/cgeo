package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializable {
    public void serialize (DataOutputStream out) throws IOException;
    public void deserialize (DataInputStream in) throws IOException;
}
