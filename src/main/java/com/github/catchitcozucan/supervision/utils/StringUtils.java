/**
 *    Original work by Ola Aronsson 2020
 *    Courtesy of nollettnoll AB &copy; 2012 - 2020
 *
 *    Licensed under the Creative Commons Attribution 4.0 International (the "License")
 *    you may not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *                https://creativecommons.org/licenses/by/4.0/
 *
 *    The software is provided “as is”, without warranty of any kind, express or
 *    implied, including but not limited to the warranties of merchantability,
 *    fitness for a particular purpose and noninfringement. In no event shall the
 *    authors or copyright holders be liable for any claim, damages or other liability,
 *    whether in an action of contract, tort or otherwise, arising from, out of or
 *    in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class StringUtils {

    private static final String EMPTY_LINES = "(?m)^[ \t]*\r?\n";
    private static final String MULTIPLE_QOUTES = "^\"|\"$";
    private static final String MULTIPLE_SPACE = "\\s+";
    private static final String ONE_SPACE = " ";
    private static final String NO_SPACE = "";
    private static final String CONTROL_CHARACTERS = "[\u0000-\u001f]";

    private StringUtils() {}

    public static String compact(String input) {
        return input.replaceAll(EMPTY_LINES, NO_SPACE).replaceAll(CONTROL_CHARACTERS, NO_SPACE).replaceAll(MULTIPLE_SPACE, ONE_SPACE).replaceAll(MULTIPLE_QOUTES, NO_SPACE);
    }

    public static boolean looksLikeTrue(String input) {
        if (!hasContents(input)) {
            return false;
        } else {
            return "true".equalsIgnoreCase(input.trim());
        }
    }

    public static boolean hasContents(String input) {
        return input != null && input.trim().length() > 0;
    }

    public static String fromStreamCloseUponFinish(InputStream stream) {
        String result = null;
        try {
            StringBuilder b = new StringBuilder();
            new BufferedReader(new InputStreamReader(stream)).lines().forEach(s -> b.append(new String(s.getBytes(), StandardCharsets.UTF_8)));
            result = b.toString();
        } catch (Exception ignore) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        }
        return compact(result); //NOSONAR - DONT GET IT..
    }

}
