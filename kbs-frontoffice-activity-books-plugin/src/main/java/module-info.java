// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activity.books.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.frontoffice.mediaview;
    requires modality.base.client.activity;
    requires modality.base.frontoffice.mainframe.fx;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires webfx.extras.carousel;
    requires webfx.extras.imagestore;
    requires webfx.extras.panes;
    requires webfx.extras.util.control;
    requires webfx.kit.util;
    requires webfx.platform.resource;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.cache.client;
    requires webfx.stack.i18n;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.operation.action;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.books;
    exports org.kadampabookings.kbs.frontoffice.activities.books.routing;
    exports org.kadampabookings.kbs.frontoffice.operations.routes.books;

    // Resources packages
    opens org.kadampabookings.kbs.frontoffice.activities.books;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.books.BooksUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.books.RouteToBooksRequestEmitter;

}