package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.event.frontoffice.activities.booking.process.event.BookEventActivity;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.BookingFormSettings;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.BookingFormPage;
import one.modality.event.frontoffice.activities.booking.process.event.bookingform.multipages.MultiPageBookingForm;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording.AudioRecordingPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary.FestivalSummaryPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching.TeachingPage;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = {
        new TeachingPage(),
        new AudioRecordingPage(),
        new FestivalSummaryPage()
    };

    public OnlineFestivalBookingForm(BookEventActivity activity, BookingFormSettings settings) {
        super(activity, settings);
    }

    @Override
    protected BookingFormPage[] getPages() {
        return pages;
    }
}
