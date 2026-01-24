package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.webtext.HtmlText;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.sections.prerequisite.DefaultPrerequisiteSection;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

/**
 * MKMC-specific prerequisite section for online empowerment events.
 * Extends DefaultPrerequisiteSection with UK residency requirement.
 *
 * <p>Displays:</p>
 * <ol>
 *   <li>Section header "Prerequisites"</li>
 *   <li>Yellow "Important Information" box with:
 *     <ul>
 *       <li>Livestream-only warning (no video recordings)</li>
 *       <li>Dynamic event dates</li>
 *     </ul>
 *   </li>
 *   <li>UK residency confirmation checkbox</li>
 * </ol>
 *
 * @author Bruno Salmon
 */
public class PrerequisiteSection extends DefaultPrerequisiteSection {

    private HtmlText dateBulletHtmlText;

    public PrerequisiteSection() {
        super();
    }

    @Override
    protected void buildUI() {
        // Use MKMC i18n keys
        setTitleKey(MKMCI18nKeys.Prerequisites);
        setImportantInfoTitleKey(MKMCI18nKeys.ImportantInformation);

        // Section header
        header = new one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader(
                titleKey,
                one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader.ICON_CHECK_CIRCLE
        );

        // Important information box with custom content
        importantInfoBox = createMKMCImportantInfoBox();

        // Confirmation box with UK residency requirement
        confirmBox = createMKMCConfirmationBox();

        container.getChildren().addAll(header, importantInfoBox, confirmBox);
        container.getStyleClass().add("booking-form-prerequisite-section");
        container.setMinWidth(0);
    }

    private VBox createMKMCImportantInfoBox() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("bookingpage-warning-box");
        box.setMinWidth(0);

        // Warning icon and "Important Information" title
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label warningIcon = new Label("\u26A0");
        warningIcon.getStyleClass().add("bookingpage-text-lg");
        Label titleLabel = I18nControls.newLabel(MKMCI18nKeys.ImportantInformation);
        titleLabel.getStyleClass().addAll("bookingpage-text-lg", "bookingpage-font-semibold", "bookingpage-text-warning");
        titleRow.getChildren().addAll(warningIcon, titleLabel);

        // Bullet 1: Livestream only warning
        HtmlText bullet1 = new HtmlText();
        bullet1.setText("\u2022 This event is <b>livestreamed only</b> - there will be no video recordings available to watch after the event");
        bullet1.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-warning");

        // Bullet 2: Event dates (dynamic)
        dateBulletHtmlText = new HtmlText();
        updateDateBulletText();
        dateBulletHtmlText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-warning");

        box.getChildren().addAll(titleRow, bullet1, dateBulletHtmlText);
        return box;
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

    @Override
    protected void onEventDatesChanged() {
        updateDateBulletText();
    }

    private VBox createMKMCConfirmationBox() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("bookingpage-card");
        box.setMinWidth(0);

        // National event notice with bold text
        HtmlText noticeText = new HtmlText();
        noticeText.setText("Since this is a national event, <b>you must be a resident of the United Kingdom</b> to attend online.");
        noticeText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-secondary");

        // Checkbox row
        HBox checkboxRow = new HBox(12);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);

        // Checkbox indicator
        StackPane checkboxIndicator = BookingPageUIBuilder.createCheckboxIndicator(confirmedProperty);

        // UK residency confirmation text
        HtmlText confirmText = new HtmlText();
        confirmText.setText("I understand I am booking to join a live-streamed event and confirm I reside in the <b>United Kingdom</b>.");
        confirmText.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-dark");

        checkboxRow.getChildren().addAll(checkboxIndicator, confirmText);
        HBox.setHgrow(confirmText, Priority.ALWAYS);

        box.getChildren().addAll(noticeText, checkboxRow);

        // Make box clickable
        box.setCursor(Cursor.HAND);
        box.setOnMouseClicked(e -> confirmedProperty.set(!confirmedProperty.get()));

        // Update CSS class when confirmed
        confirmedProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                box.getStyleClass().add("selected");
            } else {
                box.getStyleClass().remove("selected");
            }
        });

        return box;
    }
}
