package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;

import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.sections.confirmation.HasConfirmationSection;
import one.modality.booking.frontoffice.bookingpage.standard.AbstractOnlineEventModificationForm;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

/**
 * MKMC-specific booking modification form for adding audio recording options
 * to existing bookings.
 *
 * <p>Extends {@link AbstractOnlineEventModificationForm} to provide:</p>
 * <ul>
 *   <li>JOURNEY_GREEN color scheme</li>
 *   <li>MKMC-specific confirmation section</li>
 *   <li>MKMC-specific warning messages</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see AbstractOnlineEventModificationForm
 * @see AudioRecordingConfirmationSection
 */
public final class BookingModificationForm extends AbstractOnlineEventModificationForm {

    /**
     * Creates a new MKMC booking modification form.
     *
     * @param workingBookingProperties the working booking properties
     */
    public BookingModificationForm(WorkingBookingProperties workingBookingProperties) {
        super(workingBookingProperties);
    }

    /**
     * Creates a new MKMC booking modification form with explicit color scheme.
     *
     * @param workingBookingProperties the working booking properties
     * @param colorScheme              the color scheme (ignored, always uses JOURNEY_GREEN)
     */
    public BookingModificationForm(WorkingBookingProperties workingBookingProperties, BookingFormColorScheme colorScheme) {
        super(workingBookingProperties);
    }

    /**
     * Factory method to create a form from WorkingBookingProperties.
     */
    public static BookingModificationForm create(WorkingBookingProperties workingBookingProperties) {
        return new BookingModificationForm(workingBookingProperties);
    }

    // ========================================
    // AbstractOnlineEventModificationForm Implementation
    // ========================================

    @Override
    protected BookingFormColorScheme getColorScheme() {
        return BookingFormColorScheme.JOURNEY_GREEN;
    }

    @Override
    protected HasConfirmationSection createConfirmationSection() {
        return new AudioRecordingConfirmationSection();
    }

    @Override
    protected Object getNoOptionsAvailableMessageKey() {
        return MKMCI18nKeys.NoRecordingsAvailable;
    }

    @Override
    protected Object getAllOptionsPurchasedMessageKey() {
        return MKMCI18nKeys.AllRecordingsAlreadyPurchased;
    }
}
