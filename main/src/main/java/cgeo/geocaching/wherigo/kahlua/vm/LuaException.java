/*
Copyright (c) 2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
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
--
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.vm
*/
package cgeo.geocaching.wherigo.kahlua.vm;

public class LuaException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public Object errorMessage;

    public LuaException(Object errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessage() {
        if (errorMessage == null) {
            return "nil";
        }
        return errorMessage.toString();
    }
}
