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
    requires modality.crm.backoffice.organization.fx;
    requires modality.event.backoffice.activity.pricing.plugin;
    requires modality.event.client.event.fx;
    requires webfx.extras.aria;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.styles.materialdesign;
    requires webfx.extras.time.pickers;
    requires webfx.extras.util.layout;
    requires webfx.kit.util;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.windowhistory;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel;
    requires webfx.stack.orm.entity;
    requires webfx.stack.ui.action;
    requires webfx.stack.ui.action.tuner;
    requires webfx.stack.ui.controls;
    requires webfx.stack.ui.dialog;
    requires webfx.stack.ui.exceptions;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.operation.action;
    requires webfx.stack.ui.validation;

    // Exported packages
    exports org.kadampabookings.kbs.backoffice.festivalcreator;

    // Provided services
    provides one.modality.base.backoffice.mainframe.headernode.MainFrameHeaderNodeProvider with org.kadampabookings.kbs.backoffice.festivalcreator.MainFrameHeaderFestivalCreatorProvider;

}