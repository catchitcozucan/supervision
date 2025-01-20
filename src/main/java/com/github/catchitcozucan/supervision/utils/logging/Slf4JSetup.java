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
package com.github.catchitcozucan.supervision.utils.logging;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;


public class Slf4JSetup {

    private static final String LOG_LEVEL_PROPERTY = "logLevel";
    private static final String LOG_PATH_INFO = "tot_%s.log";
    private static final String LOG_PATH_INFO_PATTERN = "info.log.%i.gz";
    private static final String LOG_PATH_DEBUG = "debug_%s.log";
    private static final String LOG_PATH_DEBUG_PATTERN = "debug.log.%i.gz";
    private static final String LOG_PATH_ERROR = "error_%s.log";
    private static final String LOG_PATH_ERROR_PATTERN = "error.log.%i.gz";
    private static final int MAX_LOG_FILES = 20;
    private static final String MAX_LOG_FILE_SIZE = "100MB";
    private static final String LOG_PATTERN = "%date %level [%thread] %logger{10}.%line %msg%n";
    private static final String LOGGER_ENCODING = "UTF-8";
    private static Slf4JSetup instance;
    private static LoggerContext CONTEXT;
    private static final String LOGS = "logs";

    private Slf4JSetup(File logFolder, String applicationName, boolean rundebug) {
        CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

        // den SKA ha skapats tidigare men den finns inte på jenkins -> loggga till tempdir
        if (!logFolder.exists()) {
            logFolder = new File(System.getProperty("java.io.tmpdir"));
        }

        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern(LOG_PATTERN);
        ple.setContext(CONTEXT);
        ple.setCharset(Charset.forName(LOGGER_ENCODING));

        String folderPrefix = new StringBuilder(logFolder.getAbsolutePath()).append(File.separator).toString();

        RollingFileAppender<ILoggingEvent> fileAppenderInfo = getFilteredAppender(CONTEXT, ple, folderPrefix + String.format(LOG_PATH_INFO, applicationName), Level.INFO);
        FixedWindowRollingPolicy rollingPolicyInfo = getRollingPolicy(CONTEXT, fileAppenderInfo, LOG_PATH_INFO_PATTERN);
        setTriggerPolicy(fileAppenderInfo, rollingPolicyInfo);

        RollingFileAppender<ILoggingEvent> fileAppenderDebug = null;
        if (rundebug) {
            fileAppenderDebug = getFilteredAppender(CONTEXT, ple, folderPrefix + String.format(LOG_PATH_DEBUG, applicationName), Level.DEBUG);
            FixedWindowRollingPolicy rollingPolicyDebug = getRollingPolicy(CONTEXT, fileAppenderDebug, LOG_PATH_DEBUG_PATTERN);
            setTriggerPolicy(fileAppenderDebug, rollingPolicyDebug);
        }

        RollingFileAppender<ILoggingEvent> fileAppenderError = getFilteredAppender(CONTEXT, ple, folderPrefix + String.format(LOG_PATH_ERROR, applicationName), Level.ERROR);
        FixedWindowRollingPolicy rollingPolicyError = getRollingPolicy(CONTEXT, fileAppenderError, LOG_PATH_ERROR_PATTERN);
        setTriggerPolicy(fileAppenderError, rollingPolicyError);

        CONTEXT.start();
        ple.start();

        fileAppenderInfo.start();
        if (fileAppenderDebug != null) {
            fileAppenderDebug.start();
        }
        fileAppenderError.start();

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppenderInfo);
        if (fileAppenderDebug != null) {
            root.addAppender(fileAppenderDebug);
        }
        root.addAppender(fileAppenderError);
        root.setLevel(Level.INFO);

        StatusPrinter.print(CONTEXT);
        LoggerFactory.getLogger(Slf4JSetup.class); //NOSONAR - this _is_ bull.
    }

    public static synchronized void init(String pathToFolder, String applicationName) {
        initInternal(pathToFolder, applicationName, true);
    }

    public static synchronized void init(String pathToFolder, String applicationName, boolean runDebug) {
        initInternal(pathToFolder, applicationName, runDebug);
    }

    private static synchronized void initInternal(String pathToFolder, String applicationName, boolean runDebug) {
        if (instance == null) {
            File logFolder = new File(new StringBuilder(pathToFolder).append(File.separator).append(applicationName).append(File.separator).append(LOGS).toString());
            if (!logFolder.exists()) {
                try {
                    logFolder.mkdirs();
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format("Cannot create log folder %s", pathToFolder));
                }
            } else if (logFolder.isFile()) {
                throw new IllegalArgumentException(String.format("Provided log path %s is an existing FILE. It should be a directory!", pathToFolder));
            }
            instance = new Slf4JSetup(logFolder, applicationName, runDebug);
        }
    }

    public static synchronized void halt() {
        if (CONTEXT != null && CONTEXT.isStarted()) {
            StatusPrinter.print(CONTEXT);
            CONTEXT.stop();
        }
    }

    private static class ThresholdLoggerFilter extends Filter<ILoggingEvent> {
        private Level level;

        @Override
        public FilterReply decide(ILoggingEvent event) {
            if (level == null) {
                return FilterReply.NEUTRAL;
            }
            if (event.getLevel().isGreaterOrEqual(level)) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }

        public void setLevel(Level level) {
            this.level = level;
        }
    }

    private static RollingFileAppender<ILoggingEvent> getFilteredAppender(LoggerContext lc, PatternLayoutEncoder ple, String path, Level level) {
        RollingFileAppender<ILoggingEvent> fileAppenderInfo = new RollingFileAppender<>();
        fileAppenderInfo.setFile(path);
        fileAppenderInfo.setEncoder(ple);
        fileAppenderInfo.setContext(lc);
        ThresholdLoggerFilter filterInfo = new ThresholdLoggerFilter();
        filterInfo.setLevel(level);
        filterInfo.start();
        fileAppenderInfo.addFilter(filterInfo);
        fileAppenderInfo.setName(String.format("RollingFileAppender_%s", level.levelStr));
        return fileAppenderInfo;
    }

    private static FixedWindowRollingPolicy getRollingPolicy(LoggerContext lc, RollingFileAppender<ILoggingEvent> appender, String pattern) {
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(lc);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(pattern);
        rollingPolicy.setMaxIndex(MAX_LOG_FILES);
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.start();
        return rollingPolicy;
    }

    private static void setTriggerPolicy(RollingFileAppender<ILoggingEvent> appender, FixedWindowRollingPolicy rollingPolicy) {
        SizeBasedTriggeringPolicy triggeringPolicy = new ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf(MAX_LOG_FILE_SIZE));
        triggeringPolicy.setMaxFileSize(FileSize.valueOf(MAX_LOG_FILE_SIZE));
        appender.setTriggeringPolicy(triggeringPolicy);
        triggeringPolicy.start();
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.start();
    }
}
