package org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;

/**
 * Provider for STTP (Systematic Training and Practice Program) booking forms.
 *
 * <p>STTP is a simplified recurring event form where users register for all sessions
 * automatically - no individual date selection is needed.</p>
 *
 * @author Bruno Salmon
 * @author Claude (adapted to use STTPBookingForm)
 */
public class STTPBookingFormProvider implements BookingFormProvider {

    private static final int STTP_EVENT_TYPE_ID = 48;

    @Override
    public boolean acceptEvent(Event event) {
        return event != null && Entities.samePrimaryKey(event.getType(), STTP_EVENT_TYPE_ID);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        // No MediaEventHeader - event info is shown inside the form as Step 1
        EventBookingFormSettings settings = new EventBookingFormSettingsBuilder(event)
            .build();
        return new STTPBookingForm(activity, settings, entryPoint).getForm();
    }
}
