// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activity.home.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires kbs.frontoffice.activity.podcasts.plugin;
    requires modality.base.client.i18n;
    requires modality.base.client.mainframe.fx;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires modality.ecommerce.frontoffice.bookingform;
    requires modality.event.client.lifecycle;
    requires modality.event.frontoffice.activity.book;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.operation;
    requires webfx.extras.operation.action;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.util.animation;
    requires webfx.extras.util.dialog;
    requires webfx.kit.util;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.entity;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.home;

    // Resources packages
    opens dev.webfx.kit.css.home;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.home.FrontOfficeHomeRouting.HomeUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.home.FrontOfficeHomeRouting.RouteToHomeRequestEmitter;

}