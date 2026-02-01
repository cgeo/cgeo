/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import androidx.annotation.NonNull;

import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

public class Media extends EventTable {

    private static int media_no;

    public static void reset () {
        media_no = 1;
    }

    public int id;
    public String altText = null;
    public String type = null;

    public Media() {
        id = media_no++;
    }

    public void serialize (DataOutputStream out) throws IOException {
        out.writeInt(id);
        super.serialize(out);
    }

    public void deserialize (DataInputStream in) throws IOException {
        media_no--; // deserialize must be called directly after construction
        id = in.readInt();
        if (id >= media_no) media_no = id + 1;
        super.deserialize(in);
    }

    protected void setItem (String key, Object value) {
        if ("AltText".equals(key)) {
            altText = (String)value;
        } else if ("Resources".equals(key)) {
            LuaTable lt = (LuaTable)value;
            int n = lt.len();
            for (int i = 1; i <= n; i++) {
                LuaTable res = (LuaTable)lt.rawget(new Double(i));
                String t = (String)res.rawget("Type");
                if ("fdl".equals(t)) continue;
                type = t.toLowerCase(Locale.getDefault());
            }
        } else super.setItem(key, value);
    }

    public String jarFilename () {
        return String.valueOf(id)+"."+(type==null ? "" : type);
    }

    public void play () {
        try {
            String mime = null;
            if ("wav".equals(type)) mime = "audio/x-wav";
            else if ("mp3".equals(type)) mime = "audio/mpeg";
            Engine.ui.playSound(Engine.mediaFile(this), mime);
        } catch (IOException e) {
            // meh
        }
    }

    @NonNull
    @Override
    public String toString() {
        return id + ":" + name ;
    }
}
