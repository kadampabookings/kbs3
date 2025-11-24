package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Accommodation selection section - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class AccommodationSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public AccommodationSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Accommodation"); // TODO: I18n
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        VBox roomGrid = new VBox(16);
        roomGrid.getChildren().addAll(
                createRoomCard("Single Room", "Private room with ensuite bathroom", "£200", "AVAILABLE", "available"),
                createRoomCard("Shared Room", "Twin room shared with another participant", "£120",
                        "LIMITED AVAILABILITY", "limited"),
                createRoomCard("Camping", "Bring your own tent in our peaceful grounds", "£60", "AVAILABLE",
                        "available"),
                createRoomCard("Dormitory", "Shared dormitory style accommodation", "£80", "SOLD OUT", "soldout"),
                createRoomCard("Commuter (No Accommodation)", "Day visitor - no accommodation needed", "£0",
                        "AVAILABLE", "available"));

        container.getChildren().addAll(titleContainer, roomGrid);
    }

    private Node createRoomCard(String name, String desc, String price, String badgeText, String availability) {
        HBox card = new HBox(20);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("room-card");

        if ("soldout".equals(availability)) {
            card.setOpacity(0.6);
            card.getStyleClass().add("soldout");
        }

        VBox infoBox = new VBox(8);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox nameBox = new HBox(12);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("room-name-text");

        Label badge = new Label(badgeText);
        badge.getStyleClass().add("availability-badge");
        badge.getStyleClass().add(availability);
        badge.setPadding(new Insets(6, 14, 6, 14));

        nameBox.getChildren().addAll(nameLabel, badge);

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("room-desc-text");

        infoBox.getChildren().addAll(nameBox, descLabel);

        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("room-price-text");

        card.getChildren().addAll(infoBox, priceLabel);

        if (!"soldout".equals(availability)) {
            card.setOnMouseClicked(e -> {
                // TODO: Handle selection
                card.getStyleClass().add("selected");
            });
        }

        return card;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Accommodation"; // TODO: Use I18n key
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
