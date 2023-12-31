// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activities.home {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.web;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires webfx.extras.imagestore;
    requires webfx.extras.scalepane;
    requires webfx.extras.util.layout;
    requires webfx.kit.util;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.routing.router;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.operation.action;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.home;
    exports org.kadampabookings.kbs.frontoffice.activities.home.routing;
    exports org.kadampabookings.kbs.frontoffice.activities.home.views;
    exports org.kadampabookings.kbs.frontoffice.operations.routes.home;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.home.HomeUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.home.RouteToHomeRequestEmitter;

}