// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.mkmc.plugin {

    // Direct dependencies modules
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.eventheader;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.MKMCOnlineEmpowermentBookingFormProvider;

}