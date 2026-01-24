package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractSinglePeriodInPersonBookingForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * US Festival In-Person Booking Form - Implementation for in-person US Festival (event type 38) bookings.
 *
 * <p>This form extends {@link AbstractSinglePeriodInPersonBookingForm} and customizes:</p>
 * <ul>
 *   <li>Blue color scheme (WISDOM_BLUE)</li>
 * </ul>
 *
 * <p>Note: This form is created by {@link USFestivalEntryForm} when user selects In-Person registration.</p>
 *
 * @author Bruno Salmon
 * @see USFestivalEntryForm
 * @see AbstractSinglePeriodInPersonBookingForm
 */
public final class USFestivalInPersonBookingForm extends AbstractSinglePeriodInPersonBookingForm {

    /**
     * Creates the US Festival in-person booking form.
     *
     * @param activity   The activity providing WorkingBookingProperties
     * @param settings   The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public USFestivalInPersonBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        super(activity, settings, entryPoint);
    }

    @Override
    protected BookingFormColorScheme getColorScheme() {
        return BookingFormColorScheme.WISDOM_BLUE;
    }
}
