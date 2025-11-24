package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Section for parking space selection - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class ParkingSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public ParkingSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Parking");
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        Label noteText = new Label("Parking spaces are limited and you are encouraged to car share where possible.");
        noteText.getStyleClass().add("note-text-italic");
        noteText.setWrapText(true);

        VBox buttonGroup = new VBox(12);
        buttonGroup.setPadding(new Insets(12, 0, 0, 0));

        Button standardBtn = createParkingButton("I require a standard parking space", false);
        Button blueBadgeBtn = createParkingButton("I am a blue badge holder (must be displayed in car)", false);
        Button noNeedBtn = createParkingButton("I don't need a parking space", true);

        buttonGroup.getChildren().addAll(standardBtn, blueBadgeBtn, noNeedBtn);

        container.getChildren().addAll(titleContainer, noteText, buttonGroup);
    }

    private Button createParkingButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("parking-button");
        if (selected) {
            btn.getStyleClass().add("selected");
        }
        btn.setPadding(new Insets(14, 20, 14, 20));

        btn.setOnAction(e -> {
            // TODO: Handle parking selection
        });

        return btn;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Parking";
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
