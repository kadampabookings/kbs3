package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.*;
import one.modality.booking.frontoffice.bookingpage.pages.audiorecording.AudioRecordingPage;
import one.modality.booking.frontoffice.bookingpage.pages.closed.BookingsClosedPage;
import one.modality.booking.frontoffice.bookingpage.pages.payment.PaymentPage;
import one.modality.booking.frontoffice.bookingpage.pages.personal.PersonalDetailsPage;
import one.modality.booking.frontoffice.bookingpage.pages.prerequisite.PrerequisitePage;
import one.modality.booking.frontoffice.bookingpage.pages.summary.SummaryPage;
import one.modality.booking.frontoffice.bookingpage.pages.teaching.OnlineTeachingPage;
import one.modality.booking.frontoffice.bookingpage.pages.terms.TermsAndConditionsPage;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import one.modality.booking.frontoffice.bookingpage.pages.teaching.OnlineTeachingSection;
import one.modality.base.shared.knownitems.KnownItemI18nKeys;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

/**
 * @author Bruno Salmon
 */
final class MKMCOnlineEmpowermentBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = {
            new BookingsClosedPage(),
            new PrerequisitePage(MKMCI18nKeys.OnlineEmpowermentUKMessage, MKMCI18nKeys.OnlineEmpowermentUKConfirm),
            new CompositeBookingFormPage(KnownItemI18nKeys.TeachingsOnline, new OnlineTeachingSection()),
            new AudioRecordingPage(),
            new PersonalDetailsPage(this),
            new SummaryPage(),
            new TermsAndConditionsPage(this),
            new PaymentPage(this)
    };

    public MKMCOnlineEmpowermentBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings) {
        super(activity, settings);
        setHeader(new StepProgressHeader());
        setNavigation(new ButtonNavigation());
    }

    @Override
    public String getEventFieldsToLoad() {
        return Event.vodExpirationDate;
    }

    @Override
    public BookingFormPage[] getPages() {
        return pages;
    }

}
