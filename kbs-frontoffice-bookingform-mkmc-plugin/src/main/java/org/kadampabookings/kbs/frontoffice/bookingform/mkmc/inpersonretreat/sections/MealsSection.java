package org.kadampabookings.kbs.frontoffice.bookingform.mkmc.inpersonretreat.sections;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import one.modality.booking.client.workingbooking.WorkingBookingProperties;
import one.modality.booking.frontoffice.bookingpage.BookingFormSection;

/**
 * Section for meal selection - using CSS classes for all styling
 *
 * @author Bruno Salmon
 */
public class MealsSection implements BookingFormSection {

    private final VBox container = new VBox(16);
    private final VBox mealDetailsBox = new VBox(16);

    public MealsSection() {
        container.setPadding(new Insets(0, 0, 40, 0));

        // Section title wrapped in container for proper border styling
        VBox titleContainer = new VBox();
        titleContainer.getStyleClass().add("section-title-container");
        titleContainer.setPadding(new Insets(14, 18, 14, 18));

        Label sectionTitle = new Label("Meals");
        sectionTitle.getStyleClass().add("section-title-text");
        titleContainer.getChildren().add(sectionTitle);

        // Meals radio options
        HBox radioGroup = new HBox(8);
        ToggleGroup mealsToggle = new ToggleGroup();

        ToggleButton requireMealsBtn = createInlineRadioButton("I require meals - £25/day (Event total: £75)", mealsToggle, true);
        ToggleButton noMealsBtn = createInlineRadioButton("No meals needed", mealsToggle, false);

        radioGroup.getChildren().addAll(requireMealsBtn, noMealsBtn);

        // Pricing info label
        Label pricingInfo = new Label("Includes breakfast, lunch, and dinner from Friday 28/11 (dinner) to Sunday 30/11 (lunch).");
        pricingInfo.getStyleClass().add("note-text");
        pricingInfo.setWrapText(true);
        pricingInfo.setPadding(new Insets(12, 0, 0, 0));

        // Meal details (diet and customization)
        setupMealDetails();

        // Toggle meal details visibility based on selection
        requireMealsBtn.setOnAction(e -> mealDetailsBox.setVisible(true));
        noMealsBtn.setOnAction(e -> mealDetailsBox.setVisible(false));

        container.getChildren().addAll(titleContainer, radioGroup, pricingInfo, mealDetailsBox);
    }

    private void setupMealDetails() {
        Label dietLabel = new Label("Please select your diet option:");
        dietLabel.getStyleClass().add("section-label-text");
        dietLabel.setPadding(new Insets(12, 0, 12, 0));

        VBox dietOptions = new VBox(12);
        Button vegetarianBtn = createDietButton("Vegetarian", true);
        Button vegetarianWheatFreeBtn = createDietButton("Vegetarian/Wheat-free", false);
        Button veganBtn = createDietButton("Vegan", false);
        Button veganWheatFreeBtn = createDietButton("Vegan/Wheat-free", false);

        dietOptions.getChildren().addAll(vegetarianBtn, vegetarianWheatFreeBtn, veganBtn, veganWheatFreeBtn);

        // Meal customization
        Label customizationLabel = new Label("Optional meal adjustments:");
        customizationLabel.getStyleClass().add("section-sublabel-text");
        customizationLabel.setPadding(new Insets(24, 0, 8, 0));

        VBox customizationOptions = new VBox(8);
        Button excludeFirstDinnerBtn = createMealAdjustmentButton("Exclude first dinner (Fri 28/11)", "-£8");
        Button includeLastDinnerBtn = createMealAdjustmentButton("Include last dinner (Sun 30/11)", "+£8");

        customizationOptions.getChildren().addAll(excludeFirstDinnerBtn, includeLastDinnerBtn);

        // Allergen note
        Label noteLabel = new Label(
                "Note: We are unable to offer gluten-free meals due to risk of cross contamination. " +
                "Allergen information is available for each meal, please ask for more information if you need to " +
                "determine whether a meal is suitable for you or not. Due to our kitchen facilities and the risk of " +
                "cross contamination, it is not possible for us to guarantee the absence of allergens in any our meals.");
        noteLabel.getStyleClass().add("note-text");
        noteLabel.setWrapText(true);
        noteLabel.setPadding(new Insets(16, 0, 0, 0));

        mealDetailsBox.getChildren().addAll(dietLabel, dietOptions, customizationLabel, customizationOptions, noteLabel);
    }

    private ToggleButton createInlineRadioButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.getStyleClass().add("inline-radio-button");
        if (selected) {
            btn.getStyleClass().add("selected");
        }
        btn.setPadding(new Insets(10, 18, 10, 18));

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.getStyleClass().add("selected");
            } else {
                btn.getStyleClass().remove("selected");
            }
        });

        return btn;
    }

    private Button createDietButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("diet-button");
        if (selected) {
            btn.getStyleClass().add("selected");
        }
        btn.setPadding(new Insets(14, 20, 14, 20));

        btn.setOnAction(e -> {
            // TODO: Handle diet selection
        });

        return btn;
    }

    private Button createMealAdjustmentButton(String text, String priceAdjustment) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(12, 16, 12, 16));
        btn.getStyleClass().add("meal-adjustment-button");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("meal-adjustment-label");

        Label priceLabel = new Label(priceAdjustment);
        priceLabel.getStyleClass().add(priceAdjustment.startsWith("-") ? "meal-adjustment-price-negative" : "meal-adjustment-price-positive");

        HBox content = new HBox(10);
        content.getChildren().addAll(textLabel, priceLabel);

        btn.setGraphic(content);

        btn.setOnAction(e -> {
            // TODO: Handle meal adjustment toggle
        });

        return btn;
    }

    @Override
    public Object getTitleI18nKey() {
        return "Meals";
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
