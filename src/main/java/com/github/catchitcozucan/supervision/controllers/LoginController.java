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
package com.github.catchitcozucan.supervision.controllers;

import com.github.catchitcozucan.supervision.api.LoggedInResponse;
import com.github.catchitcozucan.supervision.api.Login;
import com.github.catchitcozucan.supervision.api.UserAndHashedPassword;
import com.github.catchitcozucan.supervision.exception.BadRequestException;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.exception.ForbiddenRequestException;
import com.github.catchitcozucan.supervision.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@Slf4j
@RequiredArgsConstructor
public class LoginController {

	private static final String JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";

	private final UserService userService;

	@PutMapping(value = "/set", produces = JSON_CHARSET_UTF_8, consumes = JSON_CHARSET_UTF_8)
	public UserAndHashedPassword create(@RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
		try {
			login.validate();
			return userService.createPassword(login.getUserName(), login.getPassword(), request, response);
		} catch (CatchitSupervisionRuntimeException exec) {
			throw new BadRequestException(exec);
		}
	}

	@PostMapping(value = "/verify", produces = JSON_CHARSET_UTF_8, consumes = JSON_CHARSET_UTF_8)
	public UserAndHashedPassword login(@RequestBody Login login, HttpServletRequest request, HttpServletResponse response) {
		try {
			login.validate();
			return userService.verifyLogin(login.getUserName(), login.getPassword(), request, response);
		} catch (CatchitSupervisionRuntimeException exec) {
			log.warn("Login failed", exec);
			throw new ForbiddenRequestException(exec);
		}
	}

	@GetMapping(value = "/loggedin", produces = JSON_CHARSET_UTF_8)
	public LoggedInResponse isLOggedIn(HttpServletRequest request, HttpServletResponse response) {
		try {
			return LoggedInResponse.builder().isLoggedIn(userService.thereIsAValidSession(request)).build();
		} catch (CatchitSupervisionRuntimeException exec) {
			throw new ForbiddenRequestException(exec);
		}
	}

}
