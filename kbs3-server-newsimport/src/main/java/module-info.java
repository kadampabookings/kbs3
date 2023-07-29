// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs3.server.newsimport {

    // Direct dependencies modules
    requires java.base;
    requires modality.base.shared.entities;
    requires webfx.extras.webtext.util;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.json;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs3.server.jobs.newsimport;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs3.server.jobs.newsimport.NewsImportJob;

}