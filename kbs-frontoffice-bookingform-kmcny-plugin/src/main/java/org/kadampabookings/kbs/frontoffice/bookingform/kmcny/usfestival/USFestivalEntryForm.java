package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.AbstractEntryForm;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultRegistrationTypeSection;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * Entry form for US Festival that presents the registration type choice (In-Person vs Online).
 *
 * <p>This form acts as a gateway that dynamically swaps to the appropriate booking form
 * based on the user's selection:</p>
 * <ul>
 *   <li>In-Person → {@link USFestivalInPersonBookingForm}</li>
 *   <li>Online → {@link USFestivalOnlineBookingForm} (coming soon)</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see AbstractEntryForm
 * @see DefaultRegistrationTypeSection
 */
public final class USFestivalEntryForm extends AbstractEntryForm {

    private DefaultRegistrationTypeSection registrationTypeSection;

    public USFestivalEntryForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        super(activity, settings, entryPoint);
        setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
    }

    @Override
    protected void initializeContent() {
        // Create the registration type section
        registrationTypeSection = new DefaultRegistrationTypeSection();
        registrationTypeSection.setColorScheme(getColorScheme());

        // Add to container
        getContainer().getChildren().add(registrationTypeSection.getView());

        // Set up type selection callback
        setupCallbacks();
    }

    private void setupCallbacks() {
        registrationTypeSection.setOnTypeSelected(type -> {
            if (type == DefaultRegistrationTypeSection.RegistrationType.IN_PERSON) {
                swapToInPersonForm();
            } else if (type == DefaultRegistrationTypeSection.RegistrationType.ONLINE) {
                swapToOnlineForm();
            }
        });

        // Override the continue callback to prevent auto-navigation within RegistrationTypeSection
        // The form swap handles the navigation
        registrationTypeSection.setOnContinuePressed(null);
    }

    private void swapToInPersonForm() {
        USFestivalInPersonBookingForm inPersonForm = new USFestivalInPersonBookingForm(getActivity(), (EventBookingFormSettings) settings, getEntryPoint());
        swapToForm(inPersonForm.getForm());
    }

    private void swapToOnlineForm() {
        USFestivalOnlineBookingForm onlineForm = new USFestivalOnlineBookingForm(getActivity(), (EventBookingFormSettings) settings, getEntryPoint());
        swapToForm(onlineForm);
    }

    @Override
    public void onWorkingBookingLoaded() {
        // Ensure content is initialized (buildUi triggers initializeContent)
        buildUi();
        // Pass working booking properties to registration type section for event header display
        registrationTypeSection.setWorkingBookingProperties(getActivity().getWorkingBookingProperties());
    }
}
