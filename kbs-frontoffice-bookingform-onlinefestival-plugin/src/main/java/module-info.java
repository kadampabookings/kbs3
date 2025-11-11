// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.onlinefestival.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.client.i18n;
    requires modality.base.client.icons;
    requires modality.base.client.time;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingelements;
    requires modality.booking.frontoffice.bookingform;
    requires modality.crm.client.authn.fx;
    requires modality.crm.client.i18n;
    requires modality.crm.frontoffice.activity.userprofile.plugin;
    requires modality.ecommerce.client.i18n;
    requires modality.ecommerce.document.service;
    requires modality.ecommerce.frontoffice.order;
    requires modality.event.client.lifecycle;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.eventheader;
    requires webfx.extras.async;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.operation;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.extras.time.format;
    requires webfx.extras.util.border;
    requires webfx.extras.util.control;
    requires webfx.extras.util.layout;
    requires webfx.extras.util.scene;
    requires webfx.extras.validation;
    requires webfx.extras.webtext;
    requires webfx.kit.util;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.uischeduler;
    requires webfx.platform.useragent;
    requires webfx.platform.util;
    requires webfx.platform.util.time;
    requires webfx.platform.windowhistory;
    requires webfx.stack.authn.login.ui.gateway.password.plugin;
    requires webfx.stack.authn.logout.client;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.entity.binding;
    requires webfx.stack.orm.entity.controls;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.closed;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.personal;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.prerequisite;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.terms;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalBookingFormProvider;

}