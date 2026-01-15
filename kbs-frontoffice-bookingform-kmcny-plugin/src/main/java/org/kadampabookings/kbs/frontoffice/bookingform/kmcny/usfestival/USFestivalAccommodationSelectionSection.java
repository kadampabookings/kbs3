package org.kadampabookings.kbs.frontoffice.bookingform.kmcny.usfestival;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import one.modality.booking.frontoffice.bookingpage.sections.DefaultAccommodationSelectionSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom accommodation selection section for US Festival that displays a price breakdown
 * at the bottom of each accommodation card showing teachings, accommodation, and meals
 * with their date ranges and prices.
 *
 * @author Claude
 */
public class USFestivalAccommodationSelectionSection extends DefaultAccommodationSelectionSection {

    /**
     * Represents a single line item in the price breakdown.
     */
    public static class PriceBreakdownItem {
        private final String category;     // "Teachings", "Accommodation", "Meals"
        private final String dateRange;    // "Jul 1-8" or "8 nights"
        private final int price;           // Price in cents
        private final Integer remainingQuantity; // Remaining availability (null if not applicable)

        public PriceBreakdownItem(String category, String dateRange, int price) {
            this(category, dateRange, price, null);
        }

        public PriceBreakdownItem(String category, String dateRange, int price, Integer remainingQuantity) {
            this.category = category;
            this.dateRange = dateRange;
            this.price = price;
            this.remainingQuantity = remainingQuantity;
        }

        public String getCategory() { return category; }
        public String getDateRange() { return dateRange; }
        public int getPrice() { return price; }
        public Integer getRemainingQuantity() { return remainingQuantity; }
    }

    // Map from itemId to breakdown items
    private final Map<Object, List<PriceBreakdownItem>> breakdownMap = new HashMap<>();

    /**
     * Sets the price breakdown for a specific accommodation option.
     *
     * @param itemId the accommodation item ID (or special IDs like "DAY_VISITOR", "SHARE_ACCOMMODATION")
     * @param breakdown the list of breakdown items
     */
    public void setBreakdownForOption(Object itemId, List<PriceBreakdownItem> breakdown) {
        breakdownMap.put(itemId, breakdown);
    }

    /**
     * Clears all stored breakdowns (call before repopulating options).
     */
    public void clearBreakdowns() {
        breakdownMap.clear();
    }

    @Override
    protected VBox createOptionCard(AccommodationOption option, boolean isSelected) {
        // Create the base card using parent implementation
        VBox card = super.createOptionCard(option, isSelected);

        // Get breakdown for this option
        List<PriceBreakdownItem> breakdown = breakdownMap.get(option.getItemId());
        if (breakdown != null && !breakdown.isEmpty()) {
            // The card structure is:
            // VBox card
            //   └── StackPane wrapper (or just content if sold out)
            //         └── VBox contentBox (first child)
            //         └── checkmark/ribbon (second child if present)
            // We need to add the breakdown section to the contentBox

            Node firstChild = card.getChildren().isEmpty() ? null : card.getChildren().get(0);
            if (firstChild instanceof StackPane) {
                StackPane wrapper = (StackPane) firstChild;
                Node contentNode = wrapper.getChildren().isEmpty() ? null : wrapper.getChildren().get(0);
                if (contentNode instanceof VBox) {
                    VBox contentBox = (VBox) contentNode;
                    VBox breakdownSection = createBreakdownSection(breakdown, option.getAvailability() == AvailabilityStatus.SOLD_OUT);
                    contentBox.getChildren().add(breakdownSection);
                }
            }
        }

        return card;
    }

    /**
     * Creates the breakdown section UI showing category, date range, and price for each item.
     */
    private VBox createBreakdownSection(List<PriceBreakdownItem> items, boolean isSoldOut) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(12, 0, 0, 0)); // Top margin to separate from description

        // Optional: Add a separator line or label
        // Label breakdownLabel = new Label("Breakdown:");
        // breakdownLabel.getStyleClass().addAll("bookingpage-text-xs", "bookingpage-font-semibold", "bookingpage-text-muted");
        // box.getChildren().add(breakdownLabel);

        for (PriceBreakdownItem item : items) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);

            // Category with date range (left side)
            String labelText = item.getCategory();
            if (item.getDateRange() != null && !item.getDateRange().isEmpty()) {
                labelText += " (" + item.getDateRange() + ")";
            }
            // Add remaining quantity for accommodation items (debug info)
            if (item.getRemainingQuantity() != null) {
                labelText += " [" + item.getRemainingQuantity() + " left]";
            }
            Label nameLabel = new Label(labelText);
            nameLabel.getStyleClass().add("bookingpage-text-xs");
            if (isSoldOut) {
                nameLabel.getStyleClass().add("bookingpage-text-muted-disabled");
            } else {
                nameLabel.getStyleClass().add("bookingpage-text-muted");
            }

            // Spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Price (right side)
            Label priceLabel = new Label(formatPrice(item.getPrice()));
            priceLabel.getStyleClass().addAll("bookingpage-text-xs", "bookingpage-font-medium");
            if (isSoldOut) {
                priceLabel.getStyleClass().add("bookingpage-text-muted-disabled");
            } else {
                priceLabel.getStyleClass().add("bookingpage-text-muted");
            }

            row.getChildren().addAll(nameLabel, spacer, priceLabel);
            box.getChildren().add(row);
        }

        return box;
    }
}
