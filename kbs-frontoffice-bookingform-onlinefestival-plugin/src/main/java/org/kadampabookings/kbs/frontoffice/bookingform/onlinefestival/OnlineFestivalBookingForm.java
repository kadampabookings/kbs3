package org.kadampabookings.kbs.frontoffice.bookingform.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormPage;
import one.modality.booking.frontoffice.bookingpage.MultiPageBookingForm;
import one.modality.booking.frontoffice.bookingpage.pages.audiorecording.AudioRecordingPage;
import one.modality.booking.frontoffice.bookingpage.pages.closed.BookingsClosedPage;
import one.modality.booking.frontoffice.bookingpage.pages.payment.PaymentPage;
import one.modality.booking.frontoffice.bookingpage.pages.personal.PersonalDetailsPage;
import one.modality.booking.frontoffice.bookingpage.pages.summary.SummaryPage;
import one.modality.booking.frontoffice.bookingpage.pages.teaching.OnlineTeachingPage;
import one.modality.booking.frontoffice.bookingpage.pages.terms.TermsAndConditionsPage;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = {
        new BookingsClosedPage(),
        new OnlineTeachingPage(),
        new AudioRecordingPage(),
        new PersonalDetailsPage(this),
        new SummaryPage(),
        new TermsAndConditionsPage(this),
        new PaymentPage(this)
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
