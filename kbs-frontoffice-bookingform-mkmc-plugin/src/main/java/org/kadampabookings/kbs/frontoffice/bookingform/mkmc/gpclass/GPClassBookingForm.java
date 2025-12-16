package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import javafx.beans.binding.Bindings;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.ecommerce.document.service.DocumentAggregate;
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
import one.modality.booking.frontoffice.bookingpage.sections.HasMemberSelectionSection.MemberInfo;
import one.modality.base.shared.entities.Person;
import one.modality.booking.client.workingbooking.FXPersonToBook;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections.ClassDateSelectionSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections.ExistingBookingSection;
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
    private ExistingBookingSection existingBookingSection;
    private GPClassSummarySection summarySection;

    // Custom pages
    private CompositeBookingFormPage selectClassesPage;
    private CompositeBookingFormPage existingBookingPage;

    // State
    private final EventBookingFormSettings settings;
    private final boolean hasExistingBooking;
    private boolean modifyingOwnBooking = true; // Default: modifying own booking

    /**
     * Creates the GP Class booking form using the builder pattern.
     *
     * @param activity The activity providing WorkingBookingProperties
     * @param settings The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public GPClassBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.settings = settings;

        // Note: hasExistingBooking is determined later when WorkingBooking is loaded
        // At construction time, WorkingBooking may not be available yet
        // The ExistingBookingSection uses isApplicableToBooking() to auto-skip when not needed
        this.hasExistingBooking = false; // Will be updated dynamically via isApplicableToBooking()

        // Create custom steps
        createCustomStep();
        createExistingBookingStep();

        // Create custom summary section that shows GP-class-specific pricing
        summarySection = new GPClassSummarySection(dateSelectionSection);

        // Build the form - all generic logic is handled by StandardBookingForm
        // The color scheme is applied as a CSS theme class to the root container
        // Note: Full payment only is the default behavior for GP classes
        StandardBookingFormBuilder builder = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.PEACE_PURPLE)  // Applied as CSS theme class
            .withShowUserBadge(true)                     // Show user badge in header
            .withCardPaymentOnly(true)                   // GP classes only accept card payment
            .withEntryPoint(entryPoint).withNavigationClickable(false);                 // Handle payment resume/modification entry points

        // Add existing booking check page first (will auto-skip if not applicable via isApplicableToBooking)
        builder.addCustomStep(existingBookingPage);      // Step 0: Existing booking check (auto-skips for new bookings)
        builder.addCustomStep(selectClassesPage);        // Step 1: Custom date selection page
        builder.withSummaryPageSupplier(() -> createSummaryPage(summarySection));  // Custom summary page
        // Note: Member selection is NOT skipped - for new bookings it shows, for existing bookings
        // the Your Information and Member Selection pages will be skipped automatically because:
        // 1. For existing bookings, user is already logged in (Your Information auto-skips)
        // 2. For existing bookings, member is selected in ExistingBookingSection
        builder.withCallbacks(this);                     // For form-specific callbacks

        this.form = builder.build();                     // Build the form
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
            dateSelectionSection.setRateType(rateType);
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
     * Creates the Existing Booking Check step (Step 0) with options to modify or book for another person.
     * This is only called when there's an existing booking to modify.
     */
    private void createExistingBookingStep() {
        existingBookingSection = new ExistingBookingSection();

        // Handle selection type changes
        existingBookingSection.setOnSelectionTypeChanged(selectionType -> {
            modifyingOwnBooking = (selectionType == ExistingBookingSection.SelectionType.MODIFY_EXISTING_BOOKING);
            // If creating a new booking for another person, we need to re-enable member selection
            // This is handled by the form's skip logic
        });

        // Handle DocumentAggregate selection (for existing booking modification)
        // This is called when user clicks Continue after selecting an existing booking
        existingBookingSection.setOnDocumentAggregateSelected(docAggregate -> {
            // Recreate WorkingBooking with the selected person's existing booking data
            // This avoids triggering FXPersonToBook which would cause a form reload
            Console.log("Recreating WorkingBooking with selected booking's DocumentAggregate");
            WorkingBooking newWorkingBooking = new WorkingBooking(
                form.getWorkingBookingProperties().getPolicyAggregate(),
                docAggregate,
                null  // no payOrderDocumentId
            );
            // Mark that member was explicitly selected - this skips the member selection page
            // Set on WorkingBooking BEFORE setWorkingBooking() so it's available during page navigation
            newWorkingBooking.setMemberExplicitlySelected(true);
            form.getWorkingBookingProperties().setWorkingBooking(newWorkingBooking);

            // Hide rate section - rate cannot be changed when modifying existing booking
            rateSection.getView().setVisible(false);
            rateSection.getView().setManaged(false);
        });

        // Handle member selection (for new booking - create fresh WorkingBooking)
        // Uses MemberInfo which has name/email captured when the data was definitely available
        existingBookingSection.setOnMemberSelected(memberInfo -> {
            if (memberInfo != null) {
                // Use the name/email stored in MemberInfo (captured when data was available)
                String fullName = memberInfo.getName();
                String email = memberInfo.getEmail();
                Person person = memberInfo.getPersonEntity();

                // Parse fullName into firstName/lastName
                String firstName = "";
                String lastName = "";
                if (fullName != null && !fullName.isEmpty()) {
                    String[] parts = fullName.trim().split("\\s+", 2);
                    firstName = parts[0];
                    lastName = parts.length > 1 ? parts[1] : "";
                }

                Console.log("Creating fresh WorkingBooking for new booking - MemberInfo details:");
                Console.log("  fullName: " + fullName);
                Console.log("  firstName (parsed): " + firstName);
                Console.log("  lastName (parsed): " + lastName);
                Console.log("  email: " + email);

                WorkingBooking newWorkingBooking = new WorkingBooking(
                    form.getWorkingBookingProperties().getPolicyAggregate(),  // Keep same PolicyAggregate
                    null,   // null = brand new booking (not existing)
                    null    // no payOrderDocumentId
                );
                // Mark that member was explicitly selected - this skips the member selection page
                // Set on WorkingBooking BEFORE setWorkingBooking() so it's available during page navigation
                newWorkingBooking.setMemberExplicitlySelected(true);

                // Set the person on the fresh document
                newWorkingBooking.getDocument().setPerson(person);

                // Copy personal details to document (required for summary display)
                newWorkingBooking.getDocument().setFirstName(firstName);
                newWorkingBooking.getDocument().setLastName(lastName);
                newWorkingBooking.getDocument().setEmail(email);

                // Verify the values were set correctly
                Console.log("  Document firstName after set: " + newWorkingBooking.getDocument().getFirstName());
                Console.log("  Document lastName after set: " + newWorkingBooking.getDocument().getLastName());

                // Update the form with the new WorkingBooking FIRST
                // This must happen BEFORE FXPersonToBook.setPersonToBook() to prevent race condition
                // where BookEventActivity.onPersonToBookChanged() creates a new WorkingBooking that
                // overwrites our flag-set one
                form.getWorkingBookingProperties().setWorkingBooking(newWorkingBooking);

                // CRITICAL: Set FXPersonToBook so submission uses correct person
                // WorkingBooking.submitChanges() reads from FXPersonToBook.getPersonToBook()
                // to determine the person primary key for the AddDocumentEvent
                // NOTE: This MUST be called AFTER setWorkingBooking() - if called before,
                // BookEventActivity.onPersonToBookChanged() triggers loadBookingWithSamePolicy()
                // which creates a new WorkingBooking without our memberExplicitlySelected flag
                FXPersonToBook.setPersonToBook(person);
                Console.log("  Set FXPersonToBook to: " + fullName);

                // IMPORTANT: Also set the attendee name directly on the summary section
                // This bypasses Document field issues - the name will be reliably displayed
                summarySection.setAttendeeName(fullName);
                summarySection.setAttendeeEmail(email);
                Console.log("  Set attendee name directly on summary section: " + fullName);
            }

            // Show rate section - user needs to select rate for new booking
            rateSection.getView().setVisible(true);
            rateSection.getView().setManaged(true);
        });

        // Handle continue button
        existingBookingSection.setOnContinuePressed(() -> {
            // Navigate to the next step (Select Classes page)
            form.navigateToNextPage();
        });

        // Combine into Existing Booking page
        existingBookingPage = new CompositeBookingFormPage(
                MKMCI18nKeys.GPExistingBookingTitle,
                existingBookingSection)
                .setStep(true)
                .setHeaderVisible(false)  // Don't show step header for this page
                .setButtons();  // Empty - section has its own Continue button
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

    /**
     * Returns whether the user has an existing booking for this event.
     */
    public boolean hasExistingBooking() {
        return hasExistingBooking;
    }

    /**
     * Returns whether the user is modifying their own booking (vs booking for another person).
     * Only relevant when hasExistingBooking() is true.
     */
    public boolean isModifyingOwnBooking() {
        return modifyingOwnBooking;
    }

    /**
     * Returns the existing booking section (only available when hasExistingBooking() is true).
     */
    public ExistingBookingSection getExistingBookingSection() {
        return existingBookingSection;
    }
}
