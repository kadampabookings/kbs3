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
 * Section displaying teaching information - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class TeachingSection implements BookingFormSection {

    private final VBox container = new VBox(16);

    public TeachingSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Teaching");
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        HBox infoBox = new HBox();
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(16, 0, 16, 0));

        VBox detailsBox = new VBox(4);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        Label titleLabel = new Label("Empowering the Mind");
        titleLabel.getStyleClass().add("event-title-text");

        Label dateLabel = new Label("Friday 28 November - Sunday 30 November");
        dateLabel.getStyleClass().add("event-dates-text");

        detailsBox.getChildren().addAll(titleLabel, dateLabel);

        Label priceLabel = new Label("£350");
        priceLabel.getStyleClass().add("event-price-label");

        infoBox.getChildren().addAll(detailsBox, priceLabel);

        container.getChildren().addAll(titleContainer, infoBox);
    }

    @Override
    public Object getTitleI18nKey() {
        return "Teaching";
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
