/*
Copyright (c) 2007 - 2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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



public interface JavaFunction {
    /**
     * General contract<br>
     * <br>
     *  Input:<br>
     *  callFrame = the frame that contains all the arguments, and where all the results should be put.<br> 
     *  nArgs = number of function arguments<br>
     *  callFrame.get(i) = an argument (0 <= i < nArgs)<br> 
     *  
     * @param callFrame - the current callframe for the function 
     * @param nArguments - number of function arguments 
     * @return N - number of return values. The N top objects on the stack are considered the return values 
     */
    public abstract int call(LuaCallFrame callFrame, int nArguments);
}
