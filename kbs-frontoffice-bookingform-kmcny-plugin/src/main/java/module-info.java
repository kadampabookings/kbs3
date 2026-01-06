// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.kmcny.plugin {

    // Direct dependencies modules
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival.USFestivalBookingFormProvider;

}