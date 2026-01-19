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
            // Enable comments section on Summary page (uses StandardBookingForm's DefaultCommentsSection)
            .withShowCommentsSection(true)
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
            return;
        }

        // Skip population logic for non-NEW_BOOKING entry points
        // Existing bookings should not have their data overwritten
        if (entryPoint != BookingFormEntryPoint.NEW_BOOKING) {
            // Still bind the sticky price header for PAY_BOOKING so user sees correct total
            if (stickyPriceHeader != null) {
                stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
            }
            return;
        }

        // Listen to total property changes as an indicator that WorkingBooking has been initialized
        workingBookingProperties.totalProperty().addListener((obs, oldValue, newValue) -> {
            populateAllOptions();
        });

        // Bind stickyPriceHeader's totalPrice to WorkingBookingProperties.totalProperty()
        // This ensures the header shows the actual booking total from WorkingBooking
        if (stickyPriceHeader != null) {
            stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
        }

        // Check if WorkingBooking is already available (e.g., when swapped from entry form)
        // In this case, the listener won't fire because the total was already set
        if (workingBookingProperties.getWorkingBooking() != null) {
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
        }

        // Get late departure date from LateDeparturePart (period after main event)
        EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();
        if (lateDeparturePart != null) {
            lateDepartureDate = lateDeparturePart.getEndDate();
        }
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
            // Reset WorkingBooking to clear previous selections when accommodation type changes
            // This ensures the booking doesn't keep items from a previous accommodation selection
            if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                workingBookingProperties.getWorkingBooking().cancelChanges();
            }

            // Pass min nights constraint and isDayVisitor to festival day section
            // Also reset dates when accommodation type changes
            if (festivalDaySection != null && option != null) {
                festivalDaySection.setMinNightsConstraint(option.getMinNights());
                festivalDaySection.setIsDayVisitor(option.isDayVisitor());
                // Reset dates when accommodation changes (user needs to re-select dates)
                festivalDaySection.reset();
            }
            // Sync breakfast with accommodation selection
            // Day visitors don't get breakfast, overnight guests do
            if (mealsSection != null && option != null) {
                boolean hasAccommodation = !option.isDayVisitor();
                mealsSection.setHasAccommodation(hasAccommodation);
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
                    } else {
                        // Single-person room or no capacity info: hide roommate section
                        roommateInfoSection.setVisible(false);
                    }
                } else {
                    // Day Visitor or no item entity: hide roommate section
                    roommateInfoSection.setVisible(false);
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
                    }
                }
            });

            // Listen for arrival time changes
            festivalDaySection.arrivalTimeProperty().addListener((obs, old, newTime) -> {
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
            audioRecordingPhaseSection.setArrivalDate(newDate);
        });

        // Bind departure date to audio recording section (for availability)
        festivalDaySection.departureDateProperty().addListener((obs, old, newDate) -> {
            audioRecordingPhaseSection.setDepartureDate(newDate);
        });

        // Listen for option selection changes
        audioRecordingPhaseSection.setOnOptionSelected(option -> {
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
            transportSection.setArrivalDate(newDate);
        });

        // Bind departure date to transport section (for shuttle availability)
        festivalDaySection.departureDateProperty().addListener((obs, old, newDate) -> {
            transportSection.setDepartureDate(newDate);
        });

        // Initialize transport section with current dates (listeners only fire on changes)
        LocalDate currentArrival = festivalDaySection.arrivalDateProperty().get();
        LocalDate currentDeparture = festivalDaySection.departureDateProperty().get();
        if (currentArrival != null) {
            transportSection.setArrivalDate(currentArrival);
        }
        if (currentDeparture != null) {
            transportSection.setDepartureDate(currentDeparture);
        }

        // Listen for any transport selection changes (parking or shuttle)
        transportSection.setOnSelectionChanged(() -> {
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
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for lunch selection changes
        mealsSection.wantsLunchProperty().addListener((obs, old, newVal) -> {
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for dinner selection changes
        mealsSection.wantsDinnerProperty().addListener((obs, old, newVal) -> {
            bookSelectedItemsIntoWorkingBooking();
            // Rebuild meal cards to ensure proper display with early/late pricing
            rebuildMealCardsIfNeeded();
        });

        // Listen for dietary preference changes (API-driven - selectedDietaryItem)
        mealsSection.selectedDietaryItemProperty().addListener((obs, old, newVal) -> {
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for legacy dietary preference changes (enum-based - dietaryPreference)
        mealsSection.dietaryPreferenceProperty().addListener((obs, old, newVal) -> {
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
            return;
        }

        // Check if the stay extends beyond event dates
        boolean isEarlyArrival = arrival.isBefore(eventStartDate);
        boolean isLateDeparture = departure.isAfter(eventEndDate);
        boolean hasExtendedStay = isEarlyArrival || isLateDeparture;

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
                // Only navigate if WorkingBooking is initialized to avoid NPE
                if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                    form.navigateToYourInformation();
                }
            }
        });
    }

    private void loadMembersIfLoggedIn() {
        Person person = FXUserPerson.getUserPerson();
        if (person != null && memberSelectionSection != null) {
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
            return;
        }

        if (accommodationSection == null) {
            return;
        }

        if (workingBookingProperties == null) {
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

        // Use event boundary dates for price calculation
        LocalDate arrivalDate = eventStartDate;
        LocalDate departureDate = eventEndDate;
        if (arrivalDate == null || departureDate == null) {
            return;
        }

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

        // Track dates for breakdown display
        LocalDate teachingMinDate = null;
        LocalDate teachingMaxDate = null;
        int accommodationNightsCount = 0;

        // 1. Book all teachings within event boundaries
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems().stream()
            .filter(si -> si.getDate() != null)
            .filter(si -> !si.getDate().isBefore(arrivalDate) && !si.getDate().isAfter(departureDate))
            .collect(java.util.stream.Collectors.toList());
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

        // Categorize meal attendances by matching with ORIGINAL ScheduledItems (which have Timeline loaded)
        List<one.modality.base.shared.entities.Attendance> breakfastAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> lunchAttendances = new ArrayList<>();
        List<one.modality.base.shared.entities.Attendance> dinnerAttendances = new ArrayList<>();

        List<one.modality.base.shared.entities.Attendance> bookedAttendances = tempBooking.getBookedAttendances();

        for (one.modality.base.shared.entities.Attendance attendance : bookedAttendances) {
            ScheduledItem attendanceSi = attendance.getScheduledItem();
            if (attendanceSi == null) {
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
        // (tempBooking.calculateTotal() might include items not shown in breakdown due to categorization)
        int breakdownTotal = breakdown.stream().mapToInt(item -> item.getPrice()).sum();

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

        // Track dates for breakdown display
        LocalDate teachingMinDate = null;
        LocalDate teachingMaxDate = null;

        // 1. Book all teachings within event boundaries (they have ScheduledItems)
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems().stream()
            .filter(si -> si.getDate() != null)
            .filter(si -> !si.getDate().isBefore(arrivalDate) && !si.getDate().isAfter(departureDate))
            .collect(java.util.stream.Collectors.toList());
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
        }

        // 3. Book all meals within event boundaries (they have ScheduledItems)
        // hasAccommodation = true since sharing accommodation guests get breakfast
        bookMealsForPriceCalculation(tempBooking, policyAggregate, arrivalDate, departureDate, true);

        // Calculate teachings + meals price from WorkingBooking
        int teachingsAndMealsPrice = tempBooking.calculateTotal();

        // Total = teachings + meals + sharing accommodation
        int totalPrice = teachingsAndMealsPrice + sharingAccommodationPrice;

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
            return;
        }

        // Get the main EventPart to find boundaries (including time)
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        // Get meal Items from PolicyAggregate timelines for breakfast detection
        final one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
        final Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;

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
        } else {
            // Fallback: use all meals if no EventPart found
            mealsWithinBoundaries = mealsItems;
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
            return null;
        }

        // First try PolicyAggregate's methods
        one.modality.base.shared.entities.EventPart earlyArrivalPart = policyAggregate.getEarlyArrivalPart();
        one.modality.base.shared.entities.EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();

        // If PolicyAggregate methods work, use them to find the main part
        if (earlyArrivalPart != null || lateDeparturePart != null) {
            for (one.modality.base.shared.entities.EventPart part : eventParts) {
                boolean isEarlyArrival = earlyArrivalPart != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(part, earlyArrivalPart);
                boolean isLateDeparture = lateDeparturePart != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(part, lateDeparturePart);

                if (!isEarlyArrival && !isLateDeparture) {
                    return part;
                }
            }
        }

        // If PolicyAggregate methods return null, identify parts by their dates
        // The main event is typically the longest part (most days)
        one.modality.base.shared.entities.EventPart longestPart = null;
        long longestDuration = -1;

        for (one.modality.base.shared.entities.EventPart part : eventParts) {
            LocalDate partStart = part.getStartDate();
            LocalDate partEnd = part.getEndDate();
            if (partStart != null && partEnd != null) {
                long duration = ChronoUnit.DAYS.between(partStart, partEnd);
                if (duration > longestDuration) {
                    longestDuration = duration;
                    longestPart = part;
                }
            }
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
            return determineMealFromTime(fallbackTime);
        }

        one.modality.base.shared.entities.ScheduledItem scheduledItem = boundary.getScheduledItem();
        if (scheduledItem == null) {
            return determineMealFromTime(fallbackTime);
        }

        // Check if the boundary item is a meal
        boolean isMeal = one.modality.base.shared.entities.util.ScheduledItems.isOfFamily(
            scheduledItem, one.modality.base.shared.knownitems.KnownItemFamily.MEALS);

        if (!isMeal) {
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

            if (lowerName.contains("breakfast") || lowerName.contains("morning")) {
                return DefaultMealsSelectionSection.MealBoundary.BREAKFAST;
            } else if (lowerName.contains("lunch") || lowerName.contains("midday")) {
                return DefaultMealsSelectionSection.MealBoundary.LUNCH;
            } else if (lowerName.contains("dinner") || lowerName.contains("supper") || lowerName.contains("evening")) {
                return DefaultMealsSelectionSection.MealBoundary.DINNER;
            }
        }

        // If we couldn't determine from name, fall back to time-based detection
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
            return;
        }
        Item sharingAccommodationItem = sharingAccommodationItemPolicy.getItem();
        if (sharingAccommodationItem == null) {
            return;
        }

        // Calculate price and breakdown using the sharing accommodation item
        AccommodationPriceResult priceResult = calculateShareAccommodationPriceWithWorkingBooking(policyAggregate, sharingAccommodationItem, arrivalDate, departureDate);

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
            return;
        }

        if (festivalDaySection == null) {
            return;
        }

        if (workingBookingProperties == null) {
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

        // Find the main EventPart to get accurate boundary dates
        // The main EventPart excludes early arrival and late departure periods
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        // Set main event boundaries on festival day section for correct time option filtering
        // This must be done BEFORE populateFromPolicyAggregate so the time options are correct
        if (mainEventPart != null) {
            LocalDate mainStartDate = mainEventPart.getStartDate();
            LocalDate mainEndDate = mainEventPart.getEndDate();

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
            }
        }

        // Populate festival days from PolicyAggregate
        festivalDaySection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        festivalDaysPopulated = true;
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
            return;
        }

        if (mealsSection == null) {
            return;
        }

        if (workingBookingProperties == null) {
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

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

            if (mainStartDate != null) {
                mealsSection.setEventBoundaryStartDate(mainStartDate);
            }
            if (mainEndDate != null) {
                mealsSection.setEventBoundaryEndDate(mainEndDate);
            }

            // Get meal ScheduledItems for isInPeriod checks (needed before creating period)
            java.util.List<one.modality.base.shared.entities.ScheduledItem> mealItems =
                policyAggregate.filterScheduledItemsOfFamily(one.modality.base.shared.knownitems.KnownItemFamily.MEALS);
            mealsSection.setMealScheduledItems(mealItems);

            // Determine which meal marks the start of the main event from the ScheduledBoundary
            // The boundary's ScheduledItem tells us exactly what item marks the boundary
            one.modality.base.shared.entities.ScheduledBoundary startBoundary = mainEventPart.getStartBoundary();
            DefaultMealsSelectionSection.MealBoundary startMeal = determineMealFromBoundary(startBoundary, mainStartTime);
            mealsSection.setEventBoundaryStartMeal(startMeal);

            // Determine which meal marks the end of the main event from the ScheduledBoundary
            one.modality.base.shared.entities.ScheduledBoundary endBoundary = mainEventPart.getEndBoundary();
            DefaultMealsSelectionSection.MealBoundary endMeal = determineMealFromBoundary(endBoundary, mainEndTime);
            mealsSection.setEventBoundaryEndMeal(endMeal);

            // Create a Period with actual times from the boundary ScheduledItems
            // The EventPart's times may be null, so we derive times from the boundary meals
            one.modality.base.shared.entities.Period mainEventPeriodWithTimes = createMainEventPeriodWithBoundaryTimes(
                mainEventPart, startBoundary, endBoundary, mealItems);
            mealsSection.setMainEventPeriod(mainEventPeriodWithTimes);
        } else {
            // Fallback to event dates if no main EventPart found
            if (eventStartDate != null) {
                mealsSection.setEventBoundaryStartDate(eventStartDate);
            }
            if (eventEndDate != null) {
                mealsSection.setEventBoundaryEndDate(eventEndDate);
            }
            // Pass meal ScheduledItems for isInPeriod checks (fallback case)
            java.util.List<one.modality.base.shared.entities.ScheduledItem> mealItems =
                policyAggregate.filterScheduledItemsOfFamily(one.modality.base.shared.knownitems.KnownItemFamily.MEALS);
            mealsSection.setMealScheduledItems(mealItems);
        }

        // Populate meals options from PolicyAggregate
        mealsSection.populateFromPolicyAggregate(policyAggregate);

        // Set WorkingBooking for DocumentBill-based pricing
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        if (workingBooking != null) {
            mealsSection.setWorkingBooking(workingBooking);
        }

        // Mark as populated to avoid re-populating
        mealsOptionsPopulated = true;
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
            return;
        }

        if (audioRecordingPhaseSection == null) {
            return;
        }

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

        // Check if there are multiple phase coverages for audio recording
        java.util.List<EventPhaseCoverage> phaseCoverages =
            policyAggregate.getAudioRecordingPhaseCoverages();

        if (phaseCoverages == null || phaseCoverages.size() <= 1) {
            // No phase coverages or only one - hide section and let additional options handle audio recording
            audioRecordingPhaseSection.setVisible(false);
            audioRecordingPhasePopulated = true;
            return;
        }

        // Set workingBookingProperties on the section (needed for price calculation)
        audioRecordingPhaseSection.setWorkingBookingProperties(workingBookingProperties);

        // Populate the section from PolicyAggregate
        audioRecordingPhaseSection.populateFromPolicyAggregate(policyAggregate);

        // Show the section
        audioRecordingPhaseSection.setVisible(true);

        // Mark as populated to avoid re-populating
        audioRecordingPhasePopulated = true;
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
            return;
        }

        if (additionalOptionsSection == null) {
            return;
        }

        if (workingBookingProperties == null) {
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            return;
        }

        // Populate transport section first (parking + shuttle items)
        if (transportSection != null) {
            // Sync arrival/departure dates to transport section before populating
            // (ensures shuttle availability is calculated with correct dates)
            if (festivalDaySection != null) {
                LocalDate arrivalDate = festivalDaySection.arrivalDateProperty().get();
                LocalDate departureDate = festivalDaySection.departureDateProperty().get();
                if (arrivalDate != null) {
                    transportSection.setArrivalDate(arrivalDate);
                }
                if (departureDate != null) {
                    transportSection.setDepartureDate(departureDate);
                }
            }
            transportSection.populateFromPolicyAggregate(policyAggregate);
            boolean hasTransport = transportSection.hasAnyOptions();
            transportSection.setVisible(hasTransport);

            // Exclude parking and transport items from additional options (handled by transport section)
            additionalOptionsSection.setExcludeParkingAndTransport(hasTransport);
        }

        // Exclude audio recording items if they're handled by the dedicated phase section
        boolean audioPhaseVisible = audioRecordingPhaseSection != null && audioRecordingPhaseSection.getView().isVisible();
        additionalOptionsSection.setExcludeAudioRecording(audioPhaseVisible);

        // Populate additional options from PolicyAggregate
        additionalOptionsSection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        additionalOptionsPopulated = true;
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
        // Apply booker details to WorkingBooking BEFORE booking items
        // This ensures age-dependent pricing is calculated correctly in the summary
        applyBookerDetailsToWorkingBooking();

        // Book selected items into WorkingBooking BEFORE summary is displayed
        // This ensures the price breakdown shows all selected options
        bookSelectedItemsIntoWorkingBooking();
    }

    @Override
    public void onPrepareNewBooking() {
        // Reset the flag so accommodation options will be repopulated
        accommodationOptionsPopulated = false;

        // Reload availability from server to get fresh room counts
        PolicyAggregate policyAggregate = workingBookingProperties != null ? workingBookingProperties.getPolicyAggregate() : null;
        if (policyAggregate != null) {
            policyAggregate.reloadAvailabilities()
                .onSuccess(v -> {
                    // Repopulate accommodation options with fresh availability data on the UI thread
                    UiScheduler.runInUiThread(() -> {
                        populateAccommodationOptions();
                    });
                })
                .onFailure(e -> {
                    // Still try to repopulate with existing data on the UI thread
                    UiScheduler.runInUiThread(() -> {
                        populateAccommodationOptions();
                    });
                });
        }
    }

    @Override
    public void onAccommodationSoldOutRecovery(HasAccommodationSelectionSection.AccommodationOption newOption, StandardBookingFormCallbacks.SoldOutRecoveryRoommateInfo roommateInfo, Runnable continueToSummary) {
        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
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

        // Compute arrival/departure dates from old accommodation dates
        // Arrival = min date, Departure = max date + 1 (since accommodation nights are before departure)
        LocalDate arrivalDate = null;
        LocalDate departureDate = null;
        if (!oldAccommodationDates.isEmpty()) {
            arrivalDate = oldAccommodationDates.stream().min(LocalDate::compareTo).orElse(null);
            LocalDate maxDate = oldAccommodationDates.stream().max(LocalDate::compareTo).orElse(null);
            departureDate = maxDate != null ? maxDate.plusDays(1) : null;
        }

        // Step 2: Remove the old accommodation document lines (keep teaching, meals, etc.)
        for (DocumentLine line : oldAccommodationLines) {
            workingBooking.removeDocumentLine(line);
        }

        // Step 3: Update the accommodation section with the new selection
        // Directly set the selectedOptionProperty to newOption, bypassing setSelectedOption()
        // which may fail to match itemId if the newOption was created in a different context
        if (accommodationSection != null) {
            accommodationSection.selectedOptionProperty().set(newOption);

            // Update festival day section constraints and dates based on new accommodation
            if (festivalDaySection != null) {
                festivalDaySection.setMinNightsConstraint(newOption.getMinNights());
                festivalDaySection.setIsDayVisitor(newOption.isDayVisitor());
                // Preserve the original arrival/departure dates so onBeforeSummary() uses them
                if (arrivalDate != null) {
                    festivalDaySection.arrivalDateProperty().set(arrivalDate);
                }
                if (departureDate != null) {
                    festivalDaySection.departureDateProperty().set(departureDate);
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
            } else {
                // Share accommodation mode: set room owner name
                String ownerName = roommateInfo.getRoomOwnerName();
                for (DocumentLine line : newAccommodationLines) {
                    workingBooking.setShareMateInfo(line, ownerName);
                }
            }
        }

        // Step 6: Navigate to summary page to review and retry submission
        continueToSummary.run();
    }

    /**
     * Applies the selected booker's personal details to the WorkingBooking.
     * This must be called before bookSelectedItemsIntoWorkingBooking() so that
     * age-dependent pricing (based on booker's birthdate) is calculated correctly.
     */
    private void applyBookerDetailsToWorkingBooking() {
        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();

        // Case 1: Household member selected (logged-in user booking for self or family member)
        HasMemberSelectionSection.MemberInfo selectedMember = form.getState().getSelectedMember();
        if (selectedMember != null && selectedMember.getPersonEntity() != null) {
            Person person = selectedMember.getPersonEntity();
            workingBooking.applyPersonalDetails(person);
            return;
        }

        // Case 2: New user (guest checkout)
        HasYourInformationSection.NewUserData newUserData = form.getState().getPendingNewUserData();
        if (newUserData != null) {
            workingBooking.applyGuestPersonalDetails(newUserData.firstName, newUserData.lastName, newUserData.email);
        }
    }

    /**
     * Books all selected items (teaching, accommodation, meals, additional options)
     * into the WorkingBooking so they appear in the price breakdown.
     */
    private void bookSelectedItemsIntoWorkingBooking() {
        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
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

        // Store roommate info on accommodation document lines (functional data)
        // User comments are handled by StandardBookingForm's DefaultCommentsSection
        storeRoommateInfoOnDocumentLines(workingBooking);

        // Update meals section with DocumentBill pricing
        // This computes prices from the actual booked items, accounting for
        // date-specific rates (early arrival, late departure have different prices)
        if (mealsSection != null) {
            mealsSection.populateFromDocumentBill();
        }
    }

    /**
     * Sets roommate info on accommodation document lines.
     *
     * <p>Roommate sharing info is stored functionally on DocumentLine entities
     * (not in the request text field).</p>
     *
     * <p>Note: User comments are handled by StandardBookingForm's DefaultCommentsSection,
     * which is enabled via the {@code withShowCommentsSection(true)} builder option.</p>
     */
    private void storeRoommateInfoOnDocumentLines(WorkingBooking workingBooking) {
        // Set roommate info on accommodation document lines (functional data, not text)
        if (roommateInfoSection != null && roommateInfoSection.isVisible()) {
            if (roommateInfoSection.isRoomBooker()) {
                // Room Booking mode: this person is the room owner, listing their roommates
                List<String> roommateNames = roommateInfoSection.getAllRoommateNames();
                if (!roommateNames.isEmpty()) {
                    setShareOwnerInfoOnDocumentLines(workingBooking, roommateNames.toArray(new String[0]));
                }
            } else {
                // Share Accommodation mode: this person is sharing someone else's room
                String ownerName = roommateInfoSection.getRoommateName();
                if (ownerName != null && !ownerName.trim().isEmpty()) {
                    setShareMateInfoOnDocumentLines(workingBooking, ownerName.trim());
                }
            }
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
            return;
        }

        // Set share owner info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareOwnerInfo(line, matesNames);
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
            return;
        }

        // Set share mate info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareMateInfo(line, ownerName);
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
        } else {
            // No date filter available, book all teaching items
            teachingItems = allTeachingItems;
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
            }
        }

        if (arrivalDate == null || departureDate == null) {
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
            return;
        }

        // Day visitor - no accommodation booking
        if (selectedOption.isDayVisitor()) {
            return;
        }

        Item selectedItem = selectedOption.getItemEntity();
        if (selectedItem == null) {
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
            }
        }

        if (arrivalDate != null && departureDate != null) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                selectedNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
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
                accommodationItems = allAccommodationItems;
            }
        } else {
            accommodationItems = allAccommodationItems;
        }

        if (!accommodationItems.isEmpty()) {
            workingBooking.bookScheduledItems(accommodationItems, false);
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
                }

                // For lunch: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip lunch on arrival day if arriving AFTERNOON or later
                // - Skip lunch on departure day if departing MORNING
                if (isLunch && finalArrivalDate != null && finalDepartureDate != null) {
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            if (mealDate == null) return false;
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) return false;
                            if (mealDate.equals(finalArrivalDate)) {
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON ||
                                    finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    return false;
                                }
                            }
                            if (mealDate.equals(finalDepartureDate)) {
                                if (finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                }

                // For dinner: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip dinner on arrival day if arriving EVENING
                // - Skip dinner on departure day if departing MORNING or AFTERNOON
                if (isDinner && finalArrivalDate != null && finalDepartureDate != null) {
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            if (mealDate == null) return false;
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) return false;
                            if (mealDate.equals(finalArrivalDate)) {
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    return false;
                                }
                            }
                            if (mealDate.equals(finalDepartureDate)) {
                                boolean isMorning = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING;
                                boolean isAfternoon = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON;
                                if (isMorning || isAfternoon) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                }

                if (!itemScheduledItems.isEmpty()) {
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
                workingBooking.unbookItem(dietSite, dietItem);
            }
        }

        if (selectedDietaryItem == null) {
            return;
        }

        // Get the site from ItemPolicy.getScope().getSite()
        one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(selectedDietaryItem);
        one.modality.base.shared.entities.Site site = null;
        if (itemPolicy != null && itemPolicy.getScope() != null) {
            site = itemPolicy.getScope().getSite();
        }

        // Dietary items are non-temporal - book without attendance records (no dates)
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
                workingBooking.unbookScheduledItems(allAudioItems);
            }
            return;
        }

        // Get the scheduled items directly from the selected option
        // This is more accurate as it includes both phase date filtering AND language (Item) filtering
        List<ScheduledItem> itemsToBook = selectedOption.getScheduledItems();

        if (itemsToBook == null || itemsToBook.isEmpty()) {
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
            workingBooking.unbookScheduledItems(itemsToUnbook);
        }

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
            return;
        }

        // Book each selected option
        for (HasAdditionalOptionsSection.AdditionalOption option : selectedOptions) {
            Item itemEntity = option.getItemEntity();
            if (itemEntity == null) {
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
                    workingBooking.bookScheduledItems(scheduledItems, true);
                }
            } else {
                // Non-temporal item - book without attendance records (no dates)
                // Get the site from ItemPolicy.getScope().getSite()
                one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(itemEntity);
                one.modality.base.shared.entities.Site site = null;
                if (itemPolicy != null && itemPolicy.getScope() != null) {
                    site = itemPolicy.getScope().getSite();
                }

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
            workingBooking.unbookScheduledItems(allTransportItems);
        }

        // Also unbook ALL parking scheduled items from PolicyAggregate to ensure complete cleanup
        // (handles edge cases where parking was booked through different paths)
        List<ScheduledItem> allPolicyParkingItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.PARKING);
        if (allPolicyParkingItems != null && !allPolicyParkingItems.isEmpty()) {
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
                workingBooking.unbookItem(site, parkingItem);
            }
        }

        // Book selected parking options (filtered by arrival/departure dates)
        List<HasTransportSection.ParkingOption> selectedParking = transportSection.getSelectedParkingOptions();
        if (!selectedParking.isEmpty()) {
            LocalDate arrivalDate = transportSection.getArrivalDate();
            LocalDate departureDate = transportSection.getDepartureDate();

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

                    if (!filteredItems.isEmpty()) {
                        workingBooking.bookScheduledItems(filteredItems, true);
                    }
                }
            }
        }

        // Book selected shuttle options (filtered by arrival/departure date based on direction)
        List<HasTransportSection.ShuttleOption> selectedShuttles = transportSection.getSelectedShuttleOptions();
        if (!selectedShuttles.isEmpty()) {
            LocalDate shuttleArrivalDate = transportSection.getArrivalDate();
            LocalDate shuttleDepartureDate = transportSection.getDepartureDate();
            Console.log("USFestivalInPersonBookingForm: Booking " + selectedShuttles.size() + " selected shuttle options" +
                " (arrival=" + shuttleArrivalDate + ", departure=" + shuttleDepartureDate + ")");

            for (HasTransportSection.ShuttleOption shuttle : selectedShuttles) {
                List<ScheduledItem> scheduledItems = shuttle.getScheduledItems();
                if (scheduledItems != null && !scheduledItems.isEmpty()) {
                    // Determine target date based on shuttle direction:
                    // OUTBOUND (airport -> venue) matches arrival date
                    // RETURN (venue -> airport) matches departure date
                    LocalDate targetDate = shuttle.isOutbound() ? shuttleArrivalDate : shuttleDepartureDate;

                    // Get ScheduledItems from PolicyAggregate to ensure proper entity store association
                    // and correct Rate linkage for price calculation
                    Item item = shuttle.getItemEntity();
                    List<ScheduledItem> filteredItems = policyAggregate.getScheduledItems().stream()
                        .filter(si -> si.getItem() != null &&
                                     dev.webfx.stack.orm.entity.Entities.samePrimaryKey(si.getItem(), item) &&
                                     targetDate != null && targetDate.equals(si.getDate()))
                        .collect(java.util.stream.Collectors.toList());

                    if (!filteredItems.isEmpty()) {
                        Console.log("USFestivalInPersonBookingForm: Booking shuttle '" + shuttle.getName() +
                            "' (" + shuttle.getDirection() + ") for date " + targetDate);
                        // DEBUG: Show details of ScheduledItems being booked
                        for (ScheduledItem si : filteredItems) {
                            Console.log("  DEBUG ScheduledItem: pk=" + si.getPrimaryKey() +
                                ", date=" + si.getDate() +
                                ", site=" + (si.getSite() != null ? si.getSite().getPrimaryKey() : "null") +
                                ", item=" + (si.getItem() != null ? si.getItem().getPrimaryKey() : "null") +
                                ", store=" + si.getStore().getClass().getSimpleName());
                        }
                        workingBooking.bookScheduledItems(filteredItems, true);
                        // DEBUG: Check attendances after booking
                        java.util.List<Attendance> addedAtts = workingBooking.getAttendancesAdded(false);
                        Console.log("  DEBUG After booking: total attendances added = " + addedAtts.size());
                        for (Attendance att : addedAtts) {
                            DocumentLine dl = att.getDocumentLine();
                            Item attItem = dl != null ? dl.getItem() : null;
                            String attItemName = attItem != null ? attItem.getName() : "null";
                            if (attItemName != null && (attItemName.contains("Newark") || attItemName.contains("KMCNY"))) {
                                ScheduledItem attSi = att.getScheduledItem();
                                Console.log("    Transport att: item='" + attItemName +
                                    "', att.date=" + att.getDate() +
                                    ", att.scheduledItem.pk=" + (attSi != null ? attSi.getPrimaryKey() : "null") +
                                    ", att.scheduledItem.date=" + (attSi != null ? attSi.getDate() : "null"));
                            }
                        }
                    } else {
                        Console.log("USFestivalInPersonBookingForm: WARNING - No scheduled items found for shuttle '" +
                            shuttle.getName() + "' on date " + targetDate);
                    }
                }
            }
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
        }
    }

    /**
     * Returns the built form for use in the UI.
     */
    public StandardBookingForm getForm() {
        return form;
    }
}
