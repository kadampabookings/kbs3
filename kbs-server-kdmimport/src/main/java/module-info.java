// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * 
 */
module kbs.server.kdmimport {

    // Direct dependencies modules
    requires java.base;
    requires modality.base.shared.entities;
    requires webfx.extras.webtext.util;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.json;
    requires webfx.platform.scheduler;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.server.jobs.kdmimport;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.server.jobs.kdmimport.KdmImportJob;

}