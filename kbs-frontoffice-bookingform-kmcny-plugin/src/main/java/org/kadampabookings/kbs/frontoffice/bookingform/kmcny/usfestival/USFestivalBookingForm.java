package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.platform.console.Console;
import javafx.beans.binding.Bindings;
import one.modality.base.shared.entities.Event;
import one.modality.base.shared.entities.EventPart;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.Person;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.knownitems.KnownItemFamily;

import java.util.List;
import one.modality.booking.client.workingbooking.HasWorkingBookingProperties;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingform.BookingFormEntryPoint;
import one.modality.booking.frontoffice.bookingpage.BookingFormButton;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.CompositeBookingFormPage;
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
import one.modality.event.frontoffice.activities.book.event.EventBookingFormSettings;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import one.modality.base.shared.entities.DocumentLine;
import one.modality.ecommerce.shared.pricecalculator.PriceCalculator;

/**
 * US Festival Booking Form - Implementation for US Festival (event type 38) bookings.
 *
 * <p>This is a comprehensive booking form with multiple custom steps:</p>
 * <ul>
 *   <li>Preliminary: Registration Type selection (In-Person / Online)</li>
 *   <li>Step 1: Accommodation selection (room options)</li>
 *   <li>Step 2: Booking Details (festival days, meals, additional options)</li>
 *   <li>Standard flow: Your Information → Member Selection → Summary → Payment → Confirmation</li>
 * </ul>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>In-Person vs Online registration (Online shows "Coming Soon")</li>
 *   <li>Room/accommodation selection with availability status</li>
 *   <li>Festival day selection with arrival/departure dates</li>
 *   <li>Meal options (lunch/dinner) with dietary preferences</li>
 *   <li>Additional options (parking, shuttle, assisted listening)</li>
 * </ul>
 *
 * @author Bruno Salmon
 * @see StandardBookingForm
 * @see StandardBookingFormBuilder
 */
public final class USFestivalBookingForm implements StandardBookingFormCallbacks {

    // The form built by the builder
    private final StandardBookingForm form;

    // Custom sections - Preliminary
    private RegistrationTypeSection registrationTypeSection;

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
    private boolean accommodationOptionsPopulated = false;

    // Event boundary dates from EventSelection and EventPart (loaded from PolicyAggregate)
    // These replace the old Event.getStartDate()/getEndDate() approach
    private LocalDate eventStartDate;      // Start date of main event (from first EventSelection part)
    private LocalDate eventEndDate;        // End date of main event (from last EventSelection part)
    private LocalDate earlyArrivalDate;    // Start date of early arrival period (from EarlyArrivalPart)
    private LocalDate lateDepartureDate;   // End date of late departure period (from LateDeparturePart)

    /**
     * Creates the US Festival booking form using the builder pattern.
     *
     * @param activity   The activity providing WorkingBookingProperties
     * @param settings   The event booking form settings
     * @param entryPoint The entry point for the booking form (NEW_BOOKING, MODIFY_BOOKING, or RESUME_PAYMENT)
     */
    public USFestivalBookingForm(HasWorkingBookingProperties activity, EventBookingFormSettings settings, BookingFormEntryPoint entryPoint) {
        this.settings = settings;
        this.workingBookingProperties = activity.getWorkingBookingProperties();

        // Create the sticky price header
        this.stickyPriceHeader = new StickyPriceHeader();
        this.stickyPriceHeader.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        // Build the form with custom steps
        StandardBookingFormBuilder builder = new StandardBookingFormBuilder(activity, settings)
            .withColorScheme(BookingFormColorScheme.WISDOM_BLUE)  // Blue theme for US Festival
            .withShowUserBadge(false)                             // Hide user badge in header
            .withCardPaymentOnly(false)                           // Allow multiple payment methods
            .withEntryPoint(entryPoint)                           // Handle payment resume/modification
            .withNavigationClickable(false)                       // Navigation only via buttons
            .withStickyHeader(stickyPriceHeader)                  // Sticky price header at top
            // Custom steps (before Your Information)
            .addCustomStep(createRegistrationTypePage())          // Preliminary: In-Person vs Online
            .addCustomStep(createAccommodationPage())             // Step 1: Your Room
            .addCustomStep(createBookingDetailsPage())            // Step 2: Festival Days, Meals, Options
            // Custom Your Information page with event header
            .withYourInformationPageSupplier(this::createYourInformationPageWithHeader)
            // Custom Member Selection page with event header
            .withMemberSelectionPageSupplier(this::createMemberSelectionPageWithHeader)
            .withCallbacks(this);

        this.form = builder.build();

        // Wire up section callbacks
        setupRegistrationTypeCallbacks();
        setupAccommodationCallbacks();
        setupAccommodationButtons();
        setupBookingDetailsCallbacks();
        setupBookingDetailsButtons();
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
     * This is needed because the form is created before the WorkingBooking is initialized.
     * The totalProperty fires when WorkingBooking calculates prices (after setWorkingBooking is called).
     */
    private void setupWorkingBookingListener() {
        if (workingBookingProperties == null) {
            Console.log("USFestivalBookingForm: WorkingBookingProperties not available, cannot set up listener");
            return;
        }

        // Listen to total property changes as an indicator that WorkingBooking has been initialized
        workingBookingProperties.totalProperty().addListener((obs, oldValue, newValue) -> {
            Console.log("USFestivalBookingForm: Total changed, attempting to populate options");
            // First populate event boundary dates from EventSelection/EventPart (needed by other methods)
            populateEventBoundaries();
            populateAccommodationOptions();
            populateFestivalDays();
            populateMealsOptions();
            populateAudioRecordingPhaseOptions();
            populateAdditionalOptions();
        });

        // Bind stickyPriceHeader's totalPrice to WorkingBookingProperties.totalProperty()
        // This ensures the header shows the actual booking total from WorkingBooking
        if (stickyPriceHeader != null) {
            stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
            Console.log("USFestivalBookingForm: Bound stickyPriceHeader.totalPrice to workingBookingProperties.totalProperty");
        }
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
            Console.log("USFestivalBookingForm: Early arrival part found - " +
                earlyArrivalPart.getStartDate() + " to " + earlyArrivalPart.getEndDate());
        }

        // Get late departure date from LateDeparturePart (period after main event)
        EventPart lateDeparturePart = policyAggregate.getLateDeparturePart();
        if (lateDeparturePart != null) {
            lateDepartureDate = lateDeparturePart.getEndDate();
            Console.log("USFestivalBookingForm: Late departure part found - " +
                lateDeparturePart.getStartDate() + " to " + lateDeparturePart.getEndDate());
        }

        Console.log("USFestivalBookingForm: Event boundaries populated - " +
            "mainEvent=" + eventStartDate + " to " + eventEndDate +
            ", earlyArrival=" + earlyArrivalDate + ", lateDeparture=" + lateDepartureDate);
    }

    // ========================================
    // Page Creation Methods
    // ========================================

    /**
     * Creates the Registration Type page (preliminary step).
     * Shows "In Person" (selectable) and "Online" (coming soon) options.
     */
    private CompositeBookingFormPage createRegistrationTypePage() {
        registrationTypeSection = new RegistrationTypeSection();
        registrationTypeSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        return new CompositeBookingFormPage(BookingPageI18nKeys.HowWouldYouLikeToAttend, registrationTypeSection)
            .setStep(false)  // Not a numbered step (preliminary)
            .setHeaderVisible(false)  // Hide step navigation on preliminary page
            .setShowingOwnSubmitButton(true);  // Has its own selection buttons
    }

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

    // ========================================
    // Callback Setup Methods
    // ========================================

    private void setupRegistrationTypeCallbacks() {
        if (registrationTypeSection == null) return;

        registrationTypeSection.setOnContinuePressed(() -> {
            // When in-person is selected, navigate to accommodation
            form.navigateToNextPage();
        });
    }

    private void setupAccommodationCallbacks() {
        if (accommodationSection == null) return;

        accommodationSection.setOnOptionSelected(option -> {
            Console.log("USFestivalBookingForm: Accommodation selected - " + option.getName());

            // Reset WorkingBooking to clear previous selections when accommodation type changes
            // This ensures the booking doesn't keep items from a previous accommodation selection
            if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                Console.log("USFestivalBookingForm: Resetting WorkingBooking due to accommodation change");
                workingBookingProperties.getWorkingBooking().cancelChanges();
            }

            // Pass min nights constraint and isDayVisitor to festival day section
            // Also reset dates when accommodation type changes
            if (festivalDaySection != null && option != null) {
                festivalDaySection.setMinNightsConstraint(option.getMinNights());
                festivalDaySection.setIsDayVisitor(option.isDayVisitor());
                // Reset dates when accommodation changes (user needs to re-select dates)
                festivalDaySection.reset();
                Console.log("USFestivalBookingForm: Festival day section reset - isDayVisitor=" + option.isDayVisitor() + ", minNights=" + option.getMinNights());
            }
            // Sync breakfast with accommodation selection
            // Day visitors don't get breakfast, overnight guests do
            if (mealsSection != null && option != null) {
                boolean hasAccommodation = !option.isDayVisitor();
                mealsSection.setHasAccommodation(hasAccommodation);
                Console.log("USFestivalBookingForm: Set hasAccommodation=" + hasAccommodation + " for meals section");
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
                    Console.log("USFestivalBookingForm: Roommate section visible for Share Accommodation");
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
                        Console.log("USFestivalBookingForm: Roommate section visible for multi-person room - capacity=" + capacity + ", minOccupancy=" + minOccupancy);
                    } else {
                        // Single-person room or no capacity info: hide roommate section
                        roommateInfoSection.setVisible(false);
                        Console.log("USFestivalBookingForm: Roommate section hidden (single-person room)");
                    }
                } else {
                    // Day Visitor or no item entity: hide roommate section
                    roommateInfoSection.setVisible(false);
                    Console.log("USFestivalBookingForm: Roommate section hidden (day visitor or no item)");
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

        // Set up buttons with validation binding for Continue button
        accommodationPage.setButtons(
            new BookingFormButton(BookingPageI18nKeys.Back,
                e -> form.navigateToPreviousPage(),
                "btn-back booking-form-btn-back"),
            new BookingFormButton(BookingPageI18nKeys.Continue,
                e -> form.navigateToNextPage(),
                "btn-primary booking-form-btn-primary",
                Bindings.not(accommodationPage.validProperty()))  // Disable when invalid
        );
    }

    private void setupBookingDetailsCallbacks() {
        if (festivalDaySection != null) {
            festivalDaySection.setOnDatesChanged((arrival, departure) -> {
                Console.log("USFestivalBookingForm: Dates changed - " + arrival + " to " + departure);
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
                }
            });

            // Listen for arrival time changes
            festivalDaySection.arrivalTimeProperty().addListener((obs, old, newTime) -> {
                Console.log("USFestivalBookingForm: Arrival time changed - " + newTime);
                if (mealsSection != null) {
                    mealsSection.setArrivalTime(newTime);
                }
                // Re-book items to update meal counts based on new arrival time
                bookSelectedItemsIntoWorkingBooking();
            });

            // Listen for departure time changes
            festivalDaySection.departureTimeProperty().addListener((obs, old, newTime) -> {
                Console.log("USFestivalBookingForm: Departure time changed - " + newTime);
                if (mealsSection != null) {
                    mealsSection.setDepartureTime(newTime);
                }
                // Re-book items to update meal counts based on new departure time
                bookSelectedItemsIntoWorkingBooking();
            });
        }

        // Set up audio recording phase selection callback
        if (audioRecordingPhaseSection != null) {
            audioRecordingPhaseSection.setOnOptionSelected(option -> {
                Console.log("USFestivalBookingForm: Audio recording phase selected - " +
                    (option != null ? option.getName() : "None"));
                // Re-book items to update audio recording selection
                bookSelectedItemsIntoWorkingBooking();
            });
        }

        // Set up meals section callbacks for meal and dietary selection changes
        setupMealsCallbacks();

        // Set up additional options section callbacks
        setupAdditionalOptionsCallbacks();
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
            Console.log("USFestivalBookingForm: Breakfast selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for lunch selection changes
        mealsSection.wantsLunchProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalBookingForm: Lunch selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for dinner selection changes
        mealsSection.wantsDinnerProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalBookingForm: Dinner selection changed - " + newVal);
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for dietary preference changes (API-driven - selectedDietaryItem)
        mealsSection.selectedDietaryItemProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalBookingForm: Dietary preference (API) changed - " +
                (newVal != null ? newVal.getName() : "None"));
            bookSelectedItemsIntoWorkingBooking();
        });

        // Listen for legacy dietary preference changes (enum-based - dietaryPreference)
        mealsSection.dietaryPreferenceProperty().addListener((obs, old, newVal) -> {
            Console.log("USFestivalBookingForm: Dietary preference (legacy) changed - " +
                (newVal != null ? newVal.name() : "None"));
            bookSelectedItemsIntoWorkingBooking();
        });
    }

    /**
     * Sets up callbacks for the additional options section to listen for selection changes.
     */
    private void setupAdditionalOptionsCallbacks() {
        if (additionalOptionsSection == null) return;

        // Listen for any option selection changes (parking, assisted listening, etc.)
        additionalOptionsSection.setOnSelectionChanged(() -> {
            Console.log("USFestivalBookingForm: Additional option selection changed");
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
            Console.log("USFestivalBookingForm: Event boundary dates not yet populated, skipping extended stay check");
            return;
        }

        // Check if the stay extends beyond event dates
        boolean isEarlyArrival = arrival.isBefore(eventStartDate);
        boolean isLateDeparture = departure.isAfter(eventEndDate);
        boolean hasExtendedStay = isEarlyArrival || isLateDeparture;

        Console.log("USFestivalBookingForm: Extended stay check - event: " + eventStartDate + " to " + eventEndDate +
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
                Console.log("USFestivalBookingForm: User logged out");
                // Only navigate if WorkingBooking is initialized to avoid NPE
                if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
                    Console.log("USFestivalBookingForm: Navigating to Your Information");
                    form.navigateToYourInformation();
                } else {
                    Console.log("USFestivalBookingForm: WorkingBooking not initialized, skipping navigation");
                }
            }
        });
    }

    private void loadMembersIfLoggedIn() {
        Person person = FXUserPerson.getUserPerson();
        if (person != null && memberSelectionSection != null) {
            Console.log("USFestivalBookingForm: Loading household members at construction time");
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
            Console.log("USFestivalBookingForm: Accommodation options already populated, skipping");
            return;
        }

        if (accommodationSection == null) {
            Console.log("USFestivalBookingForm: accommodationSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalBookingForm: WorkingBookingProperties not available yet");
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not initialized yet, will populate later");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available yet");
            return;
        }

        // Debug: log what we're working with
        int scheduledItemsCount = policyAggregate.getScheduledItems() != null ? policyAggregate.getScheduledItems().size() : 0;
        int accommodationItemsCount = policyAggregate.filterAccommodationScheduledItems().size();
        Console.log("USFestivalBookingForm: PolicyAggregate has " + scheduledItemsCount + " total scheduled items, " + accommodationItemsCount + " accommodation items");

        Console.log("USFestivalBookingForm: Populating accommodation options with WorkingBooking-based pricing");

        // Use event boundary dates for price calculation
        LocalDate arrivalDate = eventStartDate;
        LocalDate departureDate = eventEndDate;
        if (arrivalDate == null || departureDate == null) {
            Console.log("USFestivalBookingForm: Event boundary dates not available, cannot calculate prices");
            return;
        }

        Console.log("USFestivalBookingForm: Event boundaries for price calculation: " + arrivalDate + " to " + departureDate);

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

            Console.log("USFestivalBookingForm: Processing accommodation: " + item.getName());

            // Calculate availability
            int minAvailability = scheduledItems.stream()
                .mapToInt(si -> si.getGuestsAvailability() != null ? si.getGuestsAvailability() : 0)
                .min()
                .orElse(0);

            HasAccommodationSelectionSection.AvailabilityStatus status;
            if (minAvailability <= 0) {
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
            AccommodationPriceResult priceResult = calculateAccommodationPriceWithWorkingBooking(policyAggregate, item, arrivalDate, departureDate);
            Console.log("USFestivalBookingForm: Calculated price for " + item.getName() + ": " + priceResult.totalPrice);

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
        Console.log("USFestivalBookingForm: Accommodation options populated successfully with WorkingBooking prices");
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
     * @return the calculated result with total price and breakdown
     */
    private AccommodationPriceResult calculateAccommodationPriceWithWorkingBooking(PolicyAggregate policyAggregate, Item accommodationItem,
                                                               LocalDate arrivalDate, LocalDate departureDate) {
        // Create a temporary WorkingBooking for price calculation
        WorkingBooking tempBooking = new WorkingBooking(policyAggregate, null);

        Console.log("USFestivalBookingForm: Creating temp WorkingBooking for " +
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
        Console.log("USFestivalBookingForm: Booking " + teachingItems.size() + " teaching items for price calculation");
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
            Console.log("USFestivalBookingForm: Booking " + accommodationScheduledItems.size() + " accommodation items for " + accommodationItem.getName());
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
        Console.log("USFestivalBookingForm: Total price calculated: " + totalPrice + " for " +
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
                "Accommodation", accoDateRange, accoPrice));
        }

        // Meals breakdown - split by meal type (Breakfast, Lunch, Dinner)
        List<DocumentLine> mealsLines = tempBooking.getFamilyDocumentLines(KnownItemFamily.MEALS);
        if (!mealsLines.isEmpty()) {
            // Get meal Items from PolicyAggregate timelines
            one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
            one.modality.base.shared.entities.Timeline lunchTimeline = policyAggregate.getLunchTimeline();
            one.modality.base.shared.entities.Timeline dinnerTimeline = policyAggregate.getDinnerTimeline();

            Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;
            Item lunchItem = lunchTimeline != null ? lunchTimeline.getItem() : null;
            Item dinnerItem = dinnerTimeline != null ? dinnerTimeline.getItem() : null;

            Console.log("USFestivalBookingForm: Meal items from timelines - breakfast=" +
                (breakfastItem != null ? breakfastItem.getName() : "null") + ", lunch=" +
                (lunchItem != null ? lunchItem.getName() : "null") + ", dinner=" +
                (dinnerItem != null ? dinnerItem.getName() : "null"));

            // Categorize meal lines by matching Item entity
            List<DocumentLine> breakfastLines = new ArrayList<>();
            List<DocumentLine> lunchLines = new ArrayList<>();
            List<DocumentLine> dinnerLines = new ArrayList<>();

            for (DocumentLine line : mealsLines) {
                Item item = line.getItem();
                if (item == null) continue;

                // Match by Item entity (using primary key comparison)
                if (breakfastItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(item, breakfastItem)) {
                    breakfastLines.add(line);
                } else if (lunchItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(item, lunchItem)) {
                    lunchLines.add(line);
                } else if (dinnerItem != null && dev.webfx.stack.orm.entity.Entities.samePrimaryKey(item, dinnerItem)) {
                    dinnerLines.add(line);
                } else {
                    Console.log("USFestivalBookingForm: Unknown meal type - item=" + item.getName());
                }
            }

            Console.log("USFestivalBookingForm: Meal breakdown - breakfast=" + breakfastLines.size() +
                ", lunch=" + lunchLines.size() + ", dinner=" + dinnerLines.size());

            // Add breakdown for each meal type
            addMealTypeBreakdown(breakdown, calc, "Breakfast", breakfastLines);
            addMealTypeBreakdown(breakdown, calc, "Lunch", lunchLines);
            addMealTypeBreakdown(breakdown, calc, "Dinner", dinnerLines);
        }

        return new AccommodationPriceResult(totalPrice, breakdown);
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

        Console.log("USFestivalBookingForm: Calculating Share Accommodation price with item: " +
            (sharingAccommodationItem != null ? sharingAccommodationItem.getName() : "null"));

        // Track dates for breakdown display
        LocalDate teachingMinDate = null;
        LocalDate teachingMaxDate = null;

        // 1. Book all teachings within event boundaries (they have ScheduledItems)
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems().stream()
            .filter(si -> si.getDate() != null)
            .filter(si -> !si.getDate().isBefore(arrivalDate) && !si.getDate().isAfter(departureDate))
            .collect(java.util.stream.Collectors.toList());
        Console.log("USFestivalBookingForm: Booking " + teachingItems.size() + " teaching items for share accommodation price calculation");
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
            Console.log("USFestivalBookingForm: Sharing accommodation price: " + pricePerNight + " x " + accommodationNightsCount + " nights = " + sharingAccommodationPrice);
        }

        // 3. Book all meals within event boundaries (they have ScheduledItems)
        // hasAccommodation = true since sharing accommodation guests get breakfast
        bookMealsForPriceCalculation(tempBooking, policyAggregate, arrivalDate, departureDate, true);

        // Calculate teachings + meals price from WorkingBooking
        int teachingsAndMealsPrice = tempBooking.calculateTotal();
        Console.log("USFestivalBookingForm: Teachings + meals price: " + teachingsAndMealsPrice);

        // Total = teachings + meals + sharing accommodation
        int totalPrice = teachingsAndMealsPrice + sharingAccommodationPrice;
        Console.log("USFestivalBookingForm: Share Accommodation total price calculated: " + totalPrice);

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
        List<DocumentLine> mealsLines = tempBooking.getFamilyDocumentLines(KnownItemFamily.MEALS);
        if (!mealsLines.isEmpty()) {
            // Categorize meal lines by type using item name
            List<DocumentLine> breakfastLines = new ArrayList<>();
            List<DocumentLine> lunchLines = new ArrayList<>();
            List<DocumentLine> dinnerLines = new ArrayList<>();

            for (DocumentLine line : mealsLines) {
                Item item = line.getItem();
                if (item == null) continue;

                String itemName = item.getName() != null ? item.getName() : "";
                String itemNameLower = itemName.toLowerCase();

                if (itemNameLower.equals("breakfast") || itemNameLower.contains("breakfast")) {
                    breakfastLines.add(line);
                } else if (itemNameLower.equals("lunch") || itemNameLower.contains("lunch")) {
                    lunchLines.add(line);
                } else if (itemNameLower.equals("dinner") || itemNameLower.equals("supper") ||
                           itemNameLower.contains("dinner") || itemNameLower.contains("supper")) {
                    dinnerLines.add(line);
                }
            }

            // Add breakdown for each meal type
            addMealTypeBreakdown(breakdown, calc, "Breakfast", breakfastLines);
            addMealTypeBreakdown(breakdown, calc, "Lunch", lunchLines);
            addMealTypeBreakdown(breakdown, calc, "Dinner", dinnerLines);
        }

        return new AccommodationPriceResult(totalPrice, breakdown);
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
     * Adds a meal type breakdown item if there are lines for that type.
     * Shows the date range from the document lines.
     */
    private void addMealTypeBreakdown(List<USFestivalAccommodationSelectionSection.PriceBreakdownItem> breakdown,
                                       PriceCalculator calc,
                                       String mealType, List<DocumentLine> mealLines) {
        if (mealLines.isEmpty()) return;

        int price = calc.calculateDocumentLinesPrice(mealLines);
        if (price <= 0) return;

        // Get date range from document lines
        LocalDate minDate = null;
        LocalDate maxDate = null;
        for (DocumentLine line : mealLines) {
            LocalDate start = line.getStartDate();
            LocalDate end = line.getEndDate();
            if (start != null) {
                if (minDate == null || start.isBefore(minDate)) minDate = start;
                if (maxDate == null || start.isAfter(maxDate)) maxDate = start;
            }
            if (end != null) {
                if (maxDate == null || end.isAfter(maxDate)) maxDate = end;
            }
        }

        String dateRange = formatDateRange(minDate, maxDate);
        breakdown.add(new USFestivalAccommodationSelectionSection.PriceBreakdownItem(
            mealType, dateRange, price));
    }

    /**
     * Books meals into a temporary WorkingBooking for price calculation.
     * Uses EventPart boundaries to determine which meals to include - all ScheduledItems
     * between the start boundary scheduledItem and end boundary scheduledItem.
     *
     * @param workingBooking the temporary WorkingBooking
     * @param policyAggregate the policy data
     * @param arrivalDate the arrival date (used for fallback if no boundaries)
     * @param departureDate the departure date (used for fallback if no boundaries)
     * @param hasAccommodation true if booking includes accommodation (for breakfast calculation)
     */
    private void bookMealsForPriceCalculation(WorkingBooking workingBooking, PolicyAggregate policyAggregate,
                                               LocalDate arrivalDate, LocalDate departureDate, boolean hasAccommodation) {
        List<ScheduledItem> mealsItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.MEALS);
        if (mealsItems.isEmpty()) {
            Console.log("USFestivalBookingForm: No meals items for price calculation");
            return;
        }

        // Get the main EventPart to find boundaries
        one.modality.base.shared.entities.EventPart mainEventPart = findMainEventPart(policyAggregate);

        // Get boundary ScheduledItems - these define the first and last meals of the event
        ScheduledItem startBoundaryMeal = null;
        ScheduledItem endBoundaryMeal = null;

        if (mainEventPart != null) {
            one.modality.base.shared.entities.ScheduledBoundary startBoundary = mainEventPart.getStartBoundary();
            one.modality.base.shared.entities.ScheduledBoundary endBoundary = mainEventPart.getEndBoundary();

            if (startBoundary != null) {
                startBoundaryMeal = startBoundary.getScheduledItem();
            }
            if (endBoundary != null) {
                endBoundaryMeal = endBoundary.getScheduledItem();
            }

            Console.log("USFestivalBookingForm: Main EventPart boundaries - start: " +
                (startBoundaryMeal != null ? startBoundaryMeal.getPrimaryKey() + " (" + (startBoundaryMeal.getItem() != null ? startBoundaryMeal.getItem().getName() : "null") + ")" : "null") +
                ", end: " +
                (endBoundaryMeal != null ? endBoundaryMeal.getPrimaryKey() + " (" + (endBoundaryMeal.getItem() != null ? endBoundaryMeal.getItem().getName() : "null") + ")" : "null"));
        }

        // Get boundary date/time for comparison
        LocalDate startDate = null;
        LocalTime startTime = null;
        LocalDate endDate = null;
        LocalTime endTime = null;

        if (startBoundaryMeal != null) {
            startDate = startBoundaryMeal.getDate();
            startTime = startBoundaryMeal.getStartTime();
            if (startTime == null && startBoundaryMeal.getTimeline() != null) {
                startTime = startBoundaryMeal.getTimeline().getStartTime();
            }
        }
        if (endBoundaryMeal != null) {
            endDate = endBoundaryMeal.getDate();
            endTime = endBoundaryMeal.getStartTime();
            if (endTime == null && endBoundaryMeal.getTimeline() != null) {
                endTime = endBoundaryMeal.getTimeline().getStartTime();
            }
        }

        // Fallback to arrival/departure dates if no boundary info
        if (startDate == null) startDate = arrivalDate;
        if (endDate == null) endDate = departureDate;

        Console.log("USFestivalBookingForm: Meal boundaries - start: " + startDate + " " + startTime +
            ", end: " + endDate + " " + endTime);

        final LocalDate finalStartDate = startDate;
        final LocalTime finalStartTime = startTime;
        final LocalDate finalEndDate = endDate;
        final LocalTime finalEndTime = endTime;

        // Build accommodation nights set for breakfast calculation
        java.util.Set<LocalDate> accommodationNights = new java.util.HashSet<>();
        if (hasAccommodation) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                accommodationNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
        }

        // Get breakfast Item from PolicyAggregate timeline for comparison
        one.modality.base.shared.entities.Timeline breakfastTimeline = policyAggregate.getBreakfastTimeline();
        final Item breakfastItem = breakfastTimeline != null ? breakfastTimeline.getItem() : null;

        // Filter meals that fall within the boundary range
        List<ScheduledItem> mealsToBook = mealsItems.stream()
            .filter(msi -> {
                LocalDate mealDate = msi.getDate();
                if (mealDate == null) return false;

                // Get meal time for comparison
                LocalTime mealTime = msi.getStartTime();
                if (mealTime == null && msi.getTimeline() != null) {
                    mealTime = msi.getTimeline().getStartTime();
                }

                // Check if meal is within boundary range
                return isWithinBoundaries(mealDate, mealTime, finalStartDate, finalStartTime, finalEndDate, finalEndTime);
            })
            .filter(msi -> {
                // For breakfast with accommodation, only include if guest stayed overnight
                if (!hasAccommodation) return true;

                // Determine if this is a breakfast by matching Item entity
                Item item = msi.getItem();
                if (item == null) return true;

                boolean isBreakfast = breakfastItem != null &&
                    dev.webfx.stack.orm.entity.Entities.samePrimaryKey(item, breakfastItem);

                if (isBreakfast) {
                    LocalDate mealDate = msi.getDate();
                    if (mealDate == null) return false;
                    LocalDate nightBefore = mealDate.minusDays(1);
                    return accommodationNights.contains(nightBefore);
                }
                return true;
            })
            .collect(java.util.stream.Collectors.toList());

        Console.log("USFestivalBookingForm: Booking " + mealsToBook.size() + " meal items within boundaries for price calculation");

        if (!mealsToBook.isEmpty()) {
            workingBooking.bookScheduledItems(mealsToBook, false);
        }
    }

    /**
     * Finds the main EventPart (the one matching the event's start/end dates).
     */
    private one.modality.base.shared.entities.EventPart findMainEventPart(PolicyAggregate policyAggregate) {
        one.modality.base.shared.entities.Event event = policyAggregate.getEvent();
        if (event == null) return null;

        LocalDate eventStart = event.getStartDate();
        LocalDate eventEnd = event.getEndDate();
        if (eventStart == null || eventEnd == null) return null;

        dev.webfx.stack.orm.entity.EntityList<one.modality.base.shared.entities.EventPart> eventParts = policyAggregate.getEventParts();
        if (eventParts == null) return null;

        for (one.modality.base.shared.entities.EventPart part : eventParts) {
            LocalDate partStart = part.getStartDate();
            LocalDate partEnd = part.getEndDate();
            if (partStart != null && partEnd != null &&
                partStart.equals(eventStart) && partEnd.equals(eventEnd)) {
                Console.log("USFestivalBookingForm: Found main EventPart: " + part.getName());
                return part;
            }
        }

        Console.log("USFestivalBookingForm: No main EventPart found matching event dates");
        return null;
    }

    /**
     * Checks if a meal (date + time) falls within the boundary range.
     * A meal is included if it's >= start boundary and <= end boundary.
     */
    private boolean isWithinBoundaries(LocalDate mealDate, LocalTime mealTime,
                                        LocalDate startDate, LocalTime startTime,
                                        LocalDate endDate, LocalTime endTime) {
        if (mealDate == null || startDate == null || endDate == null) return false;

        // Check start boundary (meal must be >= start)
        if (mealDate.isBefore(startDate)) return false;
        if (mealDate.equals(startDate) && startTime != null && mealTime != null) {
            if (mealTime.isBefore(startTime)) return false;
        }

        // Check end boundary (meal must be <= end)
        if (mealDate.isAfter(endDate)) return false;
        if (mealDate.equals(endDate) && endTime != null && mealTime != null) {
            if (mealTime.isAfter(endTime)) return false;
        }

        return true;
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
            Console.log("USFestivalBookingForm: No sharing accommodation item configured - skipping Share Accommodation option");
            return;
        }
        Item sharingAccommodationItem = sharingAccommodationItemPolicy.getItem();
        if (sharingAccommodationItem == null) {
            Console.log("USFestivalBookingForm: Sharing accommodation ItemPolicy has no item - skipping");
            return;
        }

        Console.log("USFestivalBookingForm: Found sharing accommodation item: " + sharingAccommodationItem.getName());

        // Calculate price and breakdown using the sharing accommodation item
        AccommodationPriceResult priceResult = calculateShareAccommodationPriceWithWorkingBooking(policyAggregate, sharingAccommodationItem, arrivalDate, departureDate);
        Console.log("USFestivalBookingForm: Calculated Share Accommodation price: " + priceResult.totalPrice);

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
        Console.log("USFestivalBookingForm: Share Accommodation option added with item '" + sharingAccommodationItem.getName() + "' and price: " + priceResult.totalPrice);
    }

    /**
     * Adds the Day Visitor option with WorkingBooking-calculated price.
     * Day Visitors don't have accommodation items - they only book meals and teachings.
     */
    private void addDayVisitorOptionWithPrice(PolicyAggregate policyAggregate, LocalDate arrivalDate, LocalDate departureDate) {
        if (accommodationSection == null) return;

        // Calculate price and breakdown (teachings + meals, no accommodation, no breakfast since no overnight stay)
        AccommodationPriceResult priceResult = calculateAccommodationPriceWithWorkingBooking(policyAggregate, null, arrivalDate, departureDate);
        Console.log("USFestivalBookingForm: Calculated Day Visitor price: " + priceResult.totalPrice);

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
        Console.log("USFestivalBookingForm: Day Visitor option added with price: " + priceResult.totalPrice);
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
            Console.log("USFestivalBookingForm: Festival days already populated, skipping");
            return;
        }

        if (festivalDaySection == null) {
            Console.log("USFestivalBookingForm: festivalDaySection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalBookingForm: WorkingBookingProperties not available yet for festival days");
            return;
        }

        // Check if WorkingBooking is initialized before accessing PolicyAggregate
        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not initialized yet for festival days");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available yet for festival days");
            return;
        }

        Console.log("USFestivalBookingForm: Populating festival days from PolicyAggregate");

        // Populate festival days from PolicyAggregate
        festivalDaySection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        festivalDaysPopulated = true;
        Console.log("USFestivalBookingForm: Festival days populated successfully");
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
            Console.log("USFestivalBookingForm: Meals options already populated, skipping");
            return;
        }

        if (mealsSection == null) {
            Console.log("USFestivalBookingForm: mealsSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalBookingForm: WorkingBookingProperties not available yet for meals");
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not initialized yet for meals");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available yet for meals");
            return;
        }

        Console.log("USFestivalBookingForm: Populating meals options from PolicyAggregate");

        // Populate meals options from PolicyAggregate
        mealsSection.populateFromPolicyAggregate(policyAggregate);

        // Set WorkingBooking for DocumentBill-based pricing
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        if (workingBooking != null) {
            mealsSection.setWorkingBooking(workingBooking);
            Console.log("USFestivalBookingForm: Set WorkingBooking on mealsSection for DocumentBill pricing");
        }

        // Mark as populated to avoid re-populating
        mealsOptionsPopulated = true;
        Console.log("USFestivalBookingForm: Meals options populated successfully");
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
            Console.log("USFestivalBookingForm: Audio recording phase options already populated, skipping");
            return;
        }

        if (audioRecordingPhaseSection == null) {
            Console.log("USFestivalBookingForm: audioRecordingPhaseSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not available yet for audio recording phase options");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available yet for audio recording phase options");
            return;
        }

        // Check if there are multiple phase coverages for audio recording
        java.util.List<one.modality.base.shared.entities.PhaseCoverage> phaseCoverages =
            policyAggregate.getAudioRecordingPhaseCoverages();

        if (phaseCoverages == null || phaseCoverages.size() <= 1) {
            // No phase coverages or only one - hide section and let additional options handle audio recording
            Console.log("USFestivalBookingForm: " + (phaseCoverages == null ? 0 : phaseCoverages.size()) +
                " audio recording phase coverages found - hiding phase section");
            audioRecordingPhaseSection.setVisible(false);
            audioRecordingPhasePopulated = true;
            return;
        }

        Console.log("USFestivalBookingForm: Populating audio recording phase options from PolicyAggregate");

        // Set workingBookingProperties on the section (needed for price calculation)
        audioRecordingPhaseSection.setWorkingBookingProperties(workingBookingProperties);

        // Populate the section from PolicyAggregate
        audioRecordingPhaseSection.populateFromPolicyAggregate(policyAggregate);

        // Show the section
        audioRecordingPhaseSection.setVisible(true);

        // Mark as populated to avoid re-populating
        audioRecordingPhasePopulated = true;
        Console.log("USFestivalBookingForm: Audio recording phase options populated successfully with " +
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
            Console.log("USFestivalBookingForm: Additional options already populated, skipping");
            return;
        }

        if (additionalOptionsSection == null) {
            Console.log("USFestivalBookingForm: additionalOptionsSection is null, skipping population");
            return;
        }

        if (workingBookingProperties == null) {
            Console.log("USFestivalBookingForm: WorkingBookingProperties not available yet for additional options");
            return;
        }

        if (workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not initialized yet for additional options");
            return;
        }

        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available yet for additional options");
            return;
        }

        Console.log("USFestivalBookingForm: Populating additional options from PolicyAggregate");

        // Exclude audio recording items if they're handled by the dedicated phase section
        boolean audioPhaseVisible = audioRecordingPhaseSection != null && audioRecordingPhaseSection.getView().isVisible();
        additionalOptionsSection.setExcludeAudioRecording(audioPhaseVisible);
        Console.log("USFestivalBookingForm: Audio recording excluded from additional options: " + audioPhaseVisible);

        // Populate additional options from PolicyAggregate
        additionalOptionsSection.populateFromPolicyAggregate(policyAggregate);

        // Mark as populated to avoid re-populating
        additionalOptionsPopulated = true;
        Console.log("USFestivalBookingForm: Additional options populated successfully");
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
                Console.log("USFestivalBookingForm: Meal item '" + item.getName() + "' rate=" + dailyMealRate + "/day * " + daysToCharge + " = " + (dailyMealRate * daysToCharge));
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

    /**
     * Books all selected items (teaching, accommodation, meals, additional options)
     * into the WorkingBooking so they appear in the price breakdown.
     */
    private void bookSelectedItemsIntoWorkingBooking() {
        Console.log("USFestivalBookingForm.bookSelectedItemsIntoWorkingBooking() - Booking selected items");

        if (workingBookingProperties == null || workingBookingProperties.getWorkingBooking() == null) {
            Console.log("USFestivalBookingForm: WorkingBooking not available, skipping item booking");
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBookingProperties.getPolicyAggregate();
        if (policyAggregate == null) {
            Console.log("USFestivalBookingForm: PolicyAggregate not available, skipping item booking");
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
            if (additionalOptionsSection.needsParking()) {
                HasAdditionalOptionsSection.ParkingType parkingType = additionalOptionsSection.getParkingType();
                requestText.append("Parking: ").append(parkingType != null ? parkingType.name().toLowerCase() : "standard").append("\n");
            }
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
            Console.log("USFestivalBookingForm: Adding booking details to request: " + request);
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
            Console.log("USFestivalBookingForm: No accommodation lines found for setShareOwnerInfo");
            return;
        }

        // Set share owner info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareOwnerInfo(line, matesNames);
            Console.log("USFestivalBookingForm: Set share owner info with " + matesNames.length + " mates on DocumentLine");
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
            Console.log("USFestivalBookingForm: No accommodation lines found for setShareMateInfo");
            return;
        }

        // Set share mate info on each accommodation line
        for (one.modality.base.shared.entities.DocumentLine line : accommodationLines) {
            workingBooking.setShareMateInfo(line, ownerName);
            Console.log("USFestivalBookingForm: Set share mate info with owner '" + ownerName + "' on DocumentLine");
        }
    }

    /**
     * Books teaching ScheduledItems based on selected festival days.
     */
    private void bookTeachingItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        // Get teaching scheduled items - for now, book all teaching items
        // TODO: If festivalDaySection has specific day selection, filter by those dates
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();
        if (!teachingItems.isEmpty()) {
            Console.log("USFestivalBookingForm: Booking " + teachingItems.size() + " teaching items");
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
        Console.log("USFestivalBookingForm: Booking sharing accommodation item: " + sharingAccommodationItem.getName());

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
                Console.log("USFestivalBookingForm: Using fallback dates from teaching for sharing accommodation: " + arrivalDate + " to " + departureDate);
            }
        }

        if (arrivalDate == null || departureDate == null) {
            Console.log("USFestivalBookingForm: No dates available for sharing accommodation - skipping");
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
            Console.log("USFestivalBookingForm: No accommodation dates calculated - skipping");
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
        Console.log("USFestivalBookingForm: Booked sharing accommodation for " + accommodationDates.size() + " nights");
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
            Console.log("USFestivalBookingForm: No accommodation selected - skipping accommodation booking");
            return;
        }

        // Day visitor - no accommodation booking
        if (selectedOption.isDayVisitor()) {
            Console.log("USFestivalBookingForm: Day visitor - skipping accommodation booking");
            return;
        }

        Item selectedItem = selectedOption.getItemEntity();
        if (selectedItem == null) {
            Console.log("USFestivalBookingForm: Selected accommodation has no itemEntity - skipping");
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
                Console.log("USFestivalBookingForm: Using fallback dates from teaching for accommodation: " + arrivalDate + " to " + departureDate);
            }
        }

        if (arrivalDate != null && departureDate != null) {
            LocalDate currentDate = arrivalDate;
            while (currentDate.isBefore(departureDate)) {
                selectedNights.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }
            Console.log("USFestivalBookingForm: Selected accommodation nights: " + selectedNights);
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
                Console.log("USFestivalBookingForm: WARNING - No accommodation items match selected dates, using all items as fallback");
                accommodationItems = allAccommodationItems;
            }
        } else {
            accommodationItems = allAccommodationItems;
        }

        if (!accommodationItems.isEmpty()) {
            Console.log("USFestivalBookingForm: Booking " + accommodationItems.size() + " accommodation items for " + selectedOption.getName());
            workingBooking.bookScheduledItems(accommodationItems, false);
        } else {
            Console.log("USFestivalBookingForm: No accommodation ScheduledItems found for " + selectedOption.getName());
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
            Console.log("USFestivalBookingForm: No meals items in policy - skipping");
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
                Console.log("USFestivalBookingForm: Using fallback dates from teaching: " + arrivalDate + " to " + departureDate);
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
        Console.log("USFestivalBookingForm: Accommodation nights for breakfast calculation: " + accommodationNights);
        Console.log("USFestivalBookingForm: Arrival date: " + arrivalDate + ", Departure date: " + departureDate);
        Console.log("USFestivalBookingForm: Arrival time: " + arrivalTime + ", Departure time: " + departureTime);

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

            Console.log("USFestivalBookingForm: Processing meal item '" + item.getName() + "' → isBreakfast=" + isBreakfast + ", isLunch=" + isLunch + ", isDinner=" + isDinner);

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
                    Console.log("USFestivalBookingForm: Filtered breakfast items to dates after accommodation nights: " +
                        itemScheduledItems.stream().map(msi -> msi.getDate().toString()).collect(java.util.stream.Collectors.joining(", ")));
                }

                // For lunch: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip lunch on arrival day if arriving AFTERNOON or later
                // - Skip lunch on departure day if departing MORNING
                if (isLunch && finalArrivalDate != null && finalDepartureDate != null) {
                    Console.log("USFestivalBookingForm: Filtering lunch items. Arrival: " + finalArrivalDate + " (" + finalArrivalTime + "), Departure: " + finalDepartureDate + " (" + finalDepartureTime + ")");
                    Console.log("USFestivalBookingForm: Lunch items before filter: " + itemScheduledItems.size());
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            Console.log("USFestivalBookingForm: Checking lunch on " + mealDate);
                            if (mealDate == null) {
                                Console.log("USFestivalBookingForm: Lunch date is null, skipping");
                                return false;
                            }

                            // Exclude meals outside the stay period (before arrival or after departure)
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) {
                                Console.log("USFestivalBookingForm: Skipping lunch on " + mealDate + " (outside stay period " + finalArrivalDate + " to " + finalDepartureDate + ")");
                                return false;
                            }

                            // Arrival day: skip lunch if arriving AFTERNOON or EVENING
                            if (mealDate.equals(finalArrivalDate)) {
                                Console.log("USFestivalBookingForm: Lunch on arrival day. arrivalTime=" + finalArrivalTime);
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON ||
                                    finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    Console.log("USFestivalBookingForm: Skipping lunch on arrival day " + mealDate + " (arriving " + finalArrivalTime + ")");
                                    return false;
                                }
                            }

                            // Departure day: skip lunch if departing MORNING
                            if (mealDate.equals(finalDepartureDate)) {
                                Console.log("USFestivalBookingForm: Lunch on departure day. departureTime=" + finalDepartureTime);
                                if (finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING) {
                                    Console.log("USFestivalBookingForm: Skipping lunch on departure day " + mealDate + " (departing MORNING)");
                                    return false;
                                }
                            }

                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalBookingForm: Lunch items after filter: " + itemScheduledItems.size());
                }

                // For dinner: filter based on stay period and arrival/departure time
                // - Exclude meals before arrival date or after departure date
                // - Skip dinner on arrival day if arriving EVENING
                // - Skip dinner on departure day if departing MORNING or AFTERNOON
                if (isDinner && finalArrivalDate != null && finalDepartureDate != null) {
                    Console.log("USFestivalBookingForm: Filtering dinner/supper items. Arrival: " + finalArrivalDate + " (" + finalArrivalTime + "), Departure: " + finalDepartureDate + " (" + finalDepartureTime + ")");
                    Console.log("USFestivalBookingForm: Dinner items before filter: " + itemScheduledItems.size());
                    itemScheduledItems = itemScheduledItems.stream()
                        .filter(msi -> {
                            java.time.LocalDate mealDate = msi.getDate();
                            Console.log("USFestivalBookingForm: Checking dinner on " + mealDate);
                            if (mealDate == null) {
                                Console.log("USFestivalBookingForm: Dinner date is null, skipping");
                                return false;
                            }

                            // Exclude meals outside the stay period (before arrival or after departure)
                            if (mealDate.isBefore(finalArrivalDate) || mealDate.isAfter(finalDepartureDate)) {
                                Console.log("USFestivalBookingForm: Skipping dinner on " + mealDate + " (outside stay period " + finalArrivalDate + " to " + finalDepartureDate + ")");
                                return false;
                            }

                            // Arrival day: skip dinner if arriving EVENING
                            if (mealDate.equals(finalArrivalDate)) {
                                Console.log("USFestivalBookingForm: Dinner on arrival day. arrivalTime=" + finalArrivalTime);
                                if (finalArrivalTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.EVENING) {
                                    Console.log("USFestivalBookingForm: Skipping dinner on arrival day " + mealDate + " (arriving EVENING)");
                                    return false;
                                }
                            }

                            // Departure day: skip dinner if departing MORNING or AFTERNOON
                            if (mealDate.equals(finalDepartureDate)) {
                                boolean isMorning = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING;
                                boolean isAfternoon = finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON;
                                Console.log("USFestivalBookingForm: Dinner on departure day " + mealDate + ". departureTime=" + finalDepartureTime + " (isMorning=" + isMorning + ", isAfternoon=" + isAfternoon + ")");
                                if (isMorning || isAfternoon) {
                                    Console.log("USFestivalBookingForm: Skipping dinner on departure day " + mealDate + " (departing " + finalDepartureTime + ")");
                                    return false;
                                } else {
                                    Console.log("USFestivalBookingForm: KEEPING dinner on departure day " + mealDate + " (departing " + finalDepartureTime + " = EVENING)");
                                }
                            } else {
                                Console.log("USFestivalBookingForm: Dinner on " + mealDate + " (not departure day " + finalDepartureDate + ") - KEEPING");
                            }

                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalBookingForm: Dinner items after filter: " + itemScheduledItems.size());
                }

                if (!itemScheduledItems.isEmpty()) {
                    Console.log("USFestivalBookingForm: Booking " + itemScheduledItems.size() + " " + item.getName() + " items (addOnly=false to replace)");
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
                Console.log("USFestivalBookingForm: No API dietary item, looking for legacy '" + prefName + "' item");

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
                    Console.log("USFestivalBookingForm: Found legacy dietary item: " + selectedDietaryItem.getName());
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
                Console.log("USFestivalBookingForm: Unbooking previous dietary item '" + dietItem.getName() + "'");
                workingBooking.unbookItem(dietSite, dietItem);
            }
        }

        if (selectedDietaryItem == null) {
            Console.log("USFestivalBookingForm: No dietary preference selected (neither API nor legacy)");
            return;
        }

        // Get the site from ItemPolicy.getScope().getSite()
        one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(selectedDietaryItem);
        one.modality.base.shared.entities.Site site = null;
        if (itemPolicy != null && itemPolicy.getScope() != null) {
            site = itemPolicy.getScope().getSite();
        }

        // Dietary items are non-temporal - book without attendance records (no dates)
        Console.log("USFestivalBookingForm: Booking dietary preference '" + selectedDietaryItem.getName() + "' (non-temporal, no attendance dates)" +
            (site != null ? " at site " + site.getName() : ""));
        workingBooking.bookAtemporalItem(site, selectedDietaryItem);
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
                Console.log("USFestivalBookingForm: Unbooking " + allAudioItems.size() + " audio recording items (none selected)");
                workingBooking.unbookScheduledItems(allAudioItems);
            }
            return;
        }

        // Get the scheduled items directly from the selected option
        // This is more accurate as it includes both phase date filtering AND language (Item) filtering
        List<ScheduledItem> itemsToBook = selectedOption.getScheduledItems();

        if (itemsToBook == null || itemsToBook.isEmpty()) {
            Console.log("USFestivalBookingForm: Selected phase has no scheduled items, unbooking all audio items");
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
            Console.log("USFestivalBookingForm: Unbooking " + itemsToUnbook.size() + " audio recording items not in selected option");
            workingBooking.unbookScheduledItems(itemsToUnbook);
        }

        Console.log("USFestivalBookingForm: Booking " + itemsToBook.size() + " audio recording items for " +
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
        if (additionalOptionsSection == null) return;

        // Get all selected additional options
        List<HasAdditionalOptionsSection.AdditionalOption> selectedOptions = additionalOptionsSection.getSelectedOptions();

        if (selectedOptions.isEmpty()) {
            Console.log("USFestivalBookingForm: No additional options selected");
            return;
        }

        Console.log("USFestivalBookingForm: Processing " + selectedOptions.size() + " selected additional options");

        // Book each selected option
        for (HasAdditionalOptionsSection.AdditionalOption option : selectedOptions) {
            Item itemEntity = option.getItemEntity();
            if (itemEntity == null) {
                Console.log("USFestivalBookingForm: Option '" + option.getName() + "' has no item entity, skipping");
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
                    Console.log("USFestivalBookingForm: Booking " + scheduledItems.size() + " '" + option.getName() + "' items (temporal, with attendance dates)");
                    workingBooking.bookScheduledItems(scheduledItems, true);
                } else {
                    Console.log("USFestivalBookingForm: No scheduled items found for temporal '" + option.getName() + "'");
                }
            } else {
                // Non-temporal item - book without attendance records (no dates)
                // Get the site from ItemPolicy.getScope().getSite()
                one.modality.base.shared.entities.ItemPolicy itemPolicy = policyAggregate.getItemPolicy(itemEntity);
                one.modality.base.shared.entities.Site site = null;
                if (itemPolicy != null && itemPolicy.getScope() != null) {
                    site = itemPolicy.getScope().getSite();
                }

                Console.log("USFestivalBookingForm: Booking '" + option.getName() + "' (non-temporal, no attendance dates)" +
                    (site != null ? " at site " + site.getName() : ""));
                workingBooking.bookAtemporalItem(site, itemEntity);
            }
        }
    }

    /**
     * Books transport/shuttle items with special handling for outbound/return directions.
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
                Console.log("USFestivalBookingForm: Booking " + scheduledItems.size() + " '" + transportItem.getName() + "' transport items");
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

        Console.log("USFestivalBookingForm: Updating sticky header - room=" + roomName + ", days=" + daysCount);

        // Only update room name and days - price is bound to WorkingBookingProperties.totalProperty()
        stickyPriceHeader.setRoomName(roomName);
        stickyPriceHeader.setSelectedDays(daysCount);
    }

    /**
     * Returns the built form for use in the UI.
     */
    public StandardBookingForm getForm() {
        return form;
    }
}
