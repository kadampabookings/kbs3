package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormSettingsBuilder;
import one.modality.event.frontoffice.eventheader.EventTitleHeader;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
public final class OnlineFestivalBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        return FestivalType.isFestival(event);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, BookEventActivity activity) {
        return new OnlineFestivalBookingForm(activity, new BookingFormSettingsBuilder()
            .setEventHeader(new EventTitleHeader())
            .setHeaderMaxTopBottomPadding(62)
            .setShowNavigationBar(true)
            .setShowPriceBar(true)
            .build()
        );
    }
}
