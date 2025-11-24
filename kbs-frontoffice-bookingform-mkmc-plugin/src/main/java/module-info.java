// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.mkmc.plugin {

    // Direct dependencies modules
    requires javafx.controls;
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.base.shared.knownitems;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.eventheader;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;
    exports org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.MKMCOnlineEmpowermentBookingFormProvider, org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.MKMCInPersonRetreatBookingFormProvider;

}