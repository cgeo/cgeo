/*
 * This file is part of WhereYouGo.
 * 
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class StringToken implements Enumeration<Object> {

    private String string;
    private String delimiters;
    private int position;

    /**
     * Constructs a new {@code StringTokenizer} for the parameter string using the specified
     * delimiters. The {@code returnDelimiters} flag is set to {@code false}. If {@code delimiters} is
     * {@code null}, this constructor doesn't throw an {@code Exception}, but later calls to some
     * methods might throw a {@code NullPointerException}.
     *
     * @param string     the string to be tokenized.
     * @param delimiters the delimiters to use.
     */
    public StringToken(String string, String delimiters) {
        if (string != null) {
            this.string = string;
            this.delimiters = delimiters;
            this.position = 0;
        } else
            throw new NullPointerException();
    }

    public static ArrayList<String> parse(String data, String delimiters) {
        return parse(data, delimiters, new ArrayList<String>());
    }

    public static ArrayList<String> parse(String data, String delimiters, ArrayList<String> tokens) {
        // replace delimiters in token to receive correct item count
        data = data.replace(delimiters, " " + delimiters);
        StringToken token = new StringToken(data, delimiters);
        while (token.hasMoreTokens()) {
            tokens.add(token.nextToken().trim());
        }
        return tokens;
    }

    /**
     * Returns the number of unprocessed tokens remaining in the string.
     *
     * @return number of tokens that can be retrieved before an {@code Exception} will result from a
     * call to {@code nextToken()}.
     */
    public int countTokens() {
        int count = 0;
        boolean inToken = false;
        for (int i = position, length = string.length(); i < length; i++) {
            if (delimiters.indexOf(string.charAt(i), 0) >= 0) {
                if (inToken) {
                    count++;
                    inToken = false;
                }
            } else {
                inToken = true;
            }
        }
        if (inToken)
            count++;
        return count;
    }

    @Override
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    /**
     * Returns {@code true} if unprocessed tokens remain.
     *
     * @return {@code true} if unprocessed tokens remain.
     */
    public boolean hasMoreTokens() {
        if (delimiters == null) {
            throw new NullPointerException();
        }
        int length = string.length();
        if (position < length) {
            for (int i = position; i < length; i++)
                if (delimiters.indexOf(string.charAt(i), 0) == -1)
                    return true;
        }
        return false;
    }

    /**
     * Returns the next token in the string as an {@code Object}. This method is implemented in order
     * to satisfy the {@code Enumeration} interface.
     *
     * @return next token in the string as an {@code Object}
     * @throws NoSuchElementException if no tokens remain.
     */
    public Object nextElement() {
        return nextToken();
    }

    /**
     * Returns the next token in the string as a {@code String}.
     *
     * @return next token in the string as a {@code String}.
     * @throws NoSuchElementException if no tokens remain.
     */
    public String nextToken() {
        if (delimiters == null) {
            throw new NullPointerException();
        }
        int i = position;
        int length = string.length();

        if (i < length) {
            while (i < length && delimiters.indexOf(string.charAt(i), 0) >= 0)
                i++;
            position = i;
            if (i < length) {
                for (position++; position < length; position++)
                    if (delimiters.indexOf(string.charAt(position), 0) >= 0)
                        return string.substring(i, position);
                return string.substring(i);
            }
        }
        throw new NoSuchElementException();
    }
}
