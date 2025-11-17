// File managed by WebFX (DO NOT EDIT MANUALLY)

/**
 * The OpenJFX executable module of the KBS Front-Office (for development).
 */
module kbs.frontoffice.application.openjfx {

    // Direct dependencies modules
    requires javafx.media;
    requires javafx.web;
    requires kbs.frontoffice.application;
    requires kbs.frontoffice.footer.nktikbu;
    requires modality.ecommerce.document.service.buscall;
    requires modality.ecommerce.document.service.remote;
    requires modality.ecommerce.payment.buscall;
    requires modality.ecommerce.payment.remote;
    requires webfx.extras.canvas.blob.openjfx;
    requires webfx.extras.filepicker.openjfx;
    requires webfx.extras.fxraiser.json;
    requires webfx.extras.visual.grid.peers.openjfx;
    requires webfx.extras.webtext.peers.openjfx;
    requires webfx.kit.javafxgraphics.openjfx;
    requires webfx.kit.platform.browser.openjfx;
    requires webfx.kit.platform.visibility.openjfx;
    requires webfx.platform.ast.factory.generic;
    requires webfx.platform.blob.jre;
    requires webfx.platform.boot.java;
    requires webfx.platform.console.java;
    requires webfx.platform.fetch.jre;
    requires webfx.platform.file.jre;
    requires webfx.platform.os.jre;
    requires webfx.platform.resource.jre;
    requires webfx.platform.scheduler.jre;
    requires webfx.platform.shutdown.jre;
    requires webfx.platform.storage.jre;
    requires webfx.platform.storagelocation.jre;
    requires webfx.platform.useragent.jre.client;
    requires webfx.platform.windowhistory.jre;
    requires webfx.platform.windowlocation.jre;
    requires webfx.stack.authn.buscall;
    requires webfx.stack.authn.login.ui.portal;
    requires webfx.stack.authn.remote;
    requires webfx.stack.com.bus.json.client;
    requires webfx.stack.com.bus.json.client.websocket.jre;
    requires webfx.stack.com.websocket.jre;
    requires webfx.stack.db.query.buscall;
    requires webfx.stack.db.querypush.buscall;
    requires webfx.stack.db.querypush.client.simple;
    requires webfx.stack.db.querysubmit.jre.jdbc;
    requires webfx.stack.db.submit.buscall;
    requires webfx.stack.orm.dql.query.interceptor;
    requires webfx.stack.orm.dql.querypush.interceptor;
    requires webfx.stack.orm.dql.submit.interceptor;
    requires webfx.stack.push.client.simple;
    requires webfx.stack.session.client;

}