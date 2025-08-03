package org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival;

import one.modality.base.shared.entities.Event;
import one.modality.ecommerce.client.workingbooking.HasWorkingBookingProperties;
import one.modality.ecommerce.frontoffice.bookingform.multipages.BookingFormPage;
import one.modality.ecommerce.frontoffice.bookingform.multipages.MultiPageBookingForm;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.audiorecording.AudioRecordingPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.payment.PaymentPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.personal.PersonalDetailsPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.summary.FestivalSummaryPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.teaching.TeachingPage;
import org.kadampabookings.kbs.frontoffice.bookingforms.onlinefestival.terms.TermsAndConditionsPage;

/**
 * @author Bruno Salmon
 */
final class OnlineFestivalBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = {
        new TeachingPage(),
        new AudioRecordingPage(),
        new PersonalDetailsPage(this),
        new FestivalSummaryPage(),
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
