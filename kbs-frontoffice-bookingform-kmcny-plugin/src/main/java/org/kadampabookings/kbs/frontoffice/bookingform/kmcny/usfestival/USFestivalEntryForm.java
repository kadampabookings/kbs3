package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractRegistrationTypeEntryForm;
import one.modality.booking.frontoffice.bookingpage.standard.DefaultInPersonBookingForm;
import one.modality.booking.frontoffice.bookingpage.standard.DefaultOnlineEventBookingForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * Entry form for US Festival that presents the registration type choice (In-Person vs Online).
 *
 * <p>This form acts as a gateway that dynamically swaps to the appropriate booking form
 * based on the user's selection:</p>
 * <ul>
 *   <li>In-Person → {@link DefaultInPersonBookingForm} with WISDOM_BLUE theme</li>
 *   <li>Online → {@link DefaultOnlineEventBookingForm} with WISDOM_BLUE theme</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see AbstractRegistrationTypeEntryForm
 */
public final class USFestivalEntryForm extends AbstractRegistrationTypeEntryForm {

    public USFestivalEntryForm(
            HasWorkingBookingProperties activity,
            EventBookingFormSettings settings,
            BookingFormEntryPoint entryPoint) {
        super(activity, settings, entryPoint, BookingFormColorScheme.WISDOM_BLUE);
    }

    @Override
    protected BookingForm createInPersonForm() {
        return new DefaultInPersonBookingForm(
            getActivity(),
            (EventBookingFormSettings) settings,
            getEntryPoint(),
            BookingFormColorScheme.WISDOM_BLUE
        ).getForm();
    }

    @Override
    protected BookingForm createOnlineForm() {
        return new DefaultOnlineEventBookingForm(
            getActivity(),
            (EventBookingFormSettings) settings,
            getEntryPoint(),
            BookingFormColorScheme.WISDOM_BLUE
        ).getForm();
    }

    @Override
    protected boolean isOnlineEnabled() {
        return true;  // Online registration is now enabled
    }
}
