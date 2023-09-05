// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * 
 */
module kbs.server.organisation.update {

    // Direct dependencies modules
    requires modality.base.shared.entities;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.server.jobs.organisationupdate;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.server.jobs.organisationupdate.OrganisationUpdateJob;

}