package org.kadampabookings.kbs.server.securitywatch;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * One flaw found for one component (and, for a library list, one library), as the sources described it.
 *
 * @param id             the id shown: the CVE when the flaw has one, else the source's own id (a GHSA, …)
 * @param ids            every id the flaw is known by (its own and its aliases), for matching KEV and assessments
 * @param cvss           the CVSS base score, or null when none is published yet
 * @param severity       {@code critical} … {@code low}: the advisory's own rating, else from the score, else the
 *                       contract's rule (high when found by the early-warning pass, otherwise medium)
 * @param summary        the source's summary, already cut and cleaned ({@link Sanitize#summary})
 * @param publishedOn    the day the flaw was published, or null
 * @param fixedIn        the lowest version above the running one that fixes it, or null when none is known
 * @param uncertain      found only by keyword, only by a KEV name, without a version to compare, or not looked up:
 *                       it may apply, and gives a {@code verify} action rather than a fix
 * @param earlyWarning   found by the keyword pass
 * @param target         the library it is in (a library list's flaw), or null
 * @param targetVersion  that library's running version, or null
 * @param kevAddedOn     the day CISA listed it as exploited, or null (set from the KEV catalogue)
 *
 * @author Claude Code
 */
record Flaw(String id, Set<String> ids, Double cvss, String severity, String summary, LocalDate publishedOn,
            String fixedIn, boolean uncertain, boolean earlyWarning, String target, String targetVersion,
            LocalDate kevAddedOn) {

    boolean knownExploited() {
        return kevAddedOn != null;
    }

    Flaw withKev(LocalDate addedOn) {
        return new Flaw(id, ids, cvss, severity, summary, publishedOn, fixedIn, uncertain, earlyWarning, target,
            targetVersion, addedOn);
    }

    Flaw asUncertain() {
        return uncertain ? this : new Flaw(id, ids, cvss, severity, summary, publishedOn, fixedIn, true, earlyWarning,
            target, targetVersion, kevAddedOn);
    }

    /** The id to show: the first CVE among the ids, else the given one. */
    static String displayId(String ownId, List<String> aliases) {
        if (ownId != null && ownId.startsWith("CVE-"))
            return ownId;
        for (String alias : aliases)
            if (alias != null && alias.startsWith("CVE-"))
                return alias;
        return ownId;
    }
}
