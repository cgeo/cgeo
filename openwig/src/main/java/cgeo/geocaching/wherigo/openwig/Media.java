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

/**
 * Represents a media resource (image, audio) in a Wherigo game.
 * <p>
 * Media extends EventTable to provide access to multimedia resources embedded
 * in cartridge files. Media objects reference images, sounds, and other assets
 * that can be displayed or played during gameplay.
 * <p>
 * Key features:
 * <ul>
 * <li>References media files by unique ID in the cartridge</li>
 * <li>Supports images (png, jpg, gif) and audio (wav, mp3)</li>
 * <li>Provides alternative text for accessibility</li>
 * <li>Can be played (audio) or displayed (images) via UI</li>
 * <li>Type information extracted from resource metadata</li>
 * <li>Generates filenames for media extraction</li>
 * </ul>
 * <p>
 * Media objects are typically associated with zones, items, dialogs, and
 * other game objects to provide visual and audio feedback.
 */
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

    @Override
    public void serialize (DataOutputStream out) throws IOException {
        out.writeInt(id);
        super.serialize(out);
    }

    @Override
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
            Engine currentEngine = Engine.getCurrentInstance();
            if (currentEngine == null) return;
            
            String mime = null;
            if ("wav".equals(type)) mime = "audio/x-wav";
            else if ("mp3".equals(type)) mime = "audio/mpeg";
            currentEngine.uiInstance.playSound(Engine.mediaFile(this), mime);
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
