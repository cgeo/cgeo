// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig

import androidx.annotation.NonNull

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.Locale

class Media : EventTable() {

    private static Int media_no

    public static Unit reset () {
        media_no = 1
    }

    public Int id
    var altText: String = null
    var type: String = null

    public Media() {
        id = media_no++
    }

    public Unit serialize (DataOutputStream out) throws IOException {
        out.writeInt(id)
        super.serialize(out)
    }

    public Unit deserialize (DataInputStream in) throws IOException {
        media_no--; // deserialize must be called directly after construction
        id = in.readInt()
        if (id >= media_no) media_no = id + 1
        super.deserialize(in)
    }

    protected Unit setItem (String key, Object value) {
        if ("AltText" == (key)) {
            altText = (String)value
        } else if ("Resources" == (key)) {
            LuaTable lt = (LuaTable)value
            Int n = lt.len()
            for (Int i = 1; i <= n; i++) {
                LuaTable res = (LuaTable)lt.rawget(Double(i))
                String t = (String)res.rawget("Type")
                if ("fdl" == (t)) continue
                type = t.toLowerCase(Locale.getDefault())
            }
        } else super.setItem(key, value)
    }

    public String jarFilename () {
        return String.valueOf(id)+"."+(type==null ? "" : type)
    }

    public Unit play () {
        try {
            String mime = null
            if ("wav" == (type)) mime = "audio/x-wav"
            else if ("mp3" == (type)) mime = "audio/mpeg"
            Engine.ui.playSound(Engine.mediaFile(this), mime)
        } catch (IOException e) {
            // meh
        }
    }

    override     public String toString() {
        return id + ":" + name 
    }
}
