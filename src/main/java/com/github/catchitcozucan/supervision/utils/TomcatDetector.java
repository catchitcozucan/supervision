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

import lombok.Getter;

@Getter
public class TomcatDetector {

    private static final String TOMCAT_BOOTSTRAP_CLASS =
            "/org/apache/catalina/startup/Bootstrap.class";

    private static final String TOMCAT_EMBEDDED_CLASS =
            "/org/apache/catalina/startup/Embedded.class";

    private static TomcatDetector INSTANCE;
    private boolean runsWithinTomcat;

    private TomcatDetector() {}

    public static synchronized boolean isRunningWithinTomcat() {
        if (INSTANCE == null) {
            INSTANCE = new TomcatDetector();
            INSTANCE.runsWithinTomcat = locateClass(TOMCAT_BOOTSTRAP_CLASS);
            if (!INSTANCE.runsWithinTomcat) {
                locateClass(TOMCAT_EMBEDDED_CLASS);
            }
        }
        return INSTANCE.isRunsWithinTomcat();
    }

    private static Boolean locateClass(String className) {
        try {
            ClassLoader.getSystemClassLoader().loadClass(className);
            return Boolean.TRUE;
        } catch (ClassNotFoundException cnfe) {
            Class<?> c = INSTANCE.getClass();
            if (c.getResource(className) != null) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }
}
