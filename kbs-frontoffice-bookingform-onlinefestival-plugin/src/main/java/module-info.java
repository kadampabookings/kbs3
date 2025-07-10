// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.onlinefestival.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.client.i18n;
    requires modality.base.client.time;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.ecommerce.client.workingbooking;
    requires modality.ecommerce.document.service;
    requires modality.event.frontoffice.activity.booking.plugin;
    requires modality.event.frontoffice.eventheader;
    requires webfx.extras.panes;
    requires webfx.extras.styles.bootstrap;
    requires webfx.kit.util;
    requires webfx.platform.util;
    requires webfx.platform.util.time;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary;
    exports org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching;

    // Provided services
    provides one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.OnlineFestivalBookingFormProvider;

}