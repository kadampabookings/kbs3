// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activity.home.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.frontoffice.activity.books.plugin;
    requires kbs.frontoffice.activity.news.plugin;
    requires kbs.frontoffice.activity.podcasts.plugin;
    requires modality.base.client.application;
    requires webfx.kit.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.i18n;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;
    requires webfx.stack.ui.action;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.operation.action;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.home;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.home.HomeRouting.HomeUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.home.HomeRouting.RouteToHomeRequestEmitter;

}