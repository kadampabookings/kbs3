package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;

/**
 * Provider for the US Festival booking form (event type 38).
 *
 * <p>This provider creates the USFestivalBookingForm which features:</p>
 * <ul>
 *   <li>Registration type selection (In-Person / Online)</li>
 *   <li>Accommodation selection</li>
 *   <li>Festival day selection with meals and options</li>
 *   <li>Standard checkout flow</li>
 * </ul>
 *
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
    } //comment

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        // Build settings using the builder - event info is shown inside the form
        EventBookingFormSettings settings = new EventBookingFormSettingsBuilder(event)
            .setHeaderMaxTopBottomPadding(62)
            .setShowNavigationBar(true)
            .setShowPriceBar(false)
            .setPartialEventAllowed(true)  // Festival allows partial attendance
            .build();
        return null;
    }
}
