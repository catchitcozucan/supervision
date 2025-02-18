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
package com.github.catchitcozucan.supervision.start;

import com.github.catchitcozucan.supervision.config.SecurityAndAppConfig;
import jakarta.servlet.ServletContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

@AutoConfigurationPackage
@EnableJpaRepositories(basePackages = "com.github.catchitcozucan.supervision.repository")
@EnableTransactionManagement
@EntityScan(basePackages = "com.github.catchitcozucan.supervision.repository.enteties")
@EnableScheduling
@RestController
@SpringBootApplication(exclude = {JacksonAutoConfiguration.class}) // Exclude the automatic configuration of JACKSON
@ComponentScan("com.github.catchitcozucan.supervision.repository")
@Import({SecurityAndAppConfig.class})
public class CatchitSupervisionApplication extends SpringBootServletInitializer {

    public void shutDownSharedReactorSchedulers(ServletContext servletContext) {
        super.shutDownSharedReactorSchedulers(servletContext);
    }

    public static void main(String[] args) {
        SpringApplication.run(CatchitSupervisionApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        application.registerShutdownHook(true);
        return application.sources(CatchitSupervisionApplication.class);
    }
}
