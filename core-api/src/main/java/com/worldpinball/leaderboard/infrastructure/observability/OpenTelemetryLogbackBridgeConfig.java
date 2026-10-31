package com.worldpinball.leaderboard.infrastructure.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;

/**
 * Bridges Logback events to OpenTelemetry SdkLoggerProvider:
 * Streams application logs, MDC correlation context, and exception details
 * directly to OpenTelemetry Collector, which fans out to Seq and Grafana Loki.
 */
@Configuration
@ConditionalOnClass({SdkLoggerProvider.class, ILoggingEvent.class})
@Slf4j
public class OpenTelemetryLogbackBridgeConfig {

    public OpenTelemetryLogbackBridgeConfig(ObjectProvider<SdkLoggerProvider> loggerProvider) {
        SdkLoggerProvider provider = loggerProvider.getIfAvailable();
        if (provider == null) {
            return;
        }

        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

            UnsynchronizedAppenderBase<ILoggingEvent> appender = new UnsynchronizedAppenderBase<>() {
                @Override
                protected void append(ILoggingEvent event) {
                    try {
                        var logRecord = provider.get(event.getLoggerName() != null ? event.getLoggerName() : "com.worldpinball")
                                .logRecordBuilder()
                                .setTimestamp(Instant.ofEpochMilli(event.getTimeStamp()))
                                .setSeverity(mapSeverity(event.getLevel()))
                                .setSeverityText(event.getLevel() != null ? event.getLevel().toString() : "INFO")
                                .setBody(event.getFormattedMessage());

                        Map<String, String> mdc = event.getMDCPropertyMap();
                        if (mdc != null && !mdc.isEmpty()) {
                            mdc.forEach((k, v) -> {
                                if (k != null && v != null) {
                                    logRecord.setAttribute(AttributeKey.stringKey(k), v);
                                }
                            });
                        }
                        logRecord.emit();
                    } catch (Exception ignored) {
                        // Logging pipeline failure must never bubble up and crash request processing
                    }
                }
            };
            appender.setName("OTEL_LOGBACK_BRIDGE");
            appender.setContext(context);
            appender.start();
            rootLogger.addAppender(appender);
            log.info("OpenTelemetry Logback bridge successfully attached to ROOT logger.");
        } catch (Exception ex) {
            log.warn("Failed to initialize OpenTelemetry Logback bridge: {}", ex.getMessage());
        }
    }

    private static Severity mapSeverity(Level level) {
        if (level == null) return Severity.UNDEFINED_SEVERITY_NUMBER;
        return switch (level.levelInt) {
            case Level.TRACE_INT -> Severity.TRACE;
            case Level.DEBUG_INT -> Severity.DEBUG;
            case Level.INFO_INT -> Severity.INFO;
            case Level.WARN_INT -> Severity.WARN;
            case Level.ERROR_INT -> Severity.ERROR;
            default -> Severity.UNDEFINED_SEVERITY_NUMBER;
        };
    }
}
