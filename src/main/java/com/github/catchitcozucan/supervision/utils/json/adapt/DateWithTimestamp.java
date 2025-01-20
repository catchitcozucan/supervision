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
package com.github.catchitcozucan.supervision.utils.json.adapt;

import com.github.catchitcozucan.supervision.utils.DateUtils;

import java.util.Date;

public class DateWithTimestamp {
    private final String sourceDate;
    private static final String JSON_WRAP = "{\"value\":\"%s\",\"objectWrapperType\":\"java.lang.String\"}";

    public static DateWithTimestamp of(Long millis) {
        return new DateWithTimestamp(millis);
    }

    public static DateWithTimestamp of(Date date) {
        checkInput(date);
        return new DateWithTimestamp(date);
    }

    public static DateWithTimestamp of(String dateRepresentation) {
        checkInput(dateRepresentation);
        return new DateWithTimestamp(dateRepresentation);
    }

    public String toJson() {
        return String.format(JSON_WRAP, sourceDate);
    }

    public DateWithTimestamp(Long dateMillis) {
        checkInput(dateMillis);
        this.sourceDate = DateUtils.formatAndKeepTimeStamp(new Date(dateMillis));
    }

    public DateWithTimestamp(Date date) {
        this.sourceDate = DateUtils.formatAndKeepTimeStamp(date);
    }

    public DateWithTimestamp(String dateRepresentation) {
        this.sourceDate = DateUtils.formatAndKeepTimeStamp(dateRepresentation);
    }

    private static void checkInput(Object dateInput) {
        if (dateInput == null) {
            throw new IllegalArgumentException("Got no date input!");
        }
    }

    @Override
    public String toString() {
        return sourceDate;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DateWithTimestamp)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            return hashCode() == other.hashCode();
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
