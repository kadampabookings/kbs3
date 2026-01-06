package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;

/**
 * @author Bruno Salmon
 */
public final class USFestivalBookingFormProvider implements BookingFormProvider {

    private static final int US_FESTIVAL_EVENT_TYPE_ID = 38;

    @Override
    public boolean acceptEvent(Event event) {
        return event != null && Entities.samePrimaryKey(event.getType(), US_FESTIVAL_EVENT_TYPE_ID);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        return null;
    }
}
