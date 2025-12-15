package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.gpclass.sections;

import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.kit.util.properties.FXProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import one.modality.base.shared.entities.Event;
import one.modality.base.shared.entities.Rate;
import one.modality.base.shared.entities.formatters.EventPriceFormatter;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader;
import one.modality.booking.frontoffice.bookingpage.sections.HasRateTypeSection;
import one.modality.ecommerce.document.service.PolicyAggregate;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

import java.util.function.Consumer;

/**
 * Rate selection section for GP Class booking form.
 * Allows users to choose between Standard Rate and MKMC Member Rate.
 *
 * @author Claude
 */
public class GPClassRateSection implements BookingFormSection, HasRateTypeSection {

    // UI Components
    private final VBox container = new VBox(16);
    private VBox standardRateCard;
    private VBox memberRateCard;

    // State
    private final ObjectProperty<RateType> selectedRateType = new SimpleObjectProperty<>(RateType.STANDARD);
    private final SimpleBooleanProperty validProperty = new SimpleBooleanProperty(true);

    // Working booking reference
    private WorkingBookingProperties workingBookingProperties;

    // Rates
    private Rate standardRate;
    private Rate memberRate;
    private int standardPrice = 0;
    private int memberPrice = 0;

    // Callbacks
    private Consumer<RateType> onRateTypeChanged;

    public GPClassRateSection() {
        buildUI();
    }

    private void buildUI() {
        // Section header
        StyledSectionHeader header = new StyledSectionHeader(
                MKMCI18nKeys.YourPricingTier,
                StyledSectionHeader.ICON_TAG
        );

        // Rate cards container (FlowPane for responsive wrapping on mobile)
        FlowPane cardsContainer = new FlowPane();
        cardsContainer.setHgap(16);
        cardsContainer.setVgap(12);
        cardsContainer.setAlignment(Pos.TOP_LEFT);

        // Standard Rate card
        standardRateCard = createRateCard(
                MKMCI18nKeys.StandardRate,
                RateType.STANDARD
        );

        // Member Rate card
        memberRateCard = createRateCard(
                MKMCI18nKeys.MemberRate,
                RateType.MEMBER
        );

        cardsContainer.getChildren().addAll(standardRateCard, memberRateCard);

        container.getChildren().addAll(header, cardsContainer);
        container.getStyleClass().add("gpclass-rate-section");
        container.setMinWidth(0);
    }

    private VBox createRateCard(Object titleKey, RateType rateType) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMinWidth(160);
        card.setPrefWidth(180);
        card.setMaxWidth(220);
        card.getStyleClass().addAll("bookingpage-card", "gpclass-rate-card");
        card.setCursor(Cursor.HAND);

        // Track selection state for this card
        SimpleBooleanProperty isSelected = new SimpleBooleanProperty(selectedRateType.get() == rateType);
        selectedRateType.addListener((obs, old, newVal) -> isSelected.set(newVal == rateType));

        // Checkmark indicator (top-right) - uses CSS-themed helper
        StackPane checkmark = BookingPageUIBuilder.createCheckmarkBadgeCss(20);
        checkmark.setVisible(isSelected.get());
        isSelected.addListener((obs, old, val) -> checkmark.setVisible(val));

        // Rate type label
        Label titleLabel = I18nControls.newLabel(titleKey);
        titleLabel.getStyleClass().addAll("bookingpage-text-base", "bookingpage-font-semibold", "bookingpage-text-dark");

        // Price label (per class)
        Label priceLabel = new Label();
        priceLabel.getStyleClass().addAll("bookingpage-text-sm", "bookingpage-text-muted");

        // Store reference for price updates
        if (rateType == RateType.STANDARD) {
            priceLabel.setUserData("standardPrice");
        } else {
            priceLabel.setUserData("memberPrice");
        }

        // Card content with checkmark overlay
        BorderPane cardLayout = new BorderPane();
        VBox contentBox = new VBox(4);
        contentBox.getChildren().addAll(titleLabel, priceLabel);
        cardLayout.setCenter(contentBox);

        StackPane cardWithCheckmark = new StackPane(cardLayout, checkmark);
        StackPane.setAlignment(checkmark, Pos.TOP_RIGHT);
        StackPane.setMargin(checkmark, new Insets(-4, -4, 0, 0));

        card.getChildren().add(cardWithCheckmark);

        // Update card styling based on selection
        FXProperties.runNowAndOnPropertyChange(selected -> {
            if (selected) {
                if (!card.getStyleClass().contains("selected")) {
                    card.getStyleClass().add("selected");
                }
            } else {
                card.getStyleClass().remove("selected");
            }
        }, isSelected);

        // Click handler
        card.setOnMouseClicked(e -> {
            selectedRateType.set(rateType);
            if (onRateTypeChanged != null) {
                onRateTypeChanged.accept(rateType);
            }
            e.consume();
        });

        // Hover/selected effects are handled via CSS (.bookingpage-card:hover, .bookingpage-card.selected)
        // using color scheme variables (--booking-form-hover-border, --booking-form-primary)

        // Store price label reference for updates
        card.setUserData(priceLabel);

        return card;
    }

    private void loadData() {
        if (workingBookingProperties == null) return;

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();

        if (policyAggregate == null) return;

        // Get daily rate - Rate entity contains both standard and member prices
        Rate dailyRate = policyAggregate.getDailyRate();

        if (dailyRate != null) {
            standardRate = dailyRate;
            memberRate = dailyRate;

            // Standard price from getPrice()
            if (dailyRate.getPrice() != null) {
                standardPrice = dailyRate.getPrice();
            }

            // Member price from getFacilityFeePrice() (facility fee = member price in MKMC context)
            if (dailyRate.getFacilityFeePrice() != null) {
                memberPrice = dailyRate.getFacilityFeePrice();
            } else {
                // Fallback to standard price if no member price defined
                memberPrice = standardPrice;
            }
        }

        // Update price labels
        updatePriceLabels();

        // Notify listener of initial selection
        if (onRateTypeChanged != null) {
            onRateTypeChanged.accept(selectedRateType.get());
        }
    }

    private void updatePriceLabels() {
        // Update standard rate card price
        if (standardRateCard != null && standardRateCard.getUserData() instanceof Label) {
            Label priceLabel = (Label) standardRateCard.getUserData();
            priceLabel.setText(formatPrice(standardPrice) + " per class");
        }

        // Update member rate card price
        if (memberRateCard != null && memberRateCard.getUserData() instanceof Label) {
            Label priceLabel = (Label) memberRateCard.getUserData();
            priceLabel.setText(formatPrice(memberPrice) + " per class");
        }
    }

    private String formatPrice(int priceInCents) {
        Event event = getEvent();
        return EventPriceFormatter.formatWithCurrency(priceInCents, event);
    }

    private Event getEvent() {
        if (workingBookingProperties != null && workingBookingProperties.getWorkingBooking() != null) {
            return workingBookingProperties.getWorkingBooking().getEvent();
        }
        return null;
    }

    // === Public Accessors ===

    /**
     * Returns the price per class for the currently selected rate type.
     */
    public int getSelectedPricePerClass() {
        return selectedRateType.get() == RateType.MEMBER ? memberPrice : standardPrice;
    }

    /**
     * Returns the selected rate type property for binding.
     */
    public ObjectProperty<RateType> selectedRateTypeProperty() {
        return selectedRateType;
    }

    // === BookingFormSection Implementation ===

    @Override
    public Object getTitleI18nKey() {
        return MKMCI18nKeys.YourPricingTier;
    }

    @Override
    public Node getView() {
        return container;
    }

    @Override
    public void setWorkingBookingProperties(WorkingBookingProperties workingBookingProperties) {
        this.workingBookingProperties = workingBookingProperties;
        loadData();
    }

    @Override
    public ObservableBooleanValue validProperty() {
        return validProperty;
    }

    // === HasRateTypeSection Implementation ===

    @Override
    public ObjectProperty<one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme> colorSchemeProperty() {
        return new SimpleObjectProperty<>(one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme.DEFAULT);
    }

    @Override
    public void setColorScheme(one.modality.booking.frontoffice.bookingpage.theme.BookingFormColorScheme scheme) {
        // CSS-based theming - no-op
    }

    @Override
    public RateType getSelectedRateType() {
        return selectedRateType.get();
    }

    @Override
    public Rate getCurrentRate() {
        return selectedRateType.get() == RateType.MEMBER ? memberRate : standardRate;
    }

    @Override
    public void setOnPackageSelected(Consumer<one.modality.base.shared.entities.BookablePeriod> handler) {
        // Not used for GP class - individual date selection instead
    }

    @Override
    public void setOnRateTypeChanged(Consumer<RateType> handler) {
        this.onRateTypeChanged = handler;
    }
}
