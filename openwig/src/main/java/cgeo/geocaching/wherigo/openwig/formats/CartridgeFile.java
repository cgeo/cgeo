/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.formats
 */
package cgeo.geocaching.wherigo.openwig.formats;

import cgeo.geocaching.wherigo.openwig.platform.FileHandle;
import cgeo.geocaching.wherigo.openwig.platform.SeekableFile;
import java.io.*;

/** Implementation of the GWC cartridge format.
 * <p>
 * This class handles reading the GWC file format, extracting information
 * from its header and reading individual files stored inside.
 */
public class CartridgeFile {

    private static final byte[] CART_ID = { 0x02, 0x0a, 0x43, 0x41, 0x52, 0x54, 0x00 };
            // 02 0a CART 00

    private static final int CACHE_LIMIT = 128000; // in kB

    private SeekableFile source;

    protected Savegame savegame;

    private int files;
    private int[] offsets;
    private int[] ids;

    public double latitude, longitude;
    public String type, member, name, description, startdesc, version, author, url, device, code;
    public int iconId, splashId;

    public String filename;

    protected CartridgeFile() { }

    private boolean fileOk () throws IOException {
        byte[] buf = new byte[CART_ID.length];
        source.seek(0);
        source.readFully(buf);
        for (int i = 0; i < buf.length; i++) if (buf[i]!=CART_ID[i]) return false;
        return true;
    }

    /** Read the specified file and return a corresponding CartridgeFile object.
     *
     * @param source file representing the cartridge
     * @param savefile save file corresponding to this cartridge
     * @return a CartridgeFile object corresponding to source
     * @throws IOException
     */
    public static CartridgeFile read (SeekableFile source, FileHandle savefile)
    throws IOException {
        CartridgeFile cf = new CartridgeFile();
        cf.source = source;

        if (!cf.fileOk()) throw new IOException("invalid cartridge file");

        cf.scanOffsets();
        cf.scanHeader();

        cf.savegame = new Savegame(savefile);

        return cf;
    }

    private void scanOffsets () throws IOException {
        files = source.readShort();
        offsets = new int[files];
        ids = new int[files];
        for (int i = 0; i < files; i++) {
            ids[i] = source.readShort();
            offsets[i] = source.readInt();
        }
    }

    private void scanHeader () throws IOException {
        source.readInt(); // header length

        latitude = source.readDouble();
        longitude = source.readDouble();
        source.skip(8); // zeroes
        source.skip(4+4); // unknown long values
        splashId = source.readShort();
        iconId = source.readShort();
        type = source.readString();
        member = source.readString();
        source.skip(4+4); // unknown long values
        name = source.readString();
        source.readString(); // GUID
        description = source.readString();
        startdesc = source.readString();
        version = source.readString();
        author = source.readString();
        url = source.readString();
        device = source.readString();
        source.skip(4); // unknown long value
        code = source.readString();

        // assert source.position() ==
    }

    /** Return the Lua bytecode for this cartridge. */
    public byte[] getBytecode () throws IOException {
        source.seek(offsets[0]);
        int len = source.readInt();
        byte[] ffile = new byte[len];
        source.readFully(ffile);
        return ffile;
    }

    private int lastId = -1;
    private byte[] lastFile = null;

    /** Return data of the specified data file. */
    public byte[] getFile (int oid) throws IOException {
        if (oid == lastId) return lastFile;

        if (oid < 1) // invalid, apparently. or bytecode - lookie no touchie
            return null;

        int id = -1;
        for (int i = 0; i < ids.length; i++)
            if (ids[i] == oid) {
                id = i;
                break;
            }
        if (id == -1) return null;

        source.seek(offsets[id]);
        int a = source.read();
        // id of resource. 0 means deleted
        if (a < 1) return null;

        int ttype = source.readInt(); // we don't need this?
        int len = source.readInt();

        // we found the data - release cache
        lastFile = null;
        lastId = -1;

        byte[] ffile;
        try {
            ffile = new byte[len];
            source.readFully(ffile);
        } catch (OutOfMemoryError e) {
            return null;
        }

        if (len < CACHE_LIMIT) {
            lastId = oid;
            lastFile = ffile;
        }
        return ffile;
    }

    public Savegame getSavegame () throws IOException {
        return savegame;
    }

}
