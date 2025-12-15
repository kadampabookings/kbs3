package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification.BookingModificationForm;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification.BookingModificationFormAdapter;

/**
 * Provider for MKMC Online Empowerment booking forms.
 * Supports both new bookings and modification of existing bookings (adding audio recordings).
 *
 * @author Bruno Salmon
 */
public final class MKMCOnlineEmpowermentBookingFormProvider implements BookingFormProvider {

    private static final int EVENT_TYPE_ONLINE_EMPOWERMENT = 24;

    @Override
    public boolean acceptEvent(Event event) {
        return Entities.samePrimaryKey(event.getType(), EVENT_TYPE_ONLINE_EMPOWERMENT);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        if (entryPoint == BookingFormEntryPoint.MODIFY_BOOKING) {
            // Create modification form for adding options to existing booking
            BookingModificationForm modificationForm = BookingModificationForm.create(activity.getWorkingBookingProperties());
            return new BookingModificationFormAdapter(modificationForm);
        }

        // Default: create new booking form
        return new MKMCOnlineEmpowermentBookingForm(activity,
            new EventBookingFormSettingsBuilder(event)
                .setHeaderMaxTopBottomPadding(62)
                .setShowNavigationBar(true)
                .setShowPriceBar(false)
                .build()
        ).getForm();
    }
}
