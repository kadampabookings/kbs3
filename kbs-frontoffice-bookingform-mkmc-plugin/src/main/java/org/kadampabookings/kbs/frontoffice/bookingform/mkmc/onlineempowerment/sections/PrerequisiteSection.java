package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

import dev.webfx.extras.i18n.controls.I18nControls;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import one.modality.base.shared.entities.Event;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;
import one.modality.booking.frontoffice.bookingpage.ResettableSection;
import one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.components.StyledSectionHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Section for displaying and confirming prerequisites for online empowerment.
 * User must confirm they meet the prerequisites to proceed.
 *
 * Layout matches the design mockup:
 * 1. Section header "Prerequisites"
 * 2. Yellow "Important Information" box with bullet points
 * 3. White description box explaining the event
 * 4. Checkbox confirmation row
 *
 * @author Bruno Salmon
 */
public class PrerequisiteSection implements BookingFormSection, ResettableSection {

    private final VBox container = new VBox(16);
    private final SimpleBooleanProperty validProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<BookingFormColorScheme> colorScheme = new SimpleObjectProperty<>(BookingFormColorScheme.DEFAULT);

    private StyledSectionHeader header;
    private CheckBox confirmCheckBox;
    private VBox confirmBox;

    // Dynamic date range for the teaching sessions
    private LocalDate startDate;
    private LocalDate endDate;
    private Text dateRangeText; // Reference to update when dates change

    public PrerequisiteSection() {
        buildUI();
    }

    private void buildUI() {
        // Section header - "Prerequisites" with check icon
        header = new StyledSectionHeader(
                MKMCI18nKeys.Prerequisites,
                StyledSectionHeader.ICON_CHECK_CIRCLE
        );
        header.colorSchemeProperty().bind(colorScheme);

        // === Yellow "Important Information" box ===
        VBox importantInfoBox = createImportantInfoBox();

        // === Checkbox confirmation box (includes national event notice) ===
        confirmBox = createConfirmationBox();

        container.getChildren().addAll(header, importantInfoBox, confirmBox);
        container.getStyleClass().add("booking-form-prerequisite-section");
    }

    private VBox createImportantInfoBox() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setBackground(new Background(new BackgroundFill(
                Color.web("#FFF9E6"), new CornerRadii(8), null)));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#FFE58F"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));

        // Warning icon and "Important Information" title
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label warningIcon = new Label("\u26A0");
        warningIcon.setStyle("-fx-font-size: 20px;");
        Label titleLabel = I18nControls.newLabel(MKMCI18nKeys.ImportantInformation);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");
        titleLabel.setTextFill(Color.web("#856404"));
        titleRow.getChildren().addAll(warningIcon, titleLabel);

        // Bullet point 1: "This event is livestreamed only - there will be no video recordings..."
        TextFlow bullet1 = createBulletPoint(
                "This event is ",
                "livestreamed only",
                " - there will be no video recordings available to watch after the event"
        );

        // Bullet point 2: "This booking is for the online teaching sessions only from..." (dynamic dates)
        TextFlow bullet2 = createDateBulletPoint();

        box.getChildren().addAll(titleRow, bullet1, bullet2);
        return box;
    }

    private TextFlow createDateBulletPoint() {
        Text bullet = new Text("\u2022 ");
        bullet.setStyle("-fx-font-size: 14px;");
        bullet.setFill(Color.web("#856404"));

        Text prefixText = new Text("This booking is for the ");
        prefixText.setStyle("-fx-font-size: 14px;");
        prefixText.setFill(Color.web("#856404"));

        Text bold = new Text("online teaching sessions only");
        bold.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        bold.setFill(Color.web("#856404"));

        // Store reference to date range text so we can update it dynamically
        dateRangeText = new Text(buildDateRangeString());
        dateRangeText.setStyle("-fx-font-size: 14px;");
        dateRangeText.setFill(Color.web("#856404"));

        TextFlow flow = new TextFlow(bullet, prefixText, bold, dateRangeText);
        flow.setLineSpacing(2);
        return flow;
    }

    private String buildDateRangeString() {
        if (startDate == null || endDate == null) {
            return " - the following retreat is not included";
        }

        String dateRange = formatDateRange(startDate, endDate);
        return " from " + dateRange + " - the following retreat is not included";
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);
        DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

        if (start.getMonth() == end.getMonth() && start.getYear() == end.getYear()) {
            // Same month: "27 - 30 March 2026"
            return start.format(dayFormatter) + " - " + end.format(fullFormatter);
        } else if (start.getYear() == end.getYear()) {
            // Different months, same year: "27 February - 1 March 2026"
            return start.format(dayMonthFormatter) + " - " + end.format(fullFormatter);
        } else {
            // Different years: "27 December 2025 - 1 January 2026"
            return start.format(fullFormatter) + " - " + end.format(fullFormatter);
        }
    }

    private TextFlow createBulletPoint(String prefix, String boldText, String suffix) {
        Text bullet = new Text("\u2022 ");
        bullet.setStyle("-fx-font-size: 14px;");
        bullet.setFill(Color.web("#856404"));

        Text prefixText = new Text(prefix);
        prefixText.setStyle("-fx-font-size: 14px;");
        prefixText.setFill(Color.web("#856404"));

        Text bold = new Text(boldText);
        bold.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        bold.setFill(Color.web("#856404"));

        Text suffixText = new Text(suffix);
        suffixText.setStyle("-fx-font-size: 14px;");
        suffixText.setFill(Color.web("#856404"));

        TextFlow flow = new TextFlow(bullet, prefixText, bold, suffixText);
        flow.setLineSpacing(2);
        return flow;
    }

    private VBox createConfirmationBox() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));
        box.setBackground(new Background(new BackgroundFill(
                Color.WHITE, new CornerRadii(8), null)));
        box.setBorder(new Border(new BorderStroke(
                Color.web("#E6E7E7"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));

        // National event notice with bold text
        TextFlow noticeText = new TextFlow();
        Text notice1 = new Text("Since this is a national event, ");
        notice1.setStyle("-fx-font-size: 14px;");
        notice1.setFill(Color.web("#495057"));

        Text noticeBold = new Text("you must be a resident of the United Kingdom");
        noticeBold.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        noticeBold.setFill(Color.web("#495057"));

        Text notice2 = new Text(" to attend online.");
        notice2.setStyle("-fx-font-size: 14px;");
        notice2.setFill(Color.web("#495057"));

        noticeText.getChildren().addAll(notice1, noticeBold, notice2);
        noticeText.setLineSpacing(2);

        // Checkbox row
        HBox checkboxRow = new HBox(12);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);

        confirmCheckBox = new CheckBox();
        confirmCheckBox.setStyle("-fx-cursor: hand;");

        // Confirmation text with bold part
        TextFlow confirmText = new TextFlow();
        Text text1 = new Text("I understand I am booking to join a live-streamed event and confirm I reside in the ");
        text1.setStyle("-fx-font-size: 14px;");
        text1.setFill(Color.web("#212529"));

        Text boldText = new Text("United Kingdom");
        boldText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        boldText.setFill(Color.web("#212529"));

        Text text2 = new Text(".");
        text2.setStyle("-fx-font-size: 14px;");
        text2.setFill(Color.web("#212529"));

        confirmText.getChildren().addAll(text1, boldText, text2);

        checkboxRow.getChildren().addAll(confirmCheckBox, confirmText);
        HBox.setHgrow(confirmText, Priority.ALWAYS);

        box.getChildren().addAll(noticeText, checkboxRow);

        // Make entire box clickable to toggle checkbox
        box.setCursor(Cursor.HAND);
        box.setOnMouseClicked(e -> {
            confirmCheckBox.setSelected(!confirmCheckBox.isSelected());
        });

        // Bind validity to checkbox
        validProperty.bind(confirmCheckBox.selectedProperty());

        // Update styling when confirmed
        confirmCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateConfirmBoxStyle(newVal);
        });

        return box;
    }

    private void updateConfirmBoxStyle(boolean selected) {
        BookingFormColorScheme scheme = colorScheme.get();
        if (selected) {
            confirmBox.setBackground(new Background(new BackgroundFill(
                    scheme.getSelectedBg(), new CornerRadii(8), null)));
            confirmBox.setBorder(new Border(new BorderStroke(
                    scheme.getPrimary(), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));
        } else {
            confirmBox.setBackground(new Background(new BackgroundFill(
                    Color.WHITE, new CornerRadii(8), null)));
            confirmBox.setBorder(new Border(new BorderStroke(
                    Color.web("#E6E7E7"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));
        }
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

    public ObjectProperty<BookingFormColorScheme> colorSchemeProperty() {
        return colorScheme;
    }

    public void setColorScheme(BookingFormColorScheme scheme) {
        this.colorScheme.set(scheme);
    }

    public boolean isConfirmed() {
        return confirmCheckBox.isSelected();
    }

    public void setConfirmed(boolean confirmed) {
        confirmCheckBox.setSelected(confirmed);
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
        if (dateRangeText != null) {
            dateRangeText.setText(buildDateRangeString());
        }
    }
}
