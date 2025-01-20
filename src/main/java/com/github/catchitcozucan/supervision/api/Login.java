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
package com.github.catchitcozucan.supervision.api;

import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.exception.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Login {

    private static final String ALPHA_NUMERIC = "^[a-zA-Z0-9]*$";
    private String userName;
    private String password;

    public void validate() {
        if (!userName.matches(ALPHA_NUMERIC)) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.USERNAME_NO_ALPHA);
        }
        if (userName == null || userName.length() < 2) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.USERNAME_TOO_SHORT);
        }
        if (password == null || password.length() < 3 || password.length() > 40) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.PWD_TOO_SHORT_OR_TO_LONG);
        }
    }
}
