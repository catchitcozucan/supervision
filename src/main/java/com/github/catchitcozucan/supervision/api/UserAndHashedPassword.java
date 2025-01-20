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
package com.github.catchitcozucan.supervision.api;

import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.exception.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static com.github.catchitcozucan.supervision.service.UserService.ADMIN;
import static com.github.catchitcozucan.supervision.service.UserService.USER;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserAndHashedPassword implements UserDetails {
    private static final String ALPHA_NUMERIC = "^[a-zA-Z0-9]*$";
    private String hash;
    private String userName;

    public void validate() {
        if (!userName.matches(ALPHA_NUMERIC)) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.USERNAME_NO_ALPHA);
        }
        if (userName == null || userName.length() < 2) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.USERNAME_TOO_SHORT);
        }
        if (hash == null || hash.length() < 5 || hash.length() > 38) {
            throw new CatchitSupervisionRuntimeException(ErrorCodes.PWD_TOO_SHORT_OR_TO_LONG);
        }
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getUsername().equals("admin") ? List.of(ADMIN) : List.of(USER);
    }

    @Override
    public String getPassword() {
        return hash;
    }

    @Override
    public String getUsername() {
        return userName;
    }
}
