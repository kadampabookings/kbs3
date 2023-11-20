// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * The customized home page of the KBS Front-Office that displays the Kadampa news &amp; podcasts.
 */
module kbs.frontoffice.activity.podcast.plugin {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires modality.base.client.activity;
    requires modality.base.client.tile;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires webfx.extras.imagestore;
    requires webfx.extras.panes;
    requires webfx.extras.util.control;
    requires webfx.kit.util;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.console;
    requires webfx.platform.storage;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.orm.datasourcemodel.service;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;
    requires webfx.stack.ui.operation;
    requires webfx.stack.ui.operation.action;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.podcast;
    exports org.kadampabookings.kbs.frontoffice.activities.podcast.routing;
    exports org.kadampabookings.kbs.frontoffice.operations.routes.podcast;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.podcast.PodcastUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.podcast.RouteToPodcastRequestEmitter;

}