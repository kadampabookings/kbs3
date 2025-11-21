// File managed by WebFX (DO NOT EDIT MANUALLY)

import org.kadampabookings.kbs.frontoffice.bookingform.nkt.onlinefestival.NKTOnlineFestivalBookingFormProvider;
import org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp.STTPBookingFormProvider;

module kbs.frontoffice.bookingform.nkt.plugin {

    // Direct dependencies modules
    requires javafx.graphics;
    requires kbs.client.festivaltypes;
    requires modality.base.shared.entities;
    requires modality.booking.client.workingbooking;
    requires modality.booking.frontoffice.bookingform;
    requires modality.booking.frontoffice.bookingpage;
    requires modality.event.frontoffice.activity.book;
    requires modality.event.frontoffice.bookingform.recurringevent.plugin;
    requires modality.event.frontoffice.eventheader;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports org.kadampabookings.kbs.frontoffice.bookingform.nkt.onlinefestival;
    exports org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp;

    // Provided services
    provides one.modality.booking.frontoffice.bookingform.BookingFormProvider with NKTOnlineFestivalBookingFormProvider, STTPBookingFormProvider;

}