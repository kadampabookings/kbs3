package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.event.frontoffice.eventheader.EventTitleHeader;

/**
 * @author Bruno Salmon
 */
public final class MKMCOnlineEmpowermentBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        // For now, we use this booking form only for MKMC online empowerment weekends
        return Entities.samePrimaryKey(event.getType(), 24);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new MKMCOnlineEmpowermentBookingForm(activity,
            new EventBookingFormSettingsBuilder(event)
                .setEventHeader(new EventTitleHeader())
                .setHeaderMaxTopBottomPadding(62)
                .setShowNavigationBar(true)
                .setShowPriceBar(true)
                .build()
        );
    }
}
