package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.ecommerce.client.workingbooking.HasWorkingBookingProperties;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettingsBuilder;
import one.modality.ecommerce.frontoffice.bookingform.BookingForm;
import one.modality.ecommerce.frontoffice.bookingform.BookingFormProvider;
import one.modality.event.frontoffice.eventheader.EventTitleHeader;
import org.kadampabookings.kbs.client.festivaltypes.FestivalType;

/**
 * @author Bruno Salmon
 */
public final class OnlineFestivalBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        return FestivalType.isFestival(event);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new OnlineFestivalBookingForm(activity, new EventBookingFormSettingsBuilder()
            .setEventHeader(new EventTitleHeader())
            .setHeaderMaxTopBottomPadding(62)
            .setShowNavigationBar(true)
            .setShowPriceBar(true)
            .build()
        );
    }
}
