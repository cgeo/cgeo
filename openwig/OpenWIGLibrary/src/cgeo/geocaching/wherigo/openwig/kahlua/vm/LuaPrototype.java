/*
Copyright (c) 2007 - 2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package cgeo.geocaching.wherigo.openwig.kahlua.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class LuaPrototype {
    public int[] code;

    // Constants
    public Object[] constants;
    public LuaPrototype[] prototypes;

    public int numParams;

    public boolean isVararg;

    // debug info
    public String name;
    public int[] lines;

    public int numUpvalues;

    public int maxStacksize;
    
    public LuaPrototype() {
    }

    public LuaPrototype(DataInputStream in, boolean littleEndian, String parentName, int sizeT) throws IOException {
        int tmp;

        name = readLuaString(in, sizeT, littleEndian);
        if (name == null) {
            name = parentName;
        }

        // Commented out since they are not used
        // read line defined and last line defined
        in.readInt();
        in.readInt();

        numUpvalues = in.read();
        numParams = in.read();
        int isVararg = in.read();
        this.isVararg = (isVararg & 2) != 0;
        maxStacksize = in.read();

        int codeLen = toInt(in.readInt(), littleEndian);
        code = new int[codeLen];
        for (int i = 0; i < codeLen; i++) {
            int op = toInt(in.readInt(), littleEndian);
            code[i] = op;
        }

        int constantsLen = toInt(in.readInt(), littleEndian);
        constants = new Object[constantsLen];
        for (int i = 0; i < constantsLen; i++) {
            Object o = null;
            int type = in.read();
            switch (type) {
            case 0:
                // Do nothing - this constant is null by default
                break;
            case 1:
                int b = in.read();
                o = b == 0 ? Boolean.FALSE : Boolean.TRUE;
                break;
            case 3:
                long bits = in.readLong();
                if (littleEndian) {
                    bits = rev(bits);
                }
                o = LuaState.toDouble(Double.longBitsToDouble(bits));
                break;
            case 4:
                o = readLuaString(in, sizeT, littleEndian);
                break;
            default:
                throw new IOException("unknown constant type: " + type);
            }
            constants[i] = o;
        }

        int prototypesLen = toInt(in.readInt(), littleEndian);
        prototypes = new LuaPrototype[prototypesLen];
        for (int i = 0; i < prototypesLen; i++) {
            prototypes[i] = new LuaPrototype(in, littleEndian, name, sizeT);
        }

        // DEBUGGING INFORMATION

        // read lines
        tmp = toInt(in.readInt(), littleEndian);

        lines = new int[tmp];

        for (int i = 0; i < tmp; i++) {
            int tmp2 = toInt(in.readInt(), littleEndian);
            lines[i] = tmp2;
        }

        // skip locals
        tmp = toInt(in.readInt(), littleEndian);
        for (int i = 0; i < tmp; i++) {
            readLuaString(in, sizeT, littleEndian);
            in.readInt();
            in.readInt();
        }

        // read upvalues
        tmp = toInt(in.readInt(), littleEndian);
        for (int i = 0; i < tmp; i++) {
            readLuaString(in, sizeT, littleEndian);
        }
    }

    public String toString() {
        return name;
    }

    // NOTE: known weakness - will crash if a string is longer than 2^16 - 1
    private static String readLuaString(DataInputStream in, int sizeT, boolean littleEndian) throws IOException {
        long len = 0;

        if (sizeT == 4) {
            int i = in.readInt();
            len = toInt(i, littleEndian);
        } else if (sizeT == 8) {
            len = toLong(in.readLong(), littleEndian);
        } else {
            loadAssert(false, "Bad string size");
        }

        if (len == 0) {
            return null;
        }

        len = len - 1;

        // Change this to a proper string loader if you need longer strings.
        // The extra code needed seems unnecessary for the common use cases.
        loadAssert(len < 0x10000, "Too long string:" + len);

        int iLen = (int) len;
        byte[] stringData = new byte[2 + 1 + iLen];

        stringData[0] = (byte) ((iLen >> 8) & 0xff);
        stringData[1] = (byte) (iLen & 0xff);

        // Remember to read the trailing 0 too
        in.readFully(stringData, 2, iLen + 1);
        loadAssert(stringData[2 + iLen] == 0, "String loading");

        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(stringData));
            String s = dis.readUTF();
            dis.close();

            return s;
        } catch (IOException e) {
            return loadUndecodable(stringData);
        }
    }
    
    private static String loadUndecodable (byte[] bytes) {
        // it is unlikely to be a broken UTF, more likely someting
        // in an unknown encoding. replace every non-ASCII with '?'
        for (int i = 2; i < bytes.length; i++)
            if ((bytes[i] & 0x80) == 0x80) bytes[i] = (byte)'?';
        return new String(bytes, 2, bytes.length - 2);
    }

    public static int rev(int v) {
        int a, b, c, d;
        a = (v >>> 24) & 0xff;
        b = (v >>> 16) & 0xff;
        c = (v >>> 8) & 0xff;
        d = v & 0xff;
        return (d << 24) | (c << 16) | (b << 8) | a;

        // This works in J2SE, but not J2ME
        // return Integer.reverseBytes(v);
    }

    public static long rev(long v) {
        long a, b, c, d, e, f, g, h;
        a = (v >>> 56) & 0xff;
        b = (v >>> 48) & 0xff;
        c = (v >>> 40) & 0xff;
        d = (v >>> 32) & 0xff;
        e = (v >>> 24) & 0xff;
        f = (v >>> 16) & 0xff;
        g = (v >>> 8) & 0xff;
        h = v & 0xff;
        return  (h << 56) | (g << 48)
            | (f << 40) | (e << 32)
            | (d << 24) | (c << 16)
            | (b << 8) | a;

        // This works in J2SE, but not J2ME
        // return Long.reverseBytes(v);
    }

    public static int toInt(int bits, boolean littleEndian) {
        return littleEndian ? rev(bits) : bits;
    }

    public static long toLong(long bits, boolean littleEndian) {
        return littleEndian ? rev(bits) : bits;
    }


    public static LuaClosure loadByteCode(DataInputStream in, LuaTable env)
    throws IOException {
        int tmp;

//      Read lua header
        tmp = in.read();
        loadAssert(tmp == 27, "Signature 1");

        tmp = in.read();
        loadAssert(tmp == 'L', "Signature 2");

        tmp = in.read();
        loadAssert(tmp == 'u', "Signature 3");

        tmp = in.read();
        loadAssert(tmp == 'a', "Signature 4");

//      Version = 5.1
        tmp = in.read();
        loadAssert(tmp == 0x51, "Version");

//      Format
        tmp = in.read();
        loadAssert(tmp == 0, "Format");

//      Little Endian!
        boolean littleEndian = in.read() == 1;

//      Size of int
        tmp = in.read();
        loadAssert(tmp == 4, "Size int");

//      Size of sizeT
        int sizeT = in.read();
        loadAssert(sizeT == 4 || sizeT == 8, "Size t");

//      Size of instruction
        tmp = in.read();
        loadAssert(tmp == 4, "Size instr");

//      Size of number
        tmp = in.read();
        loadAssert(tmp == 8, "Size number");

//      Integral
        tmp = in.read();
        loadAssert(tmp == 0, "Integral");

//      Done with header, start reading functions
        LuaPrototype mainPrototype = new LuaPrototype(in, littleEndian, null, sizeT);
        LuaClosure closure = new LuaClosure(mainPrototype, env);
        return closure;
    }

    private static void loadAssert(boolean c, String message) throws IOException {
        if (!c) {
            throw new IOException("Could not load bytecode:" + message);
        }
    }

    public static LuaClosure loadByteCode(InputStream in, LuaTable env) throws IOException {
        if (!(in instanceof DataInputStream)) {
            in = new DataInputStream(in);
        }
        return loadByteCode((DataInputStream) in, env);
    }
    
    /*
     * Dump functions to stream
     */

    public void dump(OutputStream os) throws IOException {
        DataOutputStream dos;
        if (os instanceof DataOutputStream) {
            dos = (DataOutputStream) os;
        } else {
            dos = new DataOutputStream(os);
        }
        
        // signature
        dos.write(27);      
        dos.write('L');
        dos.write('u');
        dos.write('a');
        
        dos.write(0x51); // version
        dos.write(0); // format
        dos.write(0); // 0 = big endian, 1 = little endian
        
        dos.write(4); // size of int
        dos.write(4); // size t
        dos.write(4); // size of instruction
        dos.write(8); // size of number
        dos.write(0); // integral
        
        // Start dumping prototypes
        dumpPrototype(dos);
    }
    
    private void dumpPrototype(DataOutputStream dos) throws IOException {
        dumpString(name, dos);

        // Commented out since they are not used
        // read line defined and last line defined
        dos.writeInt(0);
        dos.writeInt(0);

        dos.write(numUpvalues);
        dos.write(numParams);
        dos.write(isVararg ? 2 : 0);
        dos.write(maxStacksize);
        
        int codeLen = code.length;
        dos.writeInt(codeLen);
        for (int i = 0; i < codeLen; i++) {
            dos.writeInt(code[i]);
        }

        int constantsLen = constants.length;
        dos.writeInt(constantsLen);
        for (int i = 0; i < constantsLen; i++) {
            Object o = constants[i];
            if (o == null) {
                dos.write(0);
            } else if (o instanceof Boolean) {
                dos.write(1);
                dos.write(((Boolean) o).booleanValue() ? 1 : 0);                
            } else if (o instanceof Double) {
                dos.write(3);
                Double d = (Double) o;
                dos.writeLong(Double.doubleToLongBits(d.doubleValue()));
            } else if (o instanceof String) {
                dos.write(4);
                dumpString((String) o, dos);
            } else {
                throw new RuntimeException("Bad type in constant pool");
            }
        }

        int prototypesLen = prototypes.length;
        dos.writeInt(prototypesLen);
        for (int i = 0; i < prototypesLen; i++) {
            prototypes[i].dumpPrototype(dos);
        }

        // DEBUGGING INFORMATION

        // read lines
        int linesLen = lines.length;
        dos.writeInt(linesLen);
        for (int i = 0; i < linesLen; i++) {
            dos.writeInt(lines[i]);
        }

        // skip locals
        dos.writeInt(0);

        // read upvalues
        dos.writeInt(0);        
    }

    private static void dumpString(String name, DataOutputStream dos) throws IOException {
        if (name == null) {
            dos.writeShort(0);
            return;
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DataOutputStream(baos).writeUTF(name);
        byte[] bytes = baos.toByteArray();
        int numBytes = bytes.length - 2;
        dos.writeInt(numBytes + 1); // 1 extra for lua string storage spec
        dos.write(bytes, 2, numBytes);
        dos.write(0);
    }

}
