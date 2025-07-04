// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activity.books.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.client.activity;
    requires modality.base.client.brand;
    requires modality.base.client.css;
    requires modality.base.frontoffice.mainframe.fx;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires modality.event.client.mediaview;
    requires webfx.extras.carousel;
    requires webfx.extras.i18n;
    requires webfx.extras.imagestore;
    requires webfx.extras.operation;
    requires webfx.extras.operation.action;
    requires webfx.extras.panes;
    requires webfx.extras.player;
    requires webfx.kit.util;
    requires webfx.platform.resource;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.cache.client;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.books;

    // Resources packages
    opens org.kadampabookings.kbs.frontoffice.activities.books;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.books.BooksRouting.BooksUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.books.BooksRouting.RouteToBooksRequestEmitter;

}