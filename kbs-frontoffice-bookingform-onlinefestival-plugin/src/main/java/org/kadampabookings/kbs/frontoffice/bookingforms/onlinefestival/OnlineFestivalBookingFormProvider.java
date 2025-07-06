package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.event.frontoffice.activities.booking.process.event.*;
import one.modality.event.frontoffice.eventheader.MediaEventHeader;
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
            .setEventHeader(new MediaEventHeader(false))
            .build()
        );
    }
}
