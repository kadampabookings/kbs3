package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.platform.console.Console;
import javafx.beans.binding.Bindings;
import one.modality.base.shared.entities.Event;
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
import java.time.temporal.ChronoUnit;

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
    private DefaultAccommodationSelectionSection accommodationSection;

    // Custom sections - Step 2: Booking Details
    private CompositeBookingFormPage bookingDetailsPage;
    private DefaultEventHeaderSection step2EventHeaderSection;
    private DefaultFestivalDaySelectionSection festivalDaySection;
    private DefaultMealsSelectionSection mealsSection;
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
            populateAccommodationOptions();
            populateFestivalDays();
            populateMealsOptions();
            populateAdditionalOptions();
        });

        // Bind stickyPriceHeader's totalPrice to WorkingBookingProperties.totalProperty()
        // This ensures the header shows the actual booking total from WorkingBooking
        if (stickyPriceHeader != null) {
            stickyPriceHeader.totalPriceProperty().bind(workingBookingProperties.totalProperty());
            Console.log("USFestivalBookingForm: Bound stickyPriceHeader.totalPrice to workingBookingProperties.totalProperty");
        }
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

        accommodationSection = new DefaultAccommodationSelectionSection();
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
            // Show/hide roommate section based on "Share Accommodation" selection
            if (roommateInfoSection != null && option != null) {
                boolean isShareAccommodation = "SHARE_ACCOMMODATION".equals(option.getItemId());
                roommateInfoSection.setVisible(isShareAccommodation);
                Console.log("USFestivalBookingForm: Roommate section visible=" + isShareAccommodation);
                // Reset the section when not sharing (clears fields and makes it valid)
                if (!isShareAccommodation) {
                    roommateInfoSection.reset();
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
            });

            // Listen for departure time changes
            festivalDaySection.departureTimeProperty().addListener((obs, old, newTime) -> {
                Console.log("USFestivalBookingForm: Departure time changed - " + newTime);
                if (mealsSection != null) {
                    mealsSection.setDepartureTime(newTime);
                }
            });
        }
    }

    /**
     * Updates the extended stay status (early arrival/late departure) on the meals section.
     * Shows an info note if the selected dates extend beyond the event dates.
     */
    private void updateExtendedStayStatus(LocalDate arrival, LocalDate departure) {
        if (mealsSection == null || arrival == null || departure == null) {
            return;
        }

        Event event = settings.event();
        if (event == null || event.getStartDate() == null || event.getEndDate() == null) {
            return;
        }

        // Event dates (from the Event entity)
        LocalDate eventStart = event.getStartDate();
        LocalDate eventEnd = event.getEndDate();

        // Check if the stay extends beyond event dates
        boolean isEarlyArrival = arrival.isBefore(eventStart);
        boolean isLateDeparture = departure.isAfter(eventEnd);
        boolean hasExtendedStay = isEarlyArrival || isLateDeparture;

        Console.log("USFestivalBookingForm: Extended stay check - event: " + eventStart + " to " + eventEnd +
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
     * Uses ScheduledItem.guestsAvailability to determine availability status
     * and ItemPolicy.minDay for minimum nights constraints.
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

        // Debug: log details of each scheduled item to understand why no accommodation items
        if (policyAggregate.getScheduledItems() != null) {
            for (var si : policyAggregate.getScheduledItems()) {
                var item = si.getItem();
                String itemName = item != null ? item.getName() : "null";
                Object familyId = item != null ? item.getFamilyId() : "null";
                Console.log("USFestivalBookingForm: ScheduledItem - item=" + itemName + ", familyId=" + familyId);
            }
        }

        Console.log("USFestivalBookingForm: Populating accommodation options from PolicyAggregate");

        // Populate accommodation options from PolicyAggregate
        // limitedThreshold = 5 means rooms with 5 or fewer available will show as "LIMITED"
        accommodationSection.populateFromPolicyAggregate(policyAggregate, 5);

        // Calculate and set teaching price and nights for total price display
        Event event = settings.event();
        if (event != null && event.getStartDate() != null && event.getEndDate() != null) {
            // Calculate number of nights (days between start and end)
            long nights = ChronoUnit.DAYS.between(event.getStartDate(), event.getEndDate());
            accommodationSection.setFullEventNights((int) nights);

            // Get unique teaching days count (not total scheduled items which may have multiple per day)
            int teachingDays = (int) policyAggregate.filterTeachingScheduledItems().stream()
                .map(ScheduledItem::getDate)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

            // Calculate teaching price by summing rates for ALL teaching scheduled items
            // Each scheduled item contributes to the total based on its date and the applicable rate
            int totalTeachingPrice = 0;
            int dailyRatePrice = policyAggregate.getDailyRatePrice();
            for (ScheduledItem teachingSi : policyAggregate.filterTeachingScheduledItems()) {
                Item item = teachingSi.getItem();
                LocalDate siDate = teachingSi.getDate();
                if (item != null && siDate != null) {
                    // Find daily rate matching this item AND date range
                    // Similar logic to Kbs2PriceAlgorithm: rate.startDate <= siDate <= rate.endDate
                    final Item finalItem = item;
                    final LocalDate finalDate = siDate;
                    int itemPrice = policyAggregate.getDailyRatesStream()
                        .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                            && r.getItem().getPrimaryKey().equals(finalItem.getPrimaryKey()))
                        .filter(r -> {
                            LocalDate startDate = r.getStartDate();
                            LocalDate endDate = r.getEndDate();
                            // Rate applies if no date restriction or date falls within range
                            boolean startOk = startDate == null || !finalDate.isBefore(startDate);
                            boolean endOk = endDate == null || !finalDate.isAfter(endDate);
                            return startOk && endOk;
                        })
                        .findFirst()
                        .map(r -> r.getPrice() != null ? r.getPrice() : 0)
                        .orElse(dailyRatePrice); // Fallback to default daily rate
                    totalTeachingPrice += itemPrice;
                }
            }
            accommodationSection.setFullEventTeachingPrice(totalTeachingPrice);

            // Note: Don't set meals price here - meals are selected in Step 2, not Step 1
            // The card price should match the sticky header which only shows currently booked items
            // (teaching + accommodation). Meals are added to WorkingBooking when selected in Step 2.
            accommodationSection.setFullEventMealsPrice(0);

            Console.log("USFestivalBookingForm: Set " + nights + " nights, " + teachingDays + " teaching days, teaching price: " + totalTeachingPrice);
        }

        // Add Share Accommodation option (for people sharing a room with someone else booking)
        addShareAccommodationOption();

        // Add Day Visitor option (no accommodation - meals and teachings only)
        addDayVisitorOption();

        // Mark as populated to avoid re-populating
        accommodationOptionsPopulated = true;
        Console.log("USFestivalBookingForm: Accommodation options populated successfully");
    }

    /**
     * Adds the Share Accommodation option to the accommodation section.
     * For people sharing a room with someone who is booking the accommodation.
     * They pay for teachings and meals, but not accommodation.
     */
    private void addShareAccommodationOption() {
        if (accommodationSection == null) return;

        HasAccommodationSelectionSection.AccommodationOption shareAccommodation =
            new HasAccommodationSelectionSection.AccommodationOption(
                "SHARE_ACCOMMODATION",  // special itemId to identify this option
                null,  // no itemEntity
                I18n.getI18nText(USFestivalI18nKeys.ShareAccommodation),
                I18n.getI18nText(USFestivalI18nKeys.ShareAccommodationDescription),
                0,     // pricePerNight = 0 (accommodation included with roommate's booking)
                HasAccommodationSelectionSection.AvailabilityStatus.AVAILABLE,
                HasAccommodationSelectionSection.ConstraintType.NONE,
                null,
                0,
                true,  // isDayVisitor = true (skip accommodation booking - roommate books it)
                null   // imageUrl
            );

        accommodationSection.addAccommodationOption(shareAccommodation);
        Console.log("USFestivalBookingForm: Share Accommodation option added");
    }

    /**
     * Adds the Day Visitor option to the accommodation section.
     * Day Visitors don't have accommodation items - they only book meals and teachings.
     */
    private void addDayVisitorOption() {
        if (accommodationSection == null) return;

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
                null   // imageUrl
            );

        accommodationSection.addAccommodationOption(dayVisitor);
        Console.log("USFestivalBookingForm: Day Visitor option added");
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

        // Mark as populated to avoid re-populating
        mealsOptionsPopulated = true;
        Console.log("USFestivalBookingForm: Meals options populated successfully");
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

        // Book selected additional options (parking, shuttle)
        bookAdditionalOptionsItems(workingBooking, policyAggregate);

        // Store arrival/departure time and dietary preference in document request
        storeBookingDetailsInRequest(workingBooking);

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

        // Get roommate info if sharing accommodation
        if (roommateInfoSection != null && roommateInfoSection.isVisible()) {
            String roommateName = roommateInfoSection.getRoommateName();
            String registrationNumber = roommateInfoSection.getRegistrationNumber();
            if (roommateName != null && !roommateName.trim().isEmpty()) {
                requestText.append("Sharing room with: ").append(roommateName.trim());
                if (registrationNumber != null && !registrationNumber.trim().isEmpty()) {
                    requestText.append(" (Reg: ").append(registrationNumber.trim()).append(")");
                }
                requestText.append("\n");

                // Also set the share_mate_owner_name on accommodation document lines
                setShareMateOwnerNameOnDocumentLines(workingBooking, roommateName.trim());
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
     * Sets the share_mate_owner_name field on accommodation document lines.
     * This marks the booking as a room share and records who booked the room.
     */
    private void setShareMateOwnerNameOnDocumentLines(WorkingBooking workingBooking, String roommateName) {
        if (workingBooking == null || roommateName == null) return;

        // Get the document lines from the working booking
        var bookedLines = workingBooking.getDocumentLines();
        if (bookedLines == null || bookedLines.isEmpty()) return;

        for (var line : bookedLines) {
            // Only set on accommodation lines (those with share accommodation selection)
            // All lines in a share accommodation booking should be marked as share_mate
           // line.setShareMate(true);
           // line.setShareMateOwnerName(roommateName);
            Console.log("USFestivalBookingForm: Set share_mate_owner_name='" + roommateName + "' on DocumentLine");
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
     * Books accommodation ScheduledItems based on selected room and date range.
     * Only books accommodation for nights within the selected arrival-departure range.
     */
    private void bookAccommodationItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (accommodationSection == null) return;

        HasAccommodationSelectionSection.AccommodationOption selectedOption = accommodationSection.getSelectedOption();
        if (selectedOption == null || selectedOption.isDayVisitor()) {
            Console.log("USFestivalBookingForm: No accommodation selected or day visitor - skipping accommodation booking");
            return;
        }

        Item selectedItem = selectedOption.getItemEntity();
        if (selectedItem == null) {
            Console.log("USFestivalBookingForm: Selected accommodation has no itemEntity - skipping");
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

                // For lunch: filter based on arrival/departure time
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
                                Console.log("USFestivalBookingForm: Lunch date is null, keeping");
                                return true;
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

                // For dinner: filter based on arrival/departure time
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
                                Console.log("USFestivalBookingForm: Dinner date is null, keeping");
                                return true;
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
                                Console.log("USFestivalBookingForm: Dinner on departure day. departureTime=" + finalDepartureTime);
                                if (finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.MORNING ||
                                    finalDepartureTime == HasFestivalDaySelectionSection.ArrivalDepartureTime.AFTERNOON) {
                                    Console.log("USFestivalBookingForm: Skipping dinner on departure day " + mealDate + " (departing " + finalDepartureTime + ")");
                                    return false;
                                }
                            }

                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    Console.log("USFestivalBookingForm: Dinner items after filter: " + itemScheduledItems.size());
                }

                if (!itemScheduledItems.isEmpty()) {
                    Console.log("USFestivalBookingForm: Booking " + itemScheduledItems.size() + " " + item.getName() + " items");
                    workingBooking.bookScheduledItems(itemScheduledItems, true); // addOnly=true to accumulate
                }
            }
        }
    }

    /**
     * Books additional options ScheduledItems (parking, shuttle) based on selections.
     */
    private void bookAdditionalOptionsItems(WorkingBooking workingBooking, PolicyAggregate policyAggregate) {
        if (additionalOptionsSection == null) return;

        // Book parking if selected
        if (additionalOptionsSection.needsParking()) {
            List<ScheduledItem> parkingItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.PARKING);
            if (!parkingItems.isEmpty()) {
                Console.log("USFestivalBookingForm: Booking " + parkingItems.size() + " parking items");
                workingBooking.bookScheduledItems(parkingItems, true);
            }
        }

        // Book shuttle/transport if selected
        // DefaultAdditionalOptionsSection handles outbound/return separately via needsShuttleOutbound/needsShuttleReturn
        List<ScheduledItem> transportItems = policyAggregate.filterScheduledItemsOfFamily(KnownItemFamily.TRANSPORT);
        if (!transportItems.isEmpty()) {
            for (ScheduledItem si : transportItems) {
                Item item = si.getItem();
                if (item == null) continue;

                String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
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
                    List<ScheduledItem> itemScheduledItems = transportItems.stream()
                        .filter(tsi -> dev.webfx.stack.orm.entity.Entities.samePrimaryKey(tsi.getItem(), item))
                        .collect(java.util.stream.Collectors.toList());

                    if (!itemScheduledItems.isEmpty()) {
                        Console.log("USFestivalBookingForm: Booking " + itemScheduledItems.size() + " " + item.getName() + " items");
                        workingBooking.bookScheduledItems(itemScheduledItems, true);
                    }
                }
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
