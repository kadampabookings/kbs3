// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * A button in the back-office header to allow managers to create events.
 */
module kbs.backoffice.header.festivalcreator.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.backoffice.mainframe.fx;
    requires modality.base.backoffice.mainframe.headernode;
    requires modality.base.client.i18n;
    requires modality.base.client.mainframe.fx;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.crm.backoffice.organization.fx;
    requires modality.event.backoffice.activity.pricing.plugin;
    requires modality.event.client.event.fx;
    requires webfx.extras.action;
    requires webfx.extras.aria;
    requires webfx.extras.async;
    requires webfx.extras.controlfactory;
    requires webfx.extras.exceptions;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.operation;
    requires webfx.extras.operation.action;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.styles.materialdesign;
    requires webfx.extras.time.pickers;
    requires webfx.extras.util.dialog;
    requires webfx.extras.util.layout;
    requires webfx.extras.validation;
    requires webfx.kit.util;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.windowhistory;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.backoffice.festivalcreator;

    // Provided services
    provides one.modality.base.backoffice.mainframe.headernode.MainFrameHeaderNodeProvider with org.kadampabookings.kbs.backoffice.festivalcreator.MainFrameHeaderFestivalCreatorProvider;

}