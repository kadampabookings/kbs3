// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.server.securitywatch.plugin {

    // Direct dependencies modules
    requires io.vertx.core;
    requires modality.base.server.mail;
    requires modality.base.shared.context;
    requires modality.crm.server.authn.gateway.shared;
    requires modality.crm.shared.authn;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.meta;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.platform.util.vertx;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.mail;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.entity;
    requires webfx.stack.session.state;

    // Exported packages
    exports org.kadampabookings.kbs.server.securitywatch;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.server.securitywatch.SecurityWatchJob;
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with org.kadampabookings.kbs.server.securitywatch.SecurityWatchStatusEndpoint, org.kadampabookings.kbs.server.securitywatch.SecurityWatchSummaryEndpoint, org.kadampabookings.kbs.server.securitywatch.SecurityWatchCheckNowEndpoint;

}