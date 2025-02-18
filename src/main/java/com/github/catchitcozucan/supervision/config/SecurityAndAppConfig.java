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
package com.github.catchitcozucan.supervision.config;

import com.github.catchitcozucan.core.impl.CatchIt;
import com.github.catchitcozucan.core.impl.ProcessLogging;
import com.github.catchitcozucan.core.impl.startup.NumberOfTimeUnits;
import com.github.catchitcozucan.core.interfaces.CatchItConfig;
import com.github.catchitcozucan.core.interfaces.LogConfig;
import com.github.catchitcozucan.core.interfaces.PoolConfig;
import com.github.catchitcozucan.supervision.exception.RestExceptionHandler;
import com.github.catchitcozucan.supervision.exception.CatchitSupervisionRuntimeException;
import com.github.catchitcozucan.supervision.service.UserService;
import com.github.catchitcozucan.supervision.start.CatchitSupervisionApplication;
import com.github.catchitcozucan.supervision.utils.TomcatDetector;
import com.github.catchitcozucan.supervision.utils.logging.Slf4JSetup;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebSecurity(debug = false)
@EnableMethodSecurity(securedEnabled = false, jsr250Enabled = true)
@Import({ GsonConfig.class, FlywayConfig.class, RestExceptionHandler.class, HttpClientConfig.class })
@ComponentScan(basePackages = { "com.github.catchitcozucan.supervision.repository", "com.github.catchitcozucan.supervision.service", "com.github.catchitcozucan.supervision.controllers", "com.github.catchitcozucan.supervision.exception" })
@RequiredArgsConstructor
public class SecurityAndAppConfig {
	private static final int NUMBER_OF_THREADS_IN_TASK_POOL = 10;
	private static final String COULD_NOT_FIND_USABLE_LOG_DIRECTORY = "Could not find usable log directory!";
	private static final String EMPTY = "";
	private static final String APP_NAME = CatchitSupervisionApplication.class.getSimpleName().toLowerCase().replace("application", EMPTY);
	private static final String SPACE = " ";
	private static final String COMMA = ",";
	private static Logger LOGGER;

	@Value("${catchit.corsAllowedHosts}")
	private String corsAllowedHosts;

	private final UserService userService;

	@Component
	public class CompletetelyInitiatedindicator implements SmartInitializingSingleton {
		private boolean fullyInitialized;

		@Override
		public void afterSingletonsInstantiated() {
			setUpLogging();
			initCatchIt();
			fullyInitialized = true;
		}

		public boolean isFullyInitialized() {
			return fullyInitialized;
		}
	}

	@Component
	public class PreDestroyBean {
		@PreDestroy
		public void cleanup() {
			CatchIt.halt();
			Slf4JSetup.halt();
		}
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf((csrf) -> csrf.disable());
		return http.cors(cors -> {
			cors.configurationSource(corsConfigurationSource());
		}).userDetailsService(userService)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/supervision/deletesource/*").authenticated()
						.requestMatchers("/supervision/sources").authenticated()
						.requestMatchers("/supervision/testsource").authenticated()
						.requestMatchers("/supervision/savesource").authenticated()
						.anyRequest().permitAll()).build();
	}

	@Primary
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);

		if (corsAllowedHosts != null && !corsAllowedHosts.isEmpty() && !corsAllowedHosts.trim().equals("*")) {
			String[] hostsForCors = corsAllowedHosts.replace(SPACE, EMPTY).split(COMMA);
			for (String host : hostsForCors) {
				configuration.addAllowedOrigin(host.trim());
			}
		} else {
			configuration.addAllowedOriginPattern(CorsConfiguration.ALL);
		}
		configuration.addAllowedMethod(CorsConfiguration.ALL);
		configuration.addAllowedHeader(CorsConfiguration.ALL);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private static void setUpLogging() {
		String logDir = null;
		String appName = APP_NAME;
		if (TomcatDetector.isRunningWithinTomcat() && testDirPath("/opt/tomcat/latest/logs")) {
			logDir = "/opt/tomcat/latest/logs";
		} else {
			String possiblePath = System.getProperty("user.home");
			if (testDirPath(possiblePath)) {
				logDir = possiblePath;
			} else {
				possiblePath = System.getProperty("java.io.tmpdir");
				if (testDirPath(possiblePath)) {
					logDir = possiblePath;
				}
			}
		}
		if (logDir != null) {
			Slf4JSetup.init(logDir, appName, true);
			LOGGER = LoggerFactory.getLogger(CatchitSupervisionApplication.class);
			LOGGER.info("Starting up!");
		} else {
			throw new CatchitSupervisionRuntimeException(COULD_NOT_FIND_USABLE_LOG_DIRECTORY);
		}
	}

	private static void initCatchIt() {
		CatchItConfig cfg = new CatchItConfig() {
			@Override
			public PoolConfig getPoolConfig() {
				return new PoolConfig() {
					@Override
					public NumberOfTimeUnits maxExecTimePerRunnable() {
						return new NumberOfTimeUnits(4, TimeUnit.HOURS);
					}

					@Override
					public int maxQueueSize() {
						return 0;
					}

					@Override
					public int maxNumberOfThreads() {
						return NUMBER_OF_THREADS_IN_TASK_POOL;
					}
				};
			}

			@Override
			public LogConfig getLogConfig() {
				return new LogConfig() {
					@Override
					public String getLoggingApp() {
						return CatchitSupervisionApplication.class.getSimpleName();
					}

					@Override
					public String getSytemLogParentDir() {
						return EMPTY;
					}

					@Override
					public ProcessLogging.LoggingSetupStrategy getLoggingSetupStrategy() {
						return ProcessLogging.LoggingSetupStrategy.NO_LOGGING_SETUP;
					}
				};
			}
		};
		CatchIt.init(cfg);
	}

	private static boolean testDirPath(String p) {
		Path path = Path.of(p);
		if (Files.exists(path) && Files.isWritable(path)) {
			return true;
		}
		return false;
	}
}
