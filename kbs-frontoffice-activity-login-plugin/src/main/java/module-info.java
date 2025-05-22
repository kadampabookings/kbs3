// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.activity.login.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.client.icons;
    requires modality.crm.client.activity.login.plugin;
    requires webfx.extras.panes;
    requires webfx.extras.player;
    requires webfx.extras.player.video.web.wistia;
    requires webfx.extras.player.video.web.youtube;
    requires webfx.extras.util.animation;
    requires webfx.extras.util.layout;
    requires webfx.extras.webtext;
    requires webfx.kit.util;
    requires webfx.platform.boot;
    requires webfx.platform.scheduler;
    requires webfx.platform.uischeduler;
    requires webfx.platform.util;
    requires webfx.platform.windowlocation;
    requires webfx.stack.authn.login.ui;
    requires webfx.stack.i18n;
    requires webfx.stack.orm.domainmodel.activity;
    requires webfx.stack.routing.activity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.activities.login;

    // Resources packages
    opens dev.webfx.kit.css.frontoffice.images;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with org.kadampabookings.kbs.frontoffice.activities.login.KbsFrontOfficeLoginOverriderJob;

}