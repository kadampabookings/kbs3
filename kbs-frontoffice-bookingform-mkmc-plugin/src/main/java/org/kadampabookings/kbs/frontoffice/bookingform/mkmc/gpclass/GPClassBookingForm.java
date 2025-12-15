package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass;

import dev.webfx.platform.async.Future;
import javafx.beans.binding.Bindings;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.BookingFormPage;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingForm;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormBuilder;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormCallbacks;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultEventHeaderSection;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections.ClassDateSelectionSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections.GPClassRateSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections.GPClassSummarySection;

/**
 * GP Class Booking Form - Implementation for weekly general programme class bookings.
 *
 * <p>This form allows users to:</p>
 * <ul>
 *   <li>Select individual class dates from a series</li>
 *   <li>See discount when selecting all classes (full term)</li>
 *   <li>Book for themselves or guests</li>
 *   <li>Complete payment (full payment only, no partial)</li>
 * </ul>
 *
 * <p>Uses the StandardBookingFormBuilder pattern. Only Step 1 (class date selection)
 * is custom - all other steps use default sections automatically.</p>
 *
 * <p><b>Theming:</b> Uses CSS-based theming. The color scheme is read from event settings
 * (or defaults to WISDOM_BLUE) and applied as a CSS theme class to the root container.</p>
 *
 * @author Claude
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class GPClassBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    // Custom sections
    private DefaultEventHeaderSection eventHeaderSection;
    private GPClassRateSection rateSection;
    private ClassDateSelectionSection dateSelectionSection;

    // Custom page
    private CompositeBookingFormPage selectClassesPage;

    // Settings reference for color scheme
    private final EventBookingFormSettings settings;

    /**
     * Creates the GP Class booking form using the builder pattern.
     *
     * @param activity The activity providing WorkingBookingProperties
     * @param settings The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public GPClassBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.settings = settings;

        // Create custom Step 1 (Select Classes page)
        createCustomStep();

        // Create custom summary section that shows GP-class-specific pricing
        GPClassSummarySection summarySection = new GPClassSummarySection(dateSelectionSection);

        // Build the form - all generic logic is handled by StandardBookingForm
        // The color scheme is applied as a CSS theme class to the root container
        // Note: Full payment only is the default behavior for GP classes
        this.form = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.PEACE_PURPLE)  // Applied as CSS theme class
            .withShowUserBadge(true)                     // Show user badge in header
            .withCardPaymentOnly(true)                   // GP classes only accept card payment
            .withEntryPoint(entryPoint)                  // Handle payment resume/modification entry points
            .addCustomStep(selectClassesPage)            // Step 1: Custom date selection page
            .withSummaryPageSupplier(() -> createSummaryPage(summarySection))  // Custom summary page
            .withCallbacks(this)                         // For form-specific callbacks
            .build();                                    // Steps 2-7: Uses default sections automatically
    }

    /**
     * Creates a custom summary page using the GP class summary section.
     */
    private BookingFormPage createSummaryPage(GPClassSummarySection summarySection) {
        return new CompositeBookingFormPage(BookingPageI18nKeys.Summary, summarySection)
            .setStep(true);
    }

    /**
     * Creates the custom Step 1 (Select Classes page) with GP-specific sections.
     * Sections use CSS classes for styling - no need to pass colorScheme.
     */
    private void createCustomStep() {
        // Event Header - shows event name, dates, location
        eventHeaderSection = new DefaultEventHeaderSection();

        // Rate Selection - choose between Standard and MKMC Member rate
        rateSection = new GPClassRateSection();

        // Date Selection - grid of selectable date cards
        dateSelectionSection = new ClassDateSelectionSection();

        // Link rate selection to date selection for price updates
        rateSection.setOnRateTypeChanged(rateType -> {
            // Update date selection section with new price per class
            dateSelectionSection.setPricePerClass(rateSection.getSelectedPricePerClass());
        });

        // Combine into Select Classes page
        selectClassesPage = new CompositeBookingFormPage(
                MKMCI18nKeys.GPSelectClasses,
                eventHeaderSection,
                rateSection,
                dateSelectionSection)
                .setStep(true)
                .setHeaderVisible(true);

        // Set up navigation button for custom step
        selectClassesPage.setButtons(
            BookingFormButton.async(BookingPageI18nKeys.Continue,
                button -> navigateFromSelectClassesAsync(),
                "btn-primary booking-form-btn-primary",
                Bindings.not(selectClassesPage.validProperty())));
    }

    /**
     * Gets the color scheme from event settings.
     * Defaults to WISDOM_BLUE if not configured.
     */
    private BookingFormColorScheme getColorSchemeFromEvent() {
        // TODO: Read color scheme from event settings
        // For now, default to WISDOM_BLUE for study programmes
        if (settings != null && settings.event() != null) {
            // Could read from event.getColorScheme() or similar field
            // For now, return default
        }
        return BookingFormColorScheme.WISDOM_BLUE;
    }

    /**
     * Handles navigation from the Select Classes step.
     * Uses StandardBookingForm's built-in method to continue to the next step.
     * Returns the Future from continueFromCustomSteps() so the spinner waits for async loading.
     */
    private Future<?> navigateFromSelectClassesAsync() {
        return form.continueFromCustomSteps();
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

    /**
     * Returns the date selection section for external access if needed.
     */
    public ClassDateSelectionSection getDateSelectionSection() {
        return dateSelectionSection;
    }
}
