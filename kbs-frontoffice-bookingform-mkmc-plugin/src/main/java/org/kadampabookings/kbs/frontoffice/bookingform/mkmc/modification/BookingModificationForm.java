package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;

import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.platform.async.Future;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import one.modality.base.shared.entities.Document;
import one.modality.base.shared.entities.Item;
import one.modality.base.shared.entities.Person;
import one.modality.base.shared.entities.markers.HasPersonalDetails;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
// Section imports - moved to sub-packages for better organization
import one.modality.booking.frontoffice.bookingpage.sections.audio.DefaultAudioRecordingSection;
import one.modality.booking.frontoffice.bookingpage.sections.confirmation.HasConfirmationSection;
import one.modality.booking.frontoffice.bookingpage.sections.payment.DefaultPaymentSection;
import one.modality.booking.frontoffice.bookingpage.sections.payment.HasPaymentSection;
import one.modality.booking.frontoffice.bookingpage.sections.summary.DefaultExistingBookingSummarySection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

import java.util.Set;

/**
 * Booking Modification Form - allows adding audio recording options to existing bookings.
 *
 * <p>This form uses the Default*Section classes from the modality-booking module:</p>
 * <ul>
 *   <li>{@link DefaultExistingBookingSummarySection} - displays existing booking info</li>
 *   <li>{@link DefaultAudioRecordingSection} - audio recording selection with locked items</li>
 *   <li>{@link DefaultPaymentSection} - payment options, methods, and terms acceptance</li>
 * </ul>
 *
 * <p>Implements a simplified 3-step flow:</p>
 * <ol>
 *   <li>Select Options - Choose additional audio recordings</li>
 *   <li>Payment - Select payment amount, method, accept terms, pay</li>
 *   <li>Confirmation - Success message</li>
 * </ol>
 *
 * @author Bruno Salmon
 */
public final class BookingModificationForm {

    // Steps configuration
    private static final int STEP_OPTIONS = 1;
    private static final int STEP_PAYMENT = 2;
    private static final int STEP_CONFIRMATION = 3;

    // Data
    private final WorkingBookingProperties workingBookingProperties;
    private final BookingFormColorScheme colorScheme;

    // State
    private final IntegerProperty currentStep = new SimpleIntegerProperty(STEP_OPTIONS);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty hasNewSelectionsProperty = new SimpleBooleanProperty(false);

    // Default sections from modality-booking module
    private DefaultExistingBookingSummarySection bookingSummarySection;
    private DefaultAudioRecordingSection audioRecordingSection;
    private DefaultPaymentSection paymentSection;
    private AudioRecordingConfirmationSection confirmationSection;

    // UI
    private final VBox rootContainer;
    private final VBox contentContainer;
    private final VBox stepProgressContainer;

    // Callbacks
    private Runnable onComplete;
    private Runnable onCancel;
    private Runnable onProceedToPayment;

    public BookingModificationForm(WorkingBookingProperties workingBookingProperties, BookingFormColorScheme colorScheme) {
        this.workingBookingProperties = workingBookingProperties;
        this.colorScheme = colorScheme != null ? colorScheme : BookingFormColorScheme.DEFAULT;

        this.rootContainer = new VBox(0);
        this.contentContainer = new VBox(24);
        this.stepProgressContainer = new VBox(0);

        initializeSections();
        buildUI();
    }

    /**
     * Factory method to create a form from WorkingBookingProperties.
     * Automatically determines the color scheme from the event.
     */
    public static BookingModificationForm create(WorkingBookingProperties workingBookingProperties) {
        BookingFormColorScheme scheme = determineColorScheme(workingBookingProperties);
        return new BookingModificationForm(workingBookingProperties, scheme);
    }

    private static BookingFormColorScheme determineColorScheme(WorkingBookingProperties props) {
        if (props == null || props.getWorkingBooking() == null) {
            return BookingFormColorScheme.DEFAULT;
        }
        // In a real implementation, look up the event's color scheme from configuration
        return BookingFormColorScheme.JOURNEY_GREEN;
    }

    private void initializeSections() {
        // Create sections using Default implementations from modality-booking
        bookingSummarySection = new DefaultExistingBookingSummarySection();
        audioRecordingSection = new DefaultAudioRecordingSection();
        paymentSection = new DefaultPaymentSection();

        // Set color scheme (for icon colors - CSS handles the rest)
        bookingSummarySection.setColorScheme(colorScheme);
        audioRecordingSection.setColorScheme(colorScheme);
        paymentSection.setColorScheme(colorScheme);

        // Hide package row in the summary (not relevant for modification flow)
        bookingSummarySection.setShowPackage(false);

        // Wire up sections with WorkingBookingProperties
        bookingSummarySection.setWorkingBookingProperties(workingBookingProperties);
        audioRecordingSection.setWorkingBookingProperties(workingBookingProperties);
        paymentSection.setWorkingBookingProperties(workingBookingProperties);

        // Configure payment section
        paymentSection.setOnBackPressed(() -> showStep(STEP_OPTIONS));
        paymentSection.setOnPaymentSubmit(this::handlePaymentSubmit);

        // For booking modifications (adding options), require full payment only
        paymentSection.setFullPaymentOnly(true);

        // Listen to audio recording selection changes for price updates
        audioRecordingSection.setOnSelectionChanged(this::onAudioSelectionChanged);

        // Create confirmation section
        confirmationSection = new AudioRecordingConfirmationSection();
        confirmationSection.setColorScheme(colorScheme);
        confirmationSection.setWorkingBookingProperties(workingBookingProperties);
    }

    private void onAudioSelectionChanged(Set<Item> selectedItems) {
        // Check if there are new selections beyond locked items
        Set<Item> lockedItems = audioRecordingSection.getLockedItems();
        boolean hasNew = false;
        for (Item item : selectedItems) {
            if (!lockedItems.contains(item)) {
                hasNew = true;
                break;
            }
        }
        hasNewSelectionsProperty.set(hasNew);

        // Update price calculation
        workingBookingProperties.updateAll();
    }

    private void buildUI() {
        rootContainer.setAlignment(Pos.TOP_CENTER);
        rootContainer.getStyleClass().addAll("booking-form-container", "theme-" + colorScheme.getId());

        // Container with max-width for better readability
        VBox innerContainer = new VBox(0);
        innerContainer.setMaxWidth(800);
        innerContainer.setPadding(new Insets(32, 24, 32, 24));

        // Step progress header
        innerContainer.getChildren().add(stepProgressContainer);

        // Content area
        innerContainer.getChildren().add(contentContainer);

        // Loading overlay with spinner
        MonoPane loadingOverlay = new MonoPane();
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.getStyleClass().addAll("booking-form-loading-overlay", "bookingpage-loading-overlay");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        loadingOverlay.setContent(spinner);

        loadingOverlay.visibleProperty().bind(isLoading);
        loadingOverlay.managedProperty().bind(isLoading);

        // Use StackPane to overlay loading on top of content
        StackPane mainContent = new StackPane(innerContainer, loadingOverlay);
        rootContainer.getChildren().add(mainContent);

        // Initial step display
        showStep(STEP_OPTIONS);
    }

    private void showStep(int step) {
        currentStep.set(step);
        contentContainer.getChildren().clear();

        // Update step progress
        updateStepProgress();

        switch (step) {
            case STEP_OPTIONS:
                showOptionsStep();
                break;
            case STEP_PAYMENT:
                showPaymentStep();
                break;
            case STEP_CONFIRMATION:
                showConfirmationStep();
                break;
        }
    }

    private void updateStepProgress() {
        stepProgressContainer.getChildren().clear();

        // Hide on confirmation step
        if (currentStep.get() == STEP_CONFIRMATION) {
            return;
        }

        VBox progress = createStepProgress();
        stepProgressContainer.getChildren().add(progress);
    }

    private VBox createStepProgress() {
        VBox wrapper = new VBox(0);
        wrapper.setPadding(new Insets(0, 0, 25, 0)); // 25px bottom spacing after divider
        wrapper.getStyleClass().add("booking-form-step-progress-wrapper");

        String[] stepNames = { "Options", "Payment", "Confirmation" };
        int current = currentStep.get();

        // Use StackPane to layer line behind step indicators (like StepProgressHeader)
        StackPane container = new StackPane();
        container.setPadding(new Insets(25, 0, 25, 0)); // 25px top/bottom spacing around steps
        container.getStyleClass().add("booking-form-step-progress");

        // Single progress line spanning behind all steps
        Line progressLine = new Line();
        progressLine.setStroke(Color.web("#e0e0e0"));
        progressLine.setStrokeWidth(2);
        // Line width bound to container width with padding for circles
        progressLine.endXProperty().bind(container.widthProperty().subtract(100));
        progressLine.setStartX(0);
        StackPane.setAlignment(progressLine, Pos.TOP_CENTER);
        StackPane.setMargin(progressLine, new Insets(20, 0, 0, 0)); // 20px = center of 40px circle

        // Steps row with evenly distributed step indicators
        HBox stepsRow = new HBox();
        stepsRow.setAlignment(Pos.CENTER);
        stepsRow.setFillHeight(true);

        for (int i = 1; i <= 3; i++) {
            boolean isActive = i == current;
            boolean isCompleted = i < current;

            VBox stepBox = createStepIndicator(i, stepNames[i - 1], isActive, isCompleted);
            HBox.setHgrow(stepBox, Priority.ALWAYS); // Distribute evenly
            stepsRow.getChildren().add(stepBox);
        }

        container.getChildren().addAll(progressLine, stepsRow);

        // Divider line (1px) below step indicators
        Region dividerLine = new Region();
        dividerLine.setMinHeight(1);
        dividerLine.setPrefHeight(1);
        dividerLine.setMaxHeight(1);
        dividerLine.getStyleClass().add("bookingpage-divider-light");

        wrapper.getChildren().addAll(container, dividerLine);
        return wrapper;
    }

    private VBox createStepIndicator(int stepNum, String label, boolean isActive, boolean isCompleted) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("step-progress-item");

        // Apply active/completed states to parent (CSS selectors cascade to children)
        if (isCompleted) {
            box.getStyleClass().add("completed");
        } else if (isActive) {
            box.getStyleClass().add("active");
        }

        // Circle indicator - using existing step-bubble CSS class
        StackPane circle = new StackPane();
        circle.getStyleClass().add("step-bubble");

        Label numLabel;
        if (isCompleted) {
            numLabel = new Label("\u2713"); // Checkmark
        } else {
            numLabel = new Label(String.valueOf(stepNum));
        }
        numLabel.getStyleClass().add("step-number");
        circle.getChildren().add(numLabel);

        // Label - using existing step-progress-label CSS class
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("step-progress-label");

        // Styling handled by CSS classes:
        // - .step-bubble for base styling
        // - .step-progress-item.active .step-bubble for active state
        // - .step-progress-item.completed .step-bubble for completed state
        // - .step-progress-label and .step-progress-item.active/completed .step-progress-label

        box.getChildren().addAll(circle, labelNode);
        return box;
    }

    // === Step 1: Options ===

    private void showOptionsStep() {
        // Existing booking summary
        VBox summarySection = new VBox(0);
        summarySection.getChildren().add(bookingSummarySection.getView());

        // Audio recordings section
        VBox audioSection = new VBox(0);
        audioSection.getChildren().add(audioRecordingSection.getView());

        // Warning box if no options available to select
        Node warningBox = null;
        if (audioRecordingSection.hasAnyRecordings() && !audioRecordingSection.hasAvailableRecordings()) {
            // All recordings are already purchased (locked)
            warningBox = BookingPageUIBuilder.createInfoBox(
                I18n.getI18nText(MKMCI18nKeys.AllRecordingsAlreadyPurchased),
                BookingPageUIBuilder.InfoBoxType.WARNING
            );
        } else if (!audioRecordingSection.hasAnyRecordings()) {
            // No recordings available at all
            warningBox = BookingPageUIBuilder.createInfoBox(
                I18n.getI18nText(MKMCI18nKeys.NoRecordingsAvailable),
                BookingPageUIBuilder.InfoBoxType.WARNING
            );
        }

        // Continue button - only enabled when new options are selected
        Button continueBtn = BookingPageUIBuilder.createPrimaryButton(BookingPageI18nKeys.Continue);
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setOnAction(e -> showStep(STEP_PAYMENT));
        continueBtn.disableProperty().bind(hasNewSelectionsProperty.not());

        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.getChildren().add(continueBtn);
        VBox.setMargin(buttonRow, new Insets(24, 0, 0, 0));

        if (warningBox != null) {
            VBox.setMargin(warningBox, new Insets(0, 0, 16, 0));
            contentContainer.getChildren().addAll(summarySection, audioSection, warningBox, buttonRow);
        } else {
            contentContainer.getChildren().addAll(summarySection, audioSection, buttonRow);
        }
    }

    // === Step 2: Payment ===

    private void showPaymentStep() {
        // Populate payment section with selected items
        updatePaymentFromSelections();

        // Add payment section view
        contentContainer.getChildren().add(paymentSection.getView());

        // Button row with Back and Pay buttons
        HBox buttonRow = BookingPageUIBuilder.createNavigationButtonRow();
        VBox.setMargin(buttonRow, new Insets(24, 0, 0, 0));

        // Back button
        Button backBtn = BookingPageUIBuilder.createBackButton(BookingPageI18nKeys.Back);
        backBtn.setOnAction(e -> showStep(STEP_OPTIONS));

        // Pay button - text and disabled state bound to payment section
        Button payBtn = new Button();
        payBtn.textProperty().bind(paymentSection.payButtonTextProperty());
        payBtn.disableProperty().bind(paymentSection.payButtonDisabledProperty());
        payBtn.setCursor(javafx.scene.Cursor.HAND);
        payBtn.getStyleClass().addAll("booking-form-primary-btn", "booking-form-primary-btn-text");
        payBtn.setOnAction(e -> paymentSection.submitPaymentAsync());
        payBtn.setGraphicTextGap(16);

        buttonRow.getChildren().addAll(backBtn, payBtn);
        contentContainer.getChildren().add(buttonRow);
    }

    /**
     * Populates the payment section with the newly selected audio recording options.
     */
    private void updatePaymentFromSelections() {
        paymentSection.clearBookingItems();

        // Get attendee name from FXPersonToBook
        String attendeeName = getAttendeeName();
        if (attendeeName == null || attendeeName.isEmpty()) {
            attendeeName = "Additional Options";
        }

        int totalInCents = workingBookingProperties.getBalance();

        // Add a single booking item representing the additional options
        paymentSection.addBookingItem(new HasPaymentSection.PaymentBookingItem(
            workingBookingProperties.getWorkingBooking().getDocument(),
            attendeeName,
            "Audio Recording Options",
            totalInCents
        ));

        paymentSection.setTotalAmount(totalInCents);
        paymentSection.setDepositAmount(totalInCents / 10); // 10% deposit
    }

    /**
     * Handles payment submission from DefaultPaymentSection.
     * Returns a Future to satisfy the AsyncFunction callback interface.
     */
    private Future<Void> handlePaymentSubmit(HasPaymentSection.PaymentResult result) {
        // Call the external payment callback if set
        if (onProceedToPayment != null) {
            onProceedToPayment.run();
        } else {
            // Default behavior - show confirmation
            showStep(STEP_CONFIRMATION);
        }
        return Future.succeededFuture();
    }

    /**
     * Gets the attendee name from the working booking's document.
     */
    private String getAttendeeName() {
        return getAttendeePersonalDetails().getFullName();
    }

    // === Step 3: Confirmation ===

    private void showConfirmationStep() {
        // Configure confirmation section with current data
        confirmationSection.clearConfirmedBookings();

        // Add booking reference
        String bookingRef = bookingSummarySection.getBookingReference();
        String attendeeName = getAttendeeName();
        String email = getAttendeeEmail();

        if (bookingRef != null) {
            confirmationSection.addConfirmedBooking(
                new HasConfirmationSection.ConfirmedBooking(
                    attendeeName != null ? attendeeName : "Guest",
                    email != null ? email : "",
                    bookingRef
                )
            );
        }

        // Set event info
        String eventName = "bbb";// bookingSummarySection.getEventName();
        if (eventName != null) {
            confirmationSection.setEventName(eventName);
        }

        // Set payment amounts (convert to cents)
        int totalInCents = workingBookingProperties.getBalance();
        confirmationSection.setPaymentAmounts(totalInCents, 0, totalInCents); // Full payment

        // Add the confirmation view
        contentContainer.getChildren().add(confirmationSection.getView());

        // Add action button
        Button viewBookingsBtn = I18nControls.newButton(BookingPageI18nKeys.ViewMyBookings);
        viewBookingsBtn.setCursor(javafx.scene.Cursor.HAND);
        viewBookingsBtn.getStyleClass().addAll("booking-form-primary-btn", "booking-form-primary-btn-text");
        viewBookingsBtn.setOnAction(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        VBox.setMargin(viewBookingsBtn, new Insets(24, 0, 0, 0));

        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.getChildren().add(viewBookingsBtn);
        contentContainer.getChildren().add(buttonRow);
    }

    HasPersonalDetails getAttendeePersonalDetails() {
        Document document = workingBookingProperties.getWorkingBooking().getDocument();
        Person person = document.getPerson();
        return person != null ? person : document;
    }

    /**
     * Gets the attendee email from the working booking's document.
     */
    private String getAttendeeEmail() {
        return getAttendeePersonalDetails().getEmail();
    }

    // === Public API ===

    /**
     * Returns the root UI node for this form.
     */
    public Node getView() {
        return rootContainer;
    }

    /**
     * Sets a callback to run when the modification is complete.
     */
    public void setOnComplete(Runnable callback) {
        this.onComplete = callback;
    }

    /**
     * Sets a callback to run when the user cancels the modification.
     */
    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    /**
     * Sets a callback to run when proceeding to payment.
     * The callback should handle the actual payment flow.
     */
    public void setOnProceedToPayment(Runnable callback) {
        this.onProceedToPayment = callback;
    }

    /**
     * Shows the confirmation step (call after successful payment).
     */
    public void showConfirmation() {
        showStep(STEP_CONFIRMATION);
    }

    /**
     * Returns the current step number (1-3).
     */
    public int getCurrentStep() {
        return currentStep.get();
    }

    /**
     * Returns the current step property for binding.
     */
    public ReadOnlyIntegerProperty currentStepProperty() {
        return currentStep;
    }

    /**
     * Returns the loading state property for binding.
     */
    public ReadOnlyBooleanProperty loadingProperty() {
        return isLoading;
    }


    /**
     * Returns the WorkingBookingProperties for this form.
     */
    public WorkingBookingProperties getWorkingBookingProperties() {
        return workingBookingProperties;
    }
}
