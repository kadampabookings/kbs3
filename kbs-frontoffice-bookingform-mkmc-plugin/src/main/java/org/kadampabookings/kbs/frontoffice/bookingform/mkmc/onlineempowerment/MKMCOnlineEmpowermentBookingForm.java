package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import dev.webfx.platform.async.Future;
import javafx.beans.binding.Bindings;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingForm;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormBuilder;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormCallbacks;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.booking.frontoffice.bookingpage.theme.ThemedBookingFormSection;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultAudioRecordingSection;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultEventHeaderSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections.PrerequisiteSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections.RateTypeSection;

/**
 * MKMC Online Empowerment Booking Form - Simplified implementation using the builder pattern.
 *
 * <p>This form demonstrates how simple it is to create a booking form with the
 * StandardBookingFormBuilder. All generic logic (state management, household loading,
 * navigation, payment processing, summary generation, section reset) is handled automatically.</p>
 *
 * <p>This class only needs to:</p>
 * <ul>
 *   <li>Create the custom Step 1 (Options page with MKMC-specific sections)</li>
 *   <li>Set up communication between sections (e.g., package → audio recording)</li>
 * </ul>
 *
 * <p>Default behaviors provided by StandardBookingForm:</p>
 * <ul>
 *   <li>Summary price lines are auto-generated from WorkingBooking document lines</li>
 *   <li>Sections implementing ResettableSection are auto-reset when registering another person</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class MKMCOnlineEmpowermentBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    // Color scheme for theming
    private final BookingFormColorScheme colorScheme;

    private RateTypeSection rateTypeSection;
    private DefaultAudioRecordingSection audioRecordingSection;
    private CompositeBookingFormPage optionsPage;

    /**
     * Creates the MKMC Online Empowerment booking form using the builder pattern.
     *
     * @param activity The activity providing WorkingBookingProperties
     * @param settings The event booking form settings
     */
    public MKMCOnlineEmpowermentBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings) {
        // Initialize color scheme (could be loaded from event configuration)
        this.colorScheme = BookingFormColorScheme.WISDOM_BLUE;

        // Create custom Step 1 (Options page with MKMC-specific sections)
        createCustomStep();

        // Build the form - all generic logic is handled by StandardBookingForm
        this.form = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(colorScheme)
            .addCustomStep(optionsPage)  // Step 1: Custom Options page
            .withCallbacks(this)         // For form-specific summary updates
            .build();                    // Steps 2-7: Uses default sections automatically

        // Set up communication between custom sections
        setupCustomStepCommunication();
    }

    /**
     * Creates the custom Step 1 (Options page) with MKMC-specific sections.
     */
    private void createCustomStep() {
        // Event Header - using default implementation
        DefaultEventHeaderSection eventHeaderSection = new DefaultEventHeaderSection();
        eventHeaderSection.setColorScheme(colorScheme);

        // Prerequisite confirmation (MKMC-specific)
        PrerequisiteSection prerequisiteSection = new PrerequisiteSection();
        prerequisiteSection.setColorScheme(colorScheme);

        // Combined Programme + Rate Type section (MKMC-specific)
        rateTypeSection = new RateTypeSection();
        rateTypeSection.setColorScheme(colorScheme);

        // Audio Recording option - using default implementation
        audioRecordingSection = new DefaultAudioRecordingSection();
        audioRecordingSection.setColorScheme(colorScheme);

        // Combine into Options page
        optionsPage = new CompositeBookingFormPage(
                MKMCI18nKeys.Options,
                new ThemedBookingFormSection(eventHeaderSection, colorScheme),
                new ThemedBookingFormSection(prerequisiteSection, colorScheme),
                new ThemedBookingFormSection(rateTypeSection, colorScheme),
                new ThemedBookingFormSection(audioRecordingSection, colorScheme))
                .setStep(true)
                .setHeaderVisible(true);

        // Set up navigation button for custom step
        optionsPage.setButtons(
            BookingFormButton.async(MKMCI18nKeys.Continue,
                button -> navigateFromOptionsAsync(),
                "btn-primary booking-form-btn-primary",
                Bindings.not(optionsPage.validProperty())));
    }

    /**
     * Sets up communication between custom step sections.
     */
    private void setupCustomStepCommunication() {
        // When package is selected, update audio recording section
        rateTypeSection.setOnPackageSelected(period -> audioRecordingSection.setSelectedProgramme(period));
    }

    /**
     * Handles navigation from the Options step.
     * Uses StandardBookingForm's built-in method to continue to the next step.
     */
    private Future<?> navigateFromOptionsAsync() {
        form.continueFromCustomSteps();
        return Future.succeededFuture();
    }

    // === StandardBookingFormCallbacks Implementation ===
    // Note: updateSummary and onPrepareNewBooking use default implementations
    // - Default updateSummary reads price lines from WorkingBooking document lines
    // - Default resetCustomPageSections resets sections implementing ResettableSection

    // === Public Accessor ===

    /**
     * Returns the built form for use in the UI.
     */
    public StandardBookingForm getForm() {
        return form;
    }
}
