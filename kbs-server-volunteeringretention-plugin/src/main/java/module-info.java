// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * The server job that applies the volunteering GDPR retention policy: periodic sweep of expired application data (V0074 database function) and deletion of due applicant photographs from the image store. Inert until VOLUNTEERING_RETENTION_ENABLED, dry-run until VOLUNTEERING_RETENTION_DRY_RUN=false.
 */
module kbs.server.volunteeringretention.plugin {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.meta;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.cloud.image;
    requires webfx.stack.db.datasource;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;

    // Exported packages
    exports org.kadampabookings.kbs.server.jobs.volunteeringretention;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.server.jobs.volunteeringretention.VolunteeringRetentionJob;

}
