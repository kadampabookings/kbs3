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
 * Section showing confirmation of booking submission - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class ConfirmationSection implements BookingFormSection {

    private final VBox container = new VBox(24);

    public ConfirmationSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Info banner
        VBox infoBanner = createInfoBanner();

        // Booking cards (simplified - showing structure)
        VBox bookingCards = createBookingCards();

        // Payment summary
        VBox paymentSummary = createPaymentSummary();

        container.getChildren().addAll(infoBanner, bookingCards, paymentSummary);
    }

    private VBox createInfoBanner() {
        VBox banner = new VBox(8);
        banner.getStyleClass().add("info-banner");
        banner.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("📋 Your Registered Attendees");
        title.getStyleClass().add("info-banner-title-text");

        Label desc = new Label(
                "Below is an overview of all registrations submitted. Review the details and proceed to payment when ready, " +
                "or continue adding more attendees.");
        desc.getStyleClass().add("info-banner-desc-text");
        desc.setWrapText(true);

        banner.getChildren().addAll(title, desc);
        return banner;
    }

    private VBox createBookingCards() {
        VBox cards = new VBox(24);

        // Sample booking card (in real implementation, this would be dynamic)
        VBox bookingCard = createBookingCard(
                "Sarah Johnson",
                "sarah.johnson@example.com",
                "Booking #1",
                "£840");

        cards.getChildren().add(bookingCard);
        return cards;
    }

    private VBox createBookingCard(String name, String email, String bookingNumber, String total) {
        VBox card = new VBox();
        card.getStyleClass().add("booking-card");

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.getStyleClass().add("booking-card-header");

        VBox attendeeInfo = new VBox(4);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("booking-card-attendee-name");

        Label emailLabel = new Label(email);
        emailLabel.getStyleClass().add("booking-card-attendee-email");

        attendeeInfo.getChildren().addAll(nameLabel, emailLabel);
        HBox.setHgrow(attendeeInfo, Priority.ALWAYS);

        Label bookingNum = new Label(bookingNumber);
        bookingNum.getStyleClass().add("booking-card-number");

        header.getChildren().addAll(attendeeInfo, bookingNum);

        // Content
        VBox content = new VBox(16);
        content.setPadding(new Insets(24));

        // Event info
        VBox eventInfo = new VBox(8);
        eventInfo.getStyleClass().add("booking-card-event-info");
        eventInfo.setPadding(new Insets(16));

        Label eventTitle = new Label("Empowering the Mind");
        eventTitle.getStyleClass().add("booking-card-event-title");

        Label eventDates = new Label("Fri 28 Nov - Sun 30 Nov 2025 • Standard rate");
        eventDates.getStyleClass().add("booking-card-event-dates");

        eventInfo.getChildren().addAll(eventTitle, eventDates);

        // Booking details
        VBox details = new VBox(8);
        details.getChildren().addAll(
                createBookingRow("Teaching: Empowering the Mind", "£350"),
                createBookingRow("Accommodation: Single Room (2 nights)", "£400"),
                createBookingRow("Meals: Vegetarian (Fri dinner - Sun lunch)", "£75"),
                createBookingRow("Audio Recordings: MP3 format", "£15"));

        // Total
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setPadding(new Insets(20, 0, 0, 0));
        totalRow.getStyleClass().add("booking-total-separator");

        Label totalLabel = new Label("Booking Total");
        totalLabel.getStyleClass().add("booking-total-label");

        Label totalValue = new Label(total);
        totalValue.getStyleClass().add("booking-total-value");

        HBox.setHgrow(totalLabel, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalLabel, totalValue);

        content.getChildren().addAll(eventInfo, details, totalRow);

        card.getChildren().addAll(header, content);
        return card;
    }

    private HBox createBookingRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));

        Label labelText = new Label(label);
        labelText.getStyleClass().add("booking-row-label");

        Label valueText = new Label(value);
        valueText.getStyleClass().add("booking-row-value");

        HBox.setHgrow(labelText, Priority.ALWAYS);
        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private VBox createPaymentSummary() {
        VBox summary = new VBox(16);
        summary.getStyleClass().add("payment-summary");
        summary.setPadding(new Insets(32));

        Label title = new Label("Payment Summary");
        title.getStyleClass().add("payment-summary-title");
        title.setPadding(new Insets(0, 0, 20, 0));

        VBox rows = new VBox(12);
        rows.getChildren().addAll(
                createSummaryRow("Total Bookings", "2 attendees"),
                createSummaryRow("Total Cost", "£1,345"));

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setPadding(new Insets(16, 0, 0, 0));
        totalRow.getStyleClass().add("booking-total-separator");

        Label totalLabel = new Label("Total Amount");
        totalLabel.getStyleClass().add("payment-total-label");

        Label totalValue = new Label("£1,345");
        totalValue.getStyleClass().add("cost-total-value-text");

        HBox.setHgrow(totalLabel, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalLabel, totalValue);

        summary.getChildren().addAll(title, rows, totalRow);
        return summary;
    }

    private HBox createSummaryRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));
        row.getStyleClass().add("summary-row-separator");

        Label labelText = new Label(label);
        labelText.getStyleClass().add("cost-row-label-text");

        Label valueText = new Label(value);
        valueText.getStyleClass().add("cost-row-value-text");

        HBox.setHgrow(labelText, Priority.ALWAYS);
        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Registration Submitted";
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
