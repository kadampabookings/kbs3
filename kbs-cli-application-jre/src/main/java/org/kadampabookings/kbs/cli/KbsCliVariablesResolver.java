package org.kadampabookings.kbs.cli;

import dev.webfx.platform.substitution.var.spi.impl.VariablesResolverBase;
import dev.webfx.stack.db.query.cli.ServerEndpoint;

import java.util.Optional;

/**
 * Answers the bus's server variables from this run, at configuration-substitution time.
 *
 * <h3>Why a resolver rather than system properties set before the boot</h3>
 *
 * <p>Because it lets the tool keep the platform's own entry point. The server has to be supplied by the time
 * something first reads it from the configuration — which a resolver does, using the main arguments the
 * platform has already recorded. Setting system properties instead would have to happen before
 * {@code ApplicationBooter.start}, which means owning {@code main}.
 *
 * <p>"By the time something first reads it" rather than "at a boot level": the configuration substitutes each
 * value in its getter, so this is called whenever the bus's server is first asked for, and in a one-shot
 * command that can be well after the boot. {@link RunServer#endpoint()} memoises for exactly that reason.
 *
 * <p>Registered alongside the environment and system-property resolvers, and deliberately narrow: it answers
 * three names and nothing else, so it cannot shadow any other variable this build resolves.
 *
 * <h3>What it does NOT do</h3>
 *
 * <p>It does not validate and it does not refuse. An absent or malformed {@code --server} leaves the variables
 * unresolved, the configuration falls back to {@code localhost:80}, and {@code ClientRuntime} reports the
 * mismatch against the endpoint the command actually asked for — which is a readable error, at a moment a
 * command can be refused. Refusing here would abort a boot from inside a resolver.
 *
 * @author Claude Code
 */
public final class KbsCliVariablesResolver extends VariablesResolverBase {

    @Override
    public Optional<String> resolveVariable(String variableName) {
        if (!ServerEndpoint.HOST_VARIABLE.equals(variableName)
            && !ServerEndpoint.PORT_VARIABLE.equals(variableName)
            && !ServerEndpoint.SSL_VARIABLE.equals(variableName))
            return Optional.empty();
        ServerEndpoint endpoint = RunServer.endpoint();
        if (endpoint == null)
            return Optional.empty();
        String value = ServerEndpoint.HOST_VARIABLE.equals(variableName) ? endpoint.host()
            : ServerEndpoint.PORT_VARIABLE.equals(variableName) ? String.valueOf(endpoint.port())
            : String.valueOf(endpoint.ssl());
        return passVariableSearchResult(variableName, value, "the read-scope CLI's run");
    }
}
