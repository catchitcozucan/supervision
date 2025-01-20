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
package com.github.catchitcozucan.supervision.exception;

public class ErrorCodes {
    public static final int PROXY_IS_UNKNOWN = 2;
    public static final int COULD_NOT_LOCATE_STAT_SOURCES = 3;
    public static final int USER_NAME_DOES_NOT_EXIST = 4;
    public static final int ADMIN_USER_NAME_DOES_NOT_EXIST = 5;
    public static final int MULTIPLE_ADMIN_USERS_EXISTS = 6;
    public static final int USER_NAME_ALREADY_EXISTS = 7;
    public static final int MULTIPLE_USERS_WITH_THE_SAME_NAME = 8;
    public static final int USERNAME_NO_ALPHA = 9;
    public static final int USERNAME_TOO_SHORT = 10;
    public static final int PWD_TOO_SHORT_OR_TO_LONG = 11;
    public static final int PWD_DOES_NOT_MATCH = 12;
    public static final int NO_CONTENT_TYPE_IN_RESPONSE = 12;
    public static final int BAD_CONTENT_TYPE_IN_RESPONSE = 13;
    public static final int REQUEST_GIVES_UNKNOWN_HOST_EXCEPTION = 14;
    public static final int REQUEST_GIVES_HTTP_TIMEOUT_EXCEPTION = 15;
    public static final int REQUEST_GIVES_HTTP_CONECTION_TIMEOUT_EXCEPTION = 16;
    public static final int REQUEST_GIVES_CONNECTION_REFUSED = 17;
    public static final int BAD_RESPONSE_HTTP_CODE = 18;
}
