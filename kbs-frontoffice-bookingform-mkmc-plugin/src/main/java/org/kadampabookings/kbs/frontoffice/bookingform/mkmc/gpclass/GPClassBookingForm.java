package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.sections.dates.HasClassDateSelectionSection;
import one.modality.booking.frontoffice.bookingpage.sections.existing.DefaultExistingBookingSection;
import one.modality.booking.frontoffice.bookingpage.sections.existing.HasExistingBookingSection;
import one.modality.booking.frontoffice.bookingpage.sections.generalprogram.DefaultClassDateSelectionSection;
import one.modality.booking.frontoffice.bookingpage.sections.generalprogram.DefaultGeneralProgramSummarySection;
import one.modality.booking.frontoffice.bookingpage.sections.options.GPClassRateTypeSection;
import one.modality.booking.frontoffice.bookingpage.sections.options.HasRateTypeSection;
import one.modality.booking.frontoffice.bookingpage.sections.summary.HasSummarySection;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractGeneralProgramClassBookingForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * GP Class Booking Form - MKMC implementation for weekly general programme class bookings.
 *
 * <p>This is the organization-specific implementation that extends
 * {@link AbstractGeneralProgramClassBookingForm} with MKMC-specific sections.</p>
 *
 * <p>Features provided by this form:</p>
 * <ul>
 *   <li>Select individual class dates from a series</li>
 *   <li>See discount when selecting all classes (full term)</li>
 *   <li>Book for themselves or guests</li>
 *   <li>Complete payment (full payment only, no partial)</li>
 * </ul>
 *
 * @author Claude
 * @see AbstractGeneralProgramClassBookingForm
 */
public final class GPClassBookingForm extends AbstractGeneralProgramClassBookingForm {

    /**
     * Creates the GP Class booking form.
     *
     * @param activity   the activity providing WorkingBookingProperties
     * @param settings   the event booking form settings
     * @param entryPoint the entry point (NEW_BOOKING, MODIFY_BOOKING, or PAY_BOOKING)
     */
    public GPClassBookingForm(HasWorkingBookingProperties activity,
                              EventBookingFormSettings settings,
                              BookingFormEntryPoint entryPoint) {
        super(activity, settings, entryPoint);
    }

    @Override
    protected BookingFormColorScheme getColorScheme() {
        return BookingFormColorScheme.PEACE_PURPLE;
    }

    @Override
    protected HasRateTypeSection createRateSection() {
        return new GPClassRateTypeSection();
    }

    @Override
    protected HasClassDateSelectionSection createDateSelectionSection() {
        return new DefaultClassDateSelectionSection();
    }

    @Override
    protected HasSummarySection createSummarySection(HasClassDateSelectionSection dateSelectionSection) {
        return new DefaultGeneralProgramSummarySection(dateSelectionSection);
    }

    @Override
    protected HasExistingBookingSection createExistingBookingSection() {
        return new DefaultExistingBookingSection();
    }

    @Override
    protected Object getSelectClassesPageTitleKey() {
        return BookingPageI18nKeys.SelectClassesPageTitle;
    }

    @Override
    protected Object getExistingBookingPageTitleKey() {
        return BookingPageI18nKeys.ExistingBookingPageTitle;
    }
}
