// File managed by WebFX (DO NOT EDIT MANUALLY)

module kbs.frontoffice.bookingform.nkt.plugin {

    // Direct dependencies modules
    requires javafx.graphics;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.crm.client.authn.fx;
    requires modality.event.frontoffice.activity.book;
    requires webfx.platform.console;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.nkt.festivals;
    exports org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp.STTPBookingFormProvider, org.kadampabookings.kbs.frontoffice.bookingform.nkt.festivals.SpringFestivalBookingFormProvider;

}