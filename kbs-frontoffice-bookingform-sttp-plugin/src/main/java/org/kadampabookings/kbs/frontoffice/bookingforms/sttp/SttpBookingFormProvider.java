package org.kadampabookings.kbs.frontoffice.bookingforms.sttp;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.ecommerce.client.workingbooking.HasWorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.BookEventActivity;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.event.frontoffice.bookingform.recurringevent.RecurringEventBookingForm;
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
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new RecurringEventBookingForm(event, (BookEventActivity) activity, new EventBookingFormSettingsBuilder(event)
            .setEventHeader(new MediaEventHeader(false))
            .build()
        );
    }
}
