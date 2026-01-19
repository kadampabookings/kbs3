// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.kmcny.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.crm.client.authn.fx;
    requires modality.ecommerce.policy.service;
    requires modality.ecommerce.shared.pricecalculator;
    requires modality.event.frontoffice.activity.book;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.platform.console;
    requires webfx.platform.uischeduler;
    requires webfx.platform.windowhistory;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.kmcny;
    exports org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival.USFestivalBookingFormProvider;

}