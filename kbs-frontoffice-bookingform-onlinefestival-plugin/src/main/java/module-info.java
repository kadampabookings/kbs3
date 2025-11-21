// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.onlinefestival.plugin {

    // Direct dependencies modules
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.eventheader;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.onlinefestival;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.onlinefestival.OnlineFestivalBookingFormProvider;

}