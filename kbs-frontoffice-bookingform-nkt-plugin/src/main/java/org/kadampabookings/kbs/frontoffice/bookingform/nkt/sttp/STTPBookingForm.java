package org.kadampabookings.kbs.frontoffice.bookingform.nkt.sttp;

import dev.webfx.platform.console.Console;
import one.modality.base.shared.entities.Person;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingpage.sections.summary.DefaultEventHeaderSection;
import one.modality.booking.frontoffice.bookingpage.sections.member.DefaultMemberSelectionSection;
import one.modality.booking.frontoffice.bookingpage.sections.user.DefaultYourInformationSection;
import one.modality.booking.frontoffice.bookingpage.standard.HouseholdMemberLoader;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingForm;
import one.modality.crm.shared.services.authn.fx.FXUserPerson;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormBuilder;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormCallbacks;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

/**
 * STTP Booking Form - Implementation for Systematic Training and Practice Program bookings.
 *
 * <p>This is a simplified booking form compared to GP classes:</p>
 * <ul>
 *   <li>No date selection - users register for all sessions automatically</li>
 *   <li>No rate selection - single fixed rate</li>
 *   <li>No existing booking section - payment completion uses PAY_BOOKING entry point</li>
 *   <li>Uses DefaultSummarySection (no custom summary needed)</li>
 * </ul>
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Your Information (login/registration) - skipped if logged in</li>
 *   <li>Member Selection with Event Header (who is booking for)</li>
 *   <li>Summary (review booking)</li>
 *   <li>Payment</li>
 *   <li>Confirmation</li>
 * </ol>
 *
 * <p>Household member booking is handled in the Member Selection section.
 * Members already booked for the event are shown as "Already Booked" and cannot be selected again.</p>
 *
 * @author Claude
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class STTPBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    // Custom sections
    private DefaultEventHeaderSection yourInfoEventHeaderSection;
    private DefaultEventHeaderSection memberSelectionEventHeaderSection;
    private DefaultYourInformationSection yourInformationSection;
    private DefaultMemberSelectionSection memberSelectionSection;

    // State
    private final EventBookingFormSettings settings;

    /**
     * Creates the STTP booking form using the builder pattern.
     *
     * @param activity The activity providing WorkingBookingProperties
     * @param settings The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public STTPBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.settings = settings;

        // Build the form - all generic logic is handled by StandardBookingForm
        // STTP is simpler than GP: no date selection, no rate selection, no existing booking section
        StandardBookingFormBuilder builder = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.PEACE_PURPLE)  // Applied as CSS theme class
            .withShowUserBadge(true)                     // Show user badge in header
            .withCardPaymentOnly(true)                   // STTP only accepts card payment
            .withEntryPoint(entryPoint)                  // Handle payment resume/modification entry points
            .withNavigationClickable(false)              // Can be set to true in debug mode only
            // NO existing booking section - payment completion uses PAY_BOOKING entry point
            // NO date selection step - STTP users register for all sessions automatically
            // NO rate selection step - STTP uses a single fixed rate
            // Custom Your Information page with event header at the top
            .withYourInformationPageSupplier(this::createYourInformationPageWithHeader)
            // Custom member selection page with event header at the top
            .withMemberSelectionPageSupplier(this::createMemberSelectionPageWithHeader)
            // NO custom summary - DefaultSummarySection works for STTP
            .withCallbacks(this);                        // For form-specific callbacks

        this.form = builder.build();                     // Build the form

        // Wire up Your Information callbacks (since we provided a custom page)
        setupYourInformationCallbacks();

        // Wire up member selection callbacks (since we provided a custom page)
        setupMemberSelectionCallbacks();

        // Load members immediately if user is already logged in
        // (onAfterLogin won't be called if user was already authenticated)
        loadMembersIfLoggedIn();

        // Listen for logout events and navigate to Your Information page
        setupLogoutListener();
    }

    /**
     * Sets up a listener for login/logout events.
     * - On logout: Navigate back to Your Information page
     * - On login/logout: Trigger header update to rebuild step navigation
     */
    private void setupLogoutListener() {
        FXUserPerson.userPersonProperty().addListener((obs, oldPerson, newPerson) -> {
            if (oldPerson != null && newPerson == null) {
                // User just logged out - navigate to Your Information page
                Console.log("STTPBookingForm: User logged out, navigating to Your Information");
                form.navigateToYourInformation();
            }
            // Note: We don't call form.updateHeader() here to avoid forcing a redraw
            // Your Information is not a numbered step, so step navigation stays stable
        });
    }

    /**
     * Loads household members if the user is already logged in.
     * Called at construction time because onAfterLogin() won't be triggered
     * if the user was already authenticated before opening the form.
     */
    private void loadMembersIfLoggedIn() {
        Person person = FXUserPerson.getUserPerson();
        Console.log("STTPBookingForm.loadMembersIfLoggedIn() - person: " + (person != null ? person.getFullName() : "null"));
        if (person != null && memberSelectionSection != null) {
            Console.log("  Loading household members at construction time...");
            HouseholdMemberLoader.loadMembersAsync(person, memberSelectionSection, settings.event());
        }
    }

    /**
     * Sets up callbacks for the custom Your Information section.
     * This is needed because StandardBookingForm only auto-wires callbacks for its default section.
     */
    private void setupYourInformationCallbacks() {
        if (yourInformationSection == null) return;

        // When login succeeds, update state and navigate to member selection
        yourInformationSection.setOnLoginSuccess(person -> {
            // Update form state
            form.getState().setLoggedInPerson(person);
            // Load household members (this also triggers onAfterLogin callback)
            onAfterLogin();
            // Navigate to member selection
            form.navigateToMemberSelection();
        });

        // When new user continues (guest or creating account), go directly to summary
        yourInformationSection.setOnNewUserContinue(newUserData -> {
            // Store pending new user data in state
            form.getState().setPendingNewUserData(newUserData);
            // For new users, skip member selection and go to summary
            form.navigateToSummary();
        });

        // Back button is hidden, but we still need a handler in case it becomes visible
        yourInformationSection.setOnBackPressed(() -> {
            // No previous step - Your Information is the first step for new users
        });
    }

    /**
     * Sets up callbacks for the custom member selection section.
     * This is needed because StandardBookingForm only auto-wires callbacks for its default section.
     */
    private void setupMemberSelectionCallbacks() {
        if (memberSelectionSection == null) return;

        // When a member is selected, update the form state
        memberSelectionSection.setOnMemberSelected(member -> {
            form.getState().setSelectedMember(member);
        });

        // When Continue is pressed, navigate to summary (which also updates attendee info)
        memberSelectionSection.setOnContinuePressed(() -> {
            form.navigateToSummary();
        });

        // When Back is pressed, navigate to Your Information page
        memberSelectionSection.setOnBackPressed(() -> {
            form.navigateToYourInformation();
        });
    }

    /**
     * Creates a custom Your Information page that includes the event header at the top.
     * This combines DefaultEventHeaderSection and DefaultYourInformationSection into one page.
     *
     * <p>Page visibility: Only shown when user is NOT logged in.</p>
     */
    private CompositeBookingFormPage createYourInformationPageWithHeader() {
        // Event Header - shows event name, dates, location, description, cover image
        yourInfoEventHeaderSection = new DefaultEventHeaderSection();

        // Your Information - login/registration form
        yourInformationSection = new DefaultYourInformationSection();
        yourInformationSection.setColorScheme(BookingFormColorScheme.PEACE_PURPLE);
        // Force account creation - STTP is an online program requiring login for course materials
        yourInformationSection.setForceAccountCreation(true);
        // Hide back button - when not logged in, Your Information is the first step
        yourInformationSection.setBackButtonVisible(false);

        // Create composite page with both sections
        // Override isApplicableToBooking at PAGE level to control when page is shown/skipped
        // Page is only shown when user is NOT logged in
        return new CompositeBookingFormPage(BookingPageI18nKeys.YourInformation,
                yourInfoEventHeaderSection,
                yourInformationSection) {
            @Override
            public boolean isApplicableToBooking(WorkingBooking workingBooking) {
                // Skip this page if user is already logged in
                return FXUserPerson.getUserPerson() == null;
            }
        }
                .setStep(true)  // Your Information is Step 1 when not logged in
                .setShowingOwnSubmitButton(true);  // Your Information section has its own buttons
    }

    /**
     * Creates a custom Member Selection page that includes the event header at the top.
     * This combines DefaultEventHeaderSection and DefaultMemberSelectionSection into one page.
     *
     * <p>Page visibility: Always returns true so it appears in the step navigation.
     * When not logged in, the form starts at Your Information (which comes first in page order).
     * When logged in, Your Information is skipped and Member Selection is shown.</p>
     */
    private CompositeBookingFormPage createMemberSelectionPageWithHeader() {
        // Event Header - shows event name, dates, location, description, cover image
        memberSelectionEventHeaderSection = new DefaultEventHeaderSection();

        // Member Selection - displays household members for selection
        memberSelectionSection = new DefaultMemberSelectionSection();
        memberSelectionSection.setColorScheme(BookingFormColorScheme.PEACE_PURPLE);
        // Hide back button - when logged in, Member Selection is the first step
        memberSelectionSection.setBackButtonVisible(false);

        // Create composite page with both sections
        // isApplicableToBooking returns true ALWAYS so Member Selection appears in step navigation
        // regardless of login state. Page skipping is handled by page order:
        // - Not logged in: Your Information (comes first) is shown
        // - Logged in: Your Information is skipped, Member Selection is shown
        return new CompositeBookingFormPage(BookingPageI18nKeys.MemberSelection,
                memberSelectionEventHeaderSection,
                memberSelectionSection)
                .setStep(true)
                .setShowingOwnSubmitButton(true);  // Member selection section has its own buttons
    }

    /**
     * Returns the member selection section for callback wiring.
     * StandardBookingForm uses this via getMemberSelectionSection() to set up callbacks.
     */
    public DefaultMemberSelectionSection getMemberSelectionSection() {
        return memberSelectionSection;
    }

    // === StandardBookingFormCallbacks Implementation ===

    /**
     * Called after user logs in. Loads household members into our custom member selection section.
     * This is needed because StandardBookingForm only loads members into its defaultMemberSelectionSection,
     * which is null when we provide a custom page via withMemberSelectionPageSupplier.
     */
    @Override
    public void onAfterLogin() {
        Person person = FXUserPerson.getUserPerson();
         if (person != null && memberSelectionSection != null) {
            HouseholdMemberLoader.loadMembersAsync(person, memberSelectionSection, settings.event());
        } else {
            Console.log("  SKIPPED loading - person or section is null");
        }
    }

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
