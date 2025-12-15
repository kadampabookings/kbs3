package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.modification;

import dev.webfx.extras.i18n.controls.I18nControls;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import one.modality.booking.frontoffice.bookingpage.BookingPageI18nKeys;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultConfirmationSection;

/**
 * Customized confirmation section for audio recording purchases.
 * Extends DefaultConfirmationSection to reuse existing UI patterns.
 *
 * @author Bruno Salmon
 */
public class AudioRecordingConfirmationSection extends DefaultConfirmationSection {

    @Override
    protected VBox buildSuccessHeader() {
        VBox header = new VBox(0);
        header.setAlignment(Pos.CENTER);
        VBox.setMargin(header, new Insets(0, 0, 40, 0));

        // Checkmark circle - styling via CSS class
        StackPane checkCircle = BookingPageUIBuilder.createThemedIconCircle(80);
        checkCircle.getStyleClass().add("bookingpage-confirmation-check-circle");

        SVGPath checkmark = BookingPageUIBuilder.createThemedIcon("M20 6L9 17l-5-5", 1.2);
        checkmark.setStrokeWidth(3);
        checkCircle.getChildren().add(checkmark);
        VBox.setMargin(checkCircle, new Insets(0, 0, 24, 0));

        // Custom title for audio recordings - styling via CSS classes
        Label titleLabel = I18nControls.newLabel("PurchaseComplete");
        titleLabel.getStyleClass().addAll("bookingpage-text-3xl", "bookingpage-font-bold", "bookingpage-text-primary");
        VBox.setMargin(titleLabel, new Insets(0, 0, 12, 0));

        // Custom subtitle for audio recordings
        Label subtitleLabel = I18nControls.newLabel("AudioPurchaseThankYou");
        subtitleLabel.getStyleClass().addAll("bookingpage-text-md", "bookingpage-text-muted");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(500);

        header.getChildren().addAll(checkCircle, titleLabel, subtitleLabel);
        return header;
    }

    @Override
    protected VBox buildDetailsSection() {
        // Return empty VBox - details section not relevant for audio purchases
        return new VBox();
    }

    @Override
    protected VBox buildWhatsNextSection() {
        VBox section = new VBox(0);

        // Section header
        HBox header = new StyledSectionHeader(BookingPageI18nKeys.WhatsNext, StyledSectionHeader.ICON_CHECKLIST);

        // Content card
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("bookingpage-card");

        // Step 1: Check Your Email (reuse parent pattern)
        HBox emailStep = createWhatsNextStep(
            BookingPageUIBuilder.createThemedIcon(BookingPageUIBuilder.ICON_ENVELOPE, 0.6),
            "CheckYourEmailReceiptTitle",
            "CheckYourEmailReceiptDesc",
            null
        );

        // Step 2: Access Your Recordings (KEY - Library menu info)
        HBox libraryStep = createWhatsNextStep(
            createHeadphonesIcon(),
            "AccessYourRecordingsTitle",
            "AccessYourRecordingsDesc",
            null
        );

        content.getChildren().addAll(emailStep, libraryStep);

        section.getChildren().addAll(header, content);
        VBox.setMargin(header, new Insets(0, 0, 16, 0));
        VBox.setMargin(section, new Insets(0, 0, 40, 0));

        return section;
    }

    private static final String ICON_HEADPHONES = "M3 18v-6a9 9 0 0118 0v6 M21 19a2 2 0 01-2 2h-1a2 2 0 01-2-2v-3a2 2 0 012-2h3v5z M3 19a2 2 0 002 2h1a2 2 0 002-2v-3a2 2 0 00-2-2H3v5z";

    // New icon for audio recordings
    protected SVGPath createHeadphonesIcon() {
        return BookingPageUIBuilder.createThemedIcon(ICON_HEADPHONES, 0.6);
    }
}
