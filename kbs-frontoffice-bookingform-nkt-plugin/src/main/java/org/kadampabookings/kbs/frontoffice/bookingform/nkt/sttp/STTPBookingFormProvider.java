package org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.BookEventActivity;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.event.frontoffice.bookingform.recurringevent.RecurringEventBookingForm;
import one.modality.event.frontoffice.eventheader.MediaEventHeader;

/**
 * @author Bruno Salmon
 */
public class STTPBookingFormProvider implements BookingFormProvider {

    private static final int STTP_EVENT_TYPE_ID = 48;

    @Override
    public boolean acceptEvent(Event event, BookingFormEntryPoint entryPoint) {
        // Only supports new bookings for STTP events
        if (entryPoint != BookingFormEntryPoint.NEW_BOOKING) {
            return false;
        }
        return event != null && Entities.samePrimaryKey(event.getType(), STTP_EVENT_TYPE_ID);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        return new RecurringEventBookingForm(event, (BookEventActivity) activity, new EventBookingFormSettingsBuilder(event)
            .setEventHeader(new MediaEventHeader(false))
            .build()
        );
    }
}
