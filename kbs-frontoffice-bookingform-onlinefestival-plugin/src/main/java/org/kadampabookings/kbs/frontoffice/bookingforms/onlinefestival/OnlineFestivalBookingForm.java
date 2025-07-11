package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormSettings;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.MultiPageBookingForm;
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

    public OnlineFestivalBookingForm(BookEventActivity activity, BookingFormSettings settings) {
        super(activity, settings);
    }

    @Override
    protected BookingFormPage[] getPages() {
        return pages;
    }

    @Override
    protected void updateShowSubmitButton() {
        // We don't display the generic submitButton on the last page (PaymentAmountPage) because it has its own
        setShowSubmitButton(false);
    }

}
