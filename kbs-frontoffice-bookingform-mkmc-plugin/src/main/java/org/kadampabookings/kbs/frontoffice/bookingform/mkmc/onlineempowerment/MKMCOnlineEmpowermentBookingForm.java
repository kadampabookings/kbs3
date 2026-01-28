package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.sections.options.HasRateTypeSection;
import one.modality.booking.frontoffice.bookingpage.sections.prerequisite.HasPrerequisiteSection;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractOnlineEventBookingForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections.PrerequisiteSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections.RateTypeSection;

/**
 * MKMC Online Empowerment Booking Form.
 * Extends AbstractOnlineEventBookingForm for online event bookings with audio recordings.
 *
 * <p>This form uses:</p>
 * <ul>
 *   <li>JOY_AMBER color scheme</li>
 *   <li>UK residency prerequisite section</li>
 *   <li>Custom rate type section with programme info</li>
 *   <li>Default audio recording section</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see AbstractOnlineEventBookingForm
 */
public final class MKMCOnlineEmpowermentBookingForm extends AbstractOnlineEventBookingForm {

    /**
     * Creates the MKMC Online Empowerment booking form.
     *
     * @param activity   the activity providing WorkingBookingProperties
     * @param settings   the event booking form settings
     * @param entryPoint the entry point (NEW_BOOKING, RESUME_PAYMENT, etc.)
     */
    public MKMCOnlineEmpowermentBookingForm(
            HasWorkingBookingProperties activity,
            EventBookingFormSettings settings,
            BookingFormEntryPoint entryPoint) {
        super(activity, settings, entryPoint, BookingFormColorScheme.JOY_AMBER);
    }

    @Override
    protected HasPrerequisiteSection createPrerequisiteSection() {
        return new PrerequisiteSection();
    }

    @Override
    protected HasRateTypeSection createRateTypeSection() {
        return new RateTypeSection();
    }

    @Override
    protected Object getOptionsPageTitleKey() {
        return MKMCI18nKeys.Options;
    }
}
