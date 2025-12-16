package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment;

import dev.webfx.platform.async.Future;
import javafx.beans.binding.Bindings;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingForm;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormBuilder;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormCallbacks;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
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
 * <p><b>Theming:</b> This form uses CSS-based theming. The color scheme is passed to the
 * builder, which applies a theme class (e.g., "theme-wisdom-blue") to the root container.
 * All sections inherit their styling from CSS variables defined by these theme classes.
 * No need to pass colorScheme to individual sections.</p>
 *
 * @author Bruno Salmon
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class MKMCOnlineEmpowermentBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    private RateTypeSection rateTypeSection;
    private DefaultAudioRecordingSection audioRecordingSection;
    private CompositeBookingFormPage optionsPage;

    /**
     * Creates the MKMC Online Empowerment booking form using the builder pattern.
     *
     * @param activity The activity providing WorkingBookingProperties
     * @param settings The event booking form settings
     * @param entryPoint The entry point (NEW_BOOKING, RESUME_PAYMENT, etc.)
     */
    public MKMCOnlineEmpowermentBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        // Create custom Step 1 (Options page with MKMC-specific sections)
        createCustomStep();

        // Build the form - all generic logic is handled by StandardBookingForm
        // The color scheme is applied as a CSS theme class to the root container
        this.form = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.JOY_AMBER)  // Applied as CSS theme class
            .withEntryPoint(entryPoint)  // Handle payment return redirects
            .addCustomStep(optionsPage)  // Step 1: Custom Options page
            .withCallbacks(this)         // For form-specific summary updates
            .build();                    // Steps 2-7: Uses default sections automatically

        // Set up communication between custom sections
        setupCustomStepCommunication();
    }

    /**
     * Creates the custom Step 1 (Options page) with MKMC-specific sections.
     * Sections use CSS classes for styling - no need to pass colorScheme.
     */
    private void createCustomStep() {
        // Event Header - styling handled by CSS class .booking-form-event-header
        DefaultEventHeaderSection eventHeaderSection = new DefaultEventHeaderSection();

        // Prerequisite confirmation (MKMC-specific) - styling via CSS
        PrerequisiteSection prerequisiteSection = new PrerequisiteSection();

        // Combined Programme + Rate Type section (MKMC-specific) - styling via CSS
        rateTypeSection = new RateTypeSection();

        // Audio Recording option - styling handled by CSS class .booking-form-audio-recording-card
        audioRecordingSection = new DefaultAudioRecordingSection();

        // Combine into Options page - sections inherit theme from CSS variables
        optionsPage = new CompositeBookingFormPage(
                MKMCI18nKeys.Options,
                eventHeaderSection,
                prerequisiteSection,
                rateTypeSection,
                audioRecordingSection)
                .setStep(true)
                .setHeaderVisible(true);

        // Set up navigation button for custom step
        optionsPage.setButtons(
            BookingFormButton.async(BookingPageI18nKeys.Continue,
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
