/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializable {
    public void serialize (DataOutputStream out) throws IOException;
    public void deserialize (DataInputStream in) throws IOException;
}
