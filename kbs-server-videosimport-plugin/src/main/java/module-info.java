// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.server.videosimport.plugin {

    // Direct dependencies modules
    requires modality.base.shared.entities;
    requires webfx.extras.webtext.util;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.fetch.ast.json;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.server.jobs.videosimport;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.server.jobs.videosimport.VideosImportJob;

}