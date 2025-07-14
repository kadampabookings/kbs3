package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.ecommerce.client.workingbooking.HasWorkingBookingProperties;
import one.modality.event.frontoffice.activities.booking.process.event.EventBookingFormSettings;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.bookingform.multipages.MultiPageBookingForm;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording.AudioRecordingPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment.PaymentAmountPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary.FestivalSummaryPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching.TeachingPage;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = {
        new TeachingPage(),
        new AudioRecordingPage(),
        new FestivalSummaryPage(),
        new PaymentAmountPage(this)
    };

    public OnlineFestivalBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings) {
        super(activity, settings);
    }

    @Override
    public String getEventFieldsToLoad() {
        return Event.vodExpirationDate;
    }

    @Override
    protected BookingFormPage[] getPages() {
        return pages;
    }

}
