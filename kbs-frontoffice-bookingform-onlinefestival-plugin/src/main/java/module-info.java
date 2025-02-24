// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.onlinefestival.plugin {

    // Direct dependencies modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.client.i18n;
    requires modality.base.shared.entities;
    requires modality.ecommerce.client.i18n;
    requires modality.ecommerce.client.workingbooking;
    requires modality.ecommerce.document.service;
    requires modality.event.frontoffice.activity.booking.plugin;
    requires webfx.extras.styles.bootstrap;
    requires webfx.kit.util;
    requires webfx.platform.util;
    requires webfx.platform.util.time;
    requires webfx.stack.i18n;
    requires webfx.stack.i18n.controls;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

    // Provided services
    provides one.modality.event.frontoffice.activities.booking.process.event.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalBookingFormProvider;

}