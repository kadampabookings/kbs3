package org.kadampabookings.kbs.frontoffice.bookingform.nkt.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.event.frontoffice.eventheader.EventTitleHeader;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
public final class NKTOnlineFestivalBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        return FestivalType.isFestival(event);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new NKTOnlineFestivalBookingForm(activity,
            new EventBookingFormSettingsBuilder(event)
                .setEventHeader(new EventTitleHeader())
                .setHeaderMaxTopBottomPadding(62)
                .setShowNavigationBar(true)
                .setShowPriceBar(true)
                .build()
        );
    }
}
