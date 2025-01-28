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
package com.github.catchitcozucan.supervision.service;

import com.github.catchitcozucan.supervision.api.UserAndHashedPassword;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.exception.ErrorCodes;
import com.github.catchitcozucan.supervision.repository.LoginRepository;
import com.github.catchitcozucan.supervision.repository.enteties.LoginEntity;
import com.github.catchitcozucan.supervision.utils.TomcatDetector;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.catchitcozucan.supervision.exception.ErrorCodes.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

	public static GrantedAuthority ADMIN = () -> "ADMIN";
	public static GrantedAuthority USER = () -> "USER";

	private static final String JSESSIONID = "JSESSIONID";
	private static final String THERE_IS_A_LIVE_SESSION = "There is a live session";
	private static final String WE_HAVE_NO_SESSION = "We have no session!";
	private static final String THERE_IS_A_COOKIE_AND_IS_IS_CALLED_CATCHITSUPERVISION = "There is a cookie and is is called catchitsupervision : ";
	private static final String USER_WITH_NAME_S_ALREADY_EXISTS_NOT_EXIST = "User with name %s already exists not exist";
	private static final String USER_DOES_NOT_EXIST = "User does not exist";
	private static final String ADMIN_USER_DOES_NOT_EXIST = "Admin user does not exist";
	private static final String MULTIPLE_ADMIN_USERS_EXIST = "Multiple admin users exist! That must be a corruption of data.. :)";
	private static final String MULTIPLE_USERS_WITH_USER_NAMSE_S_ALREADY_EXIST_LOGIN_TABLE_IS_CORRUPT = "Multiple users with user namse %s already exist - login-table is corrupt!";
	private static final String YUP_THERE_IS_ONE = "YUP-THERE-IS-ONE";
	private static final String ADMIN1 = "admin";
	private static final String USER1 = "user";
	private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";
	private static final String USER_S_IS_NOT_IN_SESSION = "User %s is NOT in session";
	private Argon2PasswordEncoder argo2Utility = new Argon2PasswordEncoder(4, 8, 4, 1024, 2);
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	@Value("${server.servlet.session.cookie.name}")
	private String cookieName;

	@Value("${server.servlet.contextPath}")
	private String servletPath;

	private final LoginRepository loginRepository;

	private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	public UserAndHashedPassword createPassword(String user, String pwdInput, HttpServletRequest request, HttpServletResponse response) {
		List<LoginEntity> logins = loginRepository.findAllByUsernameIs(user);
		UserAndHashedPassword userAndPwd = UserAndHashedPassword.builder().userName(user).hash(pwdInput).build();
		userAndPwd.validate();

		if (!user.equals(ADMIN1)) {
			if (!logins.isEmpty()) {
				throw new CatchitSupervisionRuntimeException(String.format(USER_WITH_NAME_S_ALREADY_EXISTS_NOT_EXIST, user), USER_NAME_ALREADY_EXISTS);
			}

			String hash = argo2Utility.encode(pwdInput);
			loginRepository.save(LoginEntity.builder().pwdHash(hash).username(userAndPwd.getUsername()).build());
			setupContext(user, USER, request, response);
		} else {
			if (logins.isEmpty()) {
				throw new CatchitSupervisionRuntimeException(ADMIN_USER_DOES_NOT_EXIST, ADMIN_USER_NAME_DOES_NOT_EXIST);
			}
			if (logins.size() != 1) {
				throw new CatchitSupervisionRuntimeException(MULTIPLE_ADMIN_USERS_EXIST, MULTIPLE_ADMIN_USERS_EXISTS);
			}

			LoginEntity adminLogin = logins.get(0);
			String hash = argo2Utility.encode(pwdInput);
			adminLogin.setPwdHash(hash);
			loginRepository.save(adminLogin);
			setupContext(ADMIN1, ADMIN, request, response);
		}
		userAndPwd.setHash(YUP_THERE_IS_ONE);
		return userAndPwd;
	}

	public UserAndHashedPassword verifyLogin(String user, String passwd, HttpServletRequest request, HttpServletResponse response) {
		List<LoginEntity> logins = loginRepository.findAllByUsernameIs(user);
		if (logins.isEmpty()) {
			LOGGER.warn("User {} does not exist", user);
			throw new CatchitSupervisionRuntimeException(USER_DOES_NOT_EXIST, USER_NAME_DOES_NOT_EXIST);
		} else if (logins.size() > 1) {
			String message = String.format(MULTIPLE_USERS_WITH_USER_NAMSE_S_ALREADY_EXIST_LOGIN_TABLE_IS_CORRUPT, user);
			LOGGER.warn(message);
			throw new CatchitSupervisionRuntimeException(message, MULTIPLE_USERS_WITH_THE_SAME_NAME);
		} else {
			String pwdHash = logins.get(0).getPwdHash();
			if (argo2Utility.matches(passwd, pwdHash)) {
				setupContext(user, user.equals(ADMIN1) ? ADMIN : USER, request, response);
				return UserAndHashedPassword.builder().userName(user).hash(YUP_THERE_IS_ONE).build();
			} else {
				LOGGER.warn("Wrong passwd provided for user {}", user);
				throw new CatchitSupervisionRuntimeException(ErrorCodes.PWD_DOES_NOT_MATCH);
			}
		}
	}

	public boolean thereIsAValidSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			log.info(THERE_IS_A_LIVE_SESSION);
		} else {
			log.info(WE_HAVE_NO_SESSION);
		}
		if (session != null && request.getCookies() != null) {

			String cName = cookieName;
			//if (TomcatDetector.isRunningWithinTomcat()) {
			//    cName = JSESSIONID;
			//}
			final String cookieNameForLookup = cName;

			Optional<Cookie> possCookie = Arrays.stream(request.getCookies()).filter(c -> {
				return c.getName().equals(cookieNameForLookup);
			}).findFirst();
			log.info(THERE_IS_A_COOKIE_AND_IS_IS_CALLED_CATCHITSUPERVISION + possCookie.isPresent());
			return possCookie.isPresent() && possCookie.get().getValue().equals(session.getId());
		}
		return adminIsAuthenticated();
	}

	private void setupContext(String userName, GrantedAuthority grantedAuthority, HttpServletRequest request, HttpServletResponse response) {
		if (!thereIsAValidSession(request)) {
			SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
			Authentication authentication = new UsernamePasswordAuthenticationToken(userName, "secret", Arrays.asList(grantedAuthority));
			securityContext.setAuthentication(authentication);
			SecurityContextHolder.setContext(securityContext);
			// Save the security context to the repo (This adds it to the HTTP session)
			securityContextRepository.saveContext(securityContext, request, response);
			// Create a new session and add the security context.
			HttpSession session = request.getSession(true);
			session.setMaxInactiveInterval(600); // ten minutes..

			// for tomcat
			session.setAttribute(SPRING_SECURITY_CONTEXT, securityContext);
			session.getServletContext().setAttribute(SPRING_SECURITY_CONTEXT, securityContext);
			LOGGER.info("User {} is now logged in and has a fresh session", userName);
		}
	}

	@Override
	public UserDetails loadUserByUsername(String user) throws UsernameNotFoundException {
		List<LoginEntity> logins = loginRepository.findAllByUsernameIs(user);
		if (logins.isEmpty()) {
			LOGGER.warn("User {} does not exist", user);
			throw new UsernameNotFoundException(USER_DOES_NOT_EXIST);
		} else if (logins.size() > 1) {
			String message = String.format(MULTIPLE_USERS_WITH_USER_NAMSE_S_ALREADY_EXIST_LOGIN_TABLE_IS_CORRUPT, user);
			LOGGER.warn(message);
			throw new UsernameNotFoundException(message);
		} else {
			if ((adminIsAuthenticated() && user.equals(ADMIN1)) || getAuthenticatedUserName().equals(user)) {
				LoginEntity loginEntity = logins.get(0);
				return UserAndHashedPassword.builder().userName(loginEntity.getUsername()).hash(loginEntity.getPwdHash()).build();
			} else {
				String message = String.format(USER_S_IS_NOT_IN_SESSION, user);
				LOGGER.warn(message);
				throw new UsernameNotFoundException(message);
			}
		}
	}

	private boolean thereIsAnAuthenticatedUser() {
		return SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null;
	}

	private String getAuthenticatedUserName() {
		if (thereIsAnAuthenticatedUser()) {
			return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
		} else {
			return "";
		}
	}

	private boolean adminIsAuthenticated() {
		return thereIsAnAuthenticatedUser() && SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals(ADMIN1);
	}
}
