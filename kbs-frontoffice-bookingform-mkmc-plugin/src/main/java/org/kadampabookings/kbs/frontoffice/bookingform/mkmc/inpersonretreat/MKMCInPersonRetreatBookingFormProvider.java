package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat;

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
public final class MKMCInPersonRetreatBookingFormProvider implements BookingFormProvider {

    @Override
    public boolean acceptEvent(Event event) {
        // TODO: Define specific criteria for this form. For now, using a placeholder ID
        return Entities.samePrimaryKey(event.getType(), 21);
    }

    @Override
    public int getPriority() {
        return APP_PRIORITY;
    }

    @Override
    public BookingForm createBookingForm(Event event, HasWorkingBookingProperties activity) {
        return new MKMCInPersonRetreatBookingForm(activity,
                new EventBookingFormSettingsBuilder(event)
                        .setEventHeader(new EventTitleHeader())
                        .setHeaderMaxTopBottomPadding(62)
                        .setShowNavigationBar(true)
                        .setNavigationClickable(false)
                        .setShowPriceBar(false)
                        .build());
    }
}
