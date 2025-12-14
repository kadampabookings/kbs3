package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.onlineempowerment.sections;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import one.modality.base.client.i18n.I18nEntities;
import one.modality.base.shared.entities.BookablePeriod;
import one.modality.base.shared.entities.Rate;
import one.modality.base.shared.entities.ScheduledItem;
import one.modality.base.shared.entities.util.Items;
import one.modality.base.shared.knownitems.KnownItemFamily;
import one.modality.booking.client.workingbooking.WorkingBooking;
import one.modality.booking.frontoffice.bookingpage.components.BookingPageUIBuilder;
import one.modality.booking.frontoffice.bookingpage.components.StyledSectionHeader;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultRateTypeSection;
import one.modality.ecommerce.document.service.PolicyAggregate;
import org.kadampabookings.kbs.frontoffice.bookingform.mkmc.MKMCI18nKeys;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Simple pricing section showing programme info and standard rate in a clean card.
 * Displays: Rate type badge, Programme title, Date range, Price.
 *
 * <p>Uses CSS-based theming. Styling is handled via CSS classes that inherit
 * theme colors from CSS variables set on the parent container.</p>
 *
 * <p>CSS classes used:</p>
 * <ul>
 *   <li>{@code .booking-form-rate-type-section} - section container</li>
 *   <li>{@code .booking-form-rate-card} - card styling with hover/selected states</li>
 *   <li>{@code .booking-form-rate-badge} - rate type badge</li>
 *   <li>{@code .bookingpage-text-*} - text styling utilities</li>
 * </ul>
 *
 * @author Bruno Salmon
 */
public class RateTypeSection extends DefaultRateTypeSection {

    private Label programmeTitleLabel;
    private Label dateRangeLabel;
    private int totalPrice;  // Calculated total price for the selected period
    private Consumer<BookablePeriod> onPackageSelected;
    private final ObjectProperty<BookablePeriod> selectedPeriod = new SimpleObjectProperty<>();
    private SimpleBooleanProperty cardSelected; // Initialized in buildUI() due to parent constructor call order

    @Override
    protected void buildUI() {
        // Initialize here due to parent constructor calling buildUI() before subclass fields are initialized
        cardSelected = new SimpleBooleanProperty(true); // Always selected (single option)

        // Section header - styling handled by CSS theme classes (e.g., "theme-vajrayogini-red")
        header = new StyledSectionHeader(
                MKMCI18nKeys.YourPricingTier,
                StyledSectionHeader.ICON_TAG
        );

        // Simple card with horizontal layout - using bookingpage-card for proper CSS theming
        card = new VBox(0);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("bookingpage-card", "selected"); // Use standard card class with selected state

        // Horizontal layout: left content + price + checkmark on right
        HBox cardContent = new HBox(16);
        cardContent.setAlignment(Pos.CENTER_LEFT);

        // Left side: badge, title, dates
        VBox leftContent = new VBox(8);
        leftContent.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        // Rate badge (e.g., "Standard Rate") - styled via CSS
        rateBadge = new Label("Standard Rate");
        rateBadge.setPadding(new Insets(4, 10, 4, 10));
        rateBadge.getStyleClass().add("booking-form-rate-badge");

        // Programme title (e.g., "Full Weekend") - styled via CSS classes
        programmeTitleLabel = new Label();
        programmeTitleLabel.getStyleClass().addAll("bookingpage-text-xl", "bookingpage-font-semibold", "bookingpage-text-dark");

        // Date range (e.g., "25 - 30 Dec 2025") - styled via CSS classes
        dateRangeLabel = new Label();
        dateRangeLabel.getStyleClass().addAll("bookingpage-text-base", "bookingpage-text-muted");

        leftContent.getChildren().addAll(rateBadge, programmeTitleLabel, dateRangeLabel);

        // Right side: Price and checkmark in a vertical stack (checkmark above price)
        VBox rightContent = new VBox(8);
        rightContent.setAlignment(Pos.CENTER_RIGHT);

        // Checkmark badge - uses CSS for theming (inherits --booking-form-primary from parent)
        StackPane checkmarkBadge = BookingPageUIBuilder.createCheckmarkBadgeCss(28);
        checkmarkBadge.setVisible(cardSelected.get());
        cardSelected.addListener((obs, old, val) -> checkmarkBadge.setVisible(val));

        // Price (e.g., "£85") - styled via CSS classes
        priceLabel = new Label();
        priceLabel.getStyleClass().addAll("bookingpage-price-large", "bookingpage-text-dark");

        rightContent.getChildren().addAll(checkmarkBadge, priceLabel);

        cardContent.getChildren().addAll(leftContent, rightContent);
        card.getChildren().add(cardContent);

        container.getChildren().addAll(header, card);
        container.getStyleClass().add("booking-form-rate-type-section");
        container.setMinWidth(0); // Allow shrinking for responsive design
        card.setMinWidth(0); // Allow card to shrink
    }

    @Override
    protected void loadData() {
        if (workingBookingProperties == null) {
            return;
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();

        if (policyAggregate == null) {
            return;
        }

        // Load programme info
        loadProgramme(policyAggregate);

        // Store current rate for reference
        currentRate = policyAggregate.getDailyRate();

        // Notify rate type selected (always standard for now)
        if (onRateTypeChanged != null) {
            onRateTypeChanged.accept(getSelectedRateType());
        }
    }

    private void loadProgramme(PolicyAggregate policyAggregate) {
        List<BookablePeriod> bookablePeriods = policyAggregate.getBookablePeriods(
                KnownItemFamily.TEACHING,
                MKMCI18nKeys.FullProgramme
        );

        if (bookablePeriods.isEmpty()) {
            return;
        }

        // Use the first bookable period
        BookablePeriod period = bookablePeriods.get(0);
        selectedPeriod.set(period);

        // Bind title to i18n entity translation
        I18nEntities.bindTranslatedEntityToTextProperty(programmeTitleLabel, period);

        // Get dates
        ScheduledItem startItem = period.getStartScheduledItem();
        ScheduledItem endItem = period.getEndScheduledItem();

        if (startItem != null && endItem != null) {
            LocalDate startDate = startItem.getDate();
            LocalDate endDate = endItem.getDate();

            if (startDate != null && endDate != null) {
                dateRangeLabel.setText(BookingPageUIBuilder.formatDateRangeShort(startDate, endDate));
            }
        }

        // Calculate total price
        List<Rate> dailyRates = policyAggregate.getDailyRates().stream()
                .filter(rate -> Items.isOfFamily(rate.getItem(), KnownItemFamily.TEACHING))
                .collect(Collectors.toList());
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();

        totalPrice = calculateTotalPrice(period, teachingItems, dailyRates);
        priceLabel.setText(formatPrice(totalPrice));

        // Book teaching items into WorkingBooking immediately when period is selected
        // This ensures WorkingBooking always has the current selections
        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        bookTeachingItemsForPeriod(workingBooking, period, teachingItems);

        // Notify listener
        if (onPackageSelected != null) {
            onPackageSelected.accept(period);
        }
    }

    /**
     * Books teaching items for the selected period into WorkingBooking.
     * Called automatically when a period is selected.
     */
    private void bookTeachingItemsForPeriod(WorkingBooking workingBooking, BookablePeriod period, List<ScheduledItem> allTeachingItems) {
        ScheduledItem startItem = period.getStartScheduledItem();
        ScheduledItem endItem = period.getEndScheduledItem();

        if (startItem == null || endItem == null) return;

        LocalDate periodStartDate = startItem.getDate();
        LocalDate periodEndDate = endItem.getDate();

        if (periodStartDate == null || periodEndDate == null) return;

        // Filter teaching items within the period date range
        List<ScheduledItem> periodTeachingItems = allTeachingItems.stream()
                .filter(item -> item.getDate() != null)
                .filter(item -> {
                    LocalDate itemDate = item.getDate();
                    return !itemDate.isBefore(periodStartDate) && !itemDate.isAfter(periodEndDate);
                })
                .collect(Collectors.toList());

        // Book items into WorkingBooking (addOnly=false to replace any existing selections)
        if (!periodTeachingItems.isEmpty()) {
            workingBooking.bookScheduledItems(periodTeachingItems, false);
        }
    }

    private int calculateTotalPrice(BookablePeriod period, List<ScheduledItem> teachingItems, List<Rate> dailyRates) {
        int total = 0;

        ScheduledItem startItem = period.getStartScheduledItem();
        ScheduledItem endItem = period.getEndScheduledItem();

        if (startItem == null || endItem == null) return total;

        LocalDate periodStartDate = startItem.getDate();
        LocalDate periodEndDate = endItem.getDate();

        if (periodStartDate == null || periodEndDate == null) return total;

        List<ScheduledItem> periodTeachingItems = teachingItems.stream()
                .filter(item -> item.getDate() != null)
                .filter(item -> {
                    LocalDate itemDate = item.getDate();
                    return !itemDate.isBefore(periodStartDate) && !itemDate.isAfter(periodEndDate);
                })
                .collect(Collectors.toList());

        for (ScheduledItem teachingItem : periodTeachingItems) {
            Rate rate = findRateForScheduledItem(teachingItem, dailyRates);
            if (rate != null && rate.getPrice() != null) {
                total += rate.getPrice();
            }
        }

        return total;
    }

    private Rate findRateForScheduledItem(ScheduledItem scheduledItem, List<Rate> rates) {
        LocalDate itemDate = scheduledItem.getDate();

        if (itemDate != null) {
            for (Rate rate : rates) {
                LocalDate rateStart = rate.getStartDate();
                LocalDate rateEnd = rate.getEndDate();

                if (rateStart != null && rateEnd != null) {
                    if (!itemDate.isBefore(rateStart) && !itemDate.isAfter(rateEnd)) {
                        return rate;
                    }
                } else if (rateStart != null && rateStart.equals(itemDate)) {
                    return rate;
                }
            }
        }

        for (Rate rate : rates) {
            if (rate.getItem() != null && scheduledItem.getItem() != null &&
                rate.getItem().getPrimaryKey().equals(scheduledItem.getItem().getPrimaryKey())) {
                return rate;
            }
        }

        return rates.isEmpty() ? null : rates.get(0);
    }

    private String formatPrice(int priceInCents) {
        double priceValue = priceInCents / 100.0;
        return "£" + Math.round(priceValue);
    }

    // === HasRateTypeSection interface ===

    public BookablePeriod getSelectedPeriod() {
        return selectedPeriod.get();
    }

    /**
     * Returns the total price (in cents) for the selected period.
     * This is calculated by summing the daily rates for all teaching items
     * within the period's date range.
     */
    public int getTotalPrice() {
        return totalPrice;
    }

    @Override
    public void setOnPackageSelected(Consumer<BookablePeriod> handler) {
        this.onPackageSelected = handler;
    }

    /**
     * Returns the list of teaching ScheduledItems within the selected period's date range.
     * This is used for booking the teaching items in WorkingBooking.
     */
    public List<ScheduledItem> getTeachingScheduledItemsForPeriod() {
        if (workingBookingProperties == null) {
            return new ArrayList<>();
        }

        WorkingBooking workingBooking = workingBookingProperties.getWorkingBooking();
        PolicyAggregate policyAggregate = workingBooking.getPolicyAggregate();

        if (policyAggregate == null) {
            return new ArrayList<>();
        }

        BookablePeriod period = selectedPeriod.get();
        if (period == null) {
            return new ArrayList<>();
        }

        ScheduledItem startItem = period.getStartScheduledItem();
        ScheduledItem endItem = period.getEndScheduledItem();

        if (startItem == null || endItem == null) {
            return new ArrayList<>();
        }

        LocalDate periodStartDate = startItem.getDate();
        LocalDate periodEndDate = endItem.getDate();

        if (periodStartDate == null || periodEndDate == null) {
            return new ArrayList<>();
        }

        // Filter teaching items within the period date range
        List<ScheduledItem> teachingItems = policyAggregate.filterTeachingScheduledItems();
        return teachingItems.stream()
                .filter(item -> item.getDate() != null)
                .filter(item -> {
                    LocalDate itemDate = item.getDate();
                    return !itemDate.isBefore(periodStartDate) && !itemDate.isAfter(periodEndDate);
                })
                .collect(Collectors.toList());
    }
}
