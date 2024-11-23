// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * The customized home page of the KBS Front-Office that displays the Kadampa news &amp; podcasts.
 */
module kbs.frontoffice.activity.podcasts.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.client.activity;
    requires modality.base.client.brand;
    requires modality.base.client.css;
    requires modality.base.client.mainframe.fx;
    requires modality.base.client.tile;
    requires modality.base.frontoffice.mainframe.fx;
    requires modality.base.frontoffice.utility;
    requires modality.base.shared.entities;
    requires modality.event.client.mediaview;
    requires webfx.extras.carousel;
    requires webfx.extras.panes;
    requires webfx.extras.switches;
    requires webfx.extras.util.animation;
    requires webfx.extras.util.control;
    requires webfx.kit.util;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.browser;
    requires webfx.platform.console;
    requires webfx.platform.storage;
    requires webfx.platform.util;
    requires webfx.platform.windowhistory;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.orm.dql;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.entity.controls;
    requires webfx.stack.orm.reactive.entities;
    requires webfx.stack.routing.router.client;
    requires webfx.stack.routing.uirouter;
    requires webfx.stack.ui.operation;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.podcasts;

    // Provided services
    provides dev.webfx.stack.routing.uirouter.UiRoute with org.kadampabookings.kbs.frontoffice.activities.podcasts.PodcastsRouting.PodcastsUiRoute;
    provides dev.webfx.stack.routing.uirouter.operations.RouteRequestEmitter with org.kadampabookings.kbs.frontoffice.activities.podcasts.PodcastsRouting.RouteToPodcastsRequestEmitter;

}