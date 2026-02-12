package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;

/**
 * Provider for the GP Class booking form.
 * Handles weekly general programme (GP) class bookings where users can select
 * individual class dates from a series.
 *
 * @author Claude
 */
public final class GPClassBookingFormProvider implements BookingFormProvider {

    // Event type for GP Classes (configurable - currently set to 22)
    private static final int EVENT_TYPE_GP_CLASS = 47;

    @Override
    public boolean acceptEvent(Event event) {
        return Entities.samePrimaryKey(event.getType(), EVENT_TYPE_GP_CLASS);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public boolean autoLoadExistingBooking(Event event) {
        return true;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity, BookingFormEntryPoint entryPoint) {
        return new GPClassBookingForm(activity,
            new EventBookingFormSettingsBuilder(event)
                .setHeaderMaxTopBottomPadding(62)
                .setShowNavigationBar(true)
                .setShowPriceBar(false)
                .setAutoLoadExistingBooking(true)  // Load existing booking for logged-in users
                .build(),
            entryPoint  // Pass entry point for payment resume handling
        ).getForm();
    }
}
