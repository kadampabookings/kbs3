package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.platform.console.Console;
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
    private DefaultEventHeaderSection step1EventHeaderSection;
    private DefaultAccommodationSelectionSection accommodationSection;

    // Custom sections - Step 2: Booking Details
    private CompositeBookingFormPage bookingDetailsPage;
    private DefaultEventHeaderSection step2EventHeaderSection;
    private DefaultFestivalDaySelectionSection festivalDaySection;
    private DefaultMealsSelectionSection mealsSection;
    private DefaultAdditionalOptionsSection additionalOptionsSection;

    // Standard sections - Your Information & Member Selection
    private DefaultEventHeaderSection yourInfoEventHeaderSection;
    private DefaultEventHeaderSection memberSelectionEventHeaderSection;
    private DefaultYourInformationSection yourInformationSection;
    private DefaultMemberSelectionSection memberSelectionSection;

    // Sticky Price Header (fixed at top when room is selected)
    private final StickyPriceHeader stickyPriceHeader;

    // State
    private final EventBookingFormSettings settings;
    private final HasWorkingBookingProperties activity;
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
        this.activity = activity;

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
        setupBookingDetailsCallbacks();
        setupBookingDetailsButtons();
        setupYourInformationCallbacks();
        setupMemberSelectionCallbacks();

        // Load members immediately if user is already logged in
        loadMembersIfLoggedIn();

        // Listen for logout events
        setupLogoutListener();

        // Set up listener for when WorkingBooking becomes available
        setupWorkingBookingListener();

        // Try to populate options from PolicyAggregate (may return early if data not ready)
        populateAccommodationOptions();
        populateFestivalDays();
        populateMealsOptions();
        populateAdditionalOptions();
    }

    /**
     * Sets up a listener to populate accommodation options when the WorkingBooking becomes available.
     * This is needed because the form is created before the WorkingBooking is initialized.
     */
    private void setupWorkingBookingListener() {
        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        return new CompositeBookingFormPage(BookingPageI18nKeys.YourRoom,
            step1EventHeaderSection,
            accommodationSection)
            .setStep(true);
    }

    /**
     * Creates the Booking Details page (Step 2).
     * Shows event header, festival day selection, meals, and additional options.
     */
    private CompositeBookingFormPage createBookingDetailsPage() {
        step2EventHeaderSection = new DefaultEventHeaderSection();

        festivalDaySection = new DefaultFestivalDaySelectionSection();
        festivalDaySection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        mealsSection = new DefaultMealsSelectionSection();
        mealsSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        additionalOptionsSection = new DefaultAdditionalOptionsSection();
        additionalOptionsSection.setColorScheme(BookingFormColorScheme.WISDOM_BLUE);

        bookingDetailsPage = new CompositeBookingFormPage(BookingPageI18nKeys.BookingDetails,
            step2EventHeaderSection,
            festivalDaySection,
            mealsSection,
            additionalOptionsSection)
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
            // Update sticky price header
            updateStickyPriceHeader();
            // Pass min nights constraint to festival day section
            if (festivalDaySection != null && option != null) {
                festivalDaySection.setMinNightsConstraint(option.getMinNights());
            }
            // Sync breakfast with accommodation selection
            // Day visitors don't get breakfast, overnight guests do
            if (mealsSection != null && option != null) {
                boolean hasAccommodation = !option.isDayVisitor();
                mealsSection.setHasAccommodation(hasAccommodation);
                Console.log("USFestivalBookingForm: Set hasAccommodation=" + hasAccommodation + " for meals section");
            }
        });

        accommodationSection.setOnContinuePressed(() -> {
            form.navigateToNextPage();
        });

        accommodationSection.setOnBackPressed(() -> {
            form.navigateToPreviousPage();
        });
    }

    private void setupBookingDetailsCallbacks() {
        if (festivalDaySection != null) {
            festivalDaySection.setOnDatesChanged((arrival, departure) -> {
                Console.log("USFestivalBookingForm: Dates changed - " + arrival + " to " + departure);
                // Update sticky price header with new days count
                updateStickyPriceHeader();
                // Update dependent sections (meals days count, etc.)
                // TODO: Wire up to working booking properties
            });
        }
    }

    /**
     * Sets up the Back and Next buttons on the Booking Details page (Step 2).
     */
    private void setupBookingDetailsButtons() {
        if (bookingDetailsPage == null) return;

        bookingDetailsPage.setButtons(
            new BookingFormButton(BookingPageI18nKeys.Back,
                e -> form.navigateToPreviousPage(),
                "btn-back booking-form-btn-back"),
            new BookingFormButton(BookingPageI18nKeys.Continue,
                e -> form.navigateToNextPage(),
                "btn-primary booking-form-btn-primary")
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
                WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

            // Calculate teaching price using daily rates for accuracy
            // Teaching price = sum of prices for all teaching attendances
            int totalTeachingPrice = 0;
            for (ScheduledItem teachingSi : policyAggregate.filterTeachingScheduledItems()) {
                Item item = teachingSi.getItem();
                if (item != null) {
                    // Try with null site first, then fallback to searching all daily rates
                    int itemPrice = policyAggregate.filterDailyRatesStreamOfSiteAndItem(null, item)
                        .findFirst()
                        .map(rate -> rate.getPrice() != null ? rate.getPrice() : 0)
                        .orElseGet(() -> {
                            // Fallback: search all daily rates for this item regardless of site
                            return policyAggregate.getDailyRatesStream()
                                .filter(r -> r.getItem() != null && r.getItem().getPrimaryKey() != null
                                    && r.getItem().getPrimaryKey().equals(item.getPrimaryKey()))
                                .findFirst()
                                .map(r -> r.getPrice() != null ? r.getPrice() : 0)
                                .orElse(0);
                        });
                    totalTeachingPrice += itemPrice;
                }
            }
            accommodationSection.setFullEventTeachingPrice(totalTeachingPrice);

            // Calculate meals price for the full event
            // Lunch/Dinner: multiplied by teaching days, Breakfast: multiplied by nights
            int totalMealsPrice = calculateFullEventMealsPrice(policyAggregate, teachingDays, (int) nights);
            accommodationSection.setFullEventMealsPrice(totalMealsPrice);

            Console.log("USFestivalBookingForm: Set " + nights + " nights, " + teachingDays + " teaching days, teaching price: " + totalTeachingPrice + ", meals price: " + totalMealsPrice);
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

        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        WorkingBookingProperties workingBookingProperties = activity.getWorkingBookingProperties();
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

        // Book selected meals
        bookMealsItems(workingBooking, policyAggregate);

        // Book selected additional options (parking, shuttle)
        bookAdditionalOptionsItems(workingBooking, policyAggregate);

        Console.log("USFestivalBookingForm.bookSelectedItemsIntoWorkingBooking() - All selected items booked");
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
     * Books meals ScheduledItems based on selected meal options.
     * Note: Breakfast is served the morning AFTER an accommodation night,
     * so breakfast on day d+1 corresponds to accommodation on day d.
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

        if (hasAccommodation) {
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
                    departureDate = teachingDatesSorted.get(teachingDatesSorted.size() - 1).plusDays(1); // departure = last teaching day + 1
                    Console.log("USFestivalBookingForm: Using fallback dates from teaching: " + arrivalDate + " to " + departureDate);
                }
            }

            if (arrivalDate != null && departureDate != null) {
                // Add all nights from arrival to departure-1
                LocalDate currentDate = arrivalDate;
                while (currentDate.isBefore(departureDate)) {
                    accommodationNights.add(currentDate);
                    currentDate = currentDate.plusDays(1);
                }
            }
        }
        Console.log("USFestivalBookingForm: Accommodation nights for breakfast calculation: " + accommodationNights);

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
     * Shows room name, number of days, and total price (teaching + accommodation).
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
        int teachingPrice = 0;
        if (festivalDaySection != null) {
            daysCount = festivalDaySection.getSelectedDaysCount();
            teachingPrice = festivalDaySection.getTotalTeachingPrice();
        }

        // Calculate accommodation price
        int nights = daysCount > 0 ? daysCount - 1 : 0; // nights = days - 1
        if (nights < 0) nights = 0;
        int accommodationPrice = selectedRoom.getPricePerNight() * nights;

        // Total price = teaching + accommodation
        int totalPrice = teachingPrice + accommodationPrice;

        Console.log("USFestivalBookingForm: Updating sticky header - room=" + roomName +
            ", days=" + daysCount + ", teaching=" + teachingPrice + ", accommodation=" + accommodationPrice +
            ", total=" + totalPrice);

        stickyPriceHeader.update(roomName, daysCount, totalPrice);
    }

    /**
     * Returns the built form for use in the UI.
     */
    public StandardBookingForm getForm() {
        return form;
    }
}
