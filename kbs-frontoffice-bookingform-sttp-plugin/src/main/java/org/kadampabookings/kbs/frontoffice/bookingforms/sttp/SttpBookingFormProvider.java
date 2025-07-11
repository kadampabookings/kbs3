package org.kadampabookings.kbs.frontoffice.bookingforms.sttp;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingForm;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormSettingsBuilder;
import one.modality.event.frontoffice.bookingforms.recurringevent.RecurringEventBookingForm;
import one.modality.event.frontoffice.eventheader.MediaEventHeader;

/**
 * @author Bruno Salmon
 */
public class SttpBookingFormProvider implements BookingFormProvider {

    private static final int STTP_EVENT_TYPE_ID = 48;

    @Override
    public boolean acceptEvent(Event event) {
        return Entities.samePrimaryKey(event.getType(), STTP_EVENT_TYPE_ID);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, BookEventActivity activity) {
        return new RecurringEventBookingForm(event, activity, new BookingFormSettingsBuilder()
            .setEventHeader(new MediaEventHeader(false))
            .build()
        );
    }
}
