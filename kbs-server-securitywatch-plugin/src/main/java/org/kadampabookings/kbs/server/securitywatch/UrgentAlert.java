package org.kadampabookings.kbs.server.securitywatch;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.mail.MailMessage;
import dev.webfx.stack.mail.MailService;
import one.modality.base.server.mail.ModalityMailMessage;
import one.modality.base.shared.context.ModalityContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * After a run succeeds, tells the platform super administrators about the actions that became urgent: known to be
 * exploited, or critical, and not already so in the previous succeeded report. ONE mail per run, to each of them;
 * nothing when nothing new is urgent.
 *
 * <ul>
 *   <li><b>The log line first</b>, starting {@link #LOG_MARKER} (for a log metric filter), with the run id, the number
 *       of actions and the number of recipients — written before any mail, so a mail that cannot be sent still leaves
 *       it. No action id: the server log is aggregated and read more widely than the page.</li>
 *   <li><b>Recipients</b>: every platform super administrator who can still sign in (their sign-in address), the same
 *       question as the authentication gateways' alerts.</li>
 *   <li><b>The mail carries no detail</b>: the subject and the body say how many actions became urgent, and to open
 *       Security watch in the back office — nothing else, no component, library, version, flaw id, severity or date. The
 *       mail service stores every mail it sends (subject and body in the {@code mail} table, addresses in
 *       {@code recipient}: ModalityServerMailServiceProvider), and those are domain-model tables that client queries
 *       can read: a detailed body would put the list of what is unpatched outside the fence this feature builds around
 *       {@code security_watch_run}. The count is the same information the summary endpoint gives, and the only value
 *       interpolated, so the body needs no escaping.</li>
 *   <li><b>Production only</b>: in {@code staging} and {@code development} the log line only, no mail — staging's
 *       database holds anonymised addresses, and a developer's server may point at a copy of real data.</li>
 *   <li><b>Never fails anything</b>: an alert that cannot be prepared or sent is logged; the run has already succeeded.</li>
 * </ul>
 *
 * <p>"Already so" compares actions by component and target (a library's name, or none) — not by id, which carries the
 * target version (it moves when a newer fix is published), nor by kind: with one action per component, the same flaw
 * can be a {@code verify} today and an {@code upgrade} tomorrow, and that alone is not news.
 *
 * @author Claude Code
 */
final class UrgentAlert {

    private UrgentAlert() {
    }

    /** The start of the log line a metric filter can match. Do not reword it without the filter. */
    static final String LOG_MARKER = "Security watch urgent";
    private static final String MAIL_FROM = "kbs@kadampa.net";
    private static final String MAIL_FROM_NAME = "Kadampa Booking System";

    // Every platform super administrator who can still read a mail: the login username, an address the person controls
    static final String PLATFORM_ADMINS_SQL =
        "select distinct fa.username from authorization_super_admin s" +
        "  join person p on p.id = s.super_admin_id join frontend_account fa on fa.id = p.frontend_account_id" +
        " where s.organization_id is null and p.removed is not true and fa.disabled is not true";

    /** Compares the report of run {@code runId} with the previous succeeded one and alerts. Never throws. */
    static void afterSuccess(int runId, String reportJson) {
        try {
            SecurityWatchStore.previousReport(runId)
                .compose(previous -> alert(runId, reportJson, previous))
                .onFailure(e -> Console.log(SecurityWatchLog.PREFIX + "⚠️ " + LOG_MARKER + ": run " + runId
                                            + " — the alert could not be prepared (" + e.getClass().getSimpleName() + ")"));
        } catch (RuntimeException e) {
            Console.log(SecurityWatchLog.PREFIX + "⚠️ " + LOG_MARKER + ": run " + runId + " — the alert could not be prepared ("
                        + e.getClass().getSimpleName() + ")");
        }
    }

    private static Future<Void> alert(int runId, String reportJson, String previousJson) {
        List<Map<String, Object>> fresh = newlyUrgent(reportJson, previousJson);
        if (fresh.isEmpty())
            return Future.succeededFuture();
        int count = fresh.size();
        String environment = JsonTree.string(JsonTree.parseObject(reportJson), "environment");
        if (!mailsIn(environment)) {
            Console.log(SecurityWatchLog.PREFIX + "🚨 " + LOG_MARKER + ": run " + runId + ", " + count
                        + " action(s) newly known exploited or critical — " + environment + ": no mail");
            return Future.succeededFuture();
        }
        return SecurityWatchStore.query(PLATFORM_ADMINS_SQL).compose(rows -> {
            Set<String> admins = addresses(rows);
            Console.log(SecurityWatchLog.PREFIX + "🚨 " + LOG_MARKER + ": run " + runId + ", " + count
                        + " action(s) newly known exploited or critical — mailing " + admins.size()
                        + " platform super administrator(s)");
            String subject = subject(count);
            String body = body(count);
            List<Future<?>> sends = new ArrayList<>();
            for (String admin : admins)
                sends.add(send(admin, subject, body));
            return Future.all(sends).<Void>mapEmpty();
        });
    }

    /** Whether the alert is mailed in this environment: production only. */
    static boolean mailsIn(String environment) {
        return "production".equals(environment);
    }

    /** The urgent actions of a report that the previous report did not already have as urgent. */
    static List<Map<String, Object>> newlyUrgent(String reportJson, String previousJson) {
        Set<String> before = new LinkedHashSet<>();
        if (previousJson != null) {
            try {
                for (Object item : JsonTree.list(JsonTree.parseObject(previousJson), "actions")) {
                    Map<String, Object> action = JsonTree.asObject(item);
                    if (isUrgent(action))
                        before.add(key(action));
                }
            } catch (IllegalArgumentException e) {
                // An unreadable previous report: everything urgent now is news
            }
        }
        List<Map<String, Object>> fresh = new ArrayList<>();
        for (Object item : JsonTree.list(JsonTree.parseObject(reportJson), "actions")) {
            Map<String, Object> action = JsonTree.asObject(item);
            if (isUrgent(action) && !before.contains(key(action)))
                fresh.add(action);
        }
        return fresh;
    }

    static boolean isUrgent(Map<String, Object> action) {
        return action != null && (JsonTree.isTrue(action, "knownExploited") || "critical".equals(JsonTree.string(action, "severity")));
    }

    private static String key(Map<String, Object> action) {
        return JsonTree.string(action, "componentId") + "|" + JsonTree.string(action, "target");
    }

    /** The subject: a count, nothing else. */
    static String subject(int count) {
        return "Security watch: " + count + (count == 1 ? " urgent action" : " urgent actions");
    }

    /** The body: fixed sentences around a count — no value from the report, the inventory or a source (class comment). */
    static String body(int count) {
        String actions = count == 1 ? "1 action" : count + " actions";
        return "<html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Security watch</title></head>"
               + "<body style=\"margin:0;padding:0;font-family:Arial,sans-serif;background:#f9fafb\">"
               + "<div style=\"max-width:600px;margin:20px auto;background:#fff;border-radius:12px;padding:20px 32px;color:#505050;font-size:15px\">"
               + "<h2 style=\"color:#c0392b\">" + subject(count) + "</h2>"
               + "<p>The daily security check found " + actions + " that became urgent: a flaw known to be exploited, or rated"
               + " critical.</p>"
               + "<p>Open <b>Security watch</b> in the back office for the detail and the steps.</p>"
               + "<p style=\"font-size:12px;color:#888\">Sent to every platform super administrator.</p>"
               + "</div></body></html>";
    }

    private static Set<String> addresses(QueryResult rows) {
        Set<String> admins = new LinkedHashSet<>();
        for (int row = 0; rows != null && row < rows.getRowCount(); row++) {
            Object value = rows.getValue(row, 0);
            if (value != null && !value.toString().isBlank())
                admins.add(value.toString().trim());
        }
        return admins;
    }

    private static Future<Void> send(String to, String subject, String body) {
        MailMessage message = MailMessage.create(MAIL_FROM, to, subject, body);
        ModalityContext context = new ModalityContext(1 /* default organizationId, as for the gateways' alerts */, null, null, null);
        return MailService.sendMail(new ModalityMailMessage(message, context, MAIL_FROM_NAME))
            .<Void>map(ignored -> null)
            .recover(e -> {
                Console.log(SecurityWatchLog.PREFIX + "⚠️ A " + LOG_MARKER + " mail could not be sent (" + e.getClass().getSimpleName() + ")");
                return Future.succeededFuture();
            });
    }
}
