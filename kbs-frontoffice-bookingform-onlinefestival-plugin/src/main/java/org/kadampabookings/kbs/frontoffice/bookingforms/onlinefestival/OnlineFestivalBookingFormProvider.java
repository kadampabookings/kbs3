package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import dev.webfx.stack.orm.entity.Entities;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingForm;
import one.modality.booking.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.event.frontoffice.eventheader.EventTitleHeader;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
public final class OnlineFestivalBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        return FestivalType.isFestival(event)
               // We use this booking form also for MKMC online empowerment weekends (hardcoded for now)
               || Entities.samePrimaryKey(event.getType(), 24);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new OnlineFestivalBookingForm(activity, new EventBookingFormSettingsBuilder(event)
            .setEventHeader(new EventTitleHeader())
            .setHeaderMaxTopBottomPadding(62)
            .setShowNavigationBar(true)
            .setShowPriceBar(true)
            .build()
        );
    }
}
