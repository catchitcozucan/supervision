/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils;

public class CompareUtils {

    public static final int UP_FOR_ACTUAL_COMPARE = 777;

    public static final int BOTH_OBJECTS_ARE_NON_NULL = 666;

    private CompareUtils() {}

    public static int nullCompare(Object o1, Object o2) {
        if (o1 == null && o2 != null) {
            return 1;
        } else if (o2 == null && o1 != null) {
            return -1;
        } else if (o1 == null && o2 == null) { //NOSONAR
            return 0;
        } else {
            return BOTH_OBJECTS_ARE_NON_NULL;
        }
    }

    public static int nullSafeCompare(Comparable o1, Comparable o2) {
        int result = nullCompare(o1, o2);
        if (result == BOTH_OBJECTS_ARE_NON_NULL) {
            return o1.compareTo(o2);
        } else {
            return result;
        }
    }

    public static int compareForNulling(Object o1, Object o2) {
        int outcome = UP_FOR_ACTUAL_COMPARE;
        if (o1 == null && o2 != null) {
            outcome = 1;
        } else if (o2 == null && o1 != null) {
            outcome = -1;
        } else if (o1 == null && o2 == null) { //NOSONAR
            outcome = 0;
        }
        return outcome;
    }
}