// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.sttp.plugin {

    // Direct dependencies modules
    requires modality.base.shared.entities;
    requires modality.ecommerce.client.workingbooking;
    requires modality.ecommerce.frontoffice.bookingform;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.bookingform.recurringevent.plugin;
    requires modality.event.frontoffice.eventheader;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingforms.sttp;

    // Provided services
    provides one.modality.ecommerce.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingforms.sttp.SttpBookingFormProvider;

}