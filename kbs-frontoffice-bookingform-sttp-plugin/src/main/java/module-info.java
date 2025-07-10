// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.sttp.plugin {

    // Direct dependencies modules
    requires modality.base.shared.entities;
    requires modality.event.frontoffice.activity.booking.plugin;
    requires modality.event.frontoffice.bookingform.recurringevent.plugin;
    requires modality.event.frontoffice.eventheader;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingforms.sttp;

    // Provided services
    provides one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingforms.sttp.SttpBookingFormProvider;

}