// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.mkmc.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.client.i18n;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.ecommerce.document.service;
    requires modality.ecommerce.shared.pricecalculator;
    requires modality.event.frontoffice.activity.book;
    requires webfx.extras.i18n;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.panes;
    requires webfx.extras.webtext;
    requires webfx.platform.async;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.MKMCOnlineEmpowermentBookingFormProvider;

}