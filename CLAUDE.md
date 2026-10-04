# CLAUDE.md — kbs3

Organization-specific extensions to Modality. This repository is a submodule of the
kbs3-aggregate checkout: keep it on the same branch (staging/prod) as the other submodules.
The project rules (boundaries, security and GDPR, reviews, schema changes) are in the
aggregate's [CLAUDE.md](../CLAUDE.md); that link resolves only inside the aggregate checkout.

## What lives here

- **The deployed server.** `kbs-server-application` declares the server's plugins
  (`<plugin-module>` lines in its `webfx.xml`); `kbs-server-application-vertx` is the
  executable built into the Vert.x fat jar. The `kbs-server-*-plugin` modules are KBS-specific
  server jobs (imports, organisation update).
- **Legacy JavaFX/GWT UI**: the `kbs-backoffice-*`, `kbs-frontoffice-*` and `kbs-client-*`
  modules. The active UI is React, in `kbs3-react/`; change these only when explicitly asked.

## The `webfx update` trap

A plugin or configuration declared in a module (a `<plugin-module>` line in `webfx.xml`, or a
`declare@<name>.properties` file, often in `modality-fork`) does nothing until WebFX
regenerates the executable's files from it: `kbs-server-application/pom.xml`,
`kbs-server-application-vertx/pom.xml` and `module-info.java`, and the merged configuration
`kbs-server-application-vertx/src/main/resources/dev/webfx/platform/conf/src-root.json`. They
are committed and "File managed by WebFX"; CI does not regenerate them (the deploy workflows'
`mvn webfx:update` step is commented out).

- `webfx update` regenerates only the repository it is run in. Run in `modality-fork`, it
  wires the module there and leaves this repository's files untouched — run it here too. The
  WebFX CLI is not on the agent's PATH: ask David to run it.
- Precedents: the passkey gateway booted inert on every environment (`11faaa55`), member
  emails reported disabled on staging (`abe5fc5d`), and three server endpoints were missing
  from the fat jar (`5d208d5b`).
- Check the result, not the declaration. For a plugin, list the fat jar
  (`unzip -l kbs-server-application-vertx/target/*-fat.jar`) after installing the plugin and
  `kbs-server-application` — the vertx module resolves them from the local Maven repository.
  For a configuration, the boot log prints a `‹ VARIABLE › was resolved from …` (or
  `couldn't be resolved`) line for each of the module's variables; none means it never loaded.
