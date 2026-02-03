/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Marker interface for objects that can be serialized to game saves.
 * <p>
 * Extends java.io.Serializable and adds explicit serialize/deserialize methods
 * for custom binary serialization used by OpenWIG's save game system.
 * <p>
 * Default implementations delegate to the Savegame class for standard serialization.
 * Classes can override these methods to provide custom serialization behavior.
 */
public interface Serializable extends java.io.Serializable {
    /**
     * Serializes this object to a data output stream.
     * <p>
     * The default implementation delegates to {@link cgeo.geocaching.wherigo.openwig.formats.Savegame#storeValue(Object, DataOutputStream)}
     * to handle the serialization. Classes that need custom serialization should override this method.
     *
     * @param out the data output stream to write to
     * @throws IOException if an I/O error occurs
     */
    default void serialize(DataOutputStream out) throws IOException {
        final Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.storeValue(this, out);
        }
    }

    /**
     * Deserializes this object from a data input stream.
     * <p>
     * The default implementation delegates to {@link cgeo.geocaching.wherigo.openwig.formats.Savegame#restoreValue(DataInputStream, Object)}
     * to handle the deserialization. Classes that need custom deserialization should override this method.
     *
     * @param in the data input stream to read from
     * @throws IOException if an I/O error occurs
     */
    default void deserialize(DataInputStream in) throws IOException {
        final Engine currentEngine = Engine.getCurrentInstance();
        if (currentEngine != null) {
            currentEngine.savegame.restoreValue(in, this);
        }
    }
}
