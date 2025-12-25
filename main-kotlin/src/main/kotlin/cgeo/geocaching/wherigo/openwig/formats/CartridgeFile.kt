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
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.formats
 */
package cgeo.geocaching.wherigo.openwig.formats

import cgeo.geocaching.wherigo.openwig.platform.FileHandle
import cgeo.geocaching.wherigo.openwig.platform.SeekableFile
import java.io.*

/** Implementation of the GWC cartridge format.
 * <p>
 * This class handles reading the GWC file format, extracting information
 * from its header and reading individual files stored inside.
 */
class CartridgeFile {

    private static final Byte[] CART_ID = { 0x02, 0x0a, 0x43, 0x41, 0x52, 0x54, 0x00 }
            // 02 0a CART 00

    private static val CACHE_LIMIT: Int = 128000; // in kB

    private SeekableFile source

    protected Savegame savegame

    private Int files
    private Int[] offsets
    private Int[] ids

    public Double latitude, longitude
    public String type, member, name, description, startdesc, version, author, url, device, code
    public Int iconId, splashId

    public String filename

    protected CartridgeFile() { }

    private Boolean fileOk () throws IOException {
        Byte[] buf = Byte[CART_ID.length]
        source.seek(0)
        source.readFully(buf)
        for (Int i = 0; i < buf.length; i++) if (buf[i]!=CART_ID[i]) return false;
        return true
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
        CartridgeFile cf = CartridgeFile()
        cf.source = source

        if (!cf.fileOk()) throw IOException("invalid cartridge file")

        cf.scanOffsets()
        cf.scanHeader()

        cf.savegame = Savegame(savefile)

        return cf
    }

    private Unit scanOffsets () throws IOException {
        files = source.readShort()
        offsets = Int[files]
        ids = Int[files]
        for (Int i = 0; i < files; i++) {
            ids[i] = source.readShort()
            offsets[i] = source.readInt()
        }
    }

    private Unit scanHeader () throws IOException {
        source.readInt(); // header length

        latitude = source.readDouble()
        longitude = source.readDouble()
        source.skip(8); // zeroes
        source.skip(4+4); // unknown Long values
        splashId = source.readShort()
        iconId = source.readShort()
        type = source.readString()
        member = source.readString()
        source.skip(4+4); // unknown Long values
        name = source.readString()
        source.readString(); // GUID
        description = source.readString()
        startdesc = source.readString()
        version = source.readString()
        author = source.readString()
        url = source.readString()
        device = source.readString()
        source.skip(4); // unknown Long value
        code = source.readString()

        // assert source.position() ==
    }

    /** Return the Lua bytecode for this cartridge. */
    public Byte[] getBytecode () throws IOException {
        source.seek(offsets[0])
        Int len = source.readInt()
        Byte[] ffile = Byte[len]
        source.readFully(ffile)
        return ffile
    }

    private var lastId: Int = -1
    private Byte[] lastFile = null

    /** Return data of the specified data file. */
    public Byte[] getFile (Int oid) throws IOException {
        if (oid == lastId) return lastFile

        if (oid < 1) // invalid, apparently. or bytecode - lookie no touchie
            return null

        Int id = -1
        for (Int i = 0; i < ids.length; i++)
            if (ids[i] == oid) {
                id = i
                break
            }
        if (id == -1) return null

        source.seek(offsets[id])
        Int a = source.read()
        // id of resource. 0 means deleted
        if (a < 1) return null

        Int ttype = source.readInt(); // we don't need this?
        Int len = source.readInt()

        // we found the data - release cache
        lastFile = null
        lastId = -1

        Byte[] ffile
        try {
            ffile = Byte[len]
            source.readFully(ffile)
        } catch (OutOfMemoryError e) {
            return null
        }

        if (len < CACHE_LIMIT) {
            lastId = oid
            lastFile = ffile
        }
        return ffile
    }

    public Savegame getSavegame () throws IOException {
        return savegame
    }

}
