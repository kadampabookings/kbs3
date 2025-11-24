package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.BookingFormPage;
import one.modality.booking.frontoffice.bookingpage.ButtonNavigation;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingpage.MultiPageBookingForm;
import one.modality.booking.frontoffice.bookingpage.StepProgressHeader;
import one.modality.booking.frontoffice.bookingpage.pages.payment.PaymentPage;
import one.modality.booking.frontoffice.bookingpage.pages.personal.PersonalDetailsPage;
import one.modality.booking.frontoffice.bookingpage.pages.summary.SummaryPage;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections.*;

/**
 * @author Bruno Salmon
 */
public final class MKMCInPersonRetreatBookingForm extends MultiPageBookingForm {

    private final BookingFormPage[] pages = createPages();

    public MKMCInPersonRetreatBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings) {
        super(activity, settings);
        setHeader(new StepProgressHeader());
        setNavigation(new ButtonNavigation());
    }

    private BookingFormPage[] createPages() {
        // CUSTOM PAGES: Event-specific pages using CompositeBookingFormPage with custom
        // sections

        // Step 1: Event & Room
        CompositeBookingFormPage eventAndRoomPage = new CompositeBookingFormPage(
                MKMCInPersonRetreatI18nKeys.EventAndRoom,
                new EventPackageSection(),
                new RateTypeSection(),
                new AccommodationSection())
                .setStep(true)
                .setHeaderVisible(true);

        // Step 2: Options (Details)
        CompositeBookingFormPage optionsPage = new CompositeBookingFormPage(
                MKMCInPersonRetreatI18nKeys.Options,
                new DatesSection(),
                new TeachingSection(),
                new MealsSection(),
                new AudioRecordingsSection(),
                new ParkingSection())
                .setStep(true);

        // STANDARD PAGES: Reuse existing framework pages for common functionality

        // Step 3: Personal Details (replaces AccountInfoSection)
        PersonalDetailsPage personalDetailsPage = new PersonalDetailsPage(this);

        // Step 4: Summary (replaces ReviewSummarySection)
        SummaryPage summaryPage = new SummaryPage();

        // Step 5: Confirmation (Basket) - Keep custom for now if specific basket
        // behavior is needed
        // TODO: Consider if this can be merged with SummaryPage or replaced with
        // standard behavior
        CompositeBookingFormPage confirmationPage = new CompositeBookingFormPage(
                MKMCInPersonRetreatI18nKeys.Confirmation,
                new ConfirmationSection())
                .setStep(true);

        // Step 6: Payment (replaces custom payment page)
        PaymentPage paymentPage = new PaymentPage(this);

        // Navigation buttons for custom pages only
        // Standard pages handle their own navigation integration

        eventAndRoomPage.setButtons(
                new BookingFormButton(MKMCInPersonRetreatI18nKeys.ContinueToOptions,
                        e -> navigateTo(optionsPage),
                        "btn-primary",
                        eventAndRoomPage.validProperty()));

        optionsPage.setButtons(
                new BookingFormButton(MKMCInPersonRetreatI18nKeys.BackToEventAndRoom,
                        e -> navigateTo(eventAndRoomPage), "btn-back"),
                new BookingFormButton(MKMCInPersonRetreatI18nKeys.ContinueToYourInfo,
                        e -> navigateTo(personalDetailsPage), "btn-primary",
                        optionsPage.validProperty()));

        // Note: PersonalDetailsPage, SummaryPage, and PaymentPage don't need custom
        // buttons
        // They integrate with the navigation system automatically

        confirmationPage.setButtons(
                new BookingFormButton(MKMCInPersonRetreatI18nKeys.BookAnotherPerson, e -> {
                    // TODO: Implement "book another person" functionality
                }, "btn-secondary"),
                new BookingFormButton(MKMCInPersonRetreatI18nKeys.ProceedToPayment,
                        e -> navigateTo(paymentPage),
                        "btn-primary"));

        return new BookingFormPage[] {
                eventAndRoomPage,
                optionsPage,
                personalDetailsPage, // Standard page
                summaryPage, // Standard page
                confirmationPage, // Custom (consider if needed)
                paymentPage // Standard page
        };
    }

    private void navigateTo(BookingFormPage page) {
        BookingFormPage[] pages = getPages();
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == page) {
                navigateToPage(i);
                return;
            }
        }
    }

    @Override
    public BookingFormPage[] getPages() {
        return pages;
    }

}
