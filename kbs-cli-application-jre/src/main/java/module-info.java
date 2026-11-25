// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * The JRE executable module of the KBS command-line client.
 */
module kbs.cli.application.jre {

    // Direct dependencies modules
    requires kbs.cli.application;
    requires modality.base.shared.domainmodel;
    requires modality.crm.shared.authn;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.boot;
    requires webfx.platform.substitution.var;
    requires webfx.platform.visibility.jre.headless;
    requires webfx.stack.db.query.cli;
    requires webfx.stack.db.submit.buscall;
    requires webfx.stack.db.submit.remote;
    requires webfx.stack.orm.dql.query.interceptor;

    // Exported packages
    exports org.kadampabookings.kbs.cli;

    // Resources packages
    opens dev.webfx.extras.i18n.exe;
    opens dev.webfx.platform.conf.exe;
    opens dev.webfx.platform.meta.exe;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.cli.KbsCliJob;
    provides dev.webfx.platform.substitution.var.spi.VariablesResolver with org.kadampabookings.kbs.cli.KbsCliVariablesResolver;

}