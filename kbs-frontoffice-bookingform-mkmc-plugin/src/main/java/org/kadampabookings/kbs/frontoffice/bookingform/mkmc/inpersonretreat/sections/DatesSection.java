package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Section for selecting event dates - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class DatesSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public DatesSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Event Dates");
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        HBox dateRow = new HBox(16);

        Label fromLabel = new Label("From:");
        fromLabel.getStyleClass().add("form-label-text");

        Button fromPicker = createDatePicker("Friday 28 November");

        Label toLabel = new Label("To:");
        toLabel.getStyleClass().add("form-label-text");

        Button toPicker = createDatePicker("Sunday 30 November");

        HBox.setHgrow(fromPicker, Priority.ALWAYS);
        HBox.setHgrow(toPicker, Priority.ALWAYS);

        dateRow.getChildren().addAll(fromLabel, fromPicker, toLabel, toPicker);

        Label noteText = new Label(
                "The event begins at 7:30 PM on the first day and concludes after lunch on the final day. " +
                "Your booking includes dinner on arrival through to lunch on departure.");
        noteText.getStyleClass().add("note-text");
        noteText.setWrapText(true);
        noteText.setPadding(new Insets(16, 0, 0, 0));

        container.getChildren().addAll(titleContainer, dateRow, noteText);
    }

    private Button createDatePicker(String text) {
        Button picker = new Button(text);
        picker.getStyleClass().add("date-picker-button");
        picker.setPadding(new Insets(12, 16, 12, 16));

        picker.setOnAction(e -> {
            // TODO: Open date picker dialog
        });

        return picker;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Event Dates";
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        // TODO: Bind to actual working booking data
    }
}
