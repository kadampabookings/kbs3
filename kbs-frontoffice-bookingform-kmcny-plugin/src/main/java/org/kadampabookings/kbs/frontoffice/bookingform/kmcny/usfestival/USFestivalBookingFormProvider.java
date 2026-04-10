package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.booking.frontoffice.bookingpage.standard.DefaultInPersonBookingForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;

/**
 * Provider for the US Festival booking form (event type 38).
 *
 * <p>This provider creates the appropriate booking form based on entry point:</p>
 * <ul>
 *   <li>NEW_BOOKING: {@link USFestivalEntryForm} - Entry form with registration type selection</li>
 *   <li>MODIFY_BOOKING/PAY_BOOKING: {@link DefaultInPersonBookingForm} - Direct in-person form</li>
 * </ul>
 *
 * <p>The entry form allows users to choose between In-Person and Online registration,
 * then dynamically swaps to the appropriate booking form.</p>
 *
 * @author Bruno Salmon
 * @see USFestivalEntryForm
 */
public final class USFestivalBookingFormProvider implements BookingFormProvider {

    @Override
    public String getBookingFormCode() {
        return "us-festival";
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        // Build settings using the builder - event info is shown inside the form
        EventBookingFormSettings settings = new EventBookingFormSettingsBuilder(event)
            .setHeaderMaxTopBottomPadding(62)
            .setShowNavigationBar(true)
            .setShowPriceBar(false)
            .setPartialEventAllowed(true)  // Festival allows partial attendance
            .build();

        // For new bookings, show entry form first to let user choose registration type
        if (entryPoint == BookingFormEntryPoint.NEW_BOOKING) {
            return new USFestivalEntryForm(activity, settings, entryPoint);
        }

        // For modifications/payments, go directly to in-person form with WISDOM_BLUE theme
        // TODO: Future enhancement - detect if existing booking is online or in-person from document
        return new DefaultInPersonBookingForm(
            activity, settings, entryPoint, BookingFormColorScheme.WISDOM_BLUE
        ).getForm();
    }
}
