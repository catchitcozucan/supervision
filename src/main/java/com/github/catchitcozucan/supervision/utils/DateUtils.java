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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DateUtils {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_NO_TIME = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final DateFormat JVM_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");//NOSONAR
    private static final DateFormat JVM_FORMAT2 = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss aaa");//NOSONAR
    private static final DateFormat JVM_FORMAT3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);//NOSONAR
    private static final DateFormat JVM_FORMAT4 = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.forLanguageTag("sv_SE"));//NOSONAR
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateFormat DF = new SimpleDateFormat(DATE_PATTERN);//NOSONAR

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private DateUtils() {}

    public static Date fromString(String dateAsString) {
        return fromString(dateAsString);
    }

    public static String formatAndKeepTimeStamp(Date date) {
        return toString(date, DATE_TIME_PATTERN);
    }

    public static String formatAndKeepTimeStamp(String dateRepr) {
        return toString(fromString(dateRepr, DATE_TIME_PATTERN), DATE_TIME_PATTERN);
    }

    public static Date fromString(String dateAsString, String datePattern) { // NOSONAR
        if (dateAsString == null || dateAsString.trim().isEmpty() || "null".equalsIgnoreCase(dateAsString)) {
            return null;
        }
        try {
            if (dateAsString.matches(DATE_NO_TIME)) {
                return org.apache.commons.lang.time.DateUtils.parseDate(dateAsString, new String[]{datePattern});
            } else {
                try { //NOSONAR - in this case they can and it no problem understanding the implications..
                    return JVM_FORMAT.parse(dateAsString);
                } catch (ParseException e2) {
                    try { //NOSONAR - in this case they can and it no problem understanding the implications..
                        return JVM_FORMAT2.parse(dateAsString);
                    } catch (ParseException ignore) {
                        try { //NOSONAR - in this case they can and it no problem understanding the implications..
                            return JVM_FORMAT3.parse(dateAsString);
                        } catch (ParseException ignore2) {
                            try { //NOSONAR - in this case they can and it no problem understanding the implications..
                                return JVM_FORMAT4.parse(dateAsString);
                            } catch (ParseException ignore3) {
                                // NOSONAR DON'T YOU MIND YOUR LITTLE TINY HEAD
                            }
                        }
                    } //NOSONAR why should you care?
                }
            }
        } catch (ParseException e1) {
            // we _should_ throw here, I think, but this is used _everywhere_ so.. I
            // do NOT dare to change it
        }
        return null;
    }

    public static String toString(Date date) {
        return date != null ? DF.format(date) : "";
    }

    public static String toString(Date date, String format) {
        DateFormat df = new SimpleDateFormat(format);//NOSONAR
        return date != null ? df.format(date) : "";
    }

    public static Date localDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date firstDateEver() {
        try {
            return SDF.parse(SDF.format(new Date(0)));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}