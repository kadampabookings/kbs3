package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.windowhistory.WindowHistory;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import one.modality.base.shared.entities.*;
import one.modality.base.shared.knownitems.KnownItemFamily;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.components.StickyPriceHeader;
import one.modality.booking.frontoffice.bookingpage.components.ValidationWarningZone;
import one.modality.booking.frontoffice.bookingpage.sections.*;
import one.modality.booking.frontoffice.bookingpage.standard.HouseholdMemberLoader;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingForm;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormBuilder;
import one.modality.booking.frontoffice.bookingpage.standard.StandardBookingFormCallbacks;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import one.modality.crm.shared.services.authn.fx.FXUserPerson;
import one.modality.ecommerce.policy.service.PolicyAggregate;
import one.modality.ecommerce.shared.pricecalculator.PriceCalculator;
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;
import org.kadampabookings.kbs.frontoffice.bookingform.kmcny.KMCNY18nKeys;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * US Festival In-Person Booking Form - Implementation for in-person US Festival (event type 38) bookings.
 *
 * <p>This form handles in-person registration with multiple custom steps:</p>
 * <ul>
 *   <li>Step 1: Accommodation selection (room options)</li>
 *   <li>Step 2: Booking Details (festival days, meals, additional options)</li>
 *   <li>Standard flow: Your Information → Member Selection → Summary → Payment → Confirmation</li>
 * </ul>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Room/accommodation selection with availability status</li>
 *   <li>Festival day selection with arrival/departure dates</li>
 *   <li>Meal options (lunch/dinner) with dietary preferences</li>
 *   <li>Additional options (parking, shuttle, assisted listening)</li>
 * </ul>
 *
 * <p>Note: This form is created by {@link USFestivalEntryForm} when user selects In-Person registration.</p>
 *
 * @author Bruno Salmon
 * @see USFestivalEntryForm
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class USFestivalInPersonBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    // Custom sections - Step 1: Accommodation
    private CompositeBookingFormPage accommodationPage;
    private DefaultEventHeaderSection step1EventHeaderSection;
    private USFestivalAccommodationSelectionSection accommodationSection;

    // Custom sections - Step 2: Booking Details
    private CompositeBookingFormPage bookingDetailsPage;
    private DefaultEventHeaderSection step2EventHeaderSection;
    private DefaultFestivalDaySelectionSection festivalDaySection;
    private DefaultMealsSelectionSection mealsSection;
    private DefaultAudioRecordingPhaseCoverageSection audioRecordingPhaseSection;
    private DefaultTransportSection transportSection;  // Combined parking and shuttle section
    private DefaultAdditionalOptionsSection additionalOptionsSection;
    private DefaultRoommateInfoSection roommateInfoSection;

    // Standard sections - Your Information & Member Selection
    private DefaultEventHeaderSection yourInfoEventHeaderSection;
    private DefaultEventHeaderSection memberSelectionEventHeaderSection;
    private DefaultYourInformationSection yourInformationSection;
    private DefaultMemberSelectionSection memberSelectionSection;

    // Sticky Price Header (fixed at top when room is selected)
    private final StickyPriceHeader stickyPriceHeader;

    // State
    private final EventBookingFormSettings settings;
    private final WorkingBookingProperties workingBookingProperties;
    private final BookingFormEntryPoint entryPoint;
    private boolean accommodationOptionsPopulated = false;

    // Event boundary dates from EventSelection and EventPart (loaded from PolicyAggregate)
    // These replace the old Event.getStartDate()/getEndDate() approach
    private LocalDate eventStartDate;      // Start date of main event (from first EventSelection part)
    private LocalDate eventEndDate;        // End date of main event (from last EventSelection part)
    private LocalDate earlyArrivalDate;    // Start date of early arrival period (from EarlyArrivalPart)
    private LocalDate lateDepartureDate;   // End date of late departure period (from LateDeparturePart)

    /**
     * Creates the US Festival in-person booking form using the builder pattern.
     *
     * @param activity   The activity providing WorkingBookingProperties
     * @param settings   The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public USFestivalInPersonBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.settings = settings;
        this.workingBookingProperties = activity.getWorkingBookingProperties();
        this.entryPoint = entryPoint;

        // Create the sticky price header
        this.stickyPriceHeader = new StickyPriceHeader();
        this.stickyPriceHeader.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        // Build the form with custom steps based on entry point
        StandardBookingFormBuilder builder = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.WISDOM_BLUE)  // Blue theme for US Festival
            .withShowUserBadge(false)                             // Hide user badge in header
            .withCardPaymentOnly(true)                            // Only allow credit/debit card payments
            .withEntryPoint(entryPoint)                           // Handle payment resume/modification
            .withNavigationClickable(false)                       // Navigation only via buttons
            .withStickyHeader(stickyPriceHeader);                 // Sticky price header at top

        // Handle different entry points
        if (entryPoint == BookingFormEntryPoint.NEW_BOOKING) {
            // New booking: show accommodation and booking details steps
            builder
                .addCustomStep(createAccommodationPage())         // Step 1: Your Room
                .addCustomStep(createBookingDetailsPage());       // Step 2: Festival Days, Meals, Options
        } else if (entryPoint == BookingFormEntryPoint.MODIFY_BOOKING) {
            // Modify booking: show informational message that modification is not supported
            builder.addCustomStep(createModifyNotSupportedPage());
        }
        // PAY_BOOKING: no custom steps - StandardBookingForm navigates directly to payment

        builder
            // Custom Your Information page with event header
            .withYourInformationPageSupplier(this::createYourInformationPageWithHeader)
            // Custom Member Selection page with event header
            .withMemberSelectionPageSupplier(this::createMemberSelectionPageWithHeader)
            .withCallbacks(this);

        this.form = builder.build();

        // Wire up section callbacks only for new bookings
        // For PAY_BOOKING/MODIFY_BOOKING, the booking data should not be modified
        if (entryPoint == BookingFormEntryPoint.NEW_BOOKING) {
            setupAccommodationCallbacks();
            setupAccommodationButtons();
            setupBookingDetailsCallbacks();
            setupBookingDetailsButtons();
        }
        setupYourInformationCallbacks();
        setupMemberSelectionCallbacks();

        // Load members immediately if user is already logged in
        loadMembersIfLoggedIn();

        // Listen for logout events
        setupLogoutListener();

        // Set up listener for when WorkingBooking becomes available
        // The listener will populate options when totalProperty fires (after WorkingBooking is set)
        setupWorkingBookingListener();
    }

    /**
     * Sets up a listener to populate accommodation options when the WorkingBooking becomes available.
     * Also checks if the WorkingBooking is already available (e.g., when swapped from entry form).
     *
     * <p>Note: For PAY_BOOKING and MODIFY_BOOKING entry points, we skip the population logic
     * since those entry points work with existing booking data that should not be overwritten.</p>
     */
    private void setupWorkingBookingListener() {
        if (workingBookingProperties == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBookingProperties not available, cannot set up listener");
            return;
        }

        // Skip population logic for non-NEW_BOOKING entry points
        // Existing bookings should not have their data overwritten
        if (entryPoint != BookingFormEntryPoint.NEW_BOOKING) {
            Console.log("USFestivalInPersonBookingForm: Skipping WorkingBooking listener for " + entryPoint);
            // Still bind the sticky price header for PAY_BOOKING so user sees correct total
            if (stickyPriceHeader != null) {
                stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
            }
            return;
        }

        // Listen to total property changes as an indicator that WorkingBooking has been initialized
        workingBookingProperties.totalProperty().addListener((obs, oldValue, newValue) -> {
            Console.log("USFestivalInPersonBookingForm: Total changed, attempting to populate options");
            populateAllOptions();
        });

        // Bind stickyPriceHeader's totalPrice to WorkingBookingProperties.totalProperty()
        // This ensures the header shows the actual booking total from WorkingBooking
        if (stickyPriceHeader != null) {
            stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
            Console.log("USFestivalInPersonBookingForm: Bound stickyPriceHeader.totalPrice to workingBookingProperties.totalProperty");
        }

        // Check if WorkingBooking is already available (e.g., when swapped from entry form)
        // In this case, the listener won't fire because the total was already set
        if (workingBookingProperties.getWorkingBooking() != null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking already available, populating options immediately");
            populateAllOptions();
        }
    }

    /**
     * Populates all booking form options (accommodation, meals, days, etc.).
     * Called when WorkingBooking becomes available or when form is swapped to.
     */
    private void populateAllOptions() {
        // First populate event boundary dates from EventSelection/EventPart (needed by other methods)
        populateEventBoundaries();
        populateAccommodationOptions();
        populateFestivalDays();
        populateMealsOptions();
        populateAudioRecordingPhaseOptions();
        populateAdditionalOptions();
        // Set terms URL from event
        setupTermsUrl();
    }

    /**
     * Populates event boundary dates from PolicyAggregate's Event and EventPart.
     *
     * <p>Uses:</p>
     * <ul>
     *   <li>PolicyAggregate.getEvent() - main event start/end dates</li>
     *   <li>PolicyAggregate.getEarlyArrivalPart() - early arrival period (before main event)</li>
     *   <li>PolicyAggregate.getLateDeparturePart() - late departure period (after main event)</li>
     * </ul>
     *
     * <p>Note: EventSelection contains parts that may include early arrival/late departure,
     * so we use the Event entity directly for main event dates.</p>
     */
    private void populateEventBoundaries() {
        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

        // Get main event dates from Event entity (the authoritative source)
        Event event = policyAggregate.getEvent();
        if (event != null) {
            eventStartDate = event.getStartDate();
            eventEndDate = event.getEndDate();
        }

        // Get early arrival date from EarlyArrivalPart (period before main event)
        EventPart earlyArrivalPart = policyAggregate.getEarlyArrivalPart();
        if (earlyArrivalPart != null) {
            earlyArrivalDate = earlyArrivalPart.getStartDate();
            Console.log("USFestivalInPersonBookingForm: Early arrival part found - " +
                earlyArrivalPart.getStartDate() + " to " + earlyArrivalPart.getEndDate());
        }

        // Get late departure date from LateDeparturePart (period after main event)
        EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();
        if (lateDeparturePart != null) {
            lateDepartureDate = lateDeparturePart.getEndDate();
            Console.log("USFestivalInPersonBookingForm: Late departure part found - " +
                lateDeparturePart.getStartDate() + " to " + lateDeparturePart.getEndDate());
        }

        Console.log("USFestivalInPersonBookingForm: Event boundaries populated - " +
            "mainEvent=" + eventStartDate + " to " + eventEndDate +
            ", earlyArrival=" + earlyArrivalDate + ", lateDeparture=" + lateDepartureDate);
    }

    // ========================================
    // Page Creation Methods
    // ========================================

    /**
     * Creates the Accommodation page (Step 1: Your Room).
     * Shows event header and room options with prices.
     */
    private CompositeBookingFormPage createAccommodationPage() {
        step1EventHeaderSection = new DefaultEventHeaderSection();

        accommodationSection = new USFestivalAccommodationSelectionSection();
        accommodationSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        accommodationPage = new CompositeBookingFormPage(BookingPageI18nKeys.YourRoom,
            step1EventHeaderSection,
            accommodationSection)
            .setStep(true);

        return accommodationPage;
    }

    /**
     * Creates the Booking Details page (Step 2).
     * Shows event header, festival day selection, meals, additional options,
     * and roommate info section (visible only when "Share Accommodation" is selected).
     */
    private CompositeBookingFormPage createBookingDetailsPage() {
        step2EventHeaderSection = new DefaultEventHeaderSection();

        festivalDaySection = new DefaultFestivalDaySelectionSection();
        festivalDaySection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        mealsSection = new DefaultMealsSelectionSection();
        mealsSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        // Audio recording phase coverage section - shows different phase options (Full Festival, Weekend 1, etc.)
        // Only visible when event has multiple phase coverages configured
        audioRecordingPhaseSection = new DefaultAudioRecordingPhaseCoverageSection();
        audioRecordingPhaseSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
        audioRecordingPhaseSection.setVisible(false);  // Hidden until populated

        // Transport section - combines parking and shuttle options in one section
        // Only visible when transport or parking options exist, shuttles enabled when dates match
        transportSection = new DefaultTransportSection();
        transportSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
        transportSection.setVisible(false);  // Hidden until transport options populated

        additionalOptionsSection = new DefaultAdditionalOptionsSection();
        additionalOptionsSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        // Roommate info section - initially hidden, shown only for "Share Accommodation"
        roommateInfoSection = new DefaultRoommateInfoSection();
        roommateInfoSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
        roommateInfoSection.setVisible(false);  // Hidden by default

        bookingDetailsPage = new CompositeBookingFormPage(BookingPageI18nKeys.BookingDetails,
            step2EventHeaderSection,
            festivalDaySection,
            mealsSection,
            audioRecordingPhaseSection,
            transportSection,           // Transport section (parking + shuttle) before additional options
            additionalOptionsSection,
            roommateInfoSection)
            .setStep(true);

        return bookingDetailsPage;
    }

    /**
     * Creates a custom Your Information page with event header at the top.
     * Page is only shown when user is NOT logged in.
     */
    private CompositeBookingFormPage createYourInformationPageWithHeader() {
        yourInfoEventHeaderSection = new DefaultEventHeaderSection();

        yourInformationSection = new DefaultYourInformationSection();
        yourInformationSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
        yourInformationSection.setForceAccountCreation(false);  // Festival allows guest checkout
        yourInformationSection.setBackButtonVisible(true);  // Can go back to booking details

        return new CompositeBookingFormPage(BookingPageI18nKeys.YourInformation,
            yourInfoEventHeaderSection,
            yourInformationSection) {
            @Override
            public boolean isApplicableToBooking(WorkingBooking workingBooking) {
                // Skip this page if user is already logged in
                return FXUserPerson.getUserPerson() == null;
            }
        }
            .setStep(true)
            .setShowingOwnSubmitButton(true);
    }

    /**
     * Creates a custom Member Selection page with event header at the top.
     * Always applicable so it appears in step navigation.
     */
    private CompositeBookingFormPage createMemberSelectionPageWithHeader() {
        memberSelectionEventHeaderSection = new DefaultEventHeaderSection();

        memberSelectionSection = new DefaultMemberSelectionSection();
        memberSelectionSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);
        memberSelectionSection.setBackButtonVisible(true);

        return new CompositeBookingFormPage(BookingPageI18nKeys.MemberSelection,
            memberSelectionEventHeaderSection,
            memberSelectionSection)
            .setStep(true)
            .setShowingOwnSubmitButton(true);
    }

    /**
     * Creates a page informing the user that booking modification is not currently supported.
     * Displayed for MODIFY_BOOKING entry point.
     *
     * <p>This page features a nice design with:</p>
     * <ul>
     *   <li>An info icon in a themed circle</li>
     *   <li>A clear title and subtitle</li>
     *   <li>Instructions to use the "Contact us about this booking" link in Orders tab</li>
     *   <li>A "Go to Orders" button for easy navigation</li>
     *   <li>No navigation buttons (Next/Back)</li>
     * </ul>
     *
     * @return A page with an informational message about modification not being available
     */
    private CompositeBookingFormPage createModifyNotSupportedPage() {
        DefaultEventHeaderSection headerSection = new DefaultEventHeaderSection();

        // Create loading spinner (shown initially until page is ready)
        ProgressIndicator loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(64, 64);
        VBox loadingBox = new VBox(loadingSpinner);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(80));
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);

        // Build centered content with icon, title, and instructions
        VBox contentBox = new VBox(24);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40, 40, 40, 40));
        contentBox.setVisible(false); // Hidden initially until data is ready
        contentBox.setManaged(false); // Don't participate in layout until visible

        // Info icon in themed circle (like confirmation checkmark)
        StackPane iconCircle = BookingPageUIBuilder.createThemedIconCircle(80);
        iconCircle.getStyleClass().add("bookingpage-confirmation-check-circle");
        // Create info icon using circle + i paths
        SVGPath infoCircle = BookingPageUIBuilder.createThemedIcon(BookingPageUIBuilder.ICON_INFO_CIRCLE, 1.2);
        SVGPath infoI = BookingPageUIBuilder.createThemedIcon(BookingPageUIBuilder.ICON_INFO_I, 1.2);
        StackPane infoIcon = new StackPane(infoCircle, infoI);
        iconCircle.getChildren().add(infoIcon);

        // Title
        Label titleLabel = I18nControls.newLabel(USFestivalI18nKeys.ModifyBookingNotSupportedTitle);
        titleLabel.getStyleClass().addAll("bookingpage-text-3xl", "bookingpage-font-bold", "bookingpage-text-primary");
        VBox.setMargin(titleLabel, new Insets(12, 0, 0, 0));

        // Subtitle
        Label subtitleLabel = I18nControls.newLabel(USFestivalI18nKeys.ModifyBookingNotAvailable);
        subtitleLabel.getStyleClass().addAll("bookingpage-text-md", "bookingpage-text-muted");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(500);

        // Instructions info box
        HBox instructionsBox = BookingPageUIBuilder.createInfoBox(
            USFestivalI18nKeys.ModifyBookingContactInstructions,
            BookingPageUIBuilder.InfoBoxType.INFO);
        instructionsBox.setMaxWidth(500);
        VBox.setMargin(instructionsBox, new Insets(8, 0, 0, 0));

        // Go to Orders button (navigates to /orders)
        Button goToOrdersButton = BookingPageUIBuilder.createPrimaryButton(USFestivalI18nKeys.GoToOrders);
        goToOrdersButton.setOnAction(e -> WindowHistory.getProvider().push("/orders"));
        VBox.setMargin(goToOrdersButton, new Insets(8, 0, 0, 0));

        contentBox.getChildren().addAll(iconCircle, titleLabel, subtitleLabel, instructionsBox, goToOrdersButton);

        // Container with both loading spinner and content (stacked)
        StackPane container = new StackPane(loadingBox, contentBox);

        // Track if content has been shown (to avoid multiple triggers)
        final boolean[] contentShown = {false};

        // Create an anonymous BookingFormSection to wrap the content
        BookingFormSection infoSection = new BookingFormSection() {
            @Override
            public Object getTitleI18nKey() {
                return USFestivalI18nKeys.ModifyBookingNotSupportedTitle;
            }

            @Override
            public Node getView() {
                return container;
            }

            @Override
            public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
                // Only show content once and when we have valid working booking data
                if (!contentShown[0] && workingBookingProperties != null) {
                    WorkingBooking wb = workingBookingProperties.getWorkingBooking();
                    // Check if booking data is actually loaded (has event or document)
                    if (wb != null && (wb.getEvent() != null || wb.getDocument() != null)) {
                        contentShown[0] = true;
                        // Use UiScheduler to ensure smooth transition
                        UiScheduler.runInUiThread(() -> {
                            loadingBox.setVisible(false);
                            loadingBox.setManaged(false);
                            contentBox.setVisible(true);
                            contentBox.setManaged(true);
                        });
                    }
                }
            }
        };

        // Create page with NO buttons (empty array) to hide Next/Back navigation
        return new CompositeBookingFormPage(
            USFestivalI18nKeys.ModifyBookingNotSupportedTitle,
            headerSection,
            infoSection
        ).setStep(true)
         .setButtons(); // Empty buttons array - hides Next button
    }

    // ========================================
    // Callback Setup Methods
    // ========================================

    private void setupAccommodationCallbacks() {
        if (accommodationSection == null) return;

        accommodationSection.setOnOptionSelected(option -> {
            Console.log("USFestivalInPersonBookingForm: Accommodation selected - " + option.getName());

            // Reset WorkingBooking to clear previous selections when accommodation type changes
            // This ensures the booking doesn't keep items from a previous accommodation selection
            if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                Console.log("USFestivalInPersonBookingForm: Resetting WorkingBooking due to accommodation change");
                workingBookingProperties.getWorkingBooking().cancelChanges();
            }

            // Pass min nights constraint and isDayVisitor to festival day section
            // Also reset dates when accommodation type changes
            if (festivalDaySection != null && option != null) {
                festivalDaySection.setMinNightsConstraint(option.getMinNights());
                festivalDaySection.setIsDayVisitor(option.isDayVisitor());
                // Reset dates when accommodation changes (user needs to re-select dates)
                festivalDaySection.reset();
                Console.log("USFestivalInPersonBookingForm: Festival day section reset - isDayVisitor=" + option.isDayVisitor() + ", minNights=" + option.getMinNights());
            }
            // Sync breakfast with accommodation selection
            // Day visitors don't get breakfast, overnight guests do
            if (mealsSection != null && option != null) {
                boolean hasAccommodation = !option.isDayVisitor();
                mealsSection.setHasAccommodation(hasAccommodation);
                Console.log("USFestivalInPersonBookingForm: Set hasAccommodation=" + hasAccommodation + " for meals section");
            }
            // Configure roommate section based on accommodation selection
            if (roommateInfoSection != null && option != null) {
                // Always reset roommate section when accommodation changes
                roommateInfoSection.reset();

                boolean isShareAccommodation = option.getItemEntity() != null && Boolean.TRUE.equals(option.getItemEntity().isShare_mate());
                boolean isDayVisitor = option.isDayVisitor();

                if (isShareAccommodation) {
                    // Share Accommodation: single roommate field (person is sharing someone else's room)
                    roommateInfoSection.setIsRoomBooker(false);
                    roommateInfoSection.setVisible(true);
                    Console.log("USFestivalInPersonBookingForm: Roommate section visible for Share Accommodation");
                } else if (!isDayVisitor && option.getItemEntity() != null) {
                    // Room booking: check capacity for multi-person rooms
                    Item item = option.getItemEntity();
                    Integer capacity = item.getCapacity();
                    if (capacity != null && capacity > 1) {
                        // Multi-person room: show roommate fields
                        roommateInfoSection.setRoomCapacity(capacity);

                        // Get minOccupancy from ItemPolicy (defaults to capacity if not defined,
                        // meaning all roommate fields are mandatory by default)
                        one.modality.base.shared.entities.ItemPolicy itemPolicy = workingBookingProperties != null
                            ? workingBookingProperties.getPolicyAggregate().getItemPolicy(item)
                            : null;
                        int minOccupancy = (itemPolicy != null && itemPolicy.getMinOccupancy() != null)
                            ? itemPolicy.getMinOccupancy() : capacity;
                        roommateInfoSection.setMinOccupancy(minOccupancy);
                        roommateInfoSection.setIsRoomBooker(true);
                        roommateInfoSection.setVisible(true);
                        Console.log("USFestivalInPersonBookingForm: Roommate section visible for multi-person room - capacity=" + capacity + ", minOccupancy=" + minOccupancy);
                    } else {
                        // Single-person room or no capacity info: hide roommate section
                        roommateInfoSection.setVisible(false);
                        Console.log("USFestivalInPersonBookingForm: Roommate section hidden (single-person room)");
                    }
                } else {
                    // Day Visitor or no item entity: hide roommate section
                    roommateInfoSection.setVisible(false);
                    Console.log("USFestivalInPersonBookingForm: Roommate section hidden (day visitor or no item)");
                }
            }
            // Update sticky price header (room name and days)
            updateStickyPriceHeader();
            // Book items into WorkingBooking to update price in header (bound to totalProperty)
            bookSelectedItemsIntoWorkingBooking();
        });

        accommodationSection.setOnContinuePressed(() -> {
            form.navigateToNextPage();
        });

        accommodationSection.setOnBackPressed(() -> {
            form.navigateToPreviousPage();
        });
    }

    // Validation warning zone for Accommodation page
    private ValidationWarningZone accommodationWarningZone;

    /**
     * Sets up the Back and Next buttons on the Accommodation page (Step 1).
     * Also adds a validation warning zone that shows when no accommodation is selected.
     */
    private void setupAccommodationButtons() {
        if (accommodationPage == null) return;

        // Create validation warning zone and register accommodation section validation
        accommodationWarningZone = new ValidationWarningZone();
        if (accommodationSection != null) {
            accommodationWarningZone.addValidationSource(
                accommodationSection.validProperty(),
                () -> I18n.getI18nText("AccommodationRequiredWarning")
            );
        }

        // Add the warning zone as footer content (persists through setWorkingBookingProperties)
        accommodationPage.setFooterContent(accommodationWarningZone);

        // Set up buttons - no Back button on first page (entry form handles going back)
        accommodationPage.setButtons(
            new BookingFormButton(BookingPageI18nKeys.Continue,
                e -> form.navigateToNextPage(),
                "btn-primary booking-form-btn-primary",
                Bindings.not(accommodationPage.validProperty()))  // Disable when invalid
        );
    }

    private void setupBookingDetailsCallbacks() {
        if (festivalDaySection != null) {
            festivalDaySection.setOnDatesChanged((arrival, departure) -> {
                Console.log("USFestivalInPersonBookingForm: Dates changed - " + arrival + " to " + departure);
                // Check if extended stay (early arrival or late departure)
                updateExtendedStayStatus(arrival, departure);
                // Update sticky price header with new days count
                updateStickyPriceHeader();
                // Book items into WorkingBooking to update price in header (bound to totalProperty)
                bookSelectedItemsIntoWorkingBooking();
                // Update meals section with dates for meal summary
                if (mealsSection != null) {
                    mealsSection.setArrivalDate(arrival);
                    mealsSection.setDepartureDate(departure);
                    // Also sync arrival/departure times - they may already be set in festivalDaySection
                    // but not yet synced to mealsSection if dates changed before times were modified
                    mealsSection.setArrivalTime(festivalDaySection.arrivalTimeProperty().get());
                    mealsSection.setDepartureTime(festivalDaySection.departureTimeProperty().get());

                    // Enable/disable early arrival and late departure pricing display
                    // based on whether selected dates fall within those periods
                    boolean hasEarlyArrival = arrival != null && eventStartDate != null && arrival.isBefore(eventStartDate);
                    boolean hasLateDeparture = departure != null && eventEndDate != null && departure.isAfter(eventEndDate);

                    // Track if state changed to determine if we need to rebuild cards
                    boolean wasShowingEarly = mealsSection.isShowEarlyArrivalPricing();
                    boolean wasShowingLate = mealsSection.isShowLateDeparturePricing();
                    boolean stateChanged = (hasEarlyArrival != wasShowingEarly) || (hasLateDeparture != wasShowingLate);

                    mealsSection.setShowEarlyArrivalPricing(hasEarlyArrival);
                    mealsSection.setShowLateDeparturePricing(hasLateDeparture);

                    // Rebuild meal cards when state changes (showing or hiding secondary prices)
                    if (stateChanged) {
                        mealsSection.rebuildMealCards();
                        Console.log("USFestivalInPersonBookingForm: Updated meal cards - earlyArrival=" + hasEarlyArrival + ", lateDeparture=" + hasLateDeparture);
                    }
                }
            });

            // Listen for arrival time changes
            festivalDaySection.arrivalTimeProperty().addListener((obs, old, newTime) -> {
                Console.log("USFestivalInPersonBookingForm: Arrival time changed - " + newTime);
                if (mealsSection != null) {
                    mealsSection.setArrivalTime(newTime);
                }
                // Re-book items to update meal counts based on new arrival time
                bookSelectedItemsIntoWorkingBooking();
                // Rebuild meal cards to ensure early/late pricing display is correct
                rebuildMealCardsIfNeeded();
            });

            // Listen for departure time changes
            festivalDaySection.departureTimeProperty().addListener((obs, old, newTime) -> {
                Console.log("USFestivalInPersonBookingForm: Departure time changed - " + newTime);
                if (mealsSection != null) {
                    mealsSection.setDepartureTime(newTime);
                }
                // Re-book items to update meal counts based on new departure time
                bookSelectedItemsIntoWorkingBooking();
                // Rebuild meal cards to ensure early/late pricing display is correct
                rebuildMealCardsIfNeeded();
            });
        }

        // Set up audio recording phase section callbacks and date binding
        setupAudioRecordingCallbacks();

        // Set up meals section callbacks for meal and dietary selection changes
        setupMealsCallbacks();

        // Set up transport section callbacks and date binding (parking + shuttle)
        setupTransportCallbacks();

        // Set up additional options section callbacks
        setupAdditionalOptionsCallbacks();
    }

    /**
     * Sets up callbacks for the audio recording phase section to:
     * - Listen for date changes from festivalDaySection to update option availability
     * - Listen for option selection changes to re-book items
     */
    private void setupAudioRecordingCallbacks() {
        if (audioRecordingPhaseSection == null || festivalDaySection == null) return;

        // Bind arrival date to audio recording section (for availability)
        festivalDaySection.arrivalDateProperty().addListener((obs, old, newDate) -> {
            Console.log("USFestivalInPersonBookingForm: Updating audio recording arrival date - " + newDate);
            audioRecordingPhaseSection.setArrivalDate(newDate);
        });

        // Bind departure date to audio recording section (for availability)
        festivalDaySection.departureDateProperty().addListener((obs, old, newDate) -> {
            Console.log("USFestivalInPersonBookingForm: Updating audio recording departure date - " + newDate);
            audioRecordingPhaseSection.setDepartureDate(newDate);
        });

        // Listen for option selection changes
        audioRecordingPhaseSection.setOnOptionSelected(option -> {
            Console.log("USFestivalInPersonBookingForm: Audio recording phase selected - " +
                (option != null ? option.getName() : "None"));
            // Re-book items to update audio recording selection
            bookSelectedItemsIntoWorkingBooking();
        });
    }

    /**
     * Sets up callbacks for the transport section (parking + shuttle) to:
     * - Listen for date changes from festivalDaySection to update shuttle availability
     * - Listen for parking and shuttle selection changes to re-book items
     */
    private void setupTransportCallbacks() {
        if (transportSection == null || festivalDaySection == null) return;

        // Bind arrival date to transport section (for shuttle availability)
        festivalDaySection.arrivalDateProperty().addListener((obs, old, newDate) -> {
            Console.log("USFestivalInPersonBookingForm: Updating transport arrival date - " + newDate);
            transportSection.setArrivalDate(newDate);
        });

        // Bind departure date to transport section (for shuttle availability)
        festivalDaySection.departureDateProperty().addListener((obs, old, newDate) -> {
            Console.log("USFestivalInPersonBookingForm: Updating transport departure date - " + newDate);
            transportSection.setDepartureDate(newDate);
        });

        // Initialize transport section with current dates (listeners only fire on changes)
        LocalDate currentArrival = festivalDaySection.arrivalDateProperty().get();
        LocalDate currentDeparture = festivalDaySection.departureDateProperty().get();
        if (currentArrival != null) {
            Console.log("USFestivalInPersonBookingForm: Initializing transport arrival date - " + currentArrival);
            transportSection.setArrivalDate(currentArrival);
        }
        if (currentDeparture != null) {
            Console.log("USFestivalInPersonBookingForm: Initializing transport departure date - " + currentDeparture);
            transportSection.setDepartureDate(currentDeparture);
        }

        // Listen for any transport selection changes (parking or shuttle)
        transportSection.setOnSelectionChanged(() -> {
            Console.log("USFestivalInPersonBookingForm: Transport selection changed - parking=" +
                transportSection.getSelectedParkingOptions().size() + " options, shuttle=" +
                transportSection.getSelectedShuttleOptions().size() + " options");
            bookSelectedItemsIntoWorkingBooking();
        });
    }

    /**
     * Sets up callbacks for the meals section to listen for:
     * - Breakfast/Lunch/Dinner selection changes
     * - Dietary preference (Vegetarian/Vegan) changes
     */
    private void setupMealsCallbacks() {
        if (mealsSection == null) return;

        // Listen for breakfast selection changes
        mealsSection.wantsBreakfastProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalInPersonBookingForm: Breakfast selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for lunch selection changes
        mealsSection.wantsLunchProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalInPersonBookingForm: Lunch selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for dinner selection changes
        mealsSection.wantsDinnerProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalInPersonBookingForm: Dinner selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for dietary preference changes (API-driven - selectedDietaryItem)
        mealsSection.selectedDietaryItemProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalInPersonBookingForm: Dietary preference (API) changed - " +
                (newVal != null ? newVal.getName() : "None"));
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for legacy dietary preference changes (enum-based - dietaryPreference)
        mealsSection.dietaryPreferenceProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalInPersonBookingForm: Dietary preference (legacy) changed - " +
                (newVal != null ? newVal.name() : "None"));
            bookSelectedItemsIntoWorkingBooking();
        });
    }

    /**
     * Rebuilds meal cards if early arrival or late departure pricing is enabled.
     * This ensures the cards reflect the correct pricing state after meal selection changes.
     */
    private void rebuildMealCardsIfNeeded() {
        if (mealsSection == null) return;

        // Only rebuild if we have early/late pricing enabled
        if (mealsSection.isShowEarlyArrivalPricing() || mealsSection.isShowLateDeparturePricing()) {
            Console.log("USFestivalInPersonBookingForm: Rebuilding meal cards to update early/late pricing display");
            mealsSection.rebuildMealCards();
        }
    }

    /**
     * Sets up callbacks for the additional options section to listen for selection changes.
     */
    private void setupAdditionalOptionsCallbacks() {
        if (additionalOptionsSection == null) return;

        // Listen for any option selection changes (parking, assisted listening, etc.)
        additionalOptionsSection.setOnSelectionChanged(() -> {
            Console.log("USFestivalInPersonBookingForm: Additional option selection changed");
            bookSelectedItemsIntoWorkingBooking();
        });
    }

    /**
     * Updates the extended stay status (early arrival/late departure) on the meals section.
     * Shows an info note if the selected dates extend beyond the event dates.
     *
     * <p>Uses event boundary dates from EventSelection/EventPart (populated by populateEventBoundaries())
     * instead of the old Event.getStartDate()/getEndDate() approach.</p>
     */
    private void updateExtendedStayStatus(LocalDate arrival, LocalDate departure) {
        if (mealsSection == null || arrival == null || departure == null) {
            return;
        }

        // Use event boundary dates from EventSelection/EventPart (loaded from PolicyAggregate)
        if (eventStartDate == null || eventEndDate == null) {
            Console.log("USFestivalInPersonBookingForm: Event boundary dates not yet populated, skipping extended stay check");
            return;
        }

        // Check if the stay extends beyond event dates
        boolean isEarlyArrival = arrival.isBefore(eventStartDate);
        boolean isLateDeparture = departure.isAfter(eventEndDate);
        boolean hasExtendedStay = isEarlyArrival || isLateDeparture;

        Console.log("USFestivalInPersonBookingForm: Extended stay check - event: " + eventStartDate + " to " + eventEndDate +
            ", selected: " + arrival + " to " + departure + ", extended: " + hasExtendedStay);

        mealsSection.setHasExtendedStay(hasExtendedStay);
    }

    // Validation warning zone for Booking Details page
    private ValidationWarningZone bookingDetailsWarningZone;

    /**
     * Sets up the Back and Next buttons on the Booking Details page (Step 2).
     * Also adds a validation warning zone that shows messages when the form is invalid.
     */
    private void setupBookingDetailsButtons() {
        if (bookingDetailsPage == null) return;

        // Create validation warning zone and register roommate section validation
        bookingDetailsWarningZone = new ValidationWarningZone();
        if (roommateInfoSection != null) {
            bookingDetailsWarningZone.addValidationSource(
                roommateInfoSection.validProperty(),
                roommateInfoSection::getValidationMessage
            );
        }

        // Add the warning zone as footer content (persists through setWorkingBookingProperties)
        bookingDetailsPage.setFooterContent(bookingDetailsWarningZone);

        // Set up buttons with validation binding for Continue button
        bookingDetailsPage.setButtons(
            new BookingFormButton(BookingPageI18nKeys.Back,
                e -> form.navigateToPreviousPage(),
                "btn-back booking-form-btn-back"),
            new BookingFormButton(BookingPageI18nKeys.Continue,
                e -> form.navigateToNextPage(),
                "btn-primary booking-form-btn-primary",
                Bindings.not(bookingDetailsPage.validProperty()))  // Disable when invalid
        );
    }

    private void setupYourInformationCallbacks() {
        if (yourInformationSection == null) return;

        yourInformationSection.setOnLoginSuccess(person -> {
            form.getState().setLoggedInPerson(person);
            onAfterLogin();
            form.navigateToMemberSelection();
        });

        yourInformationSection.setOnNewUserContinue(newUserData -> {
            form.getState().setPendingNewUserData(newUserData);
            form.navigateToSummary();
        });

        yourInformationSection.setOnBackPressed(() -> {
            // Go back to booking details
            form.navigateToPreviousPage();
        });
    }

    private void setupMemberSelectionCallbacks() {
        if (memberSelectionSection == null) return;

        memberSelectionSection.setOnMemberSelected(member -> {
            form.getState().setSelectedMember(member);
        });

        memberSelectionSection.setOnContinuePressed(() -> {
            form.navigateToSummary();
        });

        memberSelectionSection.setOnBackPressed(() -> {
            // If not logged in, go to Your Information; otherwise go to Booking Details
            if (FXUserPerson.getUserPerson() == null) {
                form.navigateToYourInformation();
            } else {
                form.navigateToPreviousPage();
            }
        });
    }

    private void setupLogoutListener() {
        FXUserPerson.userPersonProperty().addListener((obs, oldPerson, newPerson) -> {
            if (oldPerson != null && newPerson == null) {
                Console.log("USFestivalInPersonBookingForm: User logged out");
                // Only navigate if WorkingBooking is initialized to avoid NPE
                if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                    Console.log("USFestivalInPersonBookingForm: Navigating to Your Information");
                    form.navigateToYourInformation();
                } else {
                    Console.log("USFestivalInPersonBookingForm: WorkingBooking not initialized, skipping navigation");
                }
            }
        });
    }

    private void loadMembersIfLoggedIn() {
        Person person = FXUserPerson.getUserPerson();
        if (person != null && memberSelectionSection != null) {
            Console.log("USFestivalInPersonBookingForm: Loading household members at construction time");
            HouseholdMemberLoader.loadMembersAsync(person, memberSelectionSection, settings.event());
        }
    }

    // ========================================
    // Accommodation Population
    // ========================================

    /**
     * Populates accommodation options from PolicyAggregate data.
     * Uses WorkingBooking to calculate accurate prices for each accommodation type,
     * including teachings, accommodation, and meals within event boundaries.
     */
    private void populateAccommodationOptions() {
        // Skip if already populated
        if (accommodationOptionsPopulated) {
            Console.log("USFestivalInPersonBookingForm: Accommodation options already populated, skipping");
            return;
        }

        if (accommodationSection == null) {
            Console.log("USFestivalInPersonBookingForm: accommodationSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBookingProperties not available yet");
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not initialized yet, will populate later");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available yet");
            return;
        }

        // Debug: log what we're working with
        int scheduledItemsCount = policyAggregate.getScheduledItems() != null ? policyAggregate.getScheduledItems().size() : 0;
        int accommodationItemsCount = policyAggregate.filterAccommodationScheduledItems().size();
        Console.log("USFestivalInPersonBookingForm: PolicyAggregate has " + scheduledItemsCount + " total scheduled items, " + accommodationItemsCount + " accommodation items");

        Console.log("USFestivalInPersonBookingForm: Populating accommodation options with WorkingBooking-based pricing");

        // Use event boundary dates for price calculation
        LocalDate arrivalDate = eventStartDate;
        LocalDate departureDate = eventEndDate;
        if (arrivalDate == null || departureDate == null) {
            Console.log("USFestivalInPersonBookingForm: Event boundary dates not available, cannot calculate prices");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Event boundaries for price calculation: " + arrivalDate + " to " + departureDate);

        // Clear existing options and breakdowns
        accommodationSection.clearOptions();
        accommodationSection.clearBreakdowns();

        // Get accommodation items grouped by Item
        List<ScheduledItem> accommodationItems = policyAggregate.filterAccommodationScheduledItems();
        java.util.Map<Item, List<ScheduledItem>> itemScheduledItemsMap = accommodationItems.stream()
            .filter(si -> si.getItem() != null)
            .collect(java.util.stream.Collectors.groupingBy(ScheduledItem::getItem));

        // Sort by Item.getOrd() for consistent display order
        List<java.util.Map.Entry<Item, List<ScheduledItem>>> sortedEntries = itemScheduledItemsMap.entrySet().stream()
            .sorted(java.util.Comparator.comparing(e -> e.getKey().getOrd() != null ? e.getKey().getOrd() : Integer.MAX_VALUE))
            .collect(java.util.stream.Collectors.toList());

        for (java.util.Map.Entry<Item, List<ScheduledItem>> entry : sortedEntries) {
            Item item = entry.getKey();
            List<ScheduledItem> scheduledItems = entry.getValue();

            Console.log("USFestivalInPersonBookingForm: Processing accommodation: " + item.getName());

            // Calculate availability (null means unlimited, so we filter those out)
            // Only consider items with defined availability for the minimum calculation
            java.util.OptionalInt minAvailabilityOpt = scheduledItems.stream()
                .filter(si -> si.getGuestsAvailability() != null)
                .mapToInt(ScheduledItem::getGuestsAvailability)
                .min();

            // If no items have defined availability, it's unlimited (null)
            // Otherwise use the minimum value found
            Integer minAvailability = minAvailabilityOpt.isPresent() ? minAvailabilityOpt.getAsInt() : null;

            HasAccommodationSelectionSection.AvailabilityStatus status;
            if (minAvailability == null) {
                // Unlimited availability
                status = HasAccommodationSelectionSection.AvailabilityStatus.AVAILABLE;
            } else if (minAvailability <= 0) {
                status = HasAccommodationSelectionSection.AvailabilityStatus.SOLD_OUT;
            } else if (minAvailability <= 5) {
                status = HasAccommodationSelectionSection.AvailabilityStatus.LIMITED;
            } else {
                status = HasAccommodationSelectionSection.AvailabilityStatus.AVAILABLE;
            }

            // Get constraint from ItemPolicy
            one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(item);
            HasAccommodationSelectionSection.ConstraintType constraintType = HasAccommodationSelectionSection.ConstraintType.NONE;
            String constraintLabel = null;
            int minNights = 0;

            if (itemPolicy != null && itemPolicy.getMinDay() != null && itemPolicy.getMinDay() > 0) {
                constraintType = HasAccommodationSelectionSection.ConstraintType.MIN_NIGHTS;
                minNights = itemPolicy.getMinDay();
                constraintLabel = I18n.getI18nText(BookingPageI18nKeys.MinNights, minNights);
            }

            // Get price per night and perPerson flag from rates
            one.modality.base.shared.entities.Rate itemRate = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, item)
                .findFirst()
                .orElseGet(() -> policyAggregate.getDailyRatesStream()
                    .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                        && r.getItem().getPrimaryKey().equals(item.getPrimaryKey()))
                    .findFirst()
                    .orElse(null));

            int pricePerNight = itemRate != null && itemRate.getPrice() != null ? itemRate.getPrice() : 0;
            boolean perPerson = itemRate == null || !Boolean.FALSE.equals(itemRate.isPerPerson());

            // Calculate total price and breakdown using WorkingBooking
            AccommodationPriceResult priceResult = calculateAccommodationPriceWithWorkingBooking(policyAggregate, item, arrivalDate, departureDate, minAvailability);
            Console.log("USFestivalInPersonBookingForm: Calculated price for " + item.getName() + ": " + priceResult.totalPrice);

            // Store the breakdown for this option
            accommodationSection.setBreakdownForOption(item.getPrimaryKey(), priceResult.breakdown);

            // Create AccommodationOption with pre-calculated price
            HasAccommodationSelectionSection.AccommodationOption option = new HasAccommodationSelectionSection.AccommodationOption(
                item.getPrimaryKey(),
                item,
                item.getName() != null ? item.getName() : "",
                "",  // description
                pricePerNight,
                status,
                constraintType,
                constraintLabel,
                minNights,
                false,  // isDayVisitor
                null,   // imageUrl
                perPerson,
                priceResult.totalPrice  // pre-calculated total price
            );

            accommodationSection.addAccommodationOption(option);
        }

        // Add Share Accommodation option with calculated price (teachings + meals only, no accommodation)
        addShareAccommodationOptionWithPrice(policyAggregate, arrivalDate, departureDate);

        // Add Day Visitor option with calculated price (teachings + meals only, no accommodation)
        addDayVisitorOptionWithPrice(policyAggregate, arrivalDate, departureDate);

        // Mark as populated to avoid re-populating
        accommodationOptionsPopulated = true;
        Console.log("USFestivalInPersonBookingForm: Accommodation options populated successfully with WorkingBooking prices");
    }

    /**
     * Result class holding both the total price and the breakdown items for an accommodation card.
     */
    private static class AccommodationPriceResult {
        final int totalPrice;
        final List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown;

        AccommodationPriceResult(int totalPrice, List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown) {
            this.totalPrice = totalPrice;
            this.breakdown = breakdown;
        }
    }

    /**
     * Calculates the total price for an accommodation type using WorkingBooking.
     * Books teachings, accommodation, and meals within event boundaries.
     * Also calculates the price breakdown by category.
     *
     * @param policyAggregate the policy data
     * @param accommodationItem the accommodation Item to calculate price for (null for day visitor)
     * @param arrivalDate the event arrival date
     * @param departureDate the event departure date
     * @param accommodationAvailability the remaining availability for the accommodation (null if not applicable)
     * @return the calculated result with total price and breakdown
     */
    private AccommodationPriceResult calculateAccommodationPriceWithWorkingBooking(PolicyAggregate policyAggregate, Item accommodationItem,
                                                               LocalDate arrivalDate, LocalDate departureDate, Integer accommodationAvailability) {
        // Create a temporary WorkingBooking for price calculation
        WorkingBooking tempBooking = new WorkingBooking(policyAggregate, null);

        Console.log("USFestivalInPersonBookingForm: Creating temp WorkingBooking for " +
            (accommodationItem != null ? accommodationItem.getName() : "Day Visitor/Share"));

        // Track dates for breakdown display
        LocalDate teachingMinDate = null;
        LocalDate teachingMaxDate = null;
        int accommodationNightsCount = 0;

        // 1. Book all teachings within event boundaries
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems().stream()
            .filter(si -> si.getDate() != null)
            .filter(si -> !si.getDate().isBefore(arrivalDate) && !si.getDate().isAfter(departureDate))
            .collect(java.util.stream.Collectors.toList());
        Console.log("USFestivalInPersonBookingForm: Booking " + teachingItems.size() + " teaching items for price calculation");
        if (!teachingItems.isEmpty()) {
            tempBooking.bookScheduledItems(teachingItems, false);
            // Calculate date range for teachings
            for (ScheduledItem si : teachingItems) {
                LocalDate d = si.getDate();
                if (teachingMinDate == null || d.isBefore(teachingMinDate)) teachingMinDate = d;
                if (teachingMaxDate == null || d.isAfter(teachingMaxDate)) teachingMaxDate = d;
            }
        }

        // 2. Book accommodation within event boundaries (if not day visitor)
        List<ScheduledItem> accommodationScheduledItems = new ArrayList<>();
        if (accommodationItem != null) {
            accommodationScheduledItems = policyAggregate.filterAccommodationScheduledItems().stream()
                .filter(si -> dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), accommodationItem))
                .filter(si -> si.getDate() != null)
                .filter(si -> !si.getDate().isBefore(arrivalDate) && si.getDate().isBefore(departureDate)) // accommodation nights
                .collect(java.util.stream.Collectors.toList());
            Console.log("USFestivalInPersonBookingForm: Booking " + accommodationScheduledItems.size() + " accommodation items for " + accommodationItem.getName());
            if (!accommodationScheduledItems.isEmpty()) {
                tempBooking.bookScheduledItems(accommodationScheduledItems, false);
                accommodationNightsCount = accommodationScheduledItems.size();
            }
        }

        // 3. Book all meals within event boundaries
        // For card pricing, include ALL meals (lunch + dinner) for the full event stay
        // Use MORNING arrival and AFTERNOON departure to include standard meals
        bookMealsForPriceCalculation(tempBooking, policyAggregate, arrivalDate, departureDate, accommodationItem != null);

        // Calculate total price
        int totalPrice = tempBooking.calculateTotal();
        Console.log("USFestivalInPersonBookingForm: Total price calculated: " + totalPrice + " for " +
            (accommodationItem != null ? accommodationItem.getName() : "Day Visitor/Share"));

        // Build price breakdown by category
        List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown = new ArrayList<>();
        PriceCalculator calc = tempBooking.getLatestBookingPriceCalculator();

        // Teaching breakdown
        List<DocumentLine> teachingLines = tempBooking.getFamilyDocumentLines(KnownItemFamily.TEACHING);
        if (!teachingLines.isEmpty()) {
            int teachingPrice = calc.calculateDocumentLinesPrice(teachingLines);
            String teachingDateRange = formatDateRange(teachingMinDate, teachingMaxDate);
            breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
                "Teachings", teachingDateRange, teachingPrice));
        }

        // Accommodation breakdown
        List<DocumentLine> accoLines = tempBooking.getFamilyDocumentLines(KnownItemFamily.ACCOMMODATION);
        if (!accoLines.isEmpty()) {
            int accoPrice = calc.calculateDocumentLinesPrice(accoLines);
            String accoDateRange = accommodationNightsCount + " night" + (accommodationNightsCount != 1 ? "s" : "");
            breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
                "Accommodation", accoDateRange, accoPrice, accommodationAvailability));
        }

        // Meals breakdown - split by meal type (Breakfast, Lunch, Dinner)
        // Get original meal ScheduledItems from PolicyAggregate (they have Timeline loaded)
        List<ScheduledItem> originalMealsItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);

        // Get meal Items from PolicyAggregate timelines
        one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
        one.modality.base.shared.entities.Timeline lunchTimeline = policyAggregate.getLunchTimeline();
        one.modality.base.shared.entities.Timeline dinnerTimeline = policyAggregate.getDinnerTimeline();

        Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;
        Item lunchItem = lunchTimeline != null ? lunchTimeline.getItem() : null;
        Item dinnerItem = dinnerTimeline != null ? dinnerTimeline.getItem() : null;

        Console.log("USFestivalInPersonBookingForm: Meal items from PolicyAggregate - breakfast=" +
            (breakfastItem != null ? breakfastItem.getName() + " (id=" + breakfastItem.getPrimaryKey() + ")" : "null") +
            ", lunch=" + (lunchItem != null ? lunchItem.getName() + " (id=" + lunchItem.getPrimaryKey() + ")" : "null") +
            ", dinner=" + (dinnerItem != null ? dinnerItem.getName() + " (id=" + dinnerItem.getPrimaryKey() + ")" : "null"));

        // Categorize meal attendances by matching with ORIGINAL ScheduledItems (which have Timeline loaded)
        List<one.modality.base.shared.entities.Attendance> breakfastAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> lunchAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> dinnerAttendances = new ArrayList<>();

        List<one.modality.base.shared.entities.Attendance> bookedAttendances = tempBooking.getBookedAttendances();
        Console.log("USFestivalInPersonBookingForm: Processing " + bookedAttendances.size() + " booked attendances for meal categorization");

        int mealsAttendanceCount = 0;
        for (one.modality.base.shared.entities.Attendance attendance : bookedAttendances) {
            ScheduledItem attendanceSi = attendance.getScheduledItem();
            if (attendanceSi == null) {
                Console.log("USFestivalInPersonBookingForm: Attendance has null ScheduledItem");
                continue;
            }

            // Find the ORIGINAL ScheduledItem from PolicyAggregate by primary key
            // This ensures we have the Timeline loaded correctly
            ScheduledItem originalSi = null;
            for (ScheduledItem origItem : originalMealsItems) {
                if (dev.webfx.stack.orm.entity.Entities.samePrimaryKey(origItem, attendanceSi)) {
                    originalSi = origItem;
                    break;
                }
            }

            if (originalSi == null) {
                // Not a meals ScheduledItem, skip
                continue;
            }

            mealsAttendanceCount++;

            // Get the Timeline from the ORIGINAL ScheduledItem (which has it loaded)
            one.modality.base.shared.entities.Timeline siTimeline = originalSi.getTimeline();
            Item timelineItem = siTimeline != null ? siTimeline.getItem() : null;

            Console.log("USFestivalInPersonBookingForm: Meal attendance - date=" + attendance.getDate() +
                ", originalSi=" + (originalSi != null ? originalSi.getName() : "null") +
                ", timeline=" + (siTimeline != null ? "id=" + siTimeline.getPrimaryKey() + ", startTime=" + siTimeline.getStartTime() : "null") +
                ", timelineItem=" + (timelineItem != null ? timelineItem.getName() + " (id=" + timelineItem.getPrimaryKey() + ")" : "null"));

            // Match by Timeline's Item (using primary key comparison)
            if (breakfastItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, breakfastItem)) {
                breakfastAttendances.add(attendance);
                Console.log("USFestivalInPersonBookingForm: -> Matched BREAKFAST");
            } else if (lunchItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, lunchItem)) {
                lunchAttendances.add(attendance);
                Console.log("USFestivalInPersonBookingForm: -> Matched LUNCH");
            } else if (dinnerItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, dinnerItem)) {
                dinnerAttendances.add(attendance);
                Console.log("USFestivalInPersonBookingForm: -> Matched DINNER");
            } else {
                Console.log("USFestivalInPersonBookingForm: -> NO MATCH (breakfastItem=" +
                    (breakfastItem != null ? breakfastItem.getPrimaryKey() : "null") +
                    ", lunchItem=" + (lunchItem != null ? lunchItem.getPrimaryKey() : "null") +
                    ", dinnerItem=" + (dinnerItem != null ? dinnerItem.getPrimaryKey() : "null") + ")");
            }
        }

        Console.log("USFestivalInPersonBookingForm: Found " + mealsAttendanceCount + " meals attendances out of " + bookedAttendances.size() + " total");
        Console.log("USFestivalInPersonBookingForm: Meal breakdown - breakfast=" + breakfastAttendances.size() +
            ", lunch=" + lunchAttendances.size() + ", dinner=" + dinnerAttendances.size());

        // Add breakdown for each meal type using attendance count and rate
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Breakfast", breakfastAttendances, breakfastTimeline);
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Lunch", lunchAttendances, lunchTimeline);
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Dinner", dinnerAttendances, dinnerTimeline);

        // Calculate total from breakdown to ensure card price matches breakdown
        // (tempBooking.calculateTotal() might include items not shown in breakdown due to categorization)
        int breakdownTotal = breakdown.stream().mapToInt(item -> item.getPrice()).sum();
        Console.log("USFestivalInPersonBookingForm: Breakdown total: " + breakdownTotal + " (tempBooking total was: " + totalPrice + ")");

        return new AccommodationPriceResult(breakdownTotal, breakdown);
    }

    /**
     * Calculates the total price for sharing accommodation.
     * Since sharing accommodation has no ScheduledItems, we calculate its price directly from rates
     * and combine with teachings and meals which DO have ScheduledItems.
     *
     * @param policyAggregate the policy data
     * @param sharingAccommodationItem the sharing accommodation Item (has share_mate=true)
     * @param arrivalDate the event arrival date
     * @param departureDate the event departure date
     * @return the calculated result with total price and breakdown
     */
    private AccommodationPriceResult calculateShareAccommodationPriceWithWorkingBooking(PolicyAggregate policyAggregate, Item sharingAccommodationItem,
                                                                                         LocalDate arrivalDate, LocalDate departureDate) {
        // Create a temporary WorkingBooking for teachings and meals (which have ScheduledItems)
        WorkingBooking tempBooking = new WorkingBooking(policyAggregate, null);

        Console.log("USFestivalInPersonBookingForm: Calculating Share Accommodation price with item: " +
            (sharingAccommodationItem != null ? sharingAccommodationItem.getName() : "null"));

        // Track dates for breakdown display
        LocalDate teachingMinDate = null;
        LocalDate teachingMaxDate = null;

        // 1. Book all teachings within event boundaries (they have ScheduledItems)
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems().stream()
            .filter(si -> si.getDate() != null)
            .filter(si -> !si.getDate().isBefore(arrivalDate) && !si.getDate().isAfter(departureDate))
            .collect(java.util.stream.Collectors.toList());
        Console.log("USFestivalInPersonBookingForm: Booking " + teachingItems.size() + " teaching items for share accommodation price calculation");
        if (!teachingItems.isEmpty()) {
            tempBooking.bookScheduledItems(teachingItems, false);
            // Calculate date range for teachings
            for (ScheduledItem si : teachingItems) {
                LocalDate d = si.getDate();
                if (teachingMinDate == null || d.isBefore(teachingMinDate)) teachingMinDate = d;
                if (teachingMaxDate == null || d.isAfter(teachingMaxDate)) teachingMaxDate = d;
            }
        }

        // 2. Calculate sharing accommodation price directly from rate (no ScheduledItems exist)
        int accommodationNightsCount = 0;
        int sharingAccommodationPrice = 0;
        if (sharingAccommodationItem != null) {
            // Count accommodation nights (arrival to departure-1)
            LocalDate current = arrivalDate;
            while (current.isBefore(departureDate)) {
                accommodationNightsCount++;
                current = current.plusDays(1);
            }

            // Get daily rate for sharing accommodation item
            one.modality.base.shared.entities.Rate itemRate = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, sharingAccommodationItem)
                .findFirst()
                .orElseGet(() -> policyAggregate.getDailyRatesStream()
                    .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                        && r.getItem().getPrimaryKey().equals(sharingAccommodationItem.getPrimaryKey()))
                    .findFirst()
                    .orElse(null));

            int pricePerNight = itemRate != null && itemRate.getPrice() != null ? itemRate.getPrice() : 0;
            sharingAccommodationPrice = pricePerNight * accommodationNightsCount;
            Console.log("USFestivalInPersonBookingForm: Sharing accommodation price: " + pricePerNight + " x " + accommodationNightsCount + " nights = " + sharingAccommodationPrice);
        }

        // 3. Book all meals within event boundaries (they have ScheduledItems)
        // hasAccommodation = true since sharing accommodation guests get breakfast
        bookMealsForPriceCalculation(tempBooking, policyAggregate, arrivalDate, departureDate, true);

        // Calculate teachings + meals price from WorkingBooking
        int teachingsAndMealsPrice = tempBooking.calculateTotal();
        Console.log("USFestivalInPersonBookingForm: Teachings + meals price: " + teachingsAndMealsPrice);

        // Total = teachings + meals + sharing accommodation
        int totalPrice = teachingsAndMealsPrice + sharingAccommodationPrice;
        Console.log("USFestivalInPersonBookingForm: Share Accommodation total price calculated: " + totalPrice);

        // Build price breakdown by category
        List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown = new ArrayList<>();
        PriceCalculator calc = tempBooking.getLatestBookingPriceCalculator();

        // Teaching breakdown
        List<DocumentLine> teachingLines = tempBooking.getFamilyDocumentLines(KnownItemFamily.TEACHING);
        if (!teachingLines.isEmpty()) {
            int teachingPrice = calc.calculateDocumentLinesPrice(teachingLines);
            String teachingDateRange = formatDateRange(teachingMinDate, teachingMaxDate);
            breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
                "Teachings", teachingDateRange, teachingPrice));
        }

        // Sharing Accommodation breakdown (calculated directly, not from WorkingBooking)
        if (sharingAccommodationPrice > 0) {
            String accoDateRange = accommodationNightsCount + " night" + (accommodationNightsCount != 1 ? "s" : "");
            breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
                "Share Accommodation", accoDateRange, sharingAccommodationPrice));
        }

        // Meals breakdown - split by meal type (Breakfast, Lunch, Dinner)
        // Get original meal ScheduledItems from PolicyAggregate (they have Timeline loaded)
        List<ScheduledItem> originalMealsItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);

        // Get meal Items from PolicyAggregate timelines
        one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
        one.modality.base.shared.entities.Timeline lunchTimeline = policyAggregate.getLunchTimeline();
        one.modality.base.shared.entities.Timeline dinnerTimeline = policyAggregate.getDinnerTimeline();

        Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;
        Item lunchItem = lunchTimeline != null ? lunchTimeline.getItem() : null;
        Item dinnerItem = dinnerTimeline != null ? dinnerTimeline.getItem() : null;

        // Categorize meal attendances by matching with ORIGINAL ScheduledItems (which have Timeline loaded)
        List<one.modality.base.shared.entities.Attendance> breakfastAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> lunchAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> dinnerAttendances = new ArrayList<>();

        List<one.modality.base.shared.entities.Attendance> bookedAttendances = tempBooking.getBookedAttendances();

        for (one.modality.base.shared.entities.Attendance attendance : bookedAttendances) {
            ScheduledItem attendanceSi = attendance.getScheduledItem();
            if (attendanceSi == null) continue;

            // Find the ORIGINAL ScheduledItem from PolicyAggregate by primary key
            ScheduledItem originalSi = null;
            for (ScheduledItem origItem : originalMealsItems) {
                if (dev.webfx.stack.orm.entity.Entities.samePrimaryKey(origItem, attendanceSi)) {
                    originalSi = origItem;
                    break;
                }
            }

            if (originalSi == null) continue;

            // Get the Timeline from the ORIGINAL ScheduledItem (which has it loaded)
            one.modality.base.shared.entities.Timeline siTimeline = originalSi.getTimeline();
            Item timelineItem = siTimeline != null ? siTimeline.getItem() : null;

            // Match by Timeline's Item (using primary key comparison)
            if (breakfastItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, breakfastItem)) {
                breakfastAttendances.add(attendance);
            } else if (lunchItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, lunchItem)) {
                lunchAttendances.add(attendance);
            } else if (dinnerItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, dinnerItem)) {
                dinnerAttendances.add(attendance);
            }
        }

        // Add breakdown for each meal type using attendance count and rate
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Breakfast", breakfastAttendances, breakfastTimeline);
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Lunch", lunchAttendances, lunchTimeline);
        addMealAttendanceBreakdown(breakdown, policyAggregate, "Dinner", dinnerAttendances, dinnerTimeline);

        // Calculate total from breakdown to ensure card price matches breakdown
        int breakdownTotal = breakdown.stream().mapToInt(item -> item.getPrice()).sum();
        Console.log("USFestivalInPersonBookingForm: Share Accommodation breakdown total: " + breakdownTotal + " (calculated total was: " + totalPrice + ")");

        return new AccommodationPriceResult(breakdownTotal, breakdown);
    }

    /**
     * Formats a date range for display (e.g., "Jul 1-8" or "Jul 1").
     */
    private String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) return "";
        DateTimeFormatter monthDay = DateTimeFormatter.ofPattern("MMM d");
        if (endDate == null || startDate.equals(endDate)) {
            return startDate.format(monthDay);
        }
        // Same month: "Jul 1-8"
        if (startDate.getMonth() == endDate.getMonth()) {
            return startDate.format(monthDay) + "-" + endDate.getDayOfMonth();
        }
        // Different months: "Jul 1 - Aug 8"
        return startDate.format(monthDay) + " - " + endDate.format(monthDay);
    }

    /**
     * Adds a meal type breakdown item based on Attendances.
     * Uses the rate from PolicyAggregate to calculate the price.
     */
    private void addMealAttendanceBreakdown(List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown,
                                             PolicyAggregate policyAggregate,
                                             String mealType,
                                             List<one.modality.base.shared.entities.Attendance> attendances,
                                             one.modality.base.shared.entities.Timeline timeline) {
        if (attendances.isEmpty() || timeline == null) return;

        // Get the rate for this meal type from PolicyAggregate
        Item mealItem = timeline.getItem();
        if (mealItem == null) return;

        // Get daily rate for this meal item - use same approach as DefaultMealsSelectionSection
        int pricePerMeal = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, mealItem)
            .findFirst()
            .map(one.modality.base.shared.entities.Rate::getPrice)
            .orElseGet(() -> {
                // Fallback: search all daily rates for this item regardless of site
                return policyAggregate.getDailyRatesStream()
                    .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                        && r.getItem().getPrimaryKey().equals(mealItem.getPrimaryKey()))
                    .findFirst()
                    .map(one.modality.base.shared.entities.Rate::getPrice)
                    .orElse(0);
            });

        int totalPrice = pricePerMeal * attendances.size();

        Console.log("USFestivalInPersonBookingForm: " + mealType + " - " + attendances.size() +
            " attendances x $" + (pricePerMeal / 100.0) + " = $" + (totalPrice / 100.0));

        // Show item in breakdown even if price is $0 (e.g., included breakfast)
        // Get date range from attendances
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (one.modality.base.shared.entities.Attendance attendance : attendances) {
            LocalDate date = attendance.getDate();
            if (date != null) {
                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;
            }
        }

        String dateRange = formatDateRange(minDate, maxDate);
        breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
            mealType, dateRange, totalPrice));
    }

    /**
     * Books meals into a temporary WorkingBooking for price calculation.
     * Uses main EventPart boundaries to filter meals (excludes early arrival/late departure).
     *
     * @param workingBooking the temporary WorkingBooking
     * @param policyAggregate the policy data
     * @param arrivalDate the booking arrival date (for accommodation nights calculation)
     * @param departureDate the booking departure date (for accommodation nights calculation)
     * @param hasAccommodation true if booking includes accommodation (for breakfast calculation)
     */
    private void bookMealsForPriceCalculation(WorkingBooking workingBooking, PolicyAggregate policyAggregate,
                                               LocalDate arrivalDate, LocalDate departureDate, boolean hasAccommodation) {
        List<ScheduledItem> mealsItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);
        if (mealsItems.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No meals ScheduledItems found");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Found " + mealsItems.size() + " total meal ScheduledItems");

        // Get the main EventPart to find boundaries (including time)
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        if (mainEventPart != null) {
            Console.log("USFestivalInPersonBookingForm: Main EventPart: " + mainEventPart.getName() +
                " (" + mainEventPart.getStartDate() + " " + mainEventPart.getStartTime() +
                " to " + mainEventPart.getEndDate() + " " + mainEventPart.getEndTime() + ")");
        }

        // Get meal Items from PolicyAggregate timelines for breakfast detection
        final one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
        final Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;

        Console.log("USFestivalInPersonBookingForm: Breakfast item from PolicyAggregate: " +
            (breakfastItem != null ? breakfastItem.getName() + " (id=" + breakfastItem.getPrimaryKey() + ")" : "null"));

        // Build accommodation nights set for breakfast calculation
        java.util.Set<LocalDate> accommodationNights = new java.util.HashSet<>();
        if (hasAccommodation) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                accommodationNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
        }

        // Use ScheduledItems.filterOverPeriod() which properly handles time-based boundary checks
        // This uses the Period interface implemented by EventPart (via BoundaryPeriod)
        List<ScheduledItem> mealsWithinBoundaries;
        if (mainEventPart != null) {
            mealsWithinBoundaries = one.modality.base.shared.entities.util.ScheduledItems.filterOverPeriod(mealsItems, mainEventPart);
            Console.log("USFestivalInPersonBookingForm: After filterOverPeriod: " + mealsWithinBoundaries.size() + " meals within EventPart boundaries");

            // Debug: log which meals were excluded
            for (ScheduledItem msi : mealsItems) {
                if (!mealsWithinBoundaries.contains(msi)) {
                    LocalDate mealDate = msi.getDate();
                    one.modality.base.shared.entities.Timeline mealTimeline = msi.getTimeline();
                    String mealType = mealTimeline != null && mealTimeline.getItem() != null ? mealTimeline.getItem().getName() : "Unknown";
                    LocalTime mealTime = one.modality.base.shared.entities.util.ScheduledItems.getSessionStartTime(msi);
                    Console.log("USFestivalInPersonBookingForm: Excluded " + mealType + " on " + mealDate + " at " + mealTime +
                        " (outside EventPart boundaries)");
                }
            }
        } else {
            // Fallback: use all meals if no EventPart found
            mealsWithinBoundaries = mealsItems;
            Console.log("USFestivalInPersonBookingForm: No EventPart found, using all meals");
        }

        // Filter meals by arrival/departure dates and breakfast accommodation rule
        final one.modality.base.shared.entities.EventPart finalMainEventPart = mainEventPart;
        List<ScheduledItem> mealsToBook = mealsWithinBoundaries.stream()
            .filter(msi -> {
                // For breakfast with accommodation, only include if guest stayed overnight
                if (!hasAccommodation) return true;

                // Check if this is a breakfast by comparing Timeline's Item with PolicyAggregate's breakfast Item
                one.modality.base.shared.entities.Timeline msiTimeline = msi.getTimeline();
                Item timelineItem = msiTimeline != null ? msiTimeline.getItem() : null;

                boolean isBreakfast = breakfastItem != null && timelineItem != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(timelineItem, breakfastItem);

                if (isBreakfast) {
                    LocalDate mealDate = msi.getDate();
                    if (mealDate == null) return false;
                    // Breakfast is only included if guest stayed the night before
                    LocalDate nightBefore = mealDate.minusDays(1);
                    return accommodationNights.contains(nightBefore);
                }
                return true;
            })
            .collect(java.util.stream.Collectors.toList());

        Console.log("USFestivalInPersonBookingForm: Booking " + mealsToBook.size() + " meal ScheduledItems within EventPart time boundaries");

        // Book all meals
        if (!mealsToBook.isEmpty()) {
            workingBooking.bookScheduledItems(mealsToBook, false);
        }
    }

    /**
     * Finds the main EventPart (the one that is neither early arrival nor late departure).
     * Uses PolicyAggregate's getEarlyArrivalPart() and getLateDeparturePart() to identify
     * which EventParts to exclude, then returns the remaining "main" EventPart.
     */
    private one.modality.base.shared.entities.EventPart findMainEventPart(PolicyAggregate policyAggregate) {
        dev.webfx.stack.orm.entity.EntityList<one.modality.base.shared.entities.EventPart> eventParts = policyAggregate.getEventParts();
        if (eventParts == null || eventParts.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No EventParts found");
            return null;
        }

        // First try PolicyAggregate's methods
        one.modality.base.shared.entities.EventPart earlyArrivalPart = policyAggregate.getEarlyArrivalPart();
        one.modality.base.shared.entities.EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();

        Console.log("USFestivalInPersonBookingForm: PolicyAggregate early arrival part: " + (earlyArrivalPart != null ? earlyArrivalPart.getName() : "none"));
        Console.log("USFestivalInPersonBookingForm: PolicyAggregate late departure part: " + (lateDeparturePart != null ? lateDeparturePart.getName() : "none"));

        // If PolicyAggregate methods work, use them to find the main part
        if (earlyArrivalPart != null || lateDeparturePart != null) {
            for (one.modality.base.shared.entities.EventPart part : eventParts) {
                boolean isEarlyArrival = earlyArrivalPart != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(part, earlyArrivalPart);
                boolean isLateDeparture = lateDeparturePart != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(part, lateDeparturePart);

                if (!isEarlyArrival && !isLateDeparture) {
                    Console.log("USFestivalInPersonBookingForm: Found main EventPart: " + part.getName() +
                        " (" + part.getStartDate() + " " + part.getStartTime() +
                        " to " + part.getEndDate() + " " + part.getEndTime() + ")");
                    return part;
                }
            }
        }

        // If PolicyAggregate methods return null, identify parts by their dates
        // The main event is typically the longest part (most days)
        Console.log("USFestivalInPersonBookingForm: PolicyAggregate methods returned null, identifying parts by dates");
        one.modality.base.shared.entities.EventPart longestPart = null;
        long longestDuration = -1;

        for (one.modality.base.shared.entities.EventPart part : eventParts) {
            LocalDate partStart = part.getStartDate();
            LocalDate partEnd = part.getEndDate();
            if (partStart != null && partEnd != null) {
                long duration = ChronoUnit.DAYS.between(partStart, partEnd);
                Console.log("USFestivalInPersonBookingForm: EventPart " + part.getName() +
                    " (" + partStart + " to " + partEnd + ") duration: " + duration + " days");
                if (duration > longestDuration) {
                    longestDuration = duration;
                    longestPart = part;
                }
            }
        }

        if (longestPart != null) {
            Console.log("USFestivalInPersonBookingForm: Identified main EventPart by longest duration: " + longestPart.getName() +
                " (" + longestPart.getStartDate() + " " + longestPart.getStartTime() +
                " to " + longestPart.getEndDate() + " " + longestPart.getEndTime() + ")");
        } else {
            Console.log("USFestivalInPersonBookingForm: No main EventPart found");
        }

        return longestPart;
    }

    /**
     * Determines which meal boundary (BREAKFAST, LUNCH, DINNER) corresponds to a given time.
     * Used to identify which meal marks the start/end of the main event period.
     *
     * Time ranges:
     * - BREAKFAST: before 10:00 (typically 7:00-9:00)
     * - LUNCH: 10:00 to 15:00 (typically 12:00-13:30)
     * - DINNER: after 15:00 (typically 18:00-19:00)
     *
     * @param time the time to classify
     * @return the corresponding MealBoundary
     */
    private DefaultMealsSelectionSection.MealBoundary determineMealFromTime(LocalTime time) {
        if (time == null) {
            return DefaultMealsSelectionSection.MealBoundary.BREAKFAST; // Default to breakfast (start of day)
        }

        LocalTime lunchStart = LocalTime.of(10, 0);   // Lunch starts at 10:00
        LocalTime dinnerStart = LocalTime.of(15, 0);  // Dinner starts at 15:00

        if (time.isBefore(lunchStart)) {
            return DefaultMealsSelectionSection.MealBoundary.BREAKFAST;
        } else if (time.isBefore(dinnerStart)) {
            return DefaultMealsSelectionSection.MealBoundary.LUNCH;
        } else {
            return DefaultMealsSelectionSection.MealBoundary.DINNER;
        }
    }

    /**
     * Determines which meal boundary corresponds to a ScheduledBoundary.
     * Uses the actual ScheduledItem from the boundary to identify the meal type.
     * Falls back to time-based detection if the boundary item is not a meal.
     *
     * @param boundary the ScheduledBoundary from an EventPart
     * @param fallbackTime the time to use if the boundary item is not a meal
     * @return the corresponding MealBoundary
     */
    private DefaultMealsSelectionSection.MealBoundary determineMealFromBoundary(
            one.modality.base.shared.entities.ScheduledBoundary boundary, LocalTime fallbackTime) {
        if (boundary == null) {
            Console.log("USFestivalInPersonBookingForm: ScheduledBoundary is null, falling back to time-based detection");
            return determineMealFromTime(fallbackTime);
        }

        one.modality.base.shared.entities.ScheduledItem scheduledItem = boundary.getScheduledItem();
        if (scheduledItem == null) {
            Console.log("USFestivalInPersonBookingForm: Boundary ScheduledItem is null, falling back to time-based detection");
            return determineMealFromTime(fallbackTime);
        }

        // Check if the boundary item is a meal
        boolean isMeal = one.modality.base.shared.entities.util.ScheduledItems.isOfFamily(
            scheduledItem, one.modality.base.shared.knownitems.KnownItemFamily.MEALS);

        if (!isMeal) {
            Console.log("USFestivalInPersonBookingForm: Boundary item '" + scheduledItem.getName() +
                "' is not a meal, falling back to time-based detection");
            return determineMealFromTime(fallbackTime);
        }

        // Determine meal type from item name
        String itemName = scheduledItem.getName();
        if (itemName == null) {
            // Try to get from the Item entity
            one.modality.base.shared.entities.Item item = scheduledItem.getItem();
            if (item != null) {
                itemName = item.getName();
            }
        }

        if (itemName != null) {
            String lowerName = itemName.toLowerCase();
            Console.log("USFestivalInPersonBookingForm: Boundary meal item name: '" + itemName + "'");

            if (lowerName.contains("breakfast") || lowerName.contains("morning")) {
                return DefaultMealsSelectionSection.MealBoundary.BREAKFAST;
            } else if (lowerName.contains("lunch") || lowerName.contains("midday")) {
                return DefaultMealsSelectionSection.MealBoundary.LUNCH;
            } else if (lowerName.contains("dinner") || lowerName.contains("supper") || lowerName.contains("evening")) {
                return DefaultMealsSelectionSection.MealBoundary.DINNER;
            }
        }

        // If we couldn't determine from name, fall back to time-based detection
        Console.log("USFestivalInPersonBookingForm: Could not determine meal type from item name '" + itemName +
            "', falling back to time-based detection");
        return determineMealFromTime(fallbackTime);
    }

    /**
     * Creates a Period with actual times derived from the boundary ScheduledItems.
     * The EventPart's times may be null (defaulting to MIN/MAX), so we derive the actual
     * times from the boundary ScheduledItems which reference specific meals.
     *
     * @param eventPart the main EventPart
     * @param startBoundary the start ScheduledBoundary
     * @param endBoundary the end ScheduledBoundary
     * @param mealItems list of meal ScheduledItems for fallback lookup
     * @return a Period with actual boundary times
     */
    private one.modality.base.shared.entities.Period createMainEventPeriodWithBoundaryTimes(
            one.modality.base.shared.entities.EventPart eventPart,
            one.modality.base.shared.entities.ScheduledBoundary startBoundary,
            one.modality.base.shared.entities.ScheduledBoundary endBoundary,
            java.util.List<one.modality.base.shared.entities.ScheduledItem> mealItems) {

        // Get dates from the EventPart
        LocalDate startDate = eventPart.getStartDate();
        LocalDate endDate = eventPart.getEndDate();

        // Get times from the boundary ScheduledItems using the utility
        LocalTime startTime = one.modality.base.shared.entities.util.ScheduledBoundaries.getTime(startBoundary, false);
        LocalTime endTime = one.modality.base.shared.entities.util.ScheduledBoundaries.getTime(endBoundary, true);

        // Debug logging to understand what data is available
        Console.log("USFestivalInPersonBookingForm: Creating main event period with boundary times:");
        if (startBoundary != null) {
            one.modality.base.shared.entities.ScheduledItem startItem = startBoundary.getScheduledItem();
            Console.log("  startBoundary: scheduledItem=" + (startItem != null ? startItem.getName() : "null") +
                ", atStartTime=" + startBoundary.isAtStartTime());
            if (startItem != null) {
                Console.log("    startItem times: start=" + startItem.getStartTime() + ", end=" + startItem.getEndTime());
            }
        } else {
            Console.log("  startBoundary: null");
        }
        if (endBoundary != null) {
            one.modality.base.shared.entities.ScheduledItem endItem = endBoundary.getScheduledItem();
            Console.log("  endBoundary: scheduledItem=" + (endItem != null ? endItem.getName() : "null") +
                ", atStartTime=" + endBoundary.isAtStartTime());
            if (endItem != null) {
                Console.log("    endItem times: start=" + endItem.getStartTime() + ", end=" + endItem.getEndTime());
            }
        } else {
            Console.log("  endBoundary: null");
        }
        Console.log("  Derived times: startTime=" + startTime + ", endTime=" + endTime);

        // Create final variables for the anonymous class
        LocalDate finalStartDate = startDate;
        LocalTime finalStartTime = startTime;
        LocalDate finalEndDate = endDate;
        LocalTime finalEndTime = endTime;

        return new one.modality.base.shared.entities.Period() {
            @Override public LocalDate getStartDate() { return finalStartDate; }
            @Override public LocalTime getStartTime() { return finalStartTime; }
            @Override public LocalDate getEndDate() { return finalEndDate; }
            @Override public LocalTime getEndTime() { return finalEndTime; }
        };
    }

    /**
     * Adds the Share Accommodation option with WorkingBooking-calculated price.
     * For people sharing a room with someone who is booking the accommodation.
     * Uses the sharing accommodation Item from PolicyAggregate (item with share_mate=true).
     */
    private void addShareAccommodationOptionWithPrice(PolicyAggregate policyAggregate, LocalDate arrivalDate, LocalDate departureDate) {
        if (accommodationSection == null) return;

        // Get sharing accommodation item from PolicyAggregate
        one.modality.base.shared.entities.ItemPolicy sharingAccommodationItemPolicy = policyAggregate.getSharingAccommodationItemPolicy();
        if (sharingAccommodationItemPolicy == null) {
            Console.log("USFestivalInPersonBookingForm: No sharing accommodation item configured - skipping Share Accommodation option");
            return;
        }
        Item sharingAccommodationItem = sharingAccommodationItemPolicy.getItem();
        if (sharingAccommodationItem == null) {
            Console.log("USFestivalInPersonBookingForm: Sharing accommodation ItemPolicy has no item - skipping");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Found sharing accommodation item: " + sharingAccommodationItem.getName());

        // Calculate price and breakdown using the sharing accommodation item
        AccommodationPriceResult priceResult = calculateShareAccommodationPriceWithWorkingBooking(policyAggregate, sharingAccommodationItem, arrivalDate, departureDate);
        Console.log("USFestivalInPersonBookingForm: Calculated Share Accommodation price: " + priceResult.totalPrice);

        // Store the breakdown for this option using the item's primary key
        accommodationSection.setBreakdownForOption(sharingAccommodationItem.getPrimaryKey(), priceResult.breakdown);

        // Get price per night from rate (if any)
        one.modality.base.shared.entities.Rate itemRate = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, sharingAccommodationItem)
            .findFirst()
            .orElse(null);
        int pricePerNight = itemRate != null && itemRate.getPrice() != null ? itemRate.getPrice() : 0;

        HasAccommodationSelectionSection.AccommodationOption shareAccommodation =
            new HasAccommodationSelectionSection.AccommodationOption(
                sharingAccommodationItem.getPrimaryKey(),  // Use actual Item's primary key
                sharingAccommodationItem,  // Actual Item entity from database
                sharingAccommodationItem.getName() != null ? sharingAccommodationItem.getName() : I18n.getI18nText(USFestivalI18nKeys.ShareAccommodation),
                I18n.getI18nText(USFestivalI18nKeys.ShareAccommodationDescription),
                pricePerNight,  // pricePerNight from rate
                HasAccommodationSelectionSection.AvailabilityStatus.AVAILABLE,
                HasAccommodationSelectionSection.ConstraintType.NONE,
                null,
                0,
                false,  // isDayVisitor = false (we book actual sharing accommodation item)
                null,   // imageUrl
                true,   // perPerson
                priceResult.totalPrice  // pre-calculated total price
            );

        accommodationSection.addAccommodationOption(shareAccommodation);
        Console.log("USFestivalInPersonBookingForm: Share Accommodation option added with item '" + sharingAccommodationItem.getName() + "' and price: " + priceResult.totalPrice);
    }

    /**
     * Adds the Day Visitor option with WorkingBooking-calculated price.
     * Day Visitors don't have accommodation items - they only book meals and teachings.
     */
    private void addDayVisitorOptionWithPrice(PolicyAggregate policyAggregate, LocalDate arrivalDate, LocalDate departureDate) {
        if (accommodationSection == null) return;

        // Calculate price and breakdown (teachings + meals, no accommodation, no breakfast since no overnight stay)
        // Day visitor has no accommodation availability to track (pass null)
        AccommodationPriceResult priceResult = calculateAccommodationPriceWithWorkingBooking(policyAggregate, null, arrivalDate, departureDate, null);
        Console.log("USFestivalInPersonBookingForm: Calculated Day Visitor price: " + priceResult.totalPrice);

        // Store the breakdown for this option
        accommodationSection.setBreakdownForOption("DAY_VISITOR", priceResult.breakdown);

        HasAccommodationSelectionSection.AccommodationOption dayVisitor =
            new HasAccommodationSelectionSection.AccommodationOption(
                "DAY_VISITOR",  // special itemId to identify this option
                null,  // no itemEntity
                I18n.getI18nText(USFestivalI18nKeys.DayVisitor),
                I18n.getI18nText(USFestivalI18nKeys.DayVisitorDescription),
                0,     // pricePerNight = 0
                HasAccommodationSelectionSection.AvailabilityStatus.AVAILABLE,
                HasAccommodationSelectionSection.ConstraintType.NONE,
                null,
                0,
                true,  // isDayVisitor = true
                null,  // imageUrl
                true,  // perPerson
                priceResult.totalPrice  // pre-calculated total price
            );

        accommodationSection.addAccommodationOption(dayVisitor);
        Console.log("USFestivalInPersonBookingForm: Day Visitor option added with price: " + priceResult.totalPrice);
    }

    // ========================================
    // Festival Days Population
    // ========================================

    private boolean festivalDaysPopulated = false;

    /**
     * Populates festival days from PolicyAggregate data.
     * Creates FestivalDay objects for each day from event.startDate to event.endDate.
     */
    private void populateFestivalDays() {
        // Skip if already populated
        if (festivalDaysPopulated) {
            Console.log("USFestivalInPersonBookingForm: Festival days already populated, skipping");
            return;
        }

        if (festivalDaySection == null) {
            Console.log("USFestivalInPersonBookingForm: festivalDaySection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBookingProperties not available yet for festival days");
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not initialized yet for festival days");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available yet for festival days");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Populating festival days from PolicyAggregate");

        // Find the main EventPart to get accurate boundary dates
        // The main EventPart excludes early arrival and late departure periods
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        // Set main event boundaries on festival day section for correct time option filtering
        // This must be done BEFORE populateFromPolicyAggregate so the time options are correct
        if (mainEventPart != null) {
            LocalDate mainStartDate = mainEventPart.getStartDate();
            LocalDate mainEndDate = mainEventPart.getEndDate();

            Console.log("USFestivalInPersonBookingForm: Setting festival day section main event boundaries: " +
                mainStartDate + " to " + mainEndDate);

            if (mainStartDate != null) {
                festivalDaySection.setMainEventStartDate(mainStartDate);
            }
            if (mainEndDate != null) {
                festivalDaySection.setMainEventEndDate(mainEndDate);
            }
        }

        // Check if there's a late departure part
        one.modality.base.shared.entities.EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();
        // Also check manually by comparing EventPart dates to main event dates
        // (PolicyAggregate.getLateDeparturePart() may not work if event dates include early/late)
        if (lateDeparturePart == null && mainEventPart != null) {
            // Find EventPart that starts AFTER the main event ends (late departure)
            dev.webfx.stack.orm.entity.EntityList<one.modality.base.shared.entities.EventPart> eventParts = policyAggregate.getEventParts();
            LocalDate mainEndDate = mainEventPart.getEndDate();
            if (eventParts != null && mainEndDate != null) {
                for (one.modality.base.shared.entities.EventPart part : eventParts) {
                    // Skip the main event part
                    boolean isMainPart = dev.webfx.stack.orm.entity.Entities.samePrimaryKey(part, mainEventPart);
                    if (isMainPart) {
                        continue;
                    }

                    // Check if this part's start date is after the main event's end date
                    // This identifies it as a late departure part
                    LocalDate partStartDate = part.getStartDate();
                    if (partStartDate != null && !partStartDate.isBefore(mainEndDate)) {
                        lateDeparturePart = part;
                        Console.log("USFestivalInPersonBookingForm: Found late departure part by date: " + part.getName() +
                            " (starts " + partStartDate + " which is on or after main event end " + mainEndDate + ")");
                        break;
                    }
                }
            }
        }

        festivalDaySection.setHasLateDeparturePart(lateDeparturePart != null);

        // Set the late departure end date so the calendar includes those days
        if (lateDeparturePart != null) {
            LocalDate lateDepartureEndDate = lateDeparturePart.getEndDate();
            if (lateDepartureEndDate != null) {
                festivalDaySection.setLateDepartureEndDate(lateDepartureEndDate);
                Console.log("USFestivalInPersonBookingForm: Late departure end date: " + lateDepartureEndDate);
            }
        }

        Console.log("USFestivalInPersonBookingForm: Late departure part: " +
            (lateDeparturePart != null ? lateDeparturePart.getName() : "none"));

        // Populate festival days from PolicyAggregate
        festivalDaySection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        festivalDaysPopulated = true;
        Console.log("USFestivalInPersonBookingForm: Festival days populated successfully");
    }

    // ========================================
    // Meals Options Population
    // ========================================

    private boolean mealsOptionsPopulated = false;

    /**
     * Populates meals options from PolicyAggregate data.
     * Extracts meal items and their daily rates.
     */
    private void populateMealsOptions() {
        // Skip if already populated
        if (mealsOptionsPopulated) {
            Console.log("USFestivalInPersonBookingForm: Meals options already populated, skipping");
            return;
        }

        if (mealsSection == null) {
            Console.log("USFestivalInPersonBookingForm: mealsSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBookingProperties not available yet for meals");
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not initialized yet for meals");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available yet for meals");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Populating meals options from PolicyAggregate");

        // Find the main EventPart to get accurate boundary dates and times
        // The main EventPart excludes early arrival and late departure periods
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        // Set event boundary dates and meal boundaries on meals section for early arrival/late departure pricing
        // This must be done BEFORE populating from PolicyAggregate so rates can be classified correctly
        if (mainEventPart != null) {
            LocalDate mainStartDate = mainEventPart.getStartDate();
            LocalDate mainEndDate = mainEventPart.getEndDate();
            LocalTime mainStartTime = mainEventPart.getStartTime();
            LocalTime mainEndTime = mainEventPart.getEndTime();

            Console.log("USFestivalInPersonBookingForm: Main EventPart boundaries - " +
                "start: " + mainStartDate + " " + mainStartTime +
                ", end: " + mainEndDate + " " + mainEndTime);

            if (mainStartDate != null) {
                mealsSection.setEventBoundaryStartDate(mainStartDate);
                Console.log("USFestivalInPersonBookingForm: Set meals section event boundary start date: " + mainStartDate);
            }
            if (mainEndDate != null) {
                mealsSection.setEventBoundaryEndDate(mainEndDate);
                Console.log("USFestivalInPersonBookingForm: Set meals section event boundary end date: " + mainEndDate);
            }

            // Get meal ScheduledItems for isInPeriod checks (needed before creating period)
            java.util.List<one.modality.base.shared.entities.ScheduledItem> mealItems =
                policyAggregate.filterScheduledItemsOfFamily(one.modality.base.shared.knownitems.KnownItemFamily.MEALS);
            mealsSection.setMealScheduledItems(mealItems);
            Console.log("USFestivalInPersonBookingForm: Set meals section meal ScheduledItems: " + mealItems.size() + " items");

            // Determine which meal marks the start of the main event from the ScheduledBoundary
            // The boundary's ScheduledItem tells us exactly what item marks the boundary
            one.modality.base.shared.entities.ScheduledBoundary startBoundary = mainEventPart.getStartBoundary();
            DefaultMealsSelectionSection.MealBoundary startMeal = determineMealFromBoundary(startBoundary, mainStartTime);
            mealsSection.setEventBoundaryStartMeal(startMeal);
            Console.log("USFestivalInPersonBookingForm: Set meals section event boundary start meal: " + startMeal);

            // Determine which meal marks the end of the main event from the ScheduledBoundary
            one.modality.base.shared.entities.ScheduledBoundary endBoundary = mainEventPart.getEndBoundary();
            DefaultMealsSelectionSection.MealBoundary endMeal = determineMealFromBoundary(endBoundary, mainEndTime);
            mealsSection.setEventBoundaryEndMeal(endMeal);
            Console.log("USFestivalInPersonBookingForm: Set meals section event boundary end meal: " + endMeal);

            // Create a Period with actual times from the boundary ScheduledItems
            // The EventPart's times may be null, so we derive times from the boundary meals
            one.modality.base.shared.entities.Period mainEventPeriodWithTimes = createMainEventPeriodWithBoundaryTimes(
                mainEventPart, startBoundary, endBoundary, mealItems);
            mealsSection.setMainEventPeriod(mainEventPeriodWithTimes);
            Console.log("USFestivalInPersonBookingForm: Set meals section main event period with boundary times");
        } else {
            // Fallback to event dates if no main EventPart found
            Console.log("USFestivalInPersonBookingForm: No main EventPart found, using event dates as boundaries");
            if (eventStartDate != null) {
                mealsSection.setEventBoundaryStartDate(eventStartDate);
                Console.log("USFestivalInPersonBookingForm: Set meals section event boundary start: " + eventStartDate);
            }
            if (eventEndDate != null) {
                mealsSection.setEventBoundaryEndDate(eventEndDate);
                Console.log("USFestivalInPersonBookingForm: Set meals section event boundary end: " + eventEndDate);
            }
            // Pass meal ScheduledItems for isInPeriod checks (fallback case)
            java.util.List<one.modality.base.shared.entities.ScheduledItem> mealItems =
                policyAggregate.filterScheduledItemsOfFamily(one.modality.base.shared.knownitems.KnownItemFamily.MEALS);
            mealsSection.setMealScheduledItems(mealItems);
            Console.log("USFestivalInPersonBookingForm: Set meals section meal ScheduledItems: " + mealItems.size() + " items");
        }

        // Populate meals options from PolicyAggregate
        mealsSection.populateFromPolicyAggregate(policyAggregate);

        // Set WorkingBooking for DocumentBill-based pricing
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        if (workingBooking != null) {
            mealsSection.setWorkingBooking(workingBooking);
            Console.log("USFestivalInPersonBookingForm: Set WorkingBooking on mealsSection for DocumentBill pricing");
        }

        // Mark as populated to avoid re-populating
        mealsOptionsPopulated = true;
        Console.log("USFestivalInPersonBookingForm: Meals options populated successfully");
    }

    // ========================================
    // Audio Recording Phase Coverage Population
    // ========================================

    private boolean audioRecordingPhasePopulated = false;

    /**
     * Populates audio recording phase coverage options from PolicyAggregate data.
     * Shows the section only if there are multiple phase coverages available.
     */
    private void populateAudioRecordingPhaseOptions() {
        // Skip if already populated
        if (audioRecordingPhasePopulated) {
            Console.log("USFestivalInPersonBookingForm: Audio recording phase options already populated, skipping");
            return;
        }

        if (audioRecordingPhaseSection == null) {
            Console.log("USFestivalInPersonBookingForm: audioRecordingPhaseSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not available yet for audio recording phase options");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available yet for audio recording phase options");
            return;
        }

        // Check if there are multiple phase coverages for audio recording
        java.util.List<EventPhaseCoverage> phaseCoverages =
            policyAggregate.getAudioRecordingPhaseCoverages();

        if (phaseCoverages == null || phaseCoverages.size() <= 1) {
            // No phase coverages or only one - hide section and let additional options handle audio recording
            Console.log("USFestivalInPersonBookingForm: " + (phaseCoverages == null ? 0 : phaseCoverages.size()) +
                " audio recording phase coverages found - hiding phase section");
            audioRecordingPhaseSection.setVisible(false);
            audioRecordingPhasePopulated = true;
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Populating audio recording phase options from PolicyAggregate");

        // Set workingBookingProperties on the section (needed for price calculation)
        audioRecordingPhaseSection.setWorkingBookingProperties(workingBookingProperties);

        // Populate the section from PolicyAggregate
        audioRecordingPhaseSection.populateFromPolicyAggregate(policyAggregate);

        // Show the section
        audioRecordingPhaseSection.setVisible(true);

        // Mark as populated to avoid re-populating
        audioRecordingPhasePopulated = true;
        Console.log("USFestivalInPersonBookingForm: Audio recording phase options populated successfully with " +
            phaseCoverages.size() + " options");
    }

    // ========================================
    // Additional Options Population
    // ========================================

    private boolean additionalOptionsPopulated = false;

    /**
     * Populates additional options (parking, shuttle) from PolicyAggregate data.
     * Extracts parking and transport items and their rates.
     */
    private void populateAdditionalOptions() {
        // Skip if already populated
        if (additionalOptionsPopulated) {
            Console.log("USFestivalInPersonBookingForm: Additional options already populated, skipping");
            return;
        }

        if (additionalOptionsSection == null) {
            Console.log("USFestivalInPersonBookingForm: additionalOptionsSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBookingProperties not available yet for additional options");
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not initialized yet for additional options");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available yet for additional options");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Populating additional options from PolicyAggregate");

        // Populate transport section first (parking + shuttle items)
        if (transportSection != null) {
            // Sync arrival/departure dates to transport section before populating
            // (ensures shuttle availability is calculated with correct dates)
            if (festivalDaySection != null) {
                LocalDate arrivalDate = festivalDaySection.arrivalDateProperty().get();
                LocalDate departureDate = festivalDaySection.departureDateProperty().get();
                if (arrivalDate != null) {
                    Console.log("USFestivalInPersonBookingForm: Syncing transport arrival date before populate - " + arrivalDate);
                    transportSection.setArrivalDate(arrivalDate);
                }
                if (departureDate != null) {
                    Console.log("USFestivalInPersonBookingForm: Syncing transport departure date before populate - " + departureDate);
                    transportSection.setDepartureDate(departureDate);
                }
            }
            transportSection.populateFromPolicyAggregate(policyAggregate);
            boolean hasTransport = transportSection.hasAnyOptions();
            transportSection.setVisible(hasTransport);
            Console.log("USFestivalInPersonBookingForm: Transport options populated - hasAnyOptions=" + hasTransport +
                " (parking=" + transportSection.hasParkingOptions() + ", shuttle=" + transportSection.hasShuttleOptions() + ")");

            // Exclude parking and transport items from additional options (handled by transport section)
            additionalOptionsSection.setExcludeParkingAndTransport(hasTransport);
            Console.log("USFestivalInPersonBookingForm: Parking and transport excluded from additional options: " + hasTransport);
        }

        // Exclude audio recording items if they're handled by the dedicated phase section
        boolean audioPhaseVisible = audioRecordingPhaseSection != null && audioRecordingPhaseSection.getView().isVisible();
        additionalOptionsSection.setExcludeAudioRecording(audioPhaseVisible);
        Console.log("USFestivalInPersonBookingForm: Audio recording excluded from additional options: " + audioPhaseVisible);

        // Populate additional options from PolicyAggregate
        additionalOptionsSection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        additionalOptionsPopulated = true;
        Console.log("USFestivalInPersonBookingForm: Additional options populated successfully");
    }

    /**
     * Calculates the full event meals price from PolicyAggregate.
     * - Lunch/Dinner: multiplied by teaching days
     * - Breakfast: multiplied by nights (served morning after each overnight stay)
     *
     * @param policyAggregate the policy data containing rates
     * @param teachingDays number of teaching days
     * @param nights number of accommodation nights
     * @return total meals price in cents
     */
    private int calculateFullEventMealsPrice(PolicyAggregate policyAggregate, int teachingDays, int nights) {
        if (policyAggregate == null) {
            return 0;
        }

        // Get all meals ScheduledItems to find unique meal items
        var mealsScheduledItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);

        int totalMealsPrice = 0;
        java.util.Set<Object> processedItems = new java.util.HashSet<>();

        // For each unique meals item, get its daily rate and multiply by appropriate days
        for (ScheduledItem si : mealsScheduledItems) {
            var item = si.getItem();
            if (item == null || processedItems.contains(item.getPrimaryKey())) {
                continue;
            }
            processedItems.add(item.getPrimaryKey());

            String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
            boolean isBreakfast = itemName.contains("breakfast") || itemName.contains("morning");

            // Get rate for this meals item
            // Try daily rates first, then fallback to searching all rates (including fixed rates)
            int dailyMealRate = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, item)
                .findFirst()
                .map(rate -> rate.getPrice() != null ? rate.getPrice() : 0)
                .orElseGet(() -> {
                    // Fallback 1: search all daily rates for this item regardless of site
                    Integer rate = policyAggregate.getDailyRatesStream()
                        .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                            && r.getItem().getPrimaryKey().equals(item.getPrimaryKey()))
                        .findFirst()
                        .map(r -> r.getPrice() != null ? r.getPrice() : 0)
                        .orElse(null);
                    if (rate != null) return rate;

                    // Fallback 2: search ALL rates (including fixed rates) for this item
                    return policyAggregate.getRatesStream()
                        .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                            && r.getItem().getPrimaryKey().equals(item.getPrimaryKey()))
                        .findFirst()
                        .map(r -> r.getPrice() != null ? r.getPrice() : 0)
                        .orElse(0);
                });

            // Breakfast: multiply by nights (one breakfast per overnight stay)
            // Lunch/Dinner: multiply by teaching days
            int daysToCharge = isBreakfast ? nights : teachingDays;
            if (daysToCharge > 0) {
                totalMealsPrice += dailyMealRate * daysToCharge;
                Console.log("USFestivalInPersonBookingForm: Meal item '" + item.getName() + "' rate=" + dailyMealRate + "/day * " + daysToCharge + " = " + (dailyMealRate * daysToCharge));
            }
        }

        return totalMealsPrice;
    }

    // ========================================
    // StandardBookingFormCallbacks Implementation
    // ========================================

    @Override
    public void onAfterLogin() {
        Person person = FXUserPerson.getUserPerson();
        if (person != null && memberSelectionSection != null) {
            HouseholdMemberLoader.loadMembersAsync(person, memberSelectionSection, settings.event());
        }
    }

    @Override
    public void onBeforeSummary() {
        // Book selected items into WorkingBooking BEFORE summary is displayed
        // This ensures the price breakdown shows all selected options
        bookSelectedItemsIntoWorkingBooking();
    }

    @Override
    public void onPrepareNewBooking() {
        Console.log("USFestivalInPersonBookingForm.onPrepareNewBooking() - Reloading availability for new booking");

        // Reset the flag so accommodation options will be repopulated
        accommodationOptionsPopulated = false;

        // Reload availability from server to get fresh room counts
        PolicyAggregate policyAggregate = workingBookingProperties != null ? workingBookingProperties.getPolicyAggregate() : null;
        if (policyAggregate != null) {
            policyAggregate.reloadAvailabilities()
                .onSuccess(v -> {
                    Console.log("USFestivalInPersonBookingForm: Availability reloaded successfully");
                    // Repopulate accommodation options with fresh availability data on the UI thread
                    UiScheduler.runInUiThread(() -> {
                        Console.log("USFestivalInPersonBookingForm: Repopulating accommodation options on UI thread");
                        populateAccommodationOptions();
                    });
                })
                .onFailure(e -> {
                    Console.log("USFestivalInPersonBookingForm: Failed to reload availability: " + e.getMessage());
                    // Still try to repopulate with existing data on the UI thread
                    UiScheduler.runInUiThread(() -> {
                        Console.log("USFestivalInPersonBookingForm: Repopulating accommodation options (fallback) on UI thread");
                        populateAccommodationOptions();
                    });
                });
        } else {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available, skipping availability reload");
        }
    }

    @Override
    public void onAccommodationSoldOutRecovery(HasAccommodationSelectionSection.AccommodationOption newOption, StandardBookingFormCallbacks.SoldOutRecoveryRoommateInfo roommateInfo, Runnable continueToSummary) {
        Console.log("USFestivalInPersonBookingForm.onAccommodationSoldOutRecovery() - new option: " + newOption.getName());

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not available");
            continueToSummary.run();
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();

        // Step 1: Collect the attendance dates from old accommodation BEFORE removing
        java.util.Set<LocalDate> oldAccommodationDates = new java.util.HashSet<>();
        List<DocumentLine> oldAccommodationLines = workingBooking.getFamilyDocumentLines(KnownItemFamily.ACCOMMODATION);
        for (DocumentLine line : oldAccommodationLines) {
            List<Attendance> attendances = workingBooking.getLastestDocumentAggregate().getLineAttendances(line);
            for (Attendance attendance : attendances) {
                if (attendance.getDate() != null) {
                    oldAccommodationDates.add(attendance.getDate());
                }
            }
        }
        Console.log("USFestivalInPersonBookingForm: Old accommodation dates: " + oldAccommodationDates);

        // Compute arrival/departure dates from old accommodation dates
        // Arrival = min date, Departure = max date + 1 (since accommodation nights are before departure)
        LocalDate arrivalDate = null;
        LocalDate departureDate = null;
        if (!oldAccommodationDates.isEmpty()) {
            arrivalDate = oldAccommodationDates.stream().min(LocalDate::compareTo).orElse(null);
            LocalDate maxDate = oldAccommodationDates.stream().max(LocalDate::compareTo).orElse(null);
            departureDate = maxDate != null ? maxDate.plusDays(1) : null;
            Console.log("USFestivalInPersonBookingForm: Computed arrival=" + arrivalDate + ", departure=" + departureDate);
        }

        // Step 2: Remove the old accommodation document lines (keep teaching, meals, etc.)
        Console.log("USFestivalInPersonBookingForm: Removing " + oldAccommodationLines.size() + " old accommodation lines");
        for (DocumentLine line : oldAccommodationLines) {
            workingBooking.removeDocumentLine(line);
        }

        // Step 3: Update the accommodation section with the new selection
        // Directly set the selectedOptionProperty to newOption, bypassing setSelectedOption()
        // which may fail to match itemId if the newOption was created in a different context
        if (accommodationSection != null) {
            Console.log("USFestivalInPersonBookingForm: Setting accommodation directly to: " + newOption.getName());
            accommodationSection.selectedOptionProperty().set(newOption);

            // Update festival day section constraints and dates based on new accommodation
            if (festivalDaySection != null) {
                festivalDaySection.setMinNightsConstraint(newOption.getMinNights());
                festivalDaySection.setIsDayVisitor(newOption.isDayVisitor());
                // Preserve the original arrival/departure dates so onBeforeSummary() uses them
                if (arrivalDate != null) {
                    festivalDaySection.arrivalDateProperty().set(arrivalDate);
                    Console.log("USFestivalInPersonBookingForm: Set festivalDaySection arrivalDate to " + arrivalDate);
                }
                if (departureDate != null) {
                    festivalDaySection.departureDateProperty().set(departureDate);
                    Console.log("USFestivalInPersonBookingForm: Set festivalDaySection departureDate to " + departureDate);
                }
            }
        }

        // Step 4: Book the new accommodation using the same dates as the old one
        if (policyAggregate != null && !oldAccommodationDates.isEmpty()) {
            Item newItem = newOption.getItemEntity();
            if (newItem != null) {
                // Find ScheduledItems for the new accommodation item that match the old dates
                List<ScheduledItem> newAccommodationItems = policyAggregate.filterAccommodationScheduledItems().stream()
                    .filter(si -> dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), newItem))
                    .filter(si -> si.getDate() != null && oldAccommodationDates.contains(si.getDate()))
                    .collect(java.util.stream.Collectors.toList());

                Console.log("USFestivalInPersonBookingForm: Booking " + newAccommodationItems.size() + " new accommodation items for dates: " + oldAccommodationDates);
                if (!newAccommodationItems.isEmpty()) {
                    workingBooking.bookScheduledItems(newAccommodationItems, false);
                }
            }
        }

        // Step 5: Apply roommate info to the new accommodation lines if provided
        if (roommateInfo != null && roommateInfo.hasData()) {
            List<DocumentLine> newAccommodationLines = workingBooking.getFamilyDocumentLines(KnownItemFamily.ACCOMMODATION);
            if (roommateInfo.isRoomBooker()) {
                // Room booker mode: set roommate names
                String[] matesNames = roommateInfo.getRoommateNames().toArray(new String[0]);
                for (DocumentLine line : newAccommodationLines) {
                    workingBooking.setShareOwnerInfo(line, matesNames);
                }
                Console.log("USFestivalInPersonBookingForm: Set roommate names on new accommodation");
            } else {
                // Share accommodation mode: set room owner name
                String ownerName = roommateInfo.getRoomOwnerName();
                for (DocumentLine line : newAccommodationLines) {
                    workingBooking.setShareMateInfo(line, ownerName);
                }
                Console.log("USFestivalInPersonBookingForm: Set room owner name on new accommodation");
            }
        }

        Console.log("USFestivalInPersonBookingForm: New accommodation booked");

        // Step 6: Navigate to summary page to review and retry submission
        continueToSummary.run();
    }

    /**
     * Books all selected items (teaching, accommodation, meals, additional options)
     * into the WorkingBooking so they appear in the price breakdown.
     */
    private void bookSelectedItemsIntoWorkingBooking() {
        Console.log("USFestivalBookingForm.bookSelectedItemsIntoWorkingBooking() - Booking selected items");

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalInPersonBookingForm: WorkingBooking not available, skipping item booking");
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalInPersonBookingForm: PolicyAggregate not available, skipping item booking");
            return;
        }

        // Book teaching items for selected festival days
        bookTeachingItems(workingBooking, policyAggregate);

        // Book selected accommodation
        bookAccommodationItems(workingBooking, policyAggregate);

        // Book selected meals (filtered by arrival/departure time)
        bookMealsItems(workingBooking, policyAggregate);

        // Book dietary preference (vegetarian, vegan, etc.)
        bookDietaryItem(workingBooking, policyAggregate);

        // Book audio recording items based on selected phase coverage
        bookAudioRecordingItems(workingBooking, policyAggregate);

        // Book selected additional options (parking, shuttle, assisted listening, etc.)
        bookAdditionalOptionsItems(workingBooking, policyAggregate);

        // Store arrival/departure time and dietary preference in document request
        storeBookingDetailsInRequest(workingBooking);

        // Update meals section with DocumentBill pricing
        // This computes prices from the actual booked items, accounting for
        // date-specific rates (early arrival, late departure have different prices)
        if (mealsSection != null) {
            mealsSection.populateFromDocumentBill();
        }

        Console.log("USFestivalBookingForm.bookSelectedItemsIntoWorkingBooking() - All selected items booked");
    }

    /**
     * Stores arrival/departure time and other booking details in the document request.
     * This information is stored as a formatted string for reference by back-office staff.
     */
    private void storeBookingDetailsInRequest(WorkingBooking workingBooking) {
        StringBuilder requestText = new StringBuilder();

        // Get arrival/departure dates and times from festival day section
        if (festivalDaySection != null) {
            LocalDate arrivalDate = festivalDaySection.getArrivalDate();
            LocalDate departureDate = festivalDaySection.getDepartureDate();
            HasFestivalDaySelectionSection.ArrivalDepartureTime arrivalTime = festivalDaySection.arrivalTimeProperty().get();
            HasFestivalDaySelectionSection.ArrivalDepartureTime departureTime = festivalDaySection.departureTimeProperty().get();

            if (arrivalDate != null && arrivalTime != null) {
                requestText.append("Arrival: ").append(arrivalDate).append(" (").append(arrivalTime.name().toLowerCase()).append(")\n");
            }
            if (departureDate != null && departureTime != null) {
                requestText.append("Departure: ").append(departureDate).append(" (").append(departureTime.name().toLowerCase()).append(")\n");
            }
        }

        // Get dietary preference from meals section
        if (mealsSection != null) {
            Item dietaryItem = mealsSection.getSelectedDietaryItem();
            if (dietaryItem != null && dietaryItem.getName() != null) {
                requestText.append("Dietary: ").append(dietaryItem.getName()).append("\n");
            } else {
                // Fallback to legacy dietary preference
                HasMealsSelectionSection.DietaryPreference dietPref = mealsSection.getDietaryPreference();
                if (dietPref != null) {
                    requestText.append("Dietary: ").append(dietPref.name().toLowerCase()).append("\n");
                }
            }
        }

        // Get additional options
        if (additionalOptionsSection != null) {
            if (additionalOptionsSection.needsAssistedListening()) {
                requestText.append("Assisted listening device requested\n");
            }
        }

        // Get parking info from transport section (new unified parking card)
        if (transportSection != null && transportSection.isParkingEnabled()) {
            HasTransportSection.ParkingOption selectedParkingType = transportSection.getSelectedParkingType();
            String parkingTypeName = selectedParkingType != null ? selectedParkingType.getName() : "Standard";
            requestText.append("Parking: ").append(parkingTypeName.toLowerCase()).append("\n");
        }

        // Get roommate info and set on accommodation document lines using WorkingBooking API
        if (roommateInfoSection != null && roommateInfoSection.isVisible()) {
            if (roommateInfoSection.isRoomBooker()) {
                // Room Booking mode: this person is the room owner, listing their roommates
                List<String> roommateNames = roommateInfoSection.getAllRoommateNames();
                if (!roommateNames.isEmpty()) {
                    // Add to request text for reference
                    requestText.append("Roommates: ");
                    for (int i = 0; i < roommateNames.size(); i++) {
                        if (i > 0) {
                            requestText.append(", ");
                        }
                        requestText.append(roommateNames.get(i));
                    }
                    requestText.append("\n");

                    // Set share owner info on accommodation document lines
                    setShareOwnerInfoOnDocumentLines(workingBooking, roommateNames.toArray(new String[0]));
                }
            } else {
                // Share Accommodation mode: this person is sharing someone else's room
                String ownerName = roommateInfoSection.getRoommateName();
                String registrationNumber = roommateInfoSection.getRegistrationNumber();
                if (ownerName != null && !ownerName.trim().isEmpty()) {
                    // Add to request text for reference
                    requestText.append("Sharing room with: ").append(ownerName.trim());
                    if (registrationNumber != null && !registrationNumber.trim().isEmpty()) {
                        requestText.append(" (Reg: ").append(registrationNumber.trim()).append(")");
                    }
                    requestText.append("\n");

                    // Set share mate info on accommodation document lines
                    setShareMateInfoOnDocumentLines(workingBooking, ownerName.trim());
                }
            }
        }

        // Add the request if we have any text
        if (requestText.length() > 0) {
            String request = requestText.toString().trim();
            Console.log("USFestivalInPersonBookingForm: Adding booking details to request: " + request);
            workingBooking.addRequest(request);
        }
    }

    /**
     * Sets share owner info on accommodation document lines.
     * Used when this person is booking the room and listing their roommates.
     *
     * @param workingBooking the working booking
     * @param matesNames array of roommate names
     */
    private void setShareOwnerInfoOnDocumentLines(WorkingBooking workingBooking, String[] matesNames) {
        if (workingBooking == null || matesNames == null || matesNames.length == 0) return;

        // Get accommodation document lines
        List<one.modality.base.shared.entities.DocumentLine> accommodationLines =
            workingBooking.getFamilyDocumentLines(KnownItemFamily.ACCOMMODATION);

        if (accommodationLines == null || accommodationLines.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No accommodation lines found for setShareOwnerInfo");
            return;
        }

        // Set share owner info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareOwnerInfo(line, matesNames);
            Console.log("USFestivalInPersonBookingForm: Set share owner info with " + matesNames.length + " mates on DocumentLine");
        }
    }

    /**
     * Sets share mate info on accommodation document lines.
     * Used when this person is sharing someone else's room.
     *
     * @param workingBooking the working booking
     * @param ownerName the name of the room owner
     */
    private void setShareMateInfoOnDocumentLines(WorkingBooking workingBooking, String ownerName) {
        if (workingBooking == null || ownerName == null) return;

        // Get accommodation document lines
        List<one.modality.base.shared.entities.DocumentLine> accommodationLines =
            workingBooking.getFamilyDocumentLines(KnownItemFamily.ACCOMMODATION);

        if (accommodationLines == null || accommodationLines.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No accommodation lines found for setShareMateInfo");
            return;
        }

        // Set share mate info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareMateInfo(line, ownerName);
            Console.log("USFestivalInPersonBookingForm: Set share mate info with owner '" + ownerName + "' on DocumentLine");
        }
    }

    /**
     * Books teaching ScheduledItems based on selected festival days.
     */
    private void bookTeachingItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        // Get all teaching scheduled items
        List<ScheduledItem> allTeachingItems = policyAggregate.filterTeachingScheduledItems();

        // Get arrival/departure dates from festival day section to filter teaching days
        LocalDate arrivalDate = null;
        LocalDate departureDate = null;
        if (festivalDaySection != null) {
            arrivalDate = festivalDaySection.getArrivalDate();
            departureDate = festivalDaySection.getDepartureDate();
        }

        // Filter teaching items by arrival/departure dates if available
        List<ScheduledItem> teachingItems;
        if (arrivalDate != null && departureDate != null) {
            final LocalDate arrival = arrivalDate;
            final LocalDate departure = departureDate;
            teachingItems = allTeachingItems.stream()
                .filter(si -> {
                    LocalDate siDate = si.getDate();
                    if (siDate == null) return false;
                    // Include teaching days from arrival date up to (but not including) departure date
                    // Since departure is checkout day, guest attends teaching until the day before
                    return !siDate.isBefore(arrival) && siDate.isBefore(departure);
                })
                .collect(java.util.stream.Collectors.toList());
            Console.log("USFestivalInPersonBookingForm: Filtered teaching items from " + allTeachingItems.size() + " to " + teachingItems.size() + " based on dates " + arrival + " to " + departure);
        } else {
            // No date filter available, book all teaching items
            teachingItems = allTeachingItems;
            Console.log("USFestivalInPersonBookingForm: No arrival/departure dates, booking all " + teachingItems.size() + " teaching items");
        }

        if (!teachingItems.isEmpty()) {
            workingBooking.bookScheduledItems(teachingItems, false);
        }
    }

    /**
     * Books sharing accommodation item using bookTemporalButNonScheduledItem.
     * This is used when the selected accommodation has share_mate=true, meaning the guest
     * is sharing a room with someone else who is booking the actual room.
     *
     * @param workingBooking the working booking
     * @param policyAggregate the policy data
     * @param sharingAccommodationItem the sharing accommodation Item (has share_mate=true)
     */
    private void bookShareAccommodationItem(WorkingBooking workingBooking, PolicyAggregate policyAggregate, Item sharingAccommodationItem) {
        Console.log("USFestivalInPersonBookingForm: Booking sharing accommodation item: " + sharingAccommodationItem.getName());

        // Get dates from festival day section
        LocalDate arrivalDate = null;
        LocalDate departureDate = null;

        if (festivalDaySection != null) {
            arrivalDate = festivalDaySection.getArrivalDate();
            departureDate = festivalDaySection.getDepartureDate();
        }

        // Fallback: compute from teaching dates if festival day section not available
        if (arrivalDate == null || departureDate == null) {
            List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();
            List<LocalDate> teachingDatesSorted = teachingItems.stream()
                .map(ScheduledItem::getDate)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            if (!teachingDatesSorted.isEmpty()) {
                arrivalDate = teachingDatesSorted.get(0);
                departureDate = teachingDatesSorted.get(teachingDatesSorted.size() - 1).plusDays(1);
                Console.log("USFestivalInPersonBookingForm: Using fallback dates from teaching for sharing accommodation: " + arrivalDate + " to " + departureDate);
            }
        }

        if (arrivalDate == null || departureDate == null) {
            Console.log("USFestivalInPersonBookingForm: No dates available for sharing accommodation - skipping");
            return;
        }

        // Build list of accommodation nights (arrival to departure-1)
        List<LocalDate> accommodationDates = new ArrayList<>();
        LocalDate current = arrivalDate;
        while (current.isBefore(departureDate)) {
            accommodationDates.add(current);
            current = current.plusDays(1);
        }

        if (accommodationDates.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No accommodation dates calculated - skipping");
            return;
        }

        // Get site from ItemPolicy (or use null for default)
        one.modality.base.shared.entities.Site site = null;
        one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(sharingAccommodationItem);
        if (itemPolicy != null && itemPolicy.getScope() != null) {
            site = itemPolicy.getScope().getSite();
        }

        // Book using bookTemporalButNonScheduledItem (since sharing accommodation has no ScheduledItem entries)
        workingBooking.bookTemporalButNonScheduledItem(site, sharingAccommodationItem, accommodationDates, false);
        Console.log("USFestivalInPersonBookingForm: Booked sharing accommodation for " + accommodationDates.size() + " nights");
    }

    /**
     * Books accommodation ScheduledItems based on selected room and date range.
     * Only books accommodation for nights within the selected arrival-departure range.
     * Handles sharing accommodation (share_mate=true) using bookTemporalButNonScheduledItem.
     */
    private void bookAccommodationItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (accommodationSection == null) return;

        HasAccommodationSelectionSection.AccommodationOption selectedOption = accommodationSection.getSelectedOption();
        if (selectedOption == null) {
            Console.log("USFestivalInPersonBookingForm: No accommodation selected - skipping accommodation booking");
            return;
        }

        // Day visitor - no accommodation booking
        if (selectedOption.isDayVisitor()) {
            Console.log("USFestivalInPersonBookingForm: Day visitor - skipping accommodation booking");
            return;
        }

        Item selectedItem = selectedOption.getItemEntity();
        if (selectedItem == null) {
            Console.log("USFestivalInPersonBookingForm: Selected accommodation has no itemEntity - skipping");
            return;
        }

        // Check if this is sharing accommodation (has share_mate=true flag)
        if (Boolean.TRUE.equals(selectedItem.isShare_mate())) {
            bookShareAccommodationItem(workingBooking, policyAggregate, selectedItem);
            return;
        }

        // Get the selected date range from festival day section or fallback to teaching dates
        // Accommodation nights = arrival date to (departure date - 1)
        java.util.Set<java.time.LocalDate> selectedNights = new java.util.HashSet<>();
        LocalDate arrivalDate = null;
        LocalDate departureDate = null;

        // Try to get dates from festival day section
        if (festivalDaySection != null) {
            arrivalDate = festivalDaySection.getArrivalDate();
            departureDate = festivalDaySection.getDepartureDate();
        }

        // Fallback: compute from teaching dates if festival day section not available
        if (arrivalDate == null || departureDate == null) {
            List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();
            List<LocalDate> teachingDatesSorted = teachingItems.stream()
                .map(ScheduledItem::getDate)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            if (!teachingDatesSorted.isEmpty()) {
                arrivalDate = teachingDatesSorted.get(0);
                departureDate = teachingDatesSorted.get(teachingDatesSorted.size() - 1).plusDays(1);
                Console.log("USFestivalInPersonBookingForm: Using fallback dates from teaching for accommodation: " + arrivalDate + " to " + departureDate);
            }
        }

        if (arrivalDate != null && departureDate != null) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                selectedNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
            Console.log("USFestivalInPersonBookingForm: Selected accommodation nights: " + selectedNights);
        }

        // Find all accommodation ScheduledItems for this Item
        List<ScheduledItem> allAccommodationItems = policyAggregate.filterAccommodationScheduledItems().stream()
            .filter(si -> dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), selectedItem))
            .collect(java.util.stream.Collectors.toList());

        // Filter by selected nights if we have a date range
        List<ScheduledItem> accommodationItems;
        if (!selectedNights.isEmpty()) {
            accommodationItems = allAccommodationItems.stream()
                .filter(si -> {
                    LocalDate siDate = si.getDate();
                    return siDate != null && selectedNights.contains(siDate);
                })
                .collect(java.util.stream.Collectors.toList());

            // Fallback: if no items match the date filter but we have items for this room type,
            // use all items (handles bad test data where dates are wrong)
            if (accommodationItems.isEmpty() && !allAccommodationItems.isEmpty()) {
                Console.log("USFestivalInPersonBookingForm: WARNING - No accommodation items match selected dates, using all items as fallback");
                accommodationItems = allAccommodationItems;
            }
        } else {
            accommodationItems = allAccommodationItems;
        }

        if (!accommodationItems.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Booking " + accommodationItems.size() + " accommodation items for " + selectedOption.getName());
            workingBooking.bookScheduledItems(accommodationItems, false);
        } else {
            Console.log("USFestivalInPersonBookingForm: No accommodation ScheduledItems found for " + selectedOption.getName());
        }
    }

    /**
     * Books meals ScheduledItems based on selected meal options and arrival/departure time.
     *
     * Note: Breakfast is served the morning AFTER an accommodation night,
     * so breakfast on day d+1 corresponds to accommodation on day d.
     *
     * Arrival time filtering:
     * - MORNING arrival: all meals on arrival day (lunch + dinner)
     * - AFTERNOON arrival: dinner only on arrival day (skip lunch)
     * - EVENING arrival: no meals on arrival day (arrived after dinner)
     *
     * Departure time filtering:
     * - MORNING departure: no meals on departure day except breakfast (if overnight stay)
     * - AFTERNOON departure: lunch on departure day, but not dinner
     * - EVENING departure: all meals on departure day (lunch + dinner)
     */
    private void bookMealsItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (mealsSection == null) return;

        List<ScheduledItem> mealsItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);
        if (mealsItems.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No meals items in policy - skipping");
            return;
        }

        // Get accommodation nights from festival day section's selected dates
        // Accommodation nights = arrival date to (departure date - 1)
        // e.g., arrival Apr 24, departure Apr 29 means nights on Apr 24, 25, 26, 27, 28
        java.util.Set<java.time.LocalDate> accommodationNights = new java.util.HashSet<>();
        boolean hasAccommodation = accommodationSection != null
                && accommodationSection.getSelectedOption() != null
                && !accommodationSection.getSelectedOption().isDayVisitor();

        LocalDate arrivalDate = null;
        LocalDate departureDate = null;
        HasFestivalDaySelectionSection.ArrivalDepartureTime arrivalTime = null;
        HasFestivalDaySelectionSection.ArrivalDepartureTime departureTime = null;

        // Get dates and times from festival day section
        if (festivalDaySection != null) {
            arrivalDate = festivalDaySection.getArrivalDate();
            departureDate = festivalDaySection.getDepartureDate();
            arrivalTime = festivalDaySection.arrivalTimeProperty().get();
            departureTime = festivalDaySection.departureTimeProperty().get();
        }

        // Fallback: compute from teaching dates if festival day section not available
        if (arrivalDate == null || departureDate == null) {
            List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();
            List<LocalDate> teachingDatesSorted = teachingItems.stream()
                .map(ScheduledItem::getDate)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            if (!teachingDatesSorted.isEmpty()) {
                arrivalDate = teachingDatesSorted.get(0);
                departureDate = teachingDatesSorted.get(teachingDatesSorted.size() - 1).plusDays(1); // departure = last teaching day + 1
                Console.log("USFestivalInPersonBookingForm: Using fallback dates from teaching: " + arrivalDate + " to " + departureDate);
            }
        }

        // Build accommodation nights set
        if (hasAccommodation && arrivalDate != null && departureDate != null) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                accommodationNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
        }
        Console.log("USFestivalInPersonBookingForm: Accommodation nights for breakfast calculation: " + accommodationNights);
        Console.log("USFestivalInPersonBookingForm: Arrival date: " + arrivalDate + ", Departure date: " + departureDate);
        Console.log("USFestivalInPersonBookingForm: Arrival time: " + arrivalTime + ", Departure time: " + departureTime);

        // Final dates for closure
        final LocalDate finalArrivalDate = arrivalDate;
        final LocalDate finalDepartureDate = departureDate;
        // Use non-null time values - if null, default to values that don't filter (MORNING for arrival, EVENING for departure)
        final HasFestivalDaySelectionSection.ArrivalDepartureTime finalArrivalTime = arrivalTime != null ? arrivalTime : HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON;
        final HasFestivalDaySelectionSection.ArrivalDepartureTime finalDepartureTime = departureTime != null ? departureTime : HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON;

        // Get teaching dates for lunch/dinner
        java.util.Set<java.time.LocalDate> teachingDates = policyAggregate.filterTeachingScheduledItems().stream()
            .map(ScheduledItem::getDate)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        // Track which items we've already processed to avoid duplicates
        java.util.Set<Object> processedItems = new java.util.HashSet<>();

        for (ScheduledItem si : mealsItems) {
            Item item = si.getItem();
            if (item == null) continue;

            // Skip if we've already processed this item
            Object itemPk = dev.webfx.stack.orm.entity.Entities.getPrimaryKey(item);
            if (processedItems.contains(itemPk)) continue;
            processedItems.add(itemPk);

            String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
            boolean isBreakfast = itemName.contains("breakfast") || itemName.contains("morning");
            boolean isLunch = itemName.contains("lunch") || itemName.contains("midday");
            boolean isDinner = itemName.contains("dinner") || itemName.contains("evening") || itemName.contains("supper");

            Console.log("USFestivalInPersonBookingForm: Processing meal item '" + item.getName() + "' → isBreakfast=" + isBreakfast + ", isLunch=" + isLunch + ", isDinner=" + isDinner);

            // Check if this meal type should be booked based on user selection
            boolean shouldBook = false;
            if (isBreakfast) {
                shouldBook = mealsSection.wantsBreakfast();
            } else if (isLunch) {
                shouldBook = mealsSection.wantsLunch();
            } else if (isDinner) {
                shouldBook = mealsSection.wantsDinner();
            }

            if (shouldBook) {
                // Find all ScheduledItems for this specific meal Item
                List<ScheduledItem> itemScheduledItems = mealsItems.stream()
                    .filter(msi -> dev.webfx.stack.orm.entity.Entities.samePrimaryKey(msi.getItem(), item))
                    .collect(java.util.stream.Collectors.toList());

                // For breakfast: filter to dates that are accommodation night + 1
                // (breakfast on day d+1 is served after staying overnight on day d)
                if (isBreakfast && !accommodationNights.isEmpty()) {
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            if (mealDate == null) return false;
                            // Breakfast on this date is valid if there's accommodation the night before
                            java.time.LocalDate nightBefore = mealDate.minusDays(1);
                            return accommodationNights.contains(nightBefore);
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalInPersonBookingForm: Filtered breakfast items to dates after accommodation nights: " +
                        itemScheduledItems.stream().map(msi -> msi.getDate().toString()).collect(java.util.stream.Collectors.joining(", ")));
                }

                // For lunch: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip lunch on arrival day if arriving AFTERNOON or later
                // - Skip lunch on departure day if departing MORNING
                if (isLunch && finalArrivalDate != null && finalDepartureDate != null) {
                    Console.log("USFestivalInPersonBookingForm: Filtering lunch items. Arrival: " + finalArrivalDate + " (" + finalArrivalTime + "), Departure: " + finalDepartureDate + " (" + finalDepartureTime + ")");
                    Console.log("USFestivalInPersonBookingForm: Lunch items before filter: " + itemScheduledItems.size());
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            Console.log("USFestivalInPersonBookingForm: Checking lunch on " + mealDate);
                            if (mealDate == null) {
                                Console.log("USFestivalInPersonBookingForm: Lunch date is null, skipping");
                                return false;
                            }

                            // Exclude meals outside the stay period (before arrival or after departure)
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) {
                                Console.log("USFestivalInPersonBookingForm: Skipping lunch on " + mealDate + " (outside stay period " + finalArrivalDate + " to " + finalDepartureDate + ")");
                                return false;
                            }

                            // Arrival day: skip lunch if arriving AFTERNOON or EVENING
                            if (mealDate.equals(finalArrivalDate)) {
                                Console.log("USFestivalInPersonBookingForm: Lunch on arrival day. arrivalTime=" + finalArrivalTime);
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON ||
                                    finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    Console.log("USFestivalInPersonBookingForm: Skipping lunch on arrival day " + mealDate + " (arriving " + finalArrivalTime + ")");
                                    return false;
                                }
                            }

                            // Departure day: skip lunch if departing MORNING
                            if (mealDate.equals(finalDepartureDate)) {
                                Console.log("USFestivalInPersonBookingForm: Lunch on departure day. departureTime=" + finalDepartureTime);
                                if (finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING) {
                                    Console.log("USFestivalInPersonBookingForm: Skipping lunch on departure day " + mealDate + " (departing MORNING)");
                                    return false;
                                }
                            }

                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalInPersonBookingForm: Lunch items after filter: " + itemScheduledItems.size());
                }

                // For dinner: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip dinner on arrival day if arriving EVENING
                // - Skip dinner on departure day if departing MORNING or AFTERNOON
                if (isDinner && finalArrivalDate != null && finalDepartureDate != null) {
                    Console.log("USFestivalInPersonBookingForm: Filtering dinner/supper items. Arrival: " + finalArrivalDate + " (" + finalArrivalTime + "), Departure: " + finalDepartureDate + " (" + finalDepartureTime + ")");
                    Console.log("USFestivalInPersonBookingForm: Dinner items before filter: " + itemScheduledItems.size());
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            Console.log("USFestivalInPersonBookingForm: Checking dinner on " + mealDate);
                            if (mealDate == null) {
                                Console.log("USFestivalInPersonBookingForm: Dinner date is null, skipping");
                                return false;
                            }

                            // Exclude meals outside the stay period (before arrival or after departure)
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) {
                                Console.log("USFestivalInPersonBookingForm: Skipping dinner on " + mealDate + " (outside stay period " + finalArrivalDate + " to " + finalDepartureDate + ")");
                                return false;
                            }

                            // Arrival day: skip dinner if arriving EVENING
                            if (mealDate.equals(finalArrivalDate)) {
                                Console.log("USFestivalInPersonBookingForm: Dinner on arrival day. arrivalTime=" + finalArrivalTime);
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    Console.log("USFestivalInPersonBookingForm: Skipping dinner on arrival day " + mealDate + " (arriving EVENING)");
                                    return false;
                                }
                            }

                            // Departure day: skip dinner if departing MORNING or AFTERNOON
                            if (mealDate.equals(finalDepartureDate)) {
                                boolean isMorning = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING;
                                boolean isAfternoon = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON;
                                Console.log("USFestivalInPersonBookingForm: Dinner on departure day " + mealDate + ". departureTime=" + finalDepartureTime + " (isMorning=" + isMorning + ", isAfternoon=" + isAfternoon + ")");
                                if (isMorning || isAfternoon) {
                                    Console.log("USFestivalInPersonBookingForm: Skipping dinner on departure day " + mealDate + " (departing " + finalDepartureTime + ")");
                                    return false;
                                } else {
                                    Console.log("USFestivalInPersonBookingForm: KEEPING dinner on departure day " + mealDate + " (departing " + finalDepartureTime + " = EVENING)");
                                }
                            } else {
                                Console.log("USFestivalInPersonBookingForm: Dinner on " + mealDate + " (not departure day " + finalDepartureDate + ") - KEEPING");
                            }

                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalInPersonBookingForm: Dinner items after filter: " + itemScheduledItems.size());
                }

                if (!itemScheduledItems.isEmpty()) {
                    Console.log("USFestivalInPersonBookingForm: Booking " + itemScheduledItems.size() + " " + item.getName() + " items (addOnly=false to replace)");
                    // Always use addOnly=false to REPLACE items of this type
                    // This ensures filtered items replace any items from the initial state
                    workingBooking.bookScheduledItems(itemScheduledItems, false);
                }
            }
        }
    }

    /**
     * Books the selected dietary preference (vegetarian, vegan, etc.) into the WorkingBooking.
     * Dietary items are non-temporal - they create only a DocumentLine without Attendance records.
     *
     * Supports two paths:
     * 1. API-driven: Uses selectedDietaryItem (Item from DIET family in database)
     * 2. Legacy: Uses dietaryPreference enum and finds matching Item by name
     */
    private void bookDietaryItem(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (mealsSection == null) return;

        // Try API-driven dietary item first
        Item selectedDietaryItem = mealsSection.getSelectedDietaryItem();

        // Fallback to legacy: find Item by dietary preference name (Vegetarian/Vegan)
        if (selectedDietaryItem == null) {
            HasMealsSelectionSection.DietaryPreference preference = mealsSection.getDietaryPreference();
            if (preference != null) {
                String prefName = preference.name().toLowerCase();
                Console.log("USFestivalInPersonBookingForm: No API dietary item, looking for legacy '" + prefName + "' item");

                // Find an Item matching the preference name
                selectedDietaryItem = policyAggregate.getScheduledItems().stream()
                    .map(ScheduledItem::getItem)
                    .filter(java.util.Objects::nonNull)
                    .filter(item -> {
                        String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
                        return itemName.contains(prefName);
                    })
                    .findFirst()
                    .orElse(null);

                if (selectedDietaryItem != null) {
                    Console.log("USFestivalInPersonBookingForm: Found legacy dietary item: " + selectedDietaryItem.getName());
                }
            }
        }

        // Unbook all other diet items before booking the selected one
        List<one.modality.base.shared.entities.ItemPolicy> dietPolicies = policyAggregate.getDietItemPolicies();
        for (one.modality.base.shared.entities.ItemPolicy policy : dietPolicies) {
            Item dietItem = policy.getItem();
            if (dietItem != null && !dev.webfx.stack.orm.entity.Entities.samePrimaryKey(dietItem, selectedDietaryItem)) {
                one.modality.base.shared.entities.Site dietSite = null;
                if (policy.getScope() != null) {
                    dietSite = policy.getScope().getSite();
                }
                Console.log("USFestivalInPersonBookingForm: Unbooking previous dietary item '" + dietItem.getName() + "'");
                workingBooking.unbookItem(dietSite, dietItem);
            }
        }

        if (selectedDietaryItem == null) {
            Console.log("USFestivalInPersonBookingForm: No dietary preference selected (neither API nor legacy)");
            return;
        }

        // Get the site from ItemPolicy.getScope().getSite()
        one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(selectedDietaryItem);
        one.modality.base.shared.entities.Site site = null;
        if (itemPolicy != null && itemPolicy.getScope() != null) {
            site = itemPolicy.getScope().getSite();
        }

        // Dietary items are non-temporal - book without attendance records (no dates)
        Console.log("USFestivalInPersonBookingForm: Booking dietary preference '" + selectedDietaryItem.getName() + "' (non-temporal, no attendance dates)" +
            (site != null ? " at site " + site.getName() : ""));
        workingBooking.bookNonTemporalItem(site, selectedDietaryItem);
    }

    /**
     * Books audio recording ScheduledItems based on selected phase coverage.
     * If no phase is selected (or "No Audio Recordings"), unbooks all audio recording items.
     * If a phase is selected, books only the audio recording items within that phase's date range.
     */
    private void bookAudioRecordingItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        // If the phase section is not visible (no multiple phases configured), skip - let additional options handle it
        if (audioRecordingPhaseSection == null || !audioRecordingPhaseSection.getView().isVisible()) {
            return;
        }

        var selectedOption = audioRecordingPhaseSection.getSelectedOption();

        // Get all audio recording scheduled items
        List<ScheduledItem> allAudioItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.AUDIO_RECORDING);

        if (selectedOption == null || selectedOption.isNoRecordingOption()) {
            // No audio recording selected - unbook all audio items
            if (!allAudioItems.isEmpty()) {
                Console.log("USFestivalInPersonBookingForm: Unbooking " + allAudioItems.size() + " audio recording items (none selected)");
                workingBooking.unbookScheduledItems(allAudioItems);
            }
            return;
        }

        // Get the scheduled items directly from the selected option
        // This is more accurate as it includes both phase date filtering AND language (Item) filtering
        List<ScheduledItem> itemsToBook = selectedOption.getScheduledItems();

        if (itemsToBook == null || itemsToBook.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Selected phase has no scheduled items, unbooking all audio items");
            if (!allAudioItems.isEmpty()) {
                workingBooking.unbookScheduledItems(allAudioItems);
            }
            return;
        }

        // Unbook all audio items that are NOT in the selected option
        // This ensures we only have items from the selected phase + language combination
        List<ScheduledItem> itemsToUnbook = allAudioItems.stream()
            .filter(si -> !itemsToBook.contains(si))
            .collect(java.util.stream.Collectors.toList());

        if (!itemsToUnbook.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Unbooking " + itemsToUnbook.size() + " audio recording items not in selected option");
            workingBooking.unbookScheduledItems(itemsToUnbook);
        }

        Console.log("USFestivalInPersonBookingForm: Booking " + itemsToBook.size() + " audio recording items for " +
            selectedOption.getName());
        // Use addOnly=false to replace existing audio items with the selected option's items
        workingBooking.bookScheduledItems(itemsToBook, false);
    }

    /**
     * Books additional options ScheduledItems based on selections.
     * Handles all selected additional options (parking, shuttle, assisted listening, etc.)
     * Uses Item.isTemporal() to determine booking method:
     * - Temporal items (per-day): creates DocumentLine + Attendance records with dates
     * - Non-temporal items: creates only DocumentLine without any Attendance records
     */
    private void bookAdditionalOptionsItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        // Book selected shuttle options first (from dedicated shuttle section)
        bookTransportOptions(workingBooking, policyAggregate);

        if (additionalOptionsSection == null) return;

        // Get all selected additional options
        List<HasAdditionalOptionsSection.AdditionalOption> selectedOptions = additionalOptionsSection.getSelectedOptions();

        if (selectedOptions.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No additional options selected");
            return;
        }

        Console.log("USFestivalInPersonBookingForm: Processing " + selectedOptions.size() + " selected additional options");

        // Book each selected option
        for (HasAdditionalOptionsSection.AdditionalOption option : selectedOptions) {
            Item itemEntity = option.getItemEntity();
            if (itemEntity == null) {
                Console.log("USFestivalInPersonBookingForm: Option '" + option.getName() + "' has no item entity, skipping");
                continue;
            }

            // Special handling for transport (outbound/return direction)
            if (option.getItemFamily() == KnownItemFamily.TRANSPORT) {
                bookTransportOption(workingBooking, policyAggregate, itemEntity);
                continue;
            }

            // Check if item is temporal (has per-day attendance) or non-temporal (no dates)
            // Use Item.isTemporal() from database, fallback to option.isPerDay() if not set
            Boolean itemTemporal = itemEntity.isTemporal();
            boolean isTemporal = (itemTemporal != null) ? itemTemporal : option.isPerDay();

            if (isTemporal) {
                // Temporal item - book all scheduled items (creates Attendance records with dates)
                List<ScheduledItem> scheduledItems = policyAggregate.getScheduledItems().stream()
                    .filter(si -> si.getItem() != null &&
                                 dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), itemEntity))
                    .collect(java.util.stream.Collectors.toList());

                if (!scheduledItems.isEmpty()) {
                    Console.log("USFestivalInPersonBookingForm: Booking " + scheduledItems.size() + " '" + option.getName() + "' items (temporal, with attendance dates)");
                    workingBooking.bookScheduledItems(scheduledItems, true);
                } else {
                    Console.log("USFestivalInPersonBookingForm: No scheduled items found for temporal '" + option.getName() + "'");
                }
            } else {
                // Non-temporal item - book without attendance records (no dates)
                // Get the site from ItemPolicy.getScope().getSite()
                one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(itemEntity);
                one.modality.base.shared.entities.Site site = null;
                if (itemPolicy != null && itemPolicy.getScope() != null) {
                    site = itemPolicy.getScope().getSite();
                }

                Console.log("USFestivalInPersonBookingForm: Booking '" + option.getName() + "' (non-temporal, no attendance dates)" +
                    (site != null ? " at site " + site.getName() : ""));
                workingBooking.bookNonTemporalItem(site, itemEntity);
            }
        }
    }

    /**
     * Books selected transport options (parking + shuttle) from the dedicated transport section.
     * For shuttles, only books those whose scheduled date matches the user's arrival/departure date.
     */
    private void bookTransportOptions(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (transportSection == null) return;

        // First, collect ALL transport scheduled items (parking + shuttle) to unbook
        List<ScheduledItem> allTransportItems = new ArrayList<>();
        for (HasTransportSection.ParkingOption parking : transportSection.getParkingOptions()) {
            if (parking.getScheduledItems() != null) {
                allTransportItems.addAll(parking.getScheduledItems());
            }
        }
        for (HasTransportSection.ShuttleOption shuttle : transportSection.getShuttleOptions()) {
            if (shuttle.getScheduledItems() != null) {
                allTransportItems.addAll(shuttle.getScheduledItems());
            }
        }

        // Unbook all transport items first (to handle deselection)
        if (!allTransportItems.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Unbooking " + allTransportItems.size() + " transport scheduled items before rebooking selected ones");
            workingBooking.unbookScheduledItems(allTransportItems);
        }

        // Also unbook ALL parking scheduled items from PolicyAggregate to ensure complete cleanup
        // (handles edge cases where parking was booked through different paths)
        List<ScheduledItem> allPolicyParkingItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.PARKING);
        if (allPolicyParkingItems != null && !allPolicyParkingItems.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Also unbooking " + allPolicyParkingItems.size() + " parking items from PolicyAggregate");
            workingBooking.unbookScheduledItems(allPolicyParkingItems);

            // Also unbook parking Items directly (in case they were booked as non-temporal items)
            // Get unique parking Items and unbook them at each site scope
            java.util.Set<Item> parkingItems = allPolicyParkingItems.stream()
                .map(ScheduledItem::getItem)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            for (Item parkingItem : parkingItems) {
                one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(parkingItem);
                one.modality.base.shared.entities.Site site = null;
                if (itemPolicy != null && itemPolicy.getScope() != null) {
                    site = itemPolicy.getScope().getSite();
                }
                Console.log("USFestivalInPersonBookingForm: Unbooking parking Item '" + parkingItem.getName() + "' (non-temporal cleanup)");
                workingBooking.unbookItem(site, parkingItem);
            }
        }

        // Book selected parking options (filtered by arrival/departure dates)
        List<HasTransportSection.ParkingOption> selectedParking = transportSection.getSelectedParkingOptions();
        if (!selectedParking.isEmpty()) {
            LocalDate arrivalDate = transportSection.getArrivalDate();
            LocalDate departureDate = transportSection.getDepartureDate();
            Console.log("USFestivalInPersonBookingForm: Booking " + selectedParking.size() + " selected parking options" +
                " (arrival=" + arrivalDate + ", departure=" + departureDate + ")");

            for (HasTransportSection.ParkingOption parking : selectedParking) {
                List<ScheduledItem> scheduledItems = parking.getScheduledItems();
                if (scheduledItems != null && !scheduledItems.isEmpty()) {
                    // Filter scheduled items to only include those within the arrival/departure date range
                    List<ScheduledItem> filteredItems = scheduledItems.stream()
                        .filter(si -> {
                            LocalDate siDate = si.getDate();
                            if (siDate == null) return true; // Include items without a date
                            boolean afterArrival = arrivalDate == null || !siDate.isBefore(arrivalDate);
                            boolean beforeDeparture = departureDate == null || !siDate.isAfter(departureDate);
                            return afterArrival && beforeDeparture;
                        })
                        .collect(java.util.stream.Collectors.toList());

                    Console.log("USFestivalInPersonBookingForm: Booking parking '" + parking.getName() +
                        "' - " + filteredItems.size() + " of " + scheduledItems.size() + " scheduled items (filtered by dates)");
                    if (!filteredItems.isEmpty()) {
                        workingBooking.bookScheduledItems(filteredItems, true);
                    }
                } else {
                    Console.log("USFestivalInPersonBookingForm: Parking '" + parking.getName() + "' has no scheduled items to book");
                }
            }
        }

        // Book selected shuttle options
        List<HasTransportSection.ShuttleOption> selectedShuttles = transportSection.getSelectedShuttleOptions();
        if (!selectedShuttles.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: Booking " + selectedShuttles.size() + " selected shuttle options");
            for (HasTransportSection.ShuttleOption shuttle : selectedShuttles) {
                List<ScheduledItem> scheduledItems = shuttle.getScheduledItems();
                if (scheduledItems != null && !scheduledItems.isEmpty()) {
                    Console.log("USFestivalInPersonBookingForm: Booking shuttle '" + shuttle.getName() +
                        "' (" + shuttle.getDirection() + ") - " + scheduledItems.size() + " scheduled items");
                    workingBooking.bookScheduledItems(scheduledItems, true);
                } else {
                    Console.log("USFestivalInPersonBookingForm: Shuttle '" + shuttle.getName() + "' has no scheduled items to book");
                }
            }
        }

        if (selectedParking.isEmpty() && selectedShuttles.isEmpty()) {
            Console.log("USFestivalInPersonBookingForm: No transport options selected");
        }
    }

    /**
     * Books transport/shuttle items with special handling for outbound/return directions.
     * @deprecated Use transportSection for transport items instead.
     */
    private void bookTransportOption(WorkingBooking workingBooking, PolicyAggregate policyAggregate, Item transportItem) {
        String itemName = transportItem.getName() != null ? transportItem.getName().toLowerCase() : "";
        boolean shouldBook = false;

        // Check if this is outbound or return shuttle
        if (itemName.contains("outbound") || itemName.contains("arrival") || itemName.contains("to ")) {
            shouldBook = additionalOptionsSection.needsShuttleOutbound();
        } else if (itemName.contains("return") || itemName.contains("departure") || itemName.contains("from ")) {
            shouldBook = additionalOptionsSection.needsShuttleReturn();
        } else {
            // Generic shuttle - book if either direction is selected
            shouldBook = additionalOptionsSection.needsShuttleOutbound() || additionalOptionsSection.needsShuttleReturn();
        }

        if (shouldBook) {
            List<ScheduledItem> scheduledItems = policyAggregate.getScheduledItems().stream()
                .filter(si -> si.getItem() != null &&
                             dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), transportItem))
                .collect(java.util.stream.Collectors.toList());

            if (!scheduledItems.isEmpty()) {
                Console.log("USFestivalInPersonBookingForm: Booking " + scheduledItems.size() + " '" + transportItem.getName() + "' transport items");
                workingBooking.bookScheduledItems(scheduledItems, true);
            }
        }
    }

    // ========================================
    // Public Accessors
    // ========================================

    /**
     * Returns the member selection section for callback wiring.
     */
    public DefaultMemberSelectionSection getMemberSelectionSection() {
        return memberSelectionSection;
    }

    /**
     * Returns the accommodation section for external configuration.
     */
    public DefaultAccommodationSelectionSection getAccommodationSection() {
        return accommodationSection;
    }

    /**
     * Returns the festival day section for external configuration.
     */
    public DefaultFestivalDaySelectionSection getFestivalDaySection() {
        return festivalDaySection;
    }

    /**
     * Returns the meals section for external configuration.
     */
    public DefaultMealsSelectionSection getMealsSection() {
        return mealsSection;
    }

    /**
     * Returns the additional options section for external configuration.
     */
    public DefaultAdditionalOptionsSection getAdditionalOptionsSection() {
        return additionalOptionsSection;
    }

    /**
     * Returns the sticky price header component.
     * This should be added to the top of the page container by the parent activity.
     */
    public StickyPriceHeader getStickyPriceHeader() {
        return stickyPriceHeader;
    }

    // ========================================
    // Sticky Price Header Updates
    // ========================================

    /**
     * Updates the sticky price header with current selection.
     * Shows room name and number of days. Price is bound to WorkingBookingProperties.totalProperty().
     */
    private void updateStickyPriceHeader() {
        if (stickyPriceHeader == null) return;

        // Get selected accommodation
        HasAccommodationSelectionSection.AccommodationOption selectedRoom = null;
        if (accommodationSection != null) {
            selectedRoom = accommodationSection.getSelectedOption();
        }

        if (selectedRoom == null) {
            stickyPriceHeader.hide();
            return;
        }

        String roomName = selectedRoom.getName();

        // Get selected days count from festival day section
        int daysCount = 0;
        if (festivalDaySection != null) {
            daysCount = festivalDaySection.getSelectedDaysCount();
        }

        Console.log("USFestivalInPersonBookingForm: Updating sticky header - room=" + roomName + ", days=" + daysCount);

        // Only update room name and days - price is bound to WorkingBookingProperties.totalProperty()
        stickyPriceHeader.setRoomName(roomName);
        stickyPriceHeader.setSelectedDays(daysCount);
    }

    /**
     * Sets up the terms and conditions URL and custom text from the Event's termsUrlEn field.
     * This URL is displayed as a clickable link in the terms section.
     */
    private void setupTermsUrl() {
        if (workingBookingProperties == null) return;

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) return;

        Event event = policyAggregate.getEvent();
        if (event == null) return;

        // Always use custom terms text for US Festival
        form.setTermsText(I18n.getI18nText(KMCNY18nKeys.USFestivalAcceptTermsText));

        String termsUrl = event.getTermsUrlEn();
        if (termsUrl != null && !termsUrl.isEmpty()) {
            form.setTermsUrl(termsUrl);
            Console.log("USFestivalInPersonBookingForm: Set terms URL to: " + termsUrl);
        }
    }

    /**
     * Returns the built form for use in the UI.
     */
    public StandardBookingForm getForm() {
        return form;
    }
}
