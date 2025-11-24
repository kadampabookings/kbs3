package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Event package selection section - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class EventPackageSection implements BookingFormSection {

    private final VBox container = new VBox(24);
    private VBox selectedCard = null;

    public EventPackageSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Event Package"); // TODO: I18n
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        HBox packageOptions = new HBox(20);
        packageOptions.getChildren().addAll(
                createPackageCard("Meditation Course", "£350",
                        "5-day meditation program with daily teachings, guided meditations, and workshops"),
                createPackageCard("Course + Retreat", "£550",
                        "Complete 5-day course followed by 2-day retreat for deeper practice"),
                createPackageCard("Retreat Only", "£150", "Weekend meditation retreat in peaceful surroundings"));

        container.getChildren().addAll(titleContainer, packageOptions);
    }

    private Node createPackageCard(String title, String price, String desc) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("package-card");
        card.setPrefWidth(250);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("package-title-text");

        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("package-price-text");

        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("package-desc-text");
        descLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, priceLabel, descLabel);

        // Selection logic using CSS classes
        card.setOnMouseClicked(e -> {
            if (selectedCard != null) {
                selectedCard.getStyleClass().remove("selected");
            }
            card.getStyleClass().add("selected");
            selectedCard = card;
        });

        return card;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Event Package"; // TODO: Use I18n key
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
