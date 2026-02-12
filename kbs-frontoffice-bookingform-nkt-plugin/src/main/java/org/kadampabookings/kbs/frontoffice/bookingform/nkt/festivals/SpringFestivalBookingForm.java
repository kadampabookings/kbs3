package org.kadampabookings.kbs.frontoffice.bookingform.nkt.festivals;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractSinglePeriodInternationalFestival;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

import java.util.List;

/**
 * Booking form implementation for Spring Festival (international festival).
 *
 * <p>This form extends {@link AbstractSinglePeriodInternationalFestival} with
 * Spring Festival specific configuration:</p>
 * <ul>
 *   <li>JOY_AMBER color scheme (festive golden/amber theme)</li>
 *   <li>Available translation languages for Spring Festival</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see AbstractSinglePeriodInternationalFestival
 */
public final class SpringFestivalBookingForm extends AbstractSinglePeriodInternationalFestival {

    /**
     * Creates a new Spring Festival booking form.
     *
     * @param activity    the activity providing WorkingBookingProperties
     * @param settings    the event booking form settings
     * @param entryPoint  the entry point (NEW_BOOKING, MODIFY_BOOKING, PAY_BOOKING)
     */
    public SpringFestivalBookingForm(
            HasWorkingBookingProperties activity,
            EventBookingFormSettings settings,
            BookingFormEntryPoint entryPoint
    ) {
        super(activity, settings, entryPoint, BookingFormColorScheme.JOY_AMBER);
    }

    @Override
    protected BookingFormColorScheme getColorScheme() {
        return BookingFormColorScheme.JOY_AMBER;
    }

    @Override
    protected List<String> getAvailableTranslationLanguages() {
        // Return languages typically available at Spring Festival
        // This list can be made configurable via event configuration in the future
        return List.of(
            "Spanish",
            "French",
            "German",
            "Portuguese",
            "Italian",
            "Chinese",
            "Hard of Hearing (English)"
        );
    }

    // Note: getForm() is inherited from AbstractSinglePeriodInternationalFestival
    // and returns the StandardBookingForm which implements BookingForm.
}
