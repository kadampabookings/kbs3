package org.kadampabookings.kbs.server.securitywatch;

/**
 * The prefix every log line of this module starts with, so the security watch can be followed in aggregated logs.
 * Log lines name people by person id only, never by email or name: they ship to aggregation.
 *
 * @author Claude Code
 */
final class SecurityWatchLog {

    private SecurityWatchLog() {
    }

    static final String PREFIX = "[security-watch] ";
}
