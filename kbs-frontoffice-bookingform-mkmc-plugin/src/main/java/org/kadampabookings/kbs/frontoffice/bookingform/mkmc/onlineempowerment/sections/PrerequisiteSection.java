package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.webtext.HtmlText;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;
import one.modality.booking.frontoffice.bookingpage.ResettableSection;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

import java.time.LocalDate;

/**
 * Section for displaying and confirming prerequisites for online empowerment.
 * User must confirm they meet the prerequisites to proceed.
 *
 * <p>Uses CSS-based theming. Styling is handled via CSS classes that inherit
 * theme colors from CSS variables set on the parent container.</p>
 *
 * <p>Layout matches the design mockup:</p>
 * <ol>
 *   <li>Section header "Prerequisites"</li>
 *   <li>Yellow "Important Information" box with bullet points</li>
 *   <li>White description box explaining the event</li>
 *   <li>Checkbox confirmation row</li>
 * </ol>
 *
 * <p>CSS classes used:</p>
 * <ul>
 *   <li>{@code .booking-form-prerequisite-section} - section container</li>
 *   <li>{@code .bookingpage-warning-box} - important information box</li>
 *   <li>{@code .bookingpage-card} - confirmation box</li>
 *   <li>{@code .selected} - added when checkbox is selected</li>
 * </ul>
 *
 * @author Bruno Salmon
 */
public class PrerequisiteSection implements BookingFormSection, ResettableSection {

    private final VBox container = new VBox(16);
    private final SimpleBooleanProperty validProperty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty confirmedProperty = new SimpleBooleanProperty(false);

    private StyledSectionHeader header;
    private VBox confirmBox;

    // Dynamic date range for the teaching sessions
    private LocalDate startDate;
    private LocalDate endDate;
    private HtmlText dateBulletHtmlText; // Reference to update when dates change

    public PrerequisiteSection() {
        buildUI();
    }

    private void buildUI() {
        // Section header - "Prerequisites" with check icon (styling via CSS)
        header = new StyledSectionHeader(
                MKMCI18nKeys.Prerequisites,
                StyledSectionHeader.ICON_CHECK_CIRCLE
        );

        // === Yellow "Important Information" box - styled via CSS ===
        VBox importantInfoBox = createImportantInfoBox();

        // === Checkbox confirmation box (includes national event notice) ===
        confirmBox = createConfirmationBox();

        container.getChildren().addAll(header, importantInfoBox, confirmBox);
        container.getStyleClass().add("booking-form-prerequisite-section");
        container.setMinWidth(0); // Allow shrinking for responsive design
    }

    private VBox createImportantInfoBox() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("bookingpage-warning-box");
        box.setMinWidth(0); // Allow shrinking for responsive design

        // Warning icon and "Important Information" title
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label warningIcon = new Label("\u26A0");
        warningIcon.getStyleClass().add("bookingpage-text-lg");
        Label titleLabel = I18nControls.newLabel(MKMCI18nKeys.ImportantInformation);
        titleLabel.getStyleClass().addAll("bookingpage-text-lg", "bookingpage-font-semibold", "bookingpage-text-warning");
        titleRow.getChildren().addAll(warningIcon, titleLabel);

        // Bullet point 1: "This event is livestreamed only - there will be no video recordings..."
        HtmlText bullet1 = createBulletPoint(
                "This event is ",
                "livestreamed only",
                " - there will be no video recordings available to watch after the event"
        );

        // Bullet point 2: "This booking is for the online teaching sessions only from..." (dynamic dates)
        HtmlText bullet2 = createDateBulletPoint();

        box.getChildren().addAll(titleRow, bullet1, bullet2);
        return box;
    }

    private HtmlText createDateBulletPoint() {
        // Store reference to HtmlText so we can update it dynamically when dates change
        dateBulletHtmlText = new HtmlText();
        updateDateBulletText();
        dateBulletHtmlText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-warning");
        return dateBulletHtmlText;
    }

    private void updateDateBulletText() {
        if (dateBulletHtmlText != null) {
            String html = "\u2022 This booking is for the <b>online teaching sessions only</b>" + buildDateRangeString();
            dateBulletHtmlText.setText(html);
        }
    }

    private String buildDateRangeString() {
        if (startDate == null || endDate == null) {
            return " - the following retreat is not included";
        }

        String dateRange = BookingPageUIBuilder.formatDateRangeFull(startDate, endDate);
        return " from " + dateRange + " - the following retreat is not included";
    }

    private HtmlText createBulletPoint(String prefix, String boldText, String suffix) {
        HtmlText htmlText = new HtmlText();
        htmlText.setText("\u2022 " + prefix + "<b>" + boldText + "</b>" + suffix);
        htmlText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-warning");
        return htmlText;
    }

    private VBox createConfirmationBox() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("bookingpage-card");
        box.setMinWidth(0); // Allow shrinking for responsive design

        // National event notice with bold text using HtmlText
        HtmlText noticeText = new HtmlText();
        noticeText.setText("Since this is a national event, <b>you must be a resident of the United Kingdom</b> to attend online.");
        noticeText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-secondary");

        // Checkbox row
        HBox checkboxRow = new HBox(12);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);

        // Use CSS-styled checkbox indicator from BookingPageUIBuilder
        StackPane checkboxIndicator = BookingPageUIBuilder.createCheckboxIndicator(confirmedProperty);

        // Confirmation text with bold part using HtmlText
        HtmlText confirmText = new HtmlText();
        confirmText.setText("I understand I am booking to join a live-streamed event and confirm I reside in the <b>United Kingdom</b>.");
        confirmText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-dark");

        checkboxRow.getChildren().addAll(checkboxIndicator, confirmText);
        HBox.setHgrow(confirmText, Priority.ALWAYS);

        box.getChildren().addAll(noticeText, checkboxRow);

        // Make entire box clickable to toggle checkbox
        box.setCursor(Cursor.HAND);
        box.setOnMouseClicked(e -> {
            confirmedProperty.set(!confirmedProperty.get());
        });

        // Bind validity to checkbox
        validProperty.bind(confirmedProperty);

        // Update CSS class when confirmed (toggle "selected" class)
        confirmedProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                box.getStyleClass().add("selected");
            } else {
                box.getStyleClass().remove("selected");
            }
        });

        return box;
    }

    // === BookingFormSection interface ===

    @Override
    public Object getTitleI18nKey() {
        return MKMCI18nKeys.Prerequisites;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        if (workingBookingProperties == null) {
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        Event event = workingBooking.getEvent();

        if (event != null) {
            // Load event dates and update the displayed text
            setEventDates(event.getStartDate(), event.getEndDate());
        }
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return validProperty;
    }

    // === Additional accessors ===

    /**
     * @deprecated Color scheme is now handled via CSS classes on parent container.
     * Use theme classes like "theme-wisdom-blue" on a parent element instead.
     * This method is kept for API compatibility but has no effect.
     */
    @Deprecated
    public void setColorScheme(BookingFormColorScheme scheme) {
        // No-op: styling is handled via CSS classes
    }

    public boolean isConfirmed() {
        return confirmedProperty.get();
    }

    public void setConfirmed(boolean confirmed) {
        confirmedProperty.set(confirmed);
    }

    /**
     * Resets the section to its initial state.
     * Unchecks the prerequisite confirmation checkbox.
     */
    public void reset() {
        setConfirmed(false);
    }

    /**
     * Sets the event date range for the teaching sessions.
     * Updates the displayed text dynamically.
     */
    public void setEventDates(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;
        updateDateBulletText();
    }
}
