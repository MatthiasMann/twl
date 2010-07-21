/*
 * Copyright (c) 2008, Matthias Mann
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twl.utils;

/**
 * Utilities for handling texts
 * @author Matthias Mann
 */
public final class TextUtil {

    private TextUtil() {
    }
    
    /**
     * Counts the number of lines in the text. Lines are splitted with '\n'
     * @param str the text to count lines in
     * @return the number of lines - 0 for an empty string
     */
    public static int countNumLines(CharSequence str) {
        final int n = str.length();
        int count = 0;
        if(n > 0) {
            count++;
            for(int i=0 ; i<n ; i++) {
                if(str.charAt(i) == '\n') {
                    count++;
                }
            }
        }
        return count;
    }

    public static String stripNewLines(String str) {
        int idx = str.lastIndexOf('\n');
        if(idx < 0) {
            // don't waste time when no newline is present
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        do {
            if(sb.charAt(idx) == '\n') {
                sb.deleteCharAt(idx);
            }
        } while (--idx >= 0);
        return sb.toString();
    }

    public static String limitStringLength(String str, int length) {
        if(str.length() > length) {
            return str.substring(0, length);
        }
        return str;
    }

    public static String notNull(String str) {
        if(str == null) {
            return "";
        }
        return str;
    }

    /**
     * Searches for a specific character.
     * @param cs the CharSequence to search in
     * @param ch the character to search
     * @param start the start index. must be >= 0.
     * @return the index of the character or cs.length().
     */
    public static int indexOf(CharSequence cs, char ch, int start) {
        final int n = cs.length();
        for(; start<n ; start++) {
            if(cs.charAt(start) == ch) {
                return start;
            }
        }
        return n;
    }

    /**
     * Searches for a specific character.
     * @param str the String to search in
     * @param ch the character to search
     * @param start the start index. must be >= 0.
     * @return the index of the character or str.length().
     */
    public static int indexOf(String str, char ch, int start) {
        int idx = str.indexOf(ch, start);
        if(idx < 0) {
            return str.length();
        }
        return idx;
    }

    public static int skipSpaces(CharSequence s, int start) {
        return skipSpaces(s, start, s.length());
    }

    public static int skipSpaces(CharSequence s, int start, int end) {
        while(start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        return start;
    }

    public static String createString(char ch, int len) {
        char[] buf = new char[len];
        for(int i=0 ; i<len ; i++) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    private static final String ROMAN_NUMBERS = "ↂMↂↁMↁMCMDCDCXCLXLXIXVIVI";
    private static final String ROMAN_VALUES = "\u2710\u2328\u1388\u0FA0\u03E8\u0384\u01F4\u0190\144\132\62\50\12\11\5\4\1";

    /**
     * The largest number which can be converted into a roman number.
     * @see #toRomanNumberString(int)
     */
    public static final int MAX_ROMAN_INTEGER = 39999;
    
    /**
     * Creates an upper case roman number string for the given value.
     *
     * Values above 3999 need characters from unicode block "Number Forms" to be displayed correctly.
     *
     * @param value the value. Must be &gt;= 1 and &lt;= {@link #MAX_ROMAN_INTEGER}
     * @return the roman number string in upper case
     * @throws IllegalArgumentException if the value is out of range
     */
    public static String toRomanNumberString(int value) throws IllegalArgumentException {
        if(value < 1 || value > MAX_ROMAN_INTEGER) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        int idxValues = 0;
        int idxNumbers = 0;
        do {
            final int romanValue = ROMAN_VALUES.charAt(idxValues);
            final int romanNumberLen = (idxValues & 1) + 1;
            while(value >= romanValue) {
                sb.append(ROMAN_NUMBERS, idxNumbers, idxNumbers+romanNumberLen);
                value -= romanValue;
            }
            idxNumbers += romanNumberLen;
        } while (++idxValues < 17);
        return sb.toString();
    }

    private static void toLatinNumberString(int value, StringBuilder sb) {
        if(value >= 26) {
            toLatinNumberString(value / 26 - 1, sb);
            value %= 26;
        }
        sb.append((char)('A' + value));
    }

    /**
     * Creates a string which represents the specified value via latin characters like "A" ... "Z", "AA" ... "AZ", ...
     * @param value the value. Must be &gt;= 1.
     * @return the latin number string
     * @throws IllegalArgumentException if the value if &lt; 1
     */
    public static String toLatinNumberString(int value) throws IllegalArgumentException {
        if(value < 1) {
            throw new IllegalArgumentException("value");
        }
        StringBuilder sb = new StringBuilder();
        toLatinNumberString(value-1, sb);
        return sb.toString();
    }
}
