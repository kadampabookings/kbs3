// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.mkmc.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.event.frontoffice.activity.book;
    requires webfx.extras.i18n.controls;
    requires webfx.extras.webtext;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.MKMCOnlineEmpowermentBookingFormProvider, org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.GPClassBookingFormProvider;

}