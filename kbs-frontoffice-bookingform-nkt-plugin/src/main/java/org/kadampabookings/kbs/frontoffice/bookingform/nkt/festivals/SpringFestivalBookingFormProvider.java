package org.kadampabookings.kbs.frontoffice.bookingform.nkt.festivals;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;

/**
 * Provider for Spring Festival booking forms.
 *
 * <p>Spring Festival is an international Buddhist festival requiring:</p>
 * <ul>
 *   <li>Member selection (who is this booking for?)</li>
 *   <li>Child carer designation for children</li>
 *   <li>Accommodation selection</li>
 *   <li>Booking details with translation, ordination ceremony, and assistance options</li>
 * </ul>
 *
 * <p>This provider matches events with:</p>
 * <ul>
 *   <li>Event type ID 1 (SPRING_FESTIVAL), OR</li>
 *   <li>Event name containing "Spring Festival"</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see SpringFestivalBookingForm
 */
public class SpringFestivalBookingFormProvider implements BookingFormProvider {

    // Event type ID for Spring Festival events
    // This should match the event_type table in the database
    private static final int SPRING_FESTIVAL_EVENT_TYPE_ID = 35;

    @Override
    public boolean acceptEvent(Event event) {
        if (event == null) {
            return false;
        }

        // Check by event type ID
        if (Entities.samePrimaryKey(event.getType(), SPRING_FESTIVAL_EVENT_TYPE_ID)) {
            return true;
        }

        // Fallback: check by event name
        String eventName = event.getName();
        if (eventName != null && eventName.toLowerCase().contains("spring festival")) {
            return true;
        }

        return false;
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        EventBookingFormSettings settings = new EventBookingFormSettingsBuilder(event)
            .build();
        return new SpringFestivalBookingForm(activity, settings, entryPoint).getForm();
    }
}
